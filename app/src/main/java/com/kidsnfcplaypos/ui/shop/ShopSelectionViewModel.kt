package com.kidsnfcplaypos.ui.shop

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kidsnfcplaypos.data.model.MenuCategory
import com.kidsnfcplaypos.data.model.SubCategory
import com.kidsnfcplaypos.data.repository.MenuRepository
import com.kidsnfcplaypos.util.ResourceResolver
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

// Represents the entire UI state for the ShopSelectionFragment
data class ShopSelectionUiState(
    val isLoading: Boolean = true,
    val availableMenus: List<MenuCategory> = emptyList(),
    val selectedMenuId: String? = null,
    val error: String? = null
)

class ShopSelectionViewModel(
    application: Application,
    private val menuRepository: MenuRepository,
    private val resourceResolver: ResourceResolver
) : AndroidViewModel(application) {

    private val currencyFormatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

    // --- Source of Truth StateFlows ---
    private val _uiState = MutableStateFlow(ShopSelectionUiState())
    private val _cart = MutableStateFlow<Map<String, Int>>(emptyMap())

    // --- Public-facing Derived StateFlows ---
    val uiState: StateFlow<ShopSelectionUiState> = _uiState.asStateFlow()

    val shopListItems: StateFlow<List<ShopListItem>> = combine(uiState, _cart) { state, cart ->
        val selectedMenu = state.availableMenus.find { it.id == state.selectedMenuId }
        selectedMenu?.subCategories?.flatMap { subCategory ->
            val header = HeaderListItem(subCategory)
            val items = subCategory.items.map { menuItem ->
                ItemListItem(
                    menuItem = menuItem,
                    displayName = resourceResolver.getString(menuItem.nameStringResourceName),
                    displayPrice = currencyFormatter.format(menuItem.price),
                    quantity = cart[menuItem.id] ?: 0
                )
            }
            listOf(header) + items
        } ?: emptyList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val totalAmount: StateFlow<BigDecimal> = combine(uiState, _cart) { state, cart ->
        var total = BigDecimal.ZERO
        val selectedMenu = state.availableMenus.find { it.id == state.selectedMenuId }
        if (selectedMenu != null) {
            val allItemsById = selectedMenu.subCategories.flatMap { it.items }.associateBy { it.id }
            cart.forEach { (itemId, quantity) ->
                allItemsById[itemId]?.let { menuItem ->
                    total += menuItem.price.multiply(BigDecimal(quantity))
                }
            }
        }
        total
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BigDecimal.ZERO
    )

    // --- Lifecycle ---
    init {
        loadMenuCategories()
    }

    // --- Public Actions ---
    fun addItem(itemId: String) {
        val newCart = _cart.value.toMutableMap()
        val currentQuantity = newCart.getOrPut(itemId) { 0 }
        newCart[itemId] = currentQuantity + 1
        _cart.value = newCart
    }

    fun removeItem(itemId: String) {
        val newCart = _cart.value.toMutableMap()
        val currentQuantity = newCart[itemId]
        if (currentQuantity != null && currentQuantity > 1) {
            newCart[itemId] = currentQuantity - 1
        } else {
            newCart.remove(itemId)
        }
        _cart.value = newCart
    }

    fun selectMenu(menuId: String) {
        if (_uiState.value.availableMenus.any { it.id == menuId }) {
            _uiState.value = _uiState.value.copy(selectedMenuId = menuId)
        }
    }

    private fun loadMenuCategories() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            menuRepository.loadAllMenuCategories().onSuccess { categories ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    availableMenus = categories,
                    selectedMenuId = categories.firstOrNull()?.id
                )
            }.onFailure { throwable ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load menu categories: ${throwable.localizedMessage}"
                )
            }
        }
    }

    // --- Factory ---
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
