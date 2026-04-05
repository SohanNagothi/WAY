package com.example.way.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.way.R
import com.example.way.data.model.WalkSession
import com.example.way.databinding.ItemWalkHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WalkHistoryAdapter :
    ListAdapter<WalkSession, WalkHistoryAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())

    inner class ViewHolder(private val binding: ItemWalkHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(session: WalkSession) {
            binding.tvDestination.text = session.destinationName.ifEmpty { "Unknown destination" }
            binding.tvDate.text = dateFormat.format(Date(session.date))

            val mins = session.durationSeconds / 60
            val secs = session.durationSeconds % 60
            binding.tvDuration.text = "⏱ ${mins}m ${secs}s"

            // Distance
            val dist = session.distanceMeters
            binding.tvDistance.text = if (dist >= 1000) {
                String.format(Locale.US, "📍 %.1f km", dist / 1000)
            } else {
                String.format(Locale.US, "📍 %.0f m", dist)
            }

            if (session.alertTriggered) {
                binding.chipAlert.visibility = View.VISIBLE
                val triggerLabel = when (session.triggerType) {
                    "ManualSOS" -> "⚠ SOS"
                    "PhoneFall" -> "⚠ Fall"
                    "SuddenSpeed" -> "⚠ Speed"
                    "Inactivity" -> "⚠ Inactive"
                    "DistanceIncreasing" -> "⚠ Off Route"
                    else -> "⚠ Alert"
                }
                binding.chipAlert.text = triggerLabel
                binding.chipAlert.setChipBackgroundColorResource(R.color.way_error_container)
                binding.chipAlert.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.way_error)
                )
            } else {
                binding.chipAlert.visibility = View.VISIBLE
                binding.chipAlert.text = "✓ Safe"
                binding.chipAlert.setChipBackgroundColorResource(R.color.way_primary_container)
                binding.chipAlert.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.way_primary)
                )
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWalkHistoryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<WalkSession>() {
        override fun areItemsTheSame(old: WalkSession, new: WalkSession) = old.id == new.id
        override fun areContentsTheSame(old: WalkSession, new: WalkSession) = old == new
    }
}

