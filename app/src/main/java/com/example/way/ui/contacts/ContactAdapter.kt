package com.example.way.ui.contacts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.way.R
import com.example.way.data.model.Contact
import com.example.way.databinding.ItemContactBinding

class ContactAdapter(
    private val onEdit: (Contact) -> Unit,
    private val onDelete: (Contact) -> Unit,
    private val onSetPrimary: (Contact) -> Unit
) : ListAdapter<Contact, ContactAdapter.ViewHolder>(DiffCallback()) {

    inner class ViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: Contact, position: Int) {
            binding.tvPriority.text = (position + 1).toString()
            binding.tvName.text = contact.name
            binding.tvPhone.text = contact.phone

            // Show filled star for primary (priority 0)
            val starIcon = if (contact.priority == 0)
                R.drawable.ic_star_filled else R.drawable.ic_star_outline
            binding.btnStar.setImageResource(starIcon)

            binding.btnStar.setOnClickListener { onSetPrimary(contact) }
            binding.btnEdit.setOnClickListener { onEdit(contact) }
            binding.btnDelete.setOnClickListener { onDelete(contact) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemContactBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    class DiffCallback : DiffUtil.ItemCallback<Contact>() {
        override fun areItemsTheSame(old: Contact, new: Contact) = old.id == new.id
        override fun areContentsTheSame(old: Contact, new: Contact) = old == new
    }
}

