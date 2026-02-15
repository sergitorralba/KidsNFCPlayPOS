package com.kidsnfcplaypos.ui.shop

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kidsnfcplaypos.data.model.MenuCategory
import com.kidsnfcplaypos.databinding.ItemMenuSelectionBinding

class MenuSelectionAdapter(private val onItemClicked: (MenuCategory) -> Unit) :
    ListAdapter<MenuCategory, MenuSelectionAdapter.MenuViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemMenuSelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class MenuViewHolder(private val binding: ItemMenuSelectionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                onItemClicked(getItem(adapterPosition))
            }
        }

        fun bind(category: MenuCategory) {
            binding.menuName.text = category.displayName
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<MenuCategory>() {
        override fun areItemsTheSame(oldItem: MenuCategory, newItem: MenuCategory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MenuCategory, newItem: MenuCategory): Boolean {
            return oldItem == newItem
        }
    }
}
