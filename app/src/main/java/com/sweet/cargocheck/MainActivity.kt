package com.sweet.cargocheck

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var btnIbChecking: Button
    private lateinit var btnObChecking: Button
    private lateinit var btnQrGenerator: Button

    private lateinit var btnMenuToggle: ImageButton
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    // for firebase Login
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Back Button နှိပ်လိုက်တဲ့အခါ အလုပ်လုပ်မယ့် Callback
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // App Exit Dialog ကို ပြပါမယ်
                showExitDialog()
            }
        })

        btnIbChecking = findViewById(R.id.btnIbChecking)
        btnObChecking = findViewById(R.id.btnObChecking)
        btnQrGenerator = findViewById(R.id.btnQrGenerator)

        drawerLayout = findViewById(R.id.drawerLayout)
        btnMenuToggle = findViewById(R.id.btnMenuToggle)
        navigationView = findViewById(R.id.navigationView)

        // for user name calling REX-xxxx-xxxx
        val navigationView = findViewById<NavigationView>(R.id.navigationView)

        // ၁။ Header View ကို အရင်လှမ်းယူရပါမယ်
        val headerView = navigationView.getHeaderView(0)
        val tvUserAccountID = headerView.findViewById<TextView>(R.id.tvUserAccountID)

        // ၂။ Firebase ကနေ လက်ရှိ User ရဲ့ ID ကို ယူပါမယ်
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val email = currentUser.email // ဥပမာ - REX-2026-1111@cargocheck.com

            // Email ထဲကနေ @ ရဲ့ အရှေ့က ID ကိုပဲ ဖြတ်ယူမယ်
            val userID = email?.substringBefore("@")?.uppercase()

            // Header မှာ User ID ကို ပြပါမယ်
            tvUserAccountID.text = userID ?: "Unknown User"
        }

        // for login access
        auth = FirebaseAuth.getInstance()

        // OB Checking ကိုနှိပ်ရင် အရင်လုပ်ထားတဲ့ Activity ကိုသွားမယ်
        btnObChecking.setOnClickListener {
            val intent = Intent(this, OBCheckingActivity::class.java)
            startActivity(intent)
        }

        // IB Checking ကိုနှိပ်ရင် သွားမယ့် Activity (လောလောဆယ် Activity အလွတ်တစ်ခု ဆောက်ထားပေးပါ)
        btnIbChecking.setOnClickListener {
            val intent = Intent(this, IBCheckingActivity::class.java)
            startActivity(intent)
        }

        // QR Generator ကိုနှိပ်ရင် သွားမယ့် Activity (လောလောဆယ် Activity အလွတ်တစ်ခု ဆောက်ထားပေးပါ)
        btnQrGenerator.setOnClickListener {
            val intent = Intent(this, QRGeneratorActivity::class.java)
            startActivity(intent)
        }

        // 1. Gear Icon ကိုနှိပ်ရင် ညာဘက်ကနေ Drawer ကို ဖွင့်/ပိတ် လုပ်မယ်
        btnMenuToggle.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // Drawer ပွင့်/ပိတ် တာကိုစောင့်ကြည့်ပြီး Icon ပြောင်းမယ်
        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerOpened(drawerView: View) {
                // ပွင့်သွားရင် < Back (Arrow) ပုံလေးပြောင်းမယ်
                btnMenuToggle.setImageResource(android.R.drawable.ic_media_previous)
            }

            override fun onDrawerClosed(drawerView: View) {
                // ပြန်ပိတ်သွားရင် Gear ပုံလေး ပြန်ပြောင်းမယ်
                btnMenuToggle.setImageResource(R.drawable.ic_gear)
            }

            override fun onDrawerStateChanged(newState: Int) {}
        })

        // Navigation Drawer (Menu) က Item တွေ နှိပ်ရင် အလုပ်လုပ်ဖို့
        navigationView.setNavigationItemSelectedListener { menuItem ->

            // ၁။ Drawer ကို အရင်ပိတ်ပါ
            drawerLayout.closeDrawer(GravityCompat.START)

            // ၂။ Drawer Animation ပြီးအောင် (၂၅၀ မီလီစက္ကန့်ခန့်) ခေတ္တစောင့်ပြီးမှ Activity ကို ဖွင့်ပါ
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            when (menuItem.itemId) {
                R.id.nav_ib -> {
                    startActivity(Intent(this, IBCheckingActivity::class.java))
                }

                R.id.nav_ob -> {
                    startActivity(Intent(this, OBCheckingActivity::class.java))
                }

                R.id.nav_qr -> {
                    startActivity(Intent(this, QRGeneratorActivity::class.java))
                }

                R.id.nav_save -> {
                    startActivity(Intent(this, SaveActivity::class.java))
                }

                R.id.nav_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                }

                R.id.nav_logout -> showLogoutConfirmDialog()
                // ၄။ Password Change Item
                R.id.nav_change_pw -> showChangePasswordDialog()

            }
            }, 0)
//            // Menu Item ရွေးပြီးရင် Drawer ကို အလိုလို ပြန်ပိတ်မယ်
//            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }



        val actionView = navigationView.menu.findItem(R.id.nav_copy_toggle).actionView as Switch
        val prefs = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)

        // လက်ရှိ Setting အတိုင်း Switch ကို အဖွင့်အပိတ် လုပ်ထားမယ်
        actionView.isChecked = prefs.getBoolean("touch_to_copy", true)

        actionView.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("touch_to_copy", isChecked).apply()
            val status = if (isChecked) "Enabled" else "Disabled"
            Toast.makeText(this, "Touch to Copy $status", Toast.LENGTH_SHORT).show()
        }

    }

    // --- Exit Dialog Function ---
    private fun showExitDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Exit Application")
            .setMessage("Let's work systematically for better results!\n\nAre you sure you want to exit REX Check?")
            .setCancelable(false) // ဘေးကိုနှိပ်ရင် ပိတ်မသွားအောင် တားထားမည်
            .setPositiveButton("Exit") { _, _ ->
                // App အားလုံးကို အပြီးပိတ်လိုက်ခြင်း
                finishAffinity()
            }
            .setNegativeButton("Stay", null)
            .show()
    }
    private fun showChangePasswordDialog() {
        // Layout တစ်ခုဆောက်ပြီး EditText ၂ ခုထည့်ပါမယ် (Old & New Password)
        val layout = android.widget.LinearLayout(this)
        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(50, 40, 50, 10)

        val etOldPass = EditText(this)
        etOldPass.hint = "Old Password"
        etOldPass.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(etOldPass)

        val etNewPass = EditText(this)
        etNewPass.hint = "New Password"
        etNewPass.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        layout.addView(etNewPass)

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(layout)
            .setPositiveButton("Update") { _, _ ->
                val oldPass = etOldPass.text.toString().trim()
                val newPass = etNewPass.text.toString().trim()

                if (oldPass.isEmpty() || newPass.isEmpty()) {
                    Toast.makeText(this, "Fill all Password", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val user = auth.currentUser
                val email = user?.email // ဥပမာ - REX-123@cargocheck.com

                if (user != null && email != null) {
                    // ၁။ Old Password မှန်မမှန် အရင်စစ်ဆေးခြင်း (Re-authentication)
                    val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, oldPass)

                    user.reauthenticate(credential).addOnCompleteListener { reAuthTask ->
                        if (reAuthTask.isSuccessful) {
                            // ၂။ Old Password မှန်ကန်မှ Password အသစ်ကို Update လုပ်ခြင်း
                            user.updatePassword(newPass).addOnCompleteListener { updateTask ->
                                if (updateTask.isSuccessful) {
                                    Toast.makeText(this, "Password Update Successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, "Error: ${updateTask.exception?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            // Old Password မှားနေလျှင်
                            Toast.makeText(this, "Old Password Wrong", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    // ၇။ Logout Confirm လုပ်မည့် Dialog
    private fun showLogoutConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Log out")
            .setMessage("Do you want to Log out?")
            .setPositiveButton("Yes") { _, _ ->
                auth.signOut()
                // Remember Login setting ကိုလည်း ဖျက်မည်
                getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE).edit().clear().apply()

                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("No", null)
            .show()
    }
}
