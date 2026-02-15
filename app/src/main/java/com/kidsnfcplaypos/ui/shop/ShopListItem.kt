package com.kidsnfcplaypos.ui.shop

import com.kidsnfcplaypos.data.model.MenuItem
import com.kidsnfcplaypos.data.model.SubCategory

/**
 * A sealed interface to represent the different types of items that can be displayed
 * in the main shop list: either a header for a subcategory or a specific menu item.
 */
sealed interface ShopListItem {
    /**
     * A unique and stable ID used by the ListAdapter's DiffUtil to identify items.
     */
    val stableId: String
}

/**
 * Represents a header row in the list, displaying the name of a subcategory.
 */
data class HeaderListItem(val subCategory: SubCategory) : ShopListItem {
    override val stableId: String = subCategory.id
}

/**
 * Represents a content row in the list, displaying a single menu item.
 */
data class ItemListItem(
    val menuItem: MenuItem,
    val displayName: String, // The final, resolved name (e.g., "Coffee")
    val displayPrice: String,
    val quantity: Int // The final, formatted price (e.g., "$2.50")
) : ShopListItem {
    override val stableId: String = menuItem.id
}
