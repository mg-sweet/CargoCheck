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

class IBResult : AppCompatActivity() {

    private lateinit var layoutDeliveryResult: LinearLayout
    private lateinit var layoutPendingResult: LinearLayout

    // Save လုပ်ဖို့အတွက် စာသားတွေကို သိမ်းထားရန်
    private val fullDeliveryTextForSave = StringBuilder()
    private val fullPendingTextForSave = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ib_result)

        // UI Fix for Notch
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.ibResultMain)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        layoutDeliveryResult = findViewById(R.id.layoutDeliveryResult)
        layoutPendingResult = findViewById(R.id.layoutPendingResult)
        val btnSaveIB = findViewById<MaterialButton>(R.id.btnSaveIB)
        val btnBack = findViewById<MaterialButton>(R.id.btnBack)

        // ၁။ ဒေတာများ လက်ခံခြင်း
        val deliveryList = SharedData.filteredDeliveryText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        val pendingList = SharedData.filteredPendingText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        val deliverySet = deliveryList.map { it.lowercase() }.toSet()
        val pendingSet = pendingList.map { it.lowercase() }.toSet()

        // ၂။ Delivery Side Calculation
        deliveryList.distinct().forEach { waybill ->
            if (!pendingSet.contains(waybill.lowercase())) {
                val displayMsg = "[$waybill] : [Pending Data Left]"
                addErrorEntry(layoutDeliveryResult, displayMsg, waybill)
                fullDeliveryTextForSave.append(displayMsg).append("\n\n")
            }
        }

        // ၃။ Pending Side Calculation
        pendingList.distinct().forEach { waybill ->
            if (!deliverySet.contains(waybill.lowercase())) {
                val displayMsg = "[$waybill] : [Not in System]"
                addErrorEntry(layoutPendingResult, displayMsg, waybill)
                fullPendingTextForSave.append(displayMsg).append("\n\n")
            }
        }

        // ၄။ Empty State Handling & Notes ပေါင်းထည့်ခြင်း
        handleFinalIBResultsAndNotes()

        // ၅။ Buttons Logic
        btnSaveIB.setOnClickListener {
            val date = "[IB] " + SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault()).format(Date())
            saveDataLocally(SaveModel(date, fullDeliveryTextForSave.toString(), fullPendingTextForSave.toString()))
            Toast.makeText(this, "Saved Successfully!", Toast.LENGTH_SHORT).show()
        }

        btnBack.setOnClickListener { finish() }
    }

    // Error တစ်ခုချင်းစီကို နှိပ်လို့ရတဲ့ View အဖြစ် ထည့်ပေးသည့် Function
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

    private fun handleFinalIBResultsAndNotes() {
        val deliveryNote = "\nNote: Inventory တွင် စာရင်းသွင်းရန်ကျန်ခဲ့ -> အရင်ဆုံး Digital တွင်စစ်ဆေးပေးပါ အခြားမြို့မှ Waybill များ မိမိမြို့တွင် ဝင်လာနိုင်ပါသည်။ မိမိမြို့အမှန် ဖြစ်ခဲ့ပါက ပါဆယ်ထုပ်အားရှာဖွေပြီး Inventory စာရင်းအမှန်လုပ်ဆောင်ပေးပါရန်။ "
        val pendingNote = "\nNote: မြို့မကိုက်ညီသောစာစောင်များ [ Type 1: Wrong City ] / စာရင်းမသွင်းထားသောစာစောင်များ [ Type 2: No Data ] / Waybill No မှားသွင်းထားခြင်း " +
                "အထက်ပါ error များအတွက် Waybill Create / City Change Ticket တင်ရန်လိုအပ်ပါသည်။\n" +
                "[ Type 3: Wrong Waybill ] Waybill No မှားသွင်းခဲ့ပါက သက်ဆိုင်ရာ OA/ Reg Supervisor များနှင့်ချိတ်ဆက်ရပါမည်။ " +
                "အကယ်၍ Assigned သုံးရန်ကျန်ခဲ့ပါက Assigned ချပြီး Reattempt ပြန်ခေါ်ပေးရပါမည်။ " +
                "Ticket တင်ပြီးပါက နောက်တစ်ရက်တွင် ပြန်လည်စစ်ဆေးပြီး စာရင်းအမှန်လုပ်ဆောင်ပေးပါရန်။"

        // Delivery Section Check
        if (layoutDeliveryResult.childCount == 0) {
            val noError = "No Errors - All items found in Pending"
            addSimpleText(layoutDeliveryResult, noError)
            fullDeliveryTextForSave.append(noError)
        } else {
            addSimpleText(layoutDeliveryResult, deliveryNote)
            fullDeliveryTextForSave.append("\n").append(deliveryNote)
        }

        // Pending Section Check
        if (layoutPendingResult.childCount == 0) {
            val noError = "No Errors - All items found in Delivery"
            addSimpleText(layoutPendingResult, noError)
            fullPendingTextForSave.append(noError)
        } else {
            addSimpleText(layoutPendingResult, pendingNote)
            fullPendingTextForSave.append("\n").append(pendingNote)
        }
    }

    private fun addSimpleText(container: LinearLayout, text: String) {
        val tv = TextView(this)
        tv.text = formatText(text)
        tv.setPadding(24, 12, 24, 12)
        tv.textSize = 14f
        container.addView(tv)
    }

    private fun formatText(text: String): SpannableStringBuilder {
        val spannable = SpannableStringBuilder(text)
        // Waybill [ ] များကို Bold Blue လုပ်ခြင်း
        "\\[(.*?)\\]".toRegex().findAll(text).forEach {
            spannable.setSpan(StyleSpan(Typeface.BOLD), it.range.first, it.range.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(ForegroundColorSpan(Color.parseColor("#1565C0")), it.range.first, it.range.last + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        // Note: စာသားကို Bold Red လုပ်ခြင်း
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