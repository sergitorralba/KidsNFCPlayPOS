package com.kidsnfcplaypos.ui.shop

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kidsnfcplaypos.data.model.MenuItem
import com.kidsnfcplaypos.databinding.ItemShopCategoryBinding
import com.kidsnfcplaypos.databinding.ItemShopItemBinding

private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_ITEM = 1

class ShopCategoryAdapter(
    private val onAddItem: (MenuItem) -> Unit,
    private val onRemoveItem: (MenuItem) -> Unit
) : ListAdapter<ShopListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    class HeaderViewHolder(private val binding: ItemShopCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(headerItem: HeaderListItem) {
            binding.categoryName.text = headerItem.localizedName
        }
    }

    class ItemViewHolder(private val binding: ItemShopItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: ItemListItem,
            onAddItem: (MenuItem) -> Unit,
            onRemoveItem: (MenuItem) -> Unit
        ) {
            binding.itemName.text = item.displayName
            binding.itemPrice.text = item.displayPrice

            if (item.quantity > 0) {
                binding.quantityControls.visibility = View.VISIBLE
                binding.quantityText.text = item.quantity.toString()
            } else {
                binding.quantityControls.visibility = View.GONE
            }

            binding.itemRowContent.setOnClickListener { onAddItem(item.menuItem) }
            binding.addButton.setOnClickListener { onAddItem(item.menuItem) }
            binding.removeButton.setOnClickListener { onRemoveItem(item.menuItem) }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HeaderListItem -> VIEW_TYPE_HEADER
            is ItemListItem -> VIEW_TYPE_ITEM
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(ItemShopCategoryBinding.inflate(inflater, parent, false))
            VIEW_TYPE_ITEM -> ItemViewHolder(ItemShopItemBinding.inflate(inflater, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is HeaderListItem -> (holder as HeaderViewHolder).bind(item)
            is ItemListItem -> (holder as ItemViewHolder).bind(item, onAddItem, onRemoveItem)
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<ShopListItem>() {
        override fun areItemsTheSame(oldItem: ShopListItem, newItem: ShopListItem): Boolean {
            return oldItem.stableId == newItem.stableId
        }

        override fun areContentsTheSame(oldItem: ShopListItem, newItem: ShopListItem): Boolean {
            return oldItem == newItem
        }
    }
}
