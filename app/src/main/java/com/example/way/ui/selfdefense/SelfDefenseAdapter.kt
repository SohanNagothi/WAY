package com.example.way.ui.selfdefense

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.way.data.model.SelfDefenseItem
import com.example.way.databinding.ItemSafetyTipBinding
import com.example.way.databinding.ItemSafetyVideoBinding

class SelfDefenseAdapter(
    private var items: List<SelfDefenseItem> = emptyList(),
    private val onVideoOpenClick: (SelfDefenseItem.VideoTip) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_TIP = 0
        private const val TYPE_VIDEO = 1
    }

    fun submitList(newItems: List<SelfDefenseItem>) {
        items = newItems
        @Suppress("NotifyDataSetChanged")
        notifyDataSetChanged()
    }

    // Kept for API compatibility with existing fragment calls.
    fun stopAllVideos() = Unit

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is SelfDefenseItem.TextTip -> TYPE_TIP
        is SelfDefenseItem.VideoTip -> TYPE_VIDEO
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_TIP -> TipViewHolder(ItemSafetyTipBinding.inflate(inflater, parent, false))
            TYPE_VIDEO -> VideoViewHolder(ItemSafetyVideoBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SelfDefenseItem.TextTip -> (holder as TipViewHolder).bind(item)
            is SelfDefenseItem.VideoTip -> (holder as VideoViewHolder).bind(item)
        }
    }

    class TipViewHolder(private val binding: ItemSafetyTipBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var expanded = false

        fun bind(tip: SelfDefenseItem.TextTip) {
            binding.tvTitle.text = tip.title
            binding.tvBody.text = tip.body
            binding.tvBody.visibility = View.GONE
            binding.ivExpand.rotation = 0f
            expanded = false

            binding.headerRow.setOnClickListener {
                expanded = !expanded
                binding.tvBody.visibility = if (expanded) View.VISIBLE else View.GONE
                binding.ivExpand.animate()
                    .rotation(if (expanded) 180f else 0f)
                    .setDuration(200)
                    .start()
            }
        }
    }

    inner class VideoViewHolder(private val binding: ItemSafetyVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SelfDefenseItem.VideoTip) {
            binding.tvVideoTitle.text = item.title
            binding.overlayPlay.visibility = View.VISIBLE

            val openAction = View.OnClickListener { onVideoOpenClick(item) }
            binding.overlayPlay.setOnClickListener(openAction)
            binding.btnFullscreen.setOnClickListener(openAction)
        }
    }
}
