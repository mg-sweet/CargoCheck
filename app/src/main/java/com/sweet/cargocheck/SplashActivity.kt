package com.sweet.cargocheck

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val circularText = findViewById<CircularTextView>(R.id.circularText)

        circularText.textToShow = "•        R O Y A L   E X P R E S S        •       R O Y A L   E X P R E S S       •       R O Y A L   E X P R E S S       •      R O Y A L   R X"

        // စာသားကို စက်ဝိုင်းပုံစံ လှည့်ပေးမည့် Animation
        val rotate = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 6000 // ၃ စက္ကန့်အတွင်း တစ်ပတ်လည်မည်
            repeatCount = Animation.INFINITE // အမြဲတမ်းလည်နေမည်
            interpolator = android.view.animation.LinearInterpolator() // အမြန်နှုန်း ညီညီညာညာဖြစ်စေရန်
        }

        circularText.startAnimation(rotate)

        // 3 စက္ကန့်အကြာတွင် Login Activity သို့ သွားမည်
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 1000) // 2000
    }
}