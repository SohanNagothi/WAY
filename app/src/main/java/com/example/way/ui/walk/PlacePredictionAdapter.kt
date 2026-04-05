package com.example.way.ui.walk

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.way.data.remote.GeoapifyPlace
import com.example.way.databinding.ItemPlacePredictionBinding

class PlacePredictionAdapter(
    private val onClick: (GeoapifyPlace) -> Unit
) : ListAdapter<GeoapifyPlace, PlacePredictionAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemPlacePredictionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(place: GeoapifyPlace) {
            binding.tvPrimaryText.text = place.name
            binding.tvSecondaryText.text = place.fullAddress
            binding.root.setOnClickListener { onClick(place) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPlacePredictionBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<GeoapifyPlace>() {
        override fun areItemsTheSame(old: GeoapifyPlace, new: GeoapifyPlace) =
            old.placeId == new.placeId
        override fun areContentsTheSame(old: GeoapifyPlace, new: GeoapifyPlace) =
            old == new
    }
}
