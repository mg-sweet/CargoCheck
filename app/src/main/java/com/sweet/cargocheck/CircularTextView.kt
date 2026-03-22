package com.sweet.cargocheck

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class CircularTextView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // စာသားကို အပြင်ကနေ လှမ်းပြင်လို့ရအောင် ပြုလုပ်ခြင်း
    var textToShow: String = " ROYAL EXPRESS • "
        set(value) {
            field = value
            invalidate() // စာသားပြောင်းသွားရင် View ကို ပြန်ဆွဲရန်
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF0000")
        textSize = 45f // Dialog အတွက် စာလုံးအရွယ်အစားကို နည်းနည်းလျှော့ထားပါသည်
        style = Paint.Style.FILL
        strokeWidth = 2f
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        // စက်ဝိုင်းရဲ့ အချင်းဝက် (Radius) ကို View အရွယ်အစားအလိုက် တွက်ချက်ခြင်း
        val radius = Math.min(width, height) / 2.8f

        path.reset()
        path.addCircle(centerX, centerY, radius, Path.Direction.CW)

        // သတ်မှတ်ထားတဲ့ textToShow ကို Path အတိုင်း ပတ်ရေးမည်
        canvas.drawTextOnPath(textToShow, path, 0f, 0f, paint)
    }
}