package com.kidsnfcplaypos.ui.shop

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kidsnfcplaypos.databinding.BottomSheetMenuSelectionBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MenuSelectionBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetMenuSelectionBinding? = null
    private val binding get() = _binding!!

    // Share the ViewModel from the parent Fragment
    private val viewModel: ShopSelectionViewModel by activityViewModels {
        ShopSelectionViewModel.Factory(requireActivity().application)
    }
    
    private lateinit var menuAdapter: MenuSelectionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetMenuSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        menuAdapter = MenuSelectionAdapter { menuCategory ->
            viewModel.selectMenu(menuCategory.id)
            dismiss()
        }
        binding.recyclerViewMenus.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = menuAdapter
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { uiState ->
                menuAdapter.submitList(uiState.availableMenus)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "MenuSelectionBottomSheet"
    }
}
