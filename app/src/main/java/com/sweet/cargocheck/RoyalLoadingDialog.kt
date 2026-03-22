package com.sweet.cargocheck

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Window
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation

class RoyalLoadingDialog(context: Context) {
    private val dialog: Dialog = Dialog(context)
    private val circularText: CircularTextView

    init {
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.layout_loading_dialog)

        // Background ကို Transparent ဖြစ်စေရန်
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // အလုပ်လုပ်နေတုန်း အပြင်ကနှိပ်ရင် ပိတ်မရအောင် တားထားရန်
        dialog.setCancelable(false)

        circularText = dialog.findViewById(R.id.circularTextLoading)

        // Dialog အတွက် စာသားအတိုလေး သတ်မှတ်ခြင်း (စာမထပ်စေရန်)
        circularText.textToShow = "  •  R O Y A L  •  E X P R E S S"
    }

    // Animation ကို အသစ်ပြန်စတင်ပေးမည့် Function
    private fun startFreshAnimation() {
        val rotate = RotateAnimation(
            0f, 360f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 4000
            repeatCount = Animation.INFINITE
            interpolator = LinearInterpolator()
        }

        circularText.clearAnimation() // အဟောင်းရှိရင် အရင်ရှင်းထုတ်
        circularText.startAnimation(rotate) // Animation အသစ်ပြန်ထည့်
    }

    fun show() {
        if (!dialog.isShowing) {
            // ပေါ်ခါနီးတိုင်း Animation ကို အသစ်တစ်ခါ ပြန်စခိုင်းမည်
            startFreshAnimation()
            dialog.show()
        }
    }

    fun dismiss() {
        if (dialog.isShowing) {
            circularText.clearAnimation() // Memory မစားအောင် Animation ကိုပါ ရပ်ပစ်မည်
            dialog.dismiss()
        }
    }
}