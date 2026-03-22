package com.sweet.cargocheck

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory

class OBCheckingActivity : AppCompatActivity() {

    private lateinit var tvCargoData: TextView
    private lateinit var tvObData: TextView
    private lateinit var tvCargoCount: TextView
    private lateinit var tvObCount: TextView

    // Cargo Side (With Filter)
    private lateinit var autoCargoWaybill: AutoCompleteTextView
    private lateinit var autoCargoFilter: AutoCompleteTextView
    private lateinit var chipGroupCargoFilter: ChipGroup

    // OB Side (Waybill only - No Filter)
    private lateinit var autoObWaybill: AutoCompleteTextView

    private var selCargoWIdx = -1
    private var selCargoFIdx = -1
    private var selObWIdx = -1

    private lateinit var loadingDialog: RoyalLoadingDialog

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ob_checking)

        initViews()
        loadingDialog = RoyalLoadingDialog(this)

        // Back Button
        findViewById<ImageButton>(R.id.btnBackOB).setOnClickListener { finish() }

        val btnImportCargo = findViewById<Button>(R.id.btnImportCargo)
        val btnImportOb = findViewById<Button>(R.id.btnImportOb)
        val btnCheck = findViewById<Button>(R.id.btnCheck)

        // 1. Cargo Import Launcher
        val pickCargoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { lifecycleScope.launch {
                loadingDialog.show()
                val data = processFileInBackground(it)
                val fileName = getFileName(it)
                SharedData.cargoCsvData = data
                delay(800)
                if (data.isNotEmpty()) {
                    setupCargoSpinners()
                    // --- 🛠 ဒီနေရာမှာ UI ကို Update လုပ်ပါမယ် ---
                    btnImportCargo.text = "✅ Cargo File Loaded"
                    btnImportCargo.alpha = 0.8f // သွင်းပြီးသားမှန်းသိသာအောင် နည်းနည်းမှိန်ပြမယ်

                    val tvCargoFileStatus = findViewById<TextView>(R.id.tvCargoFileStatus)
                    tvCargoFileStatus.text = "Loaded: $fileName (${data.size - 1} items)"
                    tvCargoFileStatus.setTextColor(Color.parseColor("#2E7D32")) // အစိမ်းရောင်စာသားပြောင်းမယ်
                }
                loadingDialog.dismiss()
            } }
        }

        // 2. OB Import Launcher
        val pickObLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { lifecycleScope.launch {
                val loading = RoyalLoadingDialog(this@OBCheckingActivity)
                loadingDialog.show()

                val data = withContext(Dispatchers.IO) { processFileInBackground(it) }
                val fileName = getFileName(it) // ဖိုင်နာမည်ယူမယ်

                //SharedData.obCsvData = data
//                delay(800)
//                if (data.isNotEmpty()) setupObSpinners()
                // UI Thread ကို ခေတ္တအနားပေးပြီးမှ spinner ထည့်မယ်
                delay(200)
                SharedData.obCsvData = data
                if (SharedData.obCsvData.isNotEmpty()) {
                    setupObSpinners()
                    // --- 🛠 ဒီနေရာမှာ UI ကို Update လုပ်ပါမယ် ---
                    btnImportOb.text = "✅ OB File Loaded"
                    btnImportOb.alpha = 0.8f // သွင်းပြီးသားမှန်းသိသာအောင် နည်းနည်းမှိန်ပြမယ်

                    val tvObFileStatus = findViewById<TextView>(R.id.tvObFileStatus)
                    tvObFileStatus.text = "Loaded: $fileName (${data.size - 1} items)"
                    tvObFileStatus.setTextColor(Color.parseColor("#2E7D32")) // အစိမ်းရောင်စာသားပြောင်းမယ်

                }
                loadingDialog.dismiss()
            } }
        }

        btnImportCargo.setOnClickListener { pickCargoLauncher.launch("*/*") }
        btnImportOb.setOnClickListener { pickObLauncher.launch("*/*") }

        btnCheck.setOnClickListener {
            val cargoT = tvCargoData.text.toString().trim()
            val obT = tvObData.text.toString().trim()
            if (cargoT.isEmpty() || obT.isEmpty()) {
                Toast.makeText(this, "Please import and select columns first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            SharedData.filteredCargoText = cargoT
            SharedData.filteredObText = obT
            SharedData.selectedObWaybillIndex = selObWIdx
            startActivity(Intent(this, OBResult::class.java))
        }

        // --- Scroll Handling ---
        val scrollTouchListener = View.OnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            if (event.action == MotionEvent.ACTION_UP) v.parent.requestDisallowInterceptTouchEvent(false)
            false
        }
        tvCargoData.setOnTouchListener(scrollTouchListener)
        tvObData.setOnTouchListener(scrollTouchListener)
        tvCargoData.movementMethod = android.text.method.ScrollingMovementMethod()
        tvObData.movementMethod = android.text.method.ScrollingMovementMethod()
    }

    private fun initViews() {
        tvCargoData = findViewById(R.id.tvCargoData); tvObData = findViewById(R.id.tvObData)
        tvCargoCount = findViewById(R.id.tvCargoCount); tvObCount = findViewById(R.id.tvObCount)

        // Cargo Views
        autoCargoWaybill = findViewById(R.id.autoCargoWaybill)
        autoCargoFilter = findViewById(R.id.autoCargoFilter)
        chipGroupCargoFilter = findViewById(R.id.chipGroupCargoFilter)

        // OB View (Only Waybill)
        autoObWaybill = findViewById(R.id.autoObWaybill)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        tvCargoData.addTextChangedListener(createTextWatcher { tvCargoCount.text = "Cargo ($it)" })
        tvObData.addTextChangedListener(createTextWatcher { tvObCount.text = "Outbound ($it)" })
    }

    // --- Cargo Side Logic (Waybill + Filter) ---
    private fun setupCargoSpinners() {
        if (SharedData.cargoCsvData.isEmpty()) return
        val headers = SharedData.cargoCsvData[0]
        autoCargoWaybill.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, headers))
        autoCargoWaybill.setOnItemClickListener { _, _, pos, _ ->
            selCargoWIdx = pos
            updateCargoDataText()
        }

        val filterHeaders = mutableListOf("--- No Filter ---") + headers
        autoCargoFilter.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, filterHeaders))
        autoCargoFilter.setOnItemClickListener { _, _, pos, _ ->
            if (pos == 0) {
                selCargoFIdx = -1
                chipGroupCargoFilter.removeAllViews()
                updateCargoDataText()
            } else {
                selCargoFIdx = pos - 1
                generateCargoChips(selCargoFIdx)
            }
        }
    }

    private fun generateCargoChips(colIdx: Int) {
        chipGroupCargoFilter.removeAllViews()
        val uniqueValues = SharedData.cargoCsvData.drop(1)
            .mapNotNull { it.getOrNull(colIdx)?.trim() }
            .filter { it.isNotEmpty() }.distinct()

        for (value in uniqueValues) {
            val chip = Chip(this).apply {
                text = value; isCheckable = true; isChecked = true
                chipBackgroundColor = ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
                    intArrayOf(Color.parseColor("#4CAF50"), Color.parseColor("#E0E0E0"))
                )
                setTextColor(ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)), intArrayOf(Color.WHITE, Color.BLACK)))
                setOnCheckedChangeListener { _, _ -> updateCargoDataText() }
            }
            chipGroupCargoFilter.addView(chip)
        }
        updateCargoDataText()
    }

    private fun updateCargoDataText() {
        if (selCargoWIdx == -1) return
        val checkedChips = (0 until chipGroupCargoFilter.childCount)
            .map { chipGroupCargoFilter.getChildAt(it) as Chip }
            .filter { it.isChecked }.map { it.text.toString() }

        tvCargoData.text = SharedData.cargoCsvData.drop(1).mapNotNull { row ->
            val w = row.getOrNull(selCargoWIdx)?.trim() ?: ""
            val f = row.getOrNull(selCargoFIdx)?.trim() ?: ""
            if (w.isNotEmpty() && (selCargoFIdx == -1 || checkedChips.contains(f))) w else null
        }.joinToString("\n")
    }

    // --- OB Side Logic (Waybill Only) ---
    private fun setupObSpinners() {
        if (SharedData.obCsvData.isEmpty()) return
        val headers = SharedData.obCsvData[0]
        autoObWaybill.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, headers))
        autoObWaybill.setOnItemClickListener { _, _, pos, _ ->
            selObWIdx = pos
            updateObDataText()
        }
    }

    private fun updateObDataText() {
        if (selObWIdx == -1) return
        // OB မှာ filter မရှိတဲ့အတွက် ရွေးထားတဲ့ column ထဲက data တွေကို တန်းထုတ်မယ်
        tvObData.text = SharedData.obCsvData.drop(1)
            .mapNotNull { it.getOrNull(selObWIdx)?.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    // --- Background Processing (CSV/Excel) ---
    private suspend fun processFileInBackground(uri: Uri): List<List<String>> = withContext(Dispatchers.IO) {
        val data = mutableListOf<List<String>>()
        val formatter = DataFormatter()
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val name = getFileName(uri)
                if (name.endsWith(".csv", true)) {
                    stream.bufferedReader().useLines { lines ->
                        lines.forEach { data.add(it.split(",").map { it.trim().removeSurrounding("\"") }) }
                    }
                } else {
                    val workbook = WorkbookFactory.create(stream)
                    val sheet = workbook.getSheetAt(0)
                    for (row in sheet) {
                        val r = row.map { formatter.formatCellValue(it).trim().removeSurrounding("\"") }
                        if (r.any { it.isNotEmpty() }) data.add(r)
                    }
                    workbook.close()
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        data
    }

    private fun getFileName(uri: Uri): String {
        var n = ""
        contentResolver.query(uri, null, null, null, null)?.use {
            val i = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && i != -1) n = it.getString(i)
        }
        return n.ifEmpty { uri.path?.substringAfterLast('/') ?: "" }
    }

    private fun createTextWatcher(onCountChanged: (Int) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            onCountChanged(s?.toString()?.split("\n")?.count { it.isNotBlank() } ?: 0)
        }
    }
}