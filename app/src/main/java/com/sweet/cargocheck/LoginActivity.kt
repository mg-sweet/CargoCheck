package com.sweet.cargocheck

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var loadingDialog: RoyalLoadingDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val loginPrefs = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)

        loadingDialog = RoyalLoadingDialog(this)

        // Remember Me Logic
        if (auth.currentUser != null && loginPrefs.getBoolean("remember", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        setContentView(R.layout.activity_login)

        val etUserID = findViewById<EditText>(R.id.etUsername)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val cbRememberMe = findViewById<CheckBox>(R.id.cbRememberMe)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)

        btnLogin.setOnClickListener {

            val userID = etUserID.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (userID.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter ID and Password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ၁။ Internet ရှိ/မရှိ အရင်စစ်ဆေးမည်
            if (!isNetworkAvailable()) {
                showNoInternetDialog()
                return@setOnClickListener
            }

            loadingDialog.show() // Animation start

            val email = "$userID@cargocheck.com"

            // Firebase Login
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                // close animation
                loadingDialog.dismiss() // ပြီးရင် ပိတ်မည်
                if (task.isSuccessful) {
                    loginPrefs.edit().putBoolean("remember", cbRememberMe.isChecked).apply()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Login Failed: Check ID/Password", Toast.LENGTH_SHORT).show()
                }
            }
        }

        tvForgotPassword.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Operations Support")
                .setMessage("Please contact Ops department to reset your password.")
                .setPositiveButton("OK", null).show()
        }
    }

    // Internet ရှိ/မရှိ စစ်ဆေးသည့် Function
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            else -> false
        }
    }

    // Internet မရှိကြောင်း ပြသသည့် Dialog
    private fun showNoInternetDialog() {
        AlertDialog.Builder(this)
            .setTitle("No Internet Connection")
            .setMessage("Please check your internet connection and try again.")
            .setPositiveButton("Settings") { _, _ ->
                startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}