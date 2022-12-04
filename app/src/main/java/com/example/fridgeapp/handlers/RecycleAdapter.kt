package com.example.fridgeapp.handlers

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.example.fridgeapp.R
import com.example.fridgeapp.data.FridgeSnap

//адаптер recycleview, сказать нечего :)
//ну загружаем в разметку переданные данные, реально сказать нечего
class RecycleAdapter(private val snapsList: List<FridgeSnap>) :
    RecyclerView.Adapter<RecycleAdapter.ViewHolder>() {

    var onItemClick: ((FridgeSnap) -> Unit)? = null

    inner class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val previewImage: ImageView = itemView.findViewById(R.id.image_preview)
        val cardTitle: TextView = itemView.findViewById(R.id.card_title)
        val cardDate: TextView = itemView.findViewById(R.id.card_date)
        val cardID: TextView = itemView.findViewById(R.id.card_id)

        init {
            itemView.setOnClickListener {
                onItemClick?.invoke(snapsList[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.card_fridge_snap, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fridgeSnap = snapsList[position]
        if (fridgeSnap.image != "null") holder.previewImage.setImageURI(fridgeSnap.image?.toUri())
        else holder.previewImage.setImageResource(R.drawable.fridge_small_preview)

        holder.cardTitle.text = fridgeSnap.title
        holder.cardID.text = (position + 1).toString()
        holder.cardDate.text = fridgeSnap.date + " " + fridgeSnap.time
    }

    override fun getItemCount(): Int {
        return snapsList.size
    }

}