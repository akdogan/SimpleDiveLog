package com.akdogan.simpledivelog.application.ui.detailview


import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.akdogan.simpledivelog.databinding.DetailEntryItemBinding

internal class DetailViewListAdapter :
    RecyclerView.Adapter<DetailViewListAdapter.DetailListViewHolder>() {

    var dataSet: List<DetailListItem> = emptyList()
    set(value) {
        field = value
        notifyDataSetChanged()
        Log.i("DETAIL_VIEW", "dataset changed to $value")
    }

    class DetailListViewHolder(
        val binding: DetailEntryItemBinding
    ): RecyclerView.ViewHolder(binding.root){
        val titleTextView = binding.detailItemTitle
        val descriptionTextView = binding.detailItemDescription
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailListViewHolder {
        Log.i("DETAIL_VIEW", "ViewHolder created")
        val binding = DetailEntryItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DetailListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DetailListViewHolder, position: Int) {
        val item = dataSet[position]
        holder.titleTextView.text = item.title
        holder.descriptionTextView.text = item.value
        Log.i("DETAIL_VIEW", "Viewholder bound with ${item.title} | ${item.value}")
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }
}

internal data class DetailListItem(
    val title: String,
    val value: String?,
){
    val visible = value != null
}