package com.kidsnfcplaypos.ui.shop

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kidsnfcplaypos.data.model.MenuCategory
import com.kidsnfcplaypos.data.model.MenuItem
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
    val availableMenus: List<MenuCategoryUI> = emptyList(),
    val selectedMenuId: String? = null,
    val error: String? = null
)

// UI-specific model for menu category with localized name
data class MenuCategoryUI(
    val id: String,
    val localizedName: String
)

class ShopSelectionViewModel(
    application: Application,
    private val menuRepository: MenuRepository,
    private val resourceResolver: ResourceResolver
) : AndroidViewModel(application) {

    private val shopPrefs = application.getSharedPreferences("shop_prefs", android.content.Context.MODE_PRIVATE)
    private val appPrefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
    private val KEY_SELECTED_MENU = "selected_menu_id"

    private fun getCurrencyFormatter(): NumberFormat {
        return NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = java.util.Currency.getInstance("EUR")
        }
    }

    // --- Internal Data ---
    private var _allMenuCategories = emptyList<MenuCategory>()
    // All items from all menus indexed by their ID for easy lookup during total calculation
    private var _allItemsById = emptyMap<String, MenuItem>()

    // --- Source of Truth StateFlows ---
    private val _uiState = MutableStateFlow(ShopSelectionUiState())
    private val _cart = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _refreshTrigger = MutableStateFlow(0)

    // --- Public-facing Derived StateFlows ---
    val uiState: StateFlow<ShopSelectionUiState> = combine(_uiState, _refreshTrigger) { state, _ ->
        val filteredCategories = _allMenuCategories
        state.copy(availableMenus = filteredCategories.map {
            MenuCategoryUI(it.id, resourceResolver.getString(it.nameStringResourceName))
        })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value)

    val shopListItems: StateFlow<List<ShopListItem>> = combine(uiState, _cart, _refreshTrigger) { state, cart, _ ->
        Log.d("ShopSelectionVM", "Calculating shopListItems for menu: ${state.selectedMenuId}")
        val selectedMenu = _allMenuCategories.find { it.id == state.selectedMenuId }
        val formatter = getCurrencyFormatter()
        selectedMenu?.subCategories?.flatMap { subCategory ->
            // Resolve the subcategory name from resources
            val localizedSubCategoryName = resourceResolver.getString(subCategory.nameStringResourceName)
            val header = HeaderListItem(localizedSubCategoryName, subCategory.id)
            
            val items = subCategory.items.map { menuItem ->
                ItemListItem(
                    menuItem = menuItem,
                    displayName = resourceResolver.getString(menuItem.nameStringResourceName),
                    displayPrice = formatter.format(menuItem.price),
                    quantity = cart[menuItem.id] ?: 0
                )
            }
            listOf(header) + items
        } ?: emptyList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val totalAmount: StateFlow<BigDecimal> = combine(_cart, _refreshTrigger) { cart, _ ->
        var total = BigDecimal.ZERO
        cart.forEach { (itemId, quantity) ->
            _allItemsById[itemId]?.let { menuItem ->
                total += menuItem.price.multiply(BigDecimal(quantity))
            }
        }
        total
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = BigDecimal.ZERO
    )

    val cartItems: StateFlow<List<ItemListItem>> = combine(_cart, _refreshTrigger) { cart, _ ->
        val formatter = getCurrencyFormatter()
        
        // Return only items that are actually in the cart, using the global index
        cart.mapNotNull { (itemId, quantity) ->
            _allItemsById[itemId]?.let { menuItem ->
                ItemListItem(
                    menuItem = menuItem,
                    displayName = resourceResolver.getString(menuItem.nameStringResourceName),
                    displayPrice = formatter.format(menuItem.price),
                    quantity = quantity
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    // --- Lifecycle ---
    init {
        Log.d("ShopSelectionVM", "ViewModel Init")
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

    fun clearCart() {
        Log.d("ShopSelectionVM", "clearCart called! Current cart size: ${_cart.value.size}")
        _cart.value = emptyMap()
    }

    fun selectMenu(menuId: String) {
        Log.d("ShopSelectionVM", "Selecting menu: $menuId")
        if (_allMenuCategories.any { it.id == menuId }) {
            _uiState.value = _uiState.value.copy(selectedMenuId = menuId)
            shopPrefs.edit().putString(KEY_SELECTED_MENU, menuId).apply()
        }
    }

    fun refreshLocale() {
        _refreshTrigger.value += 1
    }

    private fun loadMenuCategories() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            menuRepository.loadAllMenuCategories().onSuccess { categories ->
                _allMenuCategories = categories
                // Flatten all items into a single map for global total calculation
                _allItemsById = categories.flatMap { it.subCategories }
                    .flatMap { it.items }
                    .associateBy { it.id }

                val savedMenuId = shopPrefs.getString(KEY_SELECTED_MENU, null)
                val initialMenuId = if (categories.any { it.id == savedMenuId }) {
                    savedMenuId
                } else {
                    categories.firstOrNull()?.id
                }
                
                Log.d("ShopSelectionVM", "Menus loaded. Initial selection: $initialMenuId")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedMenuId = initialMenuId
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
