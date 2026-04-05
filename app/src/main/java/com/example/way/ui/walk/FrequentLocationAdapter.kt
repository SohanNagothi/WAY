package com.example.way.ui.walk

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.way.data.model.FrequentLocation
import com.example.way.databinding.ItemFrequentLocationBinding

class FrequentLocationAdapter(
    private val onClick: (FrequentLocation) -> Unit
) : ListAdapter<FrequentLocation, FrequentLocationAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemFrequentLocationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(location: FrequentLocation) {
            binding.tvLocName.text = location.name
            binding.tvLocAddress.text = location.address.ifEmpty { "No address" }
            binding.root.setOnClickListener { onClick(location) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFrequentLocationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<FrequentLocation>() {
        override fun areItemsTheSame(old: FrequentLocation, new: FrequentLocation) = old.id == new.id
        override fun areContentsTheSame(old: FrequentLocation, new: FrequentLocation) = old == new
    }
}

