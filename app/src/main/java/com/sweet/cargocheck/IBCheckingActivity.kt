package com.sweet.cargocheck

import android.R.attr.data
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

class IBCheckingActivity : AppCompatActivity() {

    private lateinit var tvDeliveryData: TextView
    private lateinit var tvPendingData: TextView
    private lateinit var tvDeliveryCount: TextView
    private lateinit var tvPendingCount: TextView

    private lateinit var autoDeliveryWaybill: AutoCompleteTextView
    private lateinit var autoDeliveryFilter: AutoCompleteTextView
    private lateinit var chipGroupDeliveryFilter: ChipGroup

    private lateinit var autoPendingWaybill: AutoCompleteTextView
    private lateinit var autoPendingFilter: AutoCompleteTextView
    private lateinit var chipGroupPendingFilter: ChipGroup

    private var selDeliveryWIdx = -1
    private var selDeliveryFIdx = -1
    private var selPendingWIdx = -1
    private var selPendingFIdx = -1

    private lateinit var loadingDialog: RoyalLoadingDialog

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ib_checking)

        initViews()
        loadingDialog = RoyalLoadingDialog(this)

        findViewById<ImageButton>(R.id.btnBackIB).setOnClickListener {
            finish()
        }

        val btnImportDelivery = findViewById<Button>(R.id.btnImportDelivery)
        val btnImportPending = findViewById<Button>(R.id.btnImportPending)
        val btnCheckIB = findViewById<Button>(R.id.btnCheckIB)

        val pickDeliveryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { lifecycleScope.launch {
                loadingDialog.show()
                SharedData.deliveryCsvData = processFileInBackground(it)
                delay(800)
                val fileName = getFileName(it)
                val data = processFileInBackground(it)
                if (SharedData.deliveryCsvData.isNotEmpty()) {
                    setupDeliverySpinners()

                    // --- 🛠 ဒီနေရာမှာ UI ကို Update လုပ်ပါမယ် ---
                    btnImportDelivery.text = "✅ Delivery File Loaded"
                    btnImportDelivery.alpha = 0.8f // သွင်းပြီးသားမှန်းသိသာအောင် နည်းနည်းမှိန်ပြမယ်

                    val tvDeliveryFileStatus = findViewById<TextView>(R.id.tvDeliveryFileStatus)
                    tvDeliveryFileStatus.text = "Loaded: $fileName (${data.size - 1} items)"
                    tvDeliveryFileStatus.setTextColor(Color.parseColor("#2E7D32")) // အစိမ်းရောင်စာသားပြောင်းမယ်
                }
                loadingDialog.dismiss()
            } }
        }

        val pickPendingLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { lifecycleScope.launch {
                loadingDialog.show()
                SharedData.pendingCsvData = processFileInBackground(it)
                delay(800)
                val fileName = getFileName(it)
                val data = processFileInBackground(it)
                if (SharedData.pendingCsvData.isNotEmpty()) {
                    setupPendingSpinners()

                    // --- 🛠 ဒီနေရာမှာ UI ကို Update လုပ်ပါမယ် ---
                    btnImportPending.text = "✅ Pending File Loaded"
                    btnImportPending.alpha = 0.8f // သွင်းပြီးသားမှန်းသိသာအောင် နည်းနည်းမှိန်ပြမယ်

                    val tvPendingFileStatus = findViewById<TextView>(R.id.tvPendingFileStatus)
                    tvPendingFileStatus.text = "Loaded: $fileName (${data.size - 1} items)"
                    tvPendingFileStatus.setTextColor(Color.parseColor("#2E7D32")) // အစိမ်းရောင်စာသားပြောင်းမယ်
                }
                loadingDialog.dismiss()
            } }
        }

        btnImportDelivery.setOnClickListener { pickDeliveryLauncher.launch("*/*") }
        btnImportPending.setOnClickListener { pickPendingLauncher.launch("*/*") }

        btnCheckIB.setOnClickListener {
            val dText = tvDeliveryData.text.toString().trim()
            val pText = tvPendingData.text.toString().trim()
            if (dText.isEmpty() || pText.isEmpty()) {
                Toast.makeText(this, "Import and filter both files", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            SharedData.filteredDeliveryText = dText
            SharedData.filteredPendingText = pText
            startActivity(Intent(this, IBResult::class.java))
        }

        // Scroll Fix
        val scrollTouchListener = View.OnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            if (event.action == MotionEvent.ACTION_UP) v.parent.requestDisallowInterceptTouchEvent(false)
            false
        }
        tvDeliveryData.setOnTouchListener(scrollTouchListener)
        tvPendingData.setOnTouchListener(scrollTouchListener)
        tvDeliveryData.movementMethod = android.text.method.ScrollingMovementMethod()
        tvPendingData.movementMethod = android.text.method.ScrollingMovementMethod()
    }

    private fun initViews() {
        tvDeliveryData = findViewById(R.id.tvDeliveryData); tvPendingData = findViewById(R.id.tvPendingData)
        tvDeliveryCount = findViewById(R.id.tvDeliveryCount); tvPendingCount = findViewById(R.id.tvPendingCount)
        autoDeliveryWaybill = findViewById(R.id.autoDeliveryWaybill); autoDeliveryFilter = findViewById(R.id.autoDeliveryFilter)
        chipGroupDeliveryFilter = findViewById(R.id.chipGroupDeliveryFilter)
        autoPendingWaybill = findViewById(R.id.autoPendingWaybill); autoPendingFilter = findViewById(R.id.autoPendingFilter)
        chipGroupPendingFilter = findViewById(R.id.chipGroupPendingFilter)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        tvDeliveryData.addTextChangedListener(createTextWatcher { tvDeliveryCount.text = "Delivery ($it)" })
        tvPendingData.addTextChangedListener(createTextWatcher { tvPendingCount.text = "Pending ($it)" })
    }

    private fun setupDeliverySpinners() {
        val headers = SharedData.deliveryCsvData[0]
        autoDeliveryWaybill.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, headers))
        autoDeliveryWaybill.setOnItemClickListener { _, _, pos, _ -> selDeliveryWIdx = pos; updateDeliveryDataText() }

        val filterHeaders = mutableListOf("--- No Filter ---") + headers
        autoDeliveryFilter.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, filterHeaders))
        autoDeliveryFilter.setOnItemClickListener { _, _, pos, _ ->
            if (pos == 0) { selDeliveryFIdx = -1; chipGroupDeliveryFilter.removeAllViews(); updateDeliveryDataText() }
            else { selDeliveryFIdx = pos - 1; generateChips(SharedData.deliveryCsvData, chipGroupDeliveryFilter, selDeliveryFIdx) { updateDeliveryDataText() } }
        }
    }

    private fun setupPendingSpinners() {
        val headers = SharedData.pendingCsvData[0]
        autoPendingWaybill.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, headers))
        autoPendingWaybill.setOnItemClickListener { _, _, pos, _ -> selPendingWIdx = pos; updatePendingDataText() }

        val filterHeaders = mutableListOf("--- No Filter ---") + headers
        autoPendingFilter.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, filterHeaders))
        autoPendingFilter.setOnItemClickListener { _, _, pos, _ ->
            if (pos == 0) { selPendingFIdx = -1; chipGroupPendingFilter.removeAllViews(); updatePendingDataText() }
            else { selPendingFIdx = pos - 1; generateChips(SharedData.pendingCsvData, chipGroupPendingFilter, selPendingFIdx) { updatePendingDataText() } }
        }
    }

    private fun updateDeliveryDataText() { if (selDeliveryWIdx != -1) tvDeliveryData.text = getFilteredList(SharedData.deliveryCsvData, selDeliveryWIdx, selDeliveryFIdx, chipGroupDeliveryFilter) }
    private fun updatePendingDataText() { if (selPendingWIdx != -1) tvPendingData.text = getFilteredList(SharedData.pendingCsvData, selPendingWIdx, selPendingFIdx, chipGroupPendingFilter) }

    private fun generateChips(data: List<List<String>>, group: ChipGroup, colIdx: Int, onToggle: () -> Unit) {
        group.removeAllViews()
        val uniqueValues = data.drop(1).mapNotNull { it.getOrNull(colIdx)?.trim() }.filter { it.isNotEmpty() }.distinct()
        for (value in uniqueValues) {
            val chip = Chip(this).apply {
                text = value; isCheckable = true; isChecked = true
                chipBackgroundColor = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)), intArrayOf(Color.parseColor("#4CAF50"), Color.parseColor("#E0E0E0")))
                setTextColor(ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)), intArrayOf(Color.WHITE, Color.BLACK)))
                setOnCheckedChangeListener { _, _ -> onToggle() }
            }
            group.addView(chip)
        }
        onToggle()
    }

    private fun getFilteredList(data: List<List<String>>, wIdx: Int, fIdx: Int, group: ChipGroup): String {
        val checkedChips = (0 until group.childCount).map { group.getChildAt(it) as Chip }.filter { it.isChecked }.map { it.text.toString() }
        return data.drop(1).mapNotNull { row ->
            val w = row.getOrNull(wIdx)?.trim() ?: ""
            val f = row.getOrNull(fIdx)?.trim() ?: ""
            if (w.isNotEmpty() && (fIdx == -1 || checkedChips.contains(f))) w else null
        }.joinToString("\n")
    }

    private suspend fun processFileInBackground(uri: Uri): List<List<String>> = withContext(Dispatchers.IO) {
        val data = mutableListOf<List<String>>(); val formatter = DataFormatter()
        try { contentResolver.openInputStream(uri)?.use { stream ->
            val name = getFileName(uri)
            if (name.endsWith(".csv", true)) stream.bufferedReader().useLines { lines -> lines.forEach { data.add(it.split(",").map { it.trim().removeSurrounding("\"") }) } }
            else { val workbook = WorkbookFactory.create(stream); val sheet = workbook.getSheetAt(0)
                for (row in sheet) { val r = row.map { formatter.formatCellValue(it).trim().removeSurrounding("\"") }; if (r.any { it.isNotEmpty() }) data.add(r) }
                workbook.close() }
        } } catch (e: Exception) { e.printStackTrace() }
        data
    }

    private fun getFileName(uri: Uri): String {
        var n = ""; contentResolver.query(uri, null, null, null, null)?.use { val i = it.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (it.moveToFirst() && i != -1) n = it.getString(i) }
        return n.ifEmpty { uri.path?.substringAfterLast('/') ?: "" }
    }

    private fun createTextWatcher(onCountChanged: (Int) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) { onCountChanged(s?.toString()?.split("\n")?.count { it.isNotBlank() } ?: 0) }
    }
}