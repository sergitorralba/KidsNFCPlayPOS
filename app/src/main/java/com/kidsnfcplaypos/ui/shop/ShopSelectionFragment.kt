package com.kidsnfcplaypos.ui.shop

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.kidsnfcplaypos.databinding.FragmentShopSelectionBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ShopSelectionFragment : Fragment() {

    private var _binding: FragmentShopSelectionBinding? = null
    private val binding get() = _binding!!

    // Use the custom factory to instantiate the ViewModel
    private val viewModel: ShopSelectionViewModel by viewModels {
        ShopSelectionViewModel.Factory(requireActivity().application)
    }

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

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        shopCategoryAdapter = ShopCategoryAdapter { category ->
            // Handle item click: navigate to the detailed menu for this category
            Log.d("ShopSelectionFragment", "Clicked on ${category.name} (ID: ${category.id})")
            // TODO: Implement actual navigation to a detailed menu fragment
        }

        binding.recyclerViewShopCategories.apply {
            layoutManager = GridLayoutManager(context, 2) // 2 columns for "Windows Phone tiles" look
            adapter = shopCategoryAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.menuCategories.collectLatest { categories ->
                shopCategoryAdapter.submitList(categories)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collectLatest { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.errorMessage.collectLatest { message ->
                // Display error message (e.g., via Toast or a TextView)
                if (!message.isNullOrBlank()) {
                    Log.e("ShopSelectionFragment", message)
                    // TODO: Show a more user-friendly error message
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
