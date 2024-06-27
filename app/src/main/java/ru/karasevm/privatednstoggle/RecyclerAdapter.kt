package ru.karasevm.privatednstoggle

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Collections

class RecyclerAdapter(private val items: MutableList<String>, private val showDragHandle: Boolean) :
    RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {

    var onItemClick: ((Int) -> Unit)? = null
    var onItemsChanged: ((MutableList<String>) -> Unit)? = null
    var onDragStart: ((RecyclerAdapter.ViewHolder) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerAdapter.ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.recyclerview_row, parent, false)
        val vh = ViewHolder(view)
        return vh
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerAdapter.ViewHolder, position: Int) {
        val item = items[position]
        val parts = item.split(" : ")
        if (parts.size == 2) {
            holder.labelTextView.text = parts[0]
            holder.textView.text = parts[1]
        } else {
            holder.labelTextView.visibility = View.GONE
            holder.textView.text = parts[0]
        }

        if (showDragHandle) {
            holder.dragHandle.visibility = View.VISIBLE
            holder.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    onDragStart?.invoke(holder)
                }
                return@setOnTouchListener true
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun onItemMove(fromPosition: Int, toPosition: Int) {
        // Swap items in your data list
        Collections.swap(items, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        onItemsChanged?.invoke(items)
    }


    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
        val labelTextView: TextView = itemView.findViewById(R.id.labelTextView)
        val dragHandle: ImageView = itemView.findViewById(R.id.dragHandle)

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