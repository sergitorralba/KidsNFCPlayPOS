package com.kidsnfcplaypos.ui.directinput

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
import com.kidsnfcplaypos.R
import com.kidsnfcplaypos.databinding.FragmentDirectInputBinding
import com.kidsnfcplaypos.ui.payment.PaymentViewModel
import com.kidsnfcplaypos.util.LanguageDialogHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DirectInputFragment : Fragment() {

    private var _binding: FragmentDirectInputBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DirectInputViewModel by viewModels()
    
    private val paymentViewModel: PaymentViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDirectInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupMenu()
        setupKeypadListeners()
        observeViewModel()
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.settings_only_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_change_language -> {
                        LanguageDialogHelper.showLanguageSelectionDialog(requireContext(), activity)
                        true
                    }
                    R.id.action_settings -> {
                        findNavController().navigate(R.id.settingsFragment)
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun setupKeypadListeners() {
        val numberButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9
        )
        numberButtons.forEach { button ->
            button.setOnClickListener {
                viewModel.onDigitPressed(button.text.first())
            }
        }

        binding.btnDelete.setOnClickListener {
            viewModel.onDeletePressed()
        }

        binding.btnEnter.setOnClickListener {
            val amountToPay = viewModel.currentAmount.value
            val action = DirectInputFragmentDirections.actionDirectInputFragmentToPaymentSimulationFragment(
                amountToPay.toPlainString()
            )
            findNavController().navigate(action)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.formattedAmountDisplay.collectLatest { displayString ->
                binding.amountDisplayText.text = displayString
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isPaymentEnabled.collectLatest { isEnabled ->
                binding.btnEnter.isEnabled = isEnabled
            }
        }

        // Listen for persistent reset state
        viewLifecycleOwner.lifecycleScope.launch {
            paymentViewModel.shouldResetPOS.collectLatest { shouldReset ->
                if (shouldReset) {
                    Log.d("DirectInputFragment", "Resetting input due to payment success")
                    viewModel.resetInput()
                    paymentViewModel.posResetConsumed()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
