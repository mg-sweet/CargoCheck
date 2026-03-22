package com.sweet.cargocheck

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SaveAdapter(
    private val saveList: ArrayList<SaveModel>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<SaveAdapter.SaveViewHolder>() {

    class SaveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvLabelCargo: TextView = itemView.findViewById(R.id.tvLabelCargo)
        val tvLabelOb: TextView = itemView.findViewById(R.id.tvLabelOb)
        val tvSavedCargo: TextView = itemView.findViewById(R.id.tvSavedCargo)
        val tvSavedOb: TextView = itemView.findViewById(R.id.tvSavedOb)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SaveViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_save, parent, false)
        return SaveViewHolder(view)
    }

    override fun onBindViewHolder(holder: SaveViewHolder, position: Int) {
        val currentItem = saveList[position]
        holder.tvDate.text = currentItem.date
        holder.tvSavedCargo.text = currentItem.cargoResult
        holder.tvSavedOb.text = currentItem.obResult

        if (currentItem.date.contains("[IB]")) {
            holder.tvLabelCargo.text = "Delivery Result:"
            holder.tvLabelOb.text = "Pending Result:"

            // အရောင်ကို အသေမပေးဘဲ Context ကနေ ခေါ်သုံးတာ ပိုစိတ်ချရပါတယ်
            holder.tvLabelCargo.setTextColor(Color.parseColor("#2E7D32")) // Green
        } else {
            holder.tvLabelCargo.text = "Cargo Result:"
            holder.tvLabelOb.text = "OB Result:"
            holder.tvLabelCargo.setTextColor(Color.parseColor("#D32F2F")) // Red
        }

        // Logic: ရက်စွဲထဲမှာ [IB] ပါဝင်မှုကို စစ်ဆေးခြင်း
        if (currentItem.date.contains("[IB]")) {
            // IB အတွက် ခေါင်းစဉ်များ
            holder.tvLabelCargo.text = "Delivery Result:"
            holder.tvLabelOb.text = "Pending Result:"

            // အရောင်ပြောင်းလဲခြင်း (Delivery အတွက် အစိမ်းရောင်)
            holder.tvLabelCargo.setTextColor(Color.parseColor("#2E7D32"))
            holder.tvLabelOb.setTextColor(Color.parseColor("#1976D2"))
        } else {
            // OB အတွက် ခေါင်းစဉ်များ
            holder.tvLabelCargo.text = "Cargo Result:"
            holder.tvLabelOb.text = "OB Result:"

            // အရောင်ပြောင်းလဲခြင်း (Cargo အတွက် အနီရောင်)
            holder.tvLabelCargo.setTextColor(Color.parseColor("#D32F2F"))
            holder.tvLabelOb.setTextColor(Color.parseColor("#1976D2"))
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(position)
        }
    }

    override fun getItemCount(): Int {
        return saveList.size
    }
}