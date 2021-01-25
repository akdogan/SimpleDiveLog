package com.akdogan.simpledivelog.application.listview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.akdogan.simpledivelog.R
import com.akdogan.simpledivelog.datalayer.repository.DiveLogEntry


class ListViewAdapter(
    //val clickListener: DiveLogEntryListener,
    val myItemClickListener: (id: String) -> Unit
): RecyclerView.Adapter<ListViewAdapter.ListItemViewHolder>() {
    var dataSet = listOf<DiveLogEntry>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class ListItemViewHolder(private val view: View) : RecyclerView.ViewHolder(view){
        val itemLocationTextField = view.findViewById<TextView>(R.id.list_item_location)
        val itemMaxDepthTextField = view.findViewById<TextView>(R.id.list_item_max_depth)
        val itemDiveDuration = view.findViewById<TextView>(R.id.list_item_dive_duration)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListItemViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_entry_view, parent, false)
        return ListItemViewHolder(adapterLayout)
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun onBindViewHolder(holder: ListItemViewHolder, position: Int) {
        val item = dataSet[position]
        val res = holder.itemView.context.resources
        val diveNumber = res.getString(R.string.list_entry_dive_number, item.diveNumber)
        val location = res.getString(R.string.list_entry_dive_location, item.diveLocation)
        //holder.bind(item)
        //holder.itemDiveNumberTextField.text = res.getString(R.string.list_entry_dive_number, item.diveNumber) //item.diveNumber.toString()
        holder.itemLocationTextField.text = "$diveNumber $location"
        holder.itemMaxDepthTextField.text = res.getString(R.string.list_entry_max_depth, item.maxDepth)
        holder.itemDiveDuration.text = res.getString(R.string.list_entry_dive_duration, item.diveDuration)
        holder.itemView.setOnClickListener{
            myItemClickListener(item.dataBaseId)
        }

    }

    fun getDatabaseIdAtPosition(pos: Int): String{
        val id = dataSet[pos].dataBaseId
        return id
    }
}
// ItemCLicklistener defined, but i just passed it from viewModel to adapter via the fragment
class DiveLogEntryListener(val clickListener: (remoteId: String) -> Unit){
    fun onClick(item: DiveLogEntry) = clickListener(item.dataBaseId)

}