package com.kidsnfcplaypos.data.model

import kotlinx.serialization.Serializable
import java.math.BigDecimal

// Represents a single menu item (e.g., "Coffee", "Muffin")
@Serializable
data class MenuItem(
    val id: String,
    val nameStringResourceName: String, // Resource name for localized string (e.g., "menu_item_cafe_coffee")
    @Serializable(with = BigDecimalSerializer::class)
    val price: BigDecimal
)

// Represents a sub-category within a main menu category (e.g., "Drinks", "Food")
@Serializable
data class SubCategory(
    val id: String,
    val nameStringResourceName: String, // Resource name for localized string (e.g., "subcategory_cafe_drinks")
    val items: List<MenuItem>
)

// Represents a main menu category (e.g., "Cafe", "Restaurant")
@Serializable
data class MenuCategory(
    val id: String,
    val nameStringResourceName: String, // Resource name for localized string (e.g., "category_cafe")
    val subCategories: List<SubCategory>
)
