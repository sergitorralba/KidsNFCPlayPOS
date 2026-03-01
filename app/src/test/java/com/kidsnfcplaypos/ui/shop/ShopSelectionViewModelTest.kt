package com.kidsnfcplaypos.ui.shop

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.kidsnfcplaypos.data.model.MenuCategory
import com.kidsnfcplaypos.data.model.MenuItem
import com.kidsnfcplaypos.data.model.SubCategory
import com.kidsnfcplaypos.data.repository.MenuRepository
import com.kidsnfcplaypos.util.ResourceResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class ShopSelectionViewModelTest {

    private val application: Application = mock()
    private val menuRepository: MenuRepository = mock()
    private val resourceResolver: ResourceResolver = mock()
    private val sharedPreferences: SharedPreferences = mock()
    private val sharedPreferencesEditor: SharedPreferences.Editor = mock()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        whenever(application.getSharedPreferences(any(), any())).thenReturn(sharedPreferences)
        whenever(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor)
        whenever(sharedPreferencesEditor.putString(any(), any())).thenReturn(sharedPreferencesEditor)
        
        // Mock resource resolver to just return the key
        whenever(resourceResolver.getString(any())).thenAnswer { it.arguments[0] as String }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialization loads menus correctly`() = runTest {
        val categories = listOf(
            MenuCategory("cat1", "Category 1 Key", "Category 1", listOf(
                SubCategory("sub1", "Sub 1 Key", "Sub 1", listOf(
                    MenuItem("item1", "Item 1 Key", BigDecimal("5.00"))
                ))
            ))
        )
        whenever(menuRepository.loadAllMenuCategories()).thenReturn(Result.success(categories))

        val viewModel = ShopSelectionViewModel(application, menuRepository, resourceResolver)

        assertEquals(false, viewModel.uiState.value.isLoading)
        assertEquals("cat1", viewModel.uiState.value.selectedMenuId)
        assertEquals(1, viewModel.uiState.value.availableMenus.size)
    }

    @Test
    fun `addItem increases cart quantity and total amount`() = runTest {
        val item1 = MenuItem("item1", "Item 1 Key", BigDecimal("5.00"))
        val categories = listOf(
            MenuCategory("cat1", "Category 1 Key", "Category 1", listOf(
                SubCategory("sub1", "Sub 1 Key", "Sub 1", listOf(item1))
            ))
        )
        whenever(menuRepository.loadAllMenuCategories()).thenReturn(Result.success(categories))

        val viewModel = ShopSelectionViewModel(application, menuRepository, resourceResolver)
        
        viewModel.addItem("item1")
        
        assertEquals(BigDecimal("5.00"), viewModel.totalAmount.value)
        assertEquals(1, viewModel.cartItems.value.size)
        assertEquals(1, viewModel.cartItems.value[0].quantity)

        viewModel.addItem("item1")
        assertEquals(BigDecimal("10.00"), viewModel.totalAmount.value)
        assertEquals(2, viewModel.cartItems.value[0].quantity)
    }

    @Test
    fun `removeItem decreases cart quantity and total amount`() = runTest {
        val item1 = MenuItem("item1", "Item 1 Key", BigDecimal("5.00"))
        val categories = listOf(
            MenuCategory("cat1", "Category 1 Key", "Category 1", listOf(
                SubCategory("sub1", "Sub 1 Key", "Sub 1", listOf(item1))
            ))
        )
        whenever(menuRepository.loadAllMenuCategories()).thenReturn(Result.success(categories))

        val viewModel = ShopSelectionViewModel(application, menuRepository, resourceResolver)
        
        viewModel.addItem("item1")
        viewModel.addItem("item1")
        viewModel.removeItem("item1")
        
        assertEquals(BigDecimal("5.00"), viewModel.totalAmount.value)
        assertEquals(1, viewModel.cartItems.value[0].quantity)

        viewModel.removeItem("item1")
        assertEquals(BigDecimal.ZERO, viewModel.totalAmount.value)
        assertEquals(0, viewModel.cartItems.value.size)
    }

    @Test
    fun `clearCart resets cart and total`() = runTest {
        val item1 = MenuItem("item1", "Item 1 Key", BigDecimal("5.00"))
        val categories = listOf(
            MenuCategory("cat1", "Category 1 Key", "Category 1", listOf(
                SubCategory("sub1", "Sub 1 Key", "Sub 1", listOf(item1))
            ))
        )
        whenever(menuRepository.loadAllMenuCategories()).thenReturn(Result.success(categories))

        val viewModel = ShopSelectionViewModel(application, menuRepository, resourceResolver)
        
        viewModel.addItem("item1")
        viewModel.clearCart()
        
        assertEquals(BigDecimal.ZERO, viewModel.totalAmount.value)
        assertEquals(0, viewModel.cartItems.value.size)
    }
}
