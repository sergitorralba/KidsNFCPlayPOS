package com.kidsnfcplaypos.ui.directinput

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kidsnfcplaypos.databinding.FragmentDirectInputBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DirectInputFragment : Fragment() {

    private var _binding: FragmentDirectInputBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DirectInputViewModel by viewModels()

    private lateinit var digitTextViews: List<TextView>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDirectInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        digitTextViews = listOf(
            binding.intDigit1,
            binding.intDigit2,
            binding.intDigit3,
            binding.decDigit1,
            binding.decDigit2
        )

        setupKeypadListeners()
        observeViewModel()
    }

    private fun setupKeypadListeners() {
        // Number buttons
        val numberButtons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9
        )
        numberButtons.forEach { button ->
            button.setOnClickListener {
                viewModel.onDigitPressed(button.text.first())
            }
        }

        // Delete button
        binding.btnDelete.setOnClickListener {
            viewModel.onDeletePressed()
        }

        // Enter button
        binding.btnEnter.setOnClickListener {
            val amountToPay = viewModel.currentAmount.value
            val action = DirectInputFragmentDirections.actionDirectInputFragmentToPaymentSimulationFragment(
                amountToPay.toPlainString()
            )
            findNavController().navigate(action)
            viewModel.resetInput() // Reset input after navigation
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.formattedAmountDisplay.collectLatest { displayString ->
                // Update individual TextViews based on the displayString
                // Example: "___.__" -> ['_', '_', '_', '.', '_', '_']
                // We need to map this to the correct TextViews, skipping the fixed decimal point
                val chars = displayString.filter { it != '.' } // Remove decimal point from chars to map
                digitTextViews.forEachIndexed { index, textView ->
                    if (index < chars.size) {
                        textView.text = chars[index].toString()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isPaymentEnabled.collectLatest { isEnabled ->
                binding.btnEnter.isEnabled = isEnabled
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
