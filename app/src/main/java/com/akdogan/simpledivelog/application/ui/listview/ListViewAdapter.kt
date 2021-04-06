package com.akdogan.simpledivelog.application.ui.listview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.akdogan.simpledivelog.R
import com.akdogan.simpledivelog.databinding.ListEntryViewBinding
import com.akdogan.simpledivelog.datalayer.network.getThumbnailFromImageUrl
import com.akdogan.simpledivelog.datalayer.repository.DiveLogEntry
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions


class ListViewAdapter(
    val myItemClickListener: (id: String) -> Unit
) : RecyclerView.Adapter<ListViewAdapter.ListItemViewHolder>() {
    var dataSet = listOf<DiveLogEntry>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    class ListItemViewHolder(val binding: ListEntryViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val itemLocationTextField = binding.listItemLocation
        val itemMaxDepthTextField = binding.listItemMaxDepth
        val itemDiveDuration = binding.listItemDiveDuration

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListItemViewHolder {
        val binding =
            ListEntryViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListItemViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    override fun onBindViewHolder(holder: ListItemViewHolder, position: Int) {
        val item = dataSet[position]
        holder.binding.currentItem = item
        val res = holder.itemView.context.resources
        val diveNumber = res.getString(R.string.list_entry_dive_number, item.diveNumber)
        val location = res.getString(R.string.list_entry_dive_location, item.diveLocation)
        holder.itemLocationTextField.text = "$diveNumber $location"
        holder.itemMaxDepthTextField.text = res.getString(R.string.list_entry_max_depth, item.maxDepth)
        holder.itemDiveDuration.text = res.getString(R.string.list_entry_dive_duration, item.diveDuration)
        holder.itemView.setOnClickListener {
            myItemClickListener(item.dataBaseId)
        }
        Glide.with(holder.binding.listItemThumbnail)
            .load(getThumbnailFromImageUrl(item.imgUrl)?.toUri())
            .apply(
                RequestOptions()
                    .placeholder(R.drawable.loading_animation)
                    .fallback(R.drawable.ic_diver)
                    .error(R.drawable.ic_baseline_no_photography_24)
            )
            .into(holder.binding.listItemThumbnail)
    }

    fun getDatabaseIdAtPosition(pos: Int): String {
        return dataSet[pos].dataBaseId
    }
}