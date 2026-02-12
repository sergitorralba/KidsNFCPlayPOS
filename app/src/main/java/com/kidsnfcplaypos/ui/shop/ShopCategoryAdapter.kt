package com.kidsnfcplaypos.ui.shop

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kidsnfcplaypos.databinding.ItemShopCategoryBinding

class ShopCategoryAdapter(private val onItemClicked: (MenuCategoryUi) -> Unit) :
    ListAdapter<MenuCategoryUi, ShopCategoryAdapter.ShopCategoryViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopCategoryViewHolder {
        val binding = ItemShopCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShopCategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShopCategoryViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ShopCategoryViewHolder(private val binding: ItemShopCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                onItemClicked(getItem(adapterPosition))
            }
        }

        fun bind(category: MenuCategoryUi) {
            binding.categoryName.text = category.name
            // Potentially set an icon here based on category.id if you have them
            // binding.categoryIcon.setImageResource(...)
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<MenuCategoryUi>() {
        override fun areItemsTheSame(oldItem: MenuCategoryUi, newItem: MenuCategoryUi): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: MenuCategoryUi, newItem: MenuCategoryUi): Boolean {
            return oldItem == newItem
        }
    }
}
