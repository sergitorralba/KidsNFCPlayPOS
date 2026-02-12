package com.kidsnfcplaypos.data.repository

import android.content.Context
import com.kidsnfcplaypos.data.model.MenuCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException

class MenuRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    // Function to load all menu categories from assets/menus/
    suspend fun loadAllMenuCategories(): Result<List<MenuCategory>> = withContext(Dispatchers.IO) {
        try {
            val assetManager = context.assets
            val menuFiles = assetManager.list("menus") ?: arrayOf()

            val menuCategories = mutableListOf<MenuCategory>()
            for (fileName in menuFiles) {
                if (fileName.startsWith("menu_") && fileName.endsWith(".json")) {
                    val jsonString = assetManager.open("menus/$fileName").bufferedReader().use { it.readText() }
                    val menuCategory = json.decodeFromString<MenuCategory>(jsonString)
                    menuCategories.add(menuCategory)
                }
            }
            Result.success(menuCategories.sortedBy { it.id }) // Sort by ID for consistent order
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
