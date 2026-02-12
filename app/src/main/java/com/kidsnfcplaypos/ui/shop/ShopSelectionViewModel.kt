package com.kidsnfcplaypos.ui.shop

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kidsnfcplaypos.data.repository.MenuRepository
import com.kidsnfcplaypos.util.ResourceResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// UI Model for a top-level menu category tile
data class MenuCategoryUi(
    val id: String,
    val name: String // Localized name
)

class ShopSelectionViewModel(application: Application, private val menuRepository: MenuRepository, private val resourceResolver: ResourceResolver) : AndroidViewModel(application) {

    private val _menuCategories = MutableStateFlow<List<MenuCategoryUi>>(emptyList())
    val menuCategories: StateFlow<List<MenuCategoryUi>> = _menuCategories

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        loadMenuCategories()
    }

    private fun loadMenuCategories() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            val result = menuRepository.loadAllMenuCategories()
            result.onSuccess { categories ->
                _menuCategories.value = categories.map { category ->
                    MenuCategoryUi(
                        id = category.id,
                        name = resourceResolver.getString(category.nameStringResourceName)
                    )
                }
            }.onFailure { throwable ->
                _errorMessage.value = "Failed to load menu categories: ${throwable.localizedMessage}"
            }
            _isLoading.value = false
        }
    }

    // Factory for ViewModel with application and repository dependencies
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ShopSelectionViewModel::class.java)) {
                val menuRepository = MenuRepository(application.applicationContext)
                val resourceResolver = ResourceResolver(application.applicationContext)
                return ShopSelectionViewModel(application, menuRepository, resourceResolver) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
