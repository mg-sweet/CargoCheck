package com.sweet.cargocheck

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class OBResult : AppCompatActivity() {

    private lateinit var layoutCargoResult: LinearLayout
    private lateinit var layoutObResult: LinearLayout

    // Save လုပ်ဖို့အတွက် စာသားတွေကို သိမ်းထားရန်
    private val fullCargoTextForSave = StringBuilder()
    private val fullObTextForSave = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ob_result)

        // Notch Fix
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.result)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        layoutCargoResult = findViewById(R.id.layoutCargoResult)
        layoutObResult = findViewById(R.id.layoutObResult)

        val btnSave = findViewById<MaterialButton>(R.id.btnSave)
        val btnBack = findViewById<MaterialButton>(R.id.btnBack)

        // ၁။ ဒေတာများ လက်ခံခြင်း
        val cargoList = SharedData.filteredCargoText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val obList = SharedData.filteredObText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        val cargoSet = cargoList.map { it.lowercase() }.toSet()
        val obSet = obList.map { it.lowercase() }.toSet()

        // --- OB Metadata Column များ ရှာဖွေခြင်း ---
        var cusTypeIdx = -1
        var wbTypeIdx = -1
        if (SharedData.obCsvData.isNotEmpty()) {
            val headers = SharedData.obCsvData[0].map { it.lowercase().trim() }
            cusTypeIdx = headers.indexOfFirst { it.contains("cus type") || it.contains("customer type") }
            wbTypeIdx = headers.indexOfFirst { it.contains("waybill type") || it.contains("wb type") }
        }
        val obRowMap = SharedData.obCsvData.associateBy { it.getOrNull(SharedData.selectedObWaybillIndex)?.lowercase()?.trim() ?: "" }

        // ၂။ Cargo Side Calculation
        cargoList.distinct().forEach { waybill ->
            if (!obSet.contains(waybill.lowercase())) {
                val firstChar = waybill.firstOrNull()
                val lastChar = waybill.lastOrNull()
                val isLetterPattern = firstChar?.isLetter() == true && lastChar?.isLetter() == true

                val typeInfo = when {
                    waybill.startsWith("EC", true) || waybill.length > 15 ->
                        "[Type 3 : No Data] EC/International - OB Data Update မရိုက်နိုင်တာမို့ Cargo မှာသာရှိနေနိုင်ပါသည်။"
                    isLetterPattern ->
                        "[Type 2 : No Data] Digital Waybill Found (OB Date မှားနိုင် / To City Code ချိန်းထားတာမျိုးဖြစ်နိုင်)"
                    else -> "[Type 1 : No Data] Sticker Waybill Found in Cargo"
                }

                val displayMsg = "[$waybill] - $typeInfo"
                addErrorEntry(layoutCargoResult, displayMsg, waybill)
                fullCargoTextForSave.append(displayMsg).append("\n\n")
            }
        }

        // ၃။ OB Side Calculation
        obList.distinct().forEach { waybill ->
            if (!cargoSet.contains(waybill.lowercase())) {
                val firstChar = waybill.firstOrNull()
                val lastChar = waybill.lastOrNull()
                val isLetterPattern = firstChar?.isLetter() == true && lastChar?.isLetter() == true

                val rowData = obRowMap[waybill.lowercase()]
                val cusType = rowData?.getOrNull(cusTypeIdx)?.lowercase() ?: ""
                val wbType = rowData?.getOrNull(wbTypeIdx)?.lowercase() ?: ""

                val typeInfo = when {
                    waybill.startsWith("EC", true) ->
                        "[Type 4 : No Cargo] EC အစောင်များ Update စာရင်းသွင်းရန်ကျန်ရှိနေခြင်းဖြစ်နိုင်ပါသည်။ OB side မှာ EC နဲ့ စ ပါက Extra care ပါဆယ်များ ဖြစ်နိုင်ပါသည်။"
                    cusType.contains("E") && wbType.contains("digital") ->
                        "[Type 3 : No Cargo] Digital Waybill (Cus Type: Ecode / Waybill Type: Digital) - POD Image အားပြန်စစ်ပါ။ Digital နှင့် Sticker မှားရိုက်ထားခြင်း ဖြစ်နိုင်ပါသည်။"
                    isLetterPattern || (waybill.length >= 10) ->
                        "[Type 2 : No Cargo] Digital Waybill - လိုင်းမကောင်း၍ ၂ စောင်ထွက်ခြင်း/မှားရိုက်ခြင်း ဖြစ်နိုင်သဖြင့် Cus Info/POD Image စစ်ဆေးပါ။"
                    else -> "[Type 1 : No Cargo] Found in OB / Cargo မသုံးထားတာဖြစ်နိုင်။"
                }

                val displayMsg = "[$waybill] : $typeInfo"
                addErrorEntry(layoutObResult, displayMsg, waybill)
                fullObTextForSave.append(displayMsg).append("\n\n")
            }
        }

        // ၄။ Note Section & No Error Handling
        handleOBFinalResultsAndNotes()

        // ၅။ Buttons Logic
        btnSave.setOnClickListener {
            val date = "[OB] " + SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()).format(Date())
            saveDataLocally(SaveModel(date, fullCargoTextForSave.toString(), fullObTextForSave.toString()))
            Toast.makeText(this, "Saved Successfully!", Toast.LENGTH_SHORT).show()
        }

        btnBack.setOnClickListener { finish() }
    }

    // Error တစ်ခုချင်းစီကို View အဖြစ် ထည့်ပေးပြီး Click listener တပ်ပေးသည့် Function
    private fun addErrorEntry(container: LinearLayout, message: String, waybillToCopy: String) {
        val textView = TextView(this)

        // Layout parameters သတ်မှတ်ခြင်း
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 2, 0, 2) // အကွက်တွေကြားထဲက gap ကို လျှော့ထားပါတယ်
        textView.layoutParams = params

        //  Padding ကို သေသေသပ်သပ် ထည့်ခြင်း
        textView.setPadding(30, 24, 30, 24)

        // --- Background ပြင်ဆင်ခြင်း ---
        // editbox_background အစား Card နဲ့ တစ်သားတည်းဖြစ်အောင် Ripple Effect ပဲ သုံးပါမယ်
        val outValue = android.util.TypedValue()
        theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
        textView.setBackgroundResource(outValue.resourceId)

        textView.text = formatText(message)
        textView.textSize = 15f
        textView.isClickable = true
        textView.isFocusable = true

        // Click to Copy Logic
        textView.setOnClickListener {
            val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
            if (prefs.getBoolean("touch_to_copy", true)) {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Waybill", waybillToCopy)
                clipboard.setPrimaryClip(clip)

                Toast.makeText(this, "Copied: $waybillToCopy", Toast.LENGTH_SHORT).show()

                // Visual feedback (နှိပ်လိုက်ရင် ခဏလေး မှိန်သွားစေဖို့)
                textView.alpha = 0.5f
                textView.animate().alpha(1f).setDuration(300).start()
            }
        }

        // စာကြောင်းတစ်ခုချင်းစီကြားမှာ Divider (မျဉ်းပါးပါးလေး) ထည့်ချင်ရင် အောက်က code သုံးနိုင်ပါတယ်
        // (မထည့်ချင်ရင်တော့ အောက်က code ကို ဖြုတ်ထားပါ)
        val divider = View(this)
        divider.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
        divider.setBackgroundColor(Color.parseColor("#EEEEEE")) // မီးခိုးနုရောင် မျဉ်းလေး

        container.addView(textView)
        container.addView(divider)
    }

    private fun handleOBFinalResultsAndNotes() {
        val cargoNote = "\nNote: Cargo ဘက်တွင်ရှိသော်လည်း OB မရှိခြင်းသည် OB ရိုက်ရန်ကျန်ခြင်း သို့မဟုတ် Date/City Code လွဲချော်ခြင်းများ ဖြစ်နိုင်ပါသည်။ City Code လွဲချော်ခြင်းဆိုသည်မှာ System တွင် To City ပြင်ပြီး Waybill Print ပြန်မထုတ်မိပဲ Waybill အဟောင်းနှင့်သာ Cargo အသုံးပြုထားခြင်းကိုဆိုလိုပါသည်။" +
                "\nCity Code လွဲခြင်းသည် Digital Waybill တွင်သာဖြစ်နိုင်ပါသည်။"
        val obNote = "\nNote: OB တွင်ရှိပြီး Cargo မရှိခြင်းသည် Digital Waybill ပြဿနာများ သို့မဟုတ် ဒေတာအမှားများ ဖြစ်နိုင်သဖြင့် POD နှင့် System အား ပြန်လည်တိုက်စစ်ပါ။"

        // Cargo Side Check
        if (layoutCargoResult.childCount == 0) {
            val noError = "No Errors - All match"
            addSimpleText(layoutCargoResult, noError)
            fullCargoTextForSave.append(noError)
        } else {
            addSimpleText(layoutCargoResult, cargoNote)
            fullCargoTextForSave.append("\n").append(cargoNote)
        }

        // OB Side Check
        if (layoutObResult.childCount == 0) {
            val noError = "No Errors - All match"
            addSimpleText(layoutObResult, noError)
            fullObTextForSave.append(noError)
        } else {
            addSimpleText(layoutObResult, obNote)
            fullObTextForSave.append("\n").append(obNote)
        }
    }

    private fun addSimpleText(container: LinearLayout, text: String) {
        val tv = TextView(this)
        tv.text = formatText(text)
        tv.setPadding(20, 10, 20, 10)
        tv.textSize = 14f
        container.addView(tv)
    }

    private fun formatText(text: String): SpannableStringBuilder {
        val spannable = SpannableStringBuilder(text)
        "\\[(.*?)\\]".toRegex().findAll(text).forEach {
            spannable.setSpan(StyleSpan(Typeface.BOLD), it.range.first, it.range.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(ForegroundColorSpan(Color.parseColor("#1565C0")), it.range.first, it.range.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        "(Type \\d)".toRegex().findAll(text).forEach {
            spannable.setSpan(StyleSpan(Typeface.BOLD), it.range.first, it.range.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(ForegroundColorSpan(Color.RED), it.range.first, it.range.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        "Note:".toRegex().findAll(text).forEach {
            spannable.setSpan(StyleSpan(Typeface.BOLD_ITALIC), it.range.first, it.range.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(ForegroundColorSpan(Color.RED), it.range.first, it.range.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return spannable
    }

    private fun saveDataLocally(model: SaveModel) {
        val prefs = getSharedPreferences("MySavedData", Context.MODE_PRIVATE)
        val list: ArrayList<SaveModel> = Gson().fromJson(prefs.getString("save_list", "[]"), object : TypeToken<ArrayList<SaveModel>>() {}.type)
        list.add(0, model)
        prefs.edit().putString("save_list", Gson().toJson(list)).apply()
    }
}