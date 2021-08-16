package ru.karasevm.privatednstoggle

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class RecyclerAdapter(val items: MutableList<String>): RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {

    var onItemClick: ((Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_row, parent, false)
        val vh = ViewHolder(view)
        return vh
    }

    override fun onBindViewHolder(holder: RecyclerAdapter.ViewHolder, position: Int) {
        val item = items[position]

        // sets the text to the textview from our itemHolder class
        holder.textView.text = item
    }

    override fun getItemCount(): Int {
        return items.size
    }



    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
        init {
            itemView.setOnClickListener {
                onItemClick?.invoke(adapterPosition)
            }
        }
    }
    fun setData(newItems: MutableList<String>) {
        items.run {
            clear()
            addAll(newItems)
        }
    }


}