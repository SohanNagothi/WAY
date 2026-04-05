package com.example.way.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.way.R
import com.example.way.data.model.FrequentLocation
import com.example.way.databinding.ItemFrequentLocationBinding

class FrequentLocationsAdapter(
    private val onEdit: (FrequentLocation) -> Unit,
    private val onDelete: (FrequentLocation) -> Unit
) : ListAdapter<FrequentLocation, FrequentLocationsAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemFrequentLocationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(location: FrequentLocation) {
            binding.tvLocName.text = location.name
            binding.tvLocAddress.text = location.address.ifEmpty { "No address" }

            binding.root.setOnLongClickListener {
                showPopupMenu(location)
                true
            }

            binding.root.setOnClickListener {
                showPopupMenu(location)
            }
        }

        private fun showPopupMenu(location: FrequentLocation) {
            val popup = PopupMenu(binding.root.context, binding.root)
            popup.menu.add(0, 1, 0, "Edit")
            popup.menu.add(0, 2, 1, "Delete")
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> { onEdit(location); true }
                    2 -> { onDelete(location); true }
                    else -> false
                }
            }
            popup.show()
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

