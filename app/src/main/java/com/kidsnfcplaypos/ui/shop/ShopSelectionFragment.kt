package com.kidsnfcplaypos.ui.shop

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.kidsnfcplaypos.R
import com.kidsnfcplaypos.databinding.FragmentShopSelectionBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import java.math.BigDecimal

import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kidsnfcplaypos.ui.payment.PaymentEvent
import com.kidsnfcplaypos.ui.payment.PaymentViewModel
import com.kidsnfcplaypos.util.LocaleManager

class ShopSelectionFragment : Fragment() {

    private var _binding: FragmentShopSelectionBinding? = null
    private val binding get() = _binding!!

    // Use the custom factory to instantiate the ViewModel
    private val viewModel: ShopSelectionViewModel by activityViewModels {
        ShopSelectionViewModel.Factory(requireActivity().application)
    }

    private val paymentViewModel: PaymentViewModel by activityViewModels()

    private lateinit var shopCategoryAdapter: ShopCategoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShopSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.refreshLocale() // Ensure strings are fresh (e.g., after locale change)
        setupMenu()
        setupRecyclerView()
        setupPaymentButton()
        observeViewModel()
    }

    private fun setupPaymentButton() {
        binding.fabCheckout.setOnClickListener {
            val totalAmount = viewModel.totalAmount.value
            if (totalAmount > BigDecimal.ZERO) {
                val action = ShopSelectionFragmentDirections
                    .actionShopSelectionFragmentToCartSummaryFragment()
                findNavController().navigate(action)
            }
        }
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.shop_selection_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_switch_menu -> {
                        MenuSelectionBottomSheet().show(childFragmentManager, MenuSelectionBottomSheet.TAG)
                        true
                    }
                    R.id.action_change_language -> {
                        showLanguageSelectionDialog()
                        true
                    }
                    R.id.action_settings -> {
                        findNavController().navigate(R.id.action_shopSelectionFragment_to_settingsFragment)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun showLanguageSelectionDialog() {
        val languages = arrayOf("English", "Español", "Nederlands", "Català")
        val localeTags = arrayOf("en", "es", "nl", "ca")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.select_language_title))
            .setItems(languages) { _, which ->
                val selectedLocaleTag = localeTags[which]
                LocaleManager.saveLocale(requireContext(), selectedLocaleTag)
                activity?.recreate()
            }
            .show()
    }

    private fun setupRecyclerView() {
        // Instantiate the adapter, passing lambdas that call the ViewModel
        shopCategoryAdapter = ShopCategoryAdapter(
            onAddItem = { menuItem ->
                viewModel.addItem(menuItem.id)
            },
            onRemoveItem = { menuItem ->
                viewModel.removeItem(menuItem.id)
            }
        )

        binding.recyclerViewShopCategories.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = shopCategoryAdapter
        }
    }

    private fun observeViewModel() {
        // This coroutine will handle the overall screen state (loading and errors)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { uiState ->
                binding.progressBar.visibility = if (uiState.isLoading) View.VISIBLE else View.GONE

                if (!uiState.error.isNullOrBlank()) {
                    Log.e("ShopSelectionFragment", "Error: ${uiState.error}")
                    // TODO: Show a more user-friendly error message
                }
            }
        }

        // This new, separate coroutine will handle submitting the list to the adapter
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.shopListItems.collectLatest { shopList ->
                shopCategoryAdapter.submitList(shopList)
            }
        }

        // This new coroutine will handle the payment button visibility
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalAmount.collectLatest { total ->
                binding.fabCheckout.visibility = if (total > BigDecimal.ZERO) View.VISIBLE else View.GONE
            }
        }

        // Listen for successful payment to clear the cart
        viewLifecycleOwner.lifecycleScope.launch {
            paymentViewModel.eventFlow.collectLatest { event ->
                if (event == PaymentEvent.PaymentSuccess) {
                    viewModel.clearCart()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
