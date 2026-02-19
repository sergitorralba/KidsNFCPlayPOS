package com.kidsnfcplaypos.ui.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.kidsnfcplaypos.databinding.FragmentCartSummaryBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

class CartSummaryFragment : Fragment() {

    private var _binding: FragmentCartSummaryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ShopSelectionViewModel by activityViewModels {
        ShopSelectionViewModel.Factory(requireActivity().application)
    }

    private lateinit var shopAdapter: ShopCategoryAdapter

    private val currencyFormatter: NumberFormat by lazy {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = java.util.Currency.getInstance("EUR")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupPaymentButton()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        shopAdapter = ShopCategoryAdapter(
            onAddItem = { menuItem -> viewModel.addItem(menuItem.id) },
            onRemoveItem = { menuItem -> viewModel.removeItem(menuItem.id) }
        )

        binding.recyclerViewCartItems.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = shopAdapter
        }
    }

    private fun setupPaymentButton() {
        binding.buttonConfirmPayment.setOnClickListener {
            val totalAmount = viewModel.totalAmount.value
            if (totalAmount > BigDecimal.ZERO) {
                val action = CartSummaryFragmentDirections
                    .actionCartSummaryFragmentToPaymentSimulationFragment(
                        totalAmount.toPlainString()
                    )
                findNavController().navigate(action)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.cartItems.collectLatest { items ->
                shopAdapter.submitList(items)
                // If the cart becomes empty, go back
                if (items.isEmpty()) {
                    findNavController().popBackStack()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalAmount.collectLatest { total ->
                binding.textTotalAmount.text = currencyFormatter.format(total)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
