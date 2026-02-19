package com.kidsnfcplaypos.ui.shop

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kidsnfcplaypos.databinding.ItemMenuSelectionBinding

class MenuSelectionAdapter(private val onItemClicked: (MenuCategoryUI) -> Unit) :
    ListAdapter<MenuCategoryUI, MenuSelectionAdapter.MenuViewHolder>(DiffCallback()) {

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
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    Log.d("MenuSelectionAdapter", "Menu clicked: ${item.id}")
                    onItemClicked(item)
                }
            }
        }

        fun bind(category: MenuCategoryUI) {
            binding.menuName.text = category.localizedName
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<MenuCategoryUI>() {
        override fun areItemsTheSame(oldItem: MenuCategoryUI, newItem: MenuCategoryUI): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MenuCategoryUI, newItem: MenuCategoryUI): Boolean {
            return oldItem == newItem
        }
    }
}
