package com.sweet.cargocheck

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.ArrayList

class SaveActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var saveAdapter: SaveAdapter
    private var saveList: ArrayList<SaveModel> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_save)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish() // လက်ရှိ Activity ကို ပိတ်ပြီး အရင်စာမျက်နှာကို ပြန်သွားမည်
        }
        recyclerView = findViewById(R.id.recyclerViewSave)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Clear All Button Logic (activity_save.xml ထဲမှာ ခလုတ် ID: btnClearAll ထည့်ထားရပါမယ်)
        val btnClearAll = findViewById<Button>(R.id.btnClearAll)
        btnClearAll?.setOnClickListener {
            if (saveList.isEmpty()) return@setOnClickListener
            AlertDialog.Builder(this)
                .setTitle("Clear All History")
                .setMessage("Do you want to clear All History?")
                .setPositiveButton("Yes") { _, _ -> clearAll() }
                .setNegativeButton("No", null)
                .show()
        }

        loadSavedData()
    }

    private fun loadSavedData() {
        val sharedPreferences = getSharedPreferences("MySavedData", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("save_list", null)

        if (json != null) {
            val type = object : TypeToken<ArrayList<SaveModel>>() {}.type
            saveList = gson.fromJson(json, type)
        }

        saveAdapter = SaveAdapter(saveList) { position ->
            showDeleteConfirmDialog(position)
        }
        recyclerView.adapter = saveAdapter
    }

    private fun showDeleteConfirmDialog(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("To Delete!")
            .setMessage("Do you want to delete this record?")
            .setPositiveButton("Yes") { _, _ -> deleteItem(position) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteItem(position: Int) {
        saveList.removeAt(position)
        saveAdapter.notifyItemRemoved(position)
        saveAdapter.notifyItemRangeChanged(position, saveList.size)
        saveToDisk()
        Toast.makeText(this, "Record was deleted!", Toast.LENGTH_SHORT).show()
    }

    private fun clearAll() {
        saveList.clear()
        saveAdapter.notifyDataSetChanged()
        saveToDisk()
        Toast.makeText(this, "All record were deleted!", Toast.LENGTH_SHORT).show()
    }

    private fun saveToDisk() {
        val sharedPreferences = getSharedPreferences("MySavedData", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("save_list", Gson().toJson(saveList))
        editor.apply()
    }
}