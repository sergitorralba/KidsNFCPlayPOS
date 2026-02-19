package com.kidsnfcplaypos.ui.shop

import com.kidsnfcplaypos.data.model.MenuItem
import com.kidsnfcplaypos.data.model.SubCategory

sealed interface ShopListItem {
    val stableId: String
}

data class HeaderListItem(val localizedName: String, val id: String) : ShopListItem {
    override val stableId: String = id
}

data class ItemListItem(
    val menuItem: MenuItem,
    val displayName: String,
    val displayPrice: String,
    val quantity: Int
) : ShopListItem {
    override val stableId: String = menuItem.id
}
