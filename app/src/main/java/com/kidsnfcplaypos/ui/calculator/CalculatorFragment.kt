package com.kidsnfcplaypos.ui.calculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kidsnfcplaypos.databinding.FragmentCalculatorBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CalculatorFragment : Fragment() {

    private var _binding: FragmentCalculatorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupKeypadListeners()
        observeViewModel()
    }

    private fun setupKeypadListeners() {
        // Number buttons
        listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9
        ).forEach { button ->
            button.setOnClickListener { viewModel.onDigit(button.text.first()) }
        }

        // Operation buttons
        binding.btnPlus.setOnClickListener { viewModel.onOperation(CalculatorOperation.Add) }
        binding.btnSubtract.setOnClickListener { viewModel.onOperation(CalculatorOperation.Subtract) }
        binding.btnMultiply.setOnClickListener { viewModel.onOperation(CalculatorOperation.Multiply) }
        binding.btnDivide.setOnClickListener { viewModel.onOperation(CalculatorOperation.Divide) }
        binding.btnPercent.setOnClickListener { viewModel.onPercent() }

        // Other buttons
        binding.btnClear.setOnClickListener { viewModel.onClear() }
        binding.btnDecimal.setOnClickListener { viewModel.onDecimal() }
        binding.btnNegate.setOnClickListener { viewModel.onNegate() }
        binding.btnEquals.setOnClickListener { viewModel.onEquals() }

        // Pay button
        binding.btnPay.setOnClickListener {
            val finalAmount = viewModel.getCurrentCalculatedAmount()
            val action = CalculatorFragmentDirections.actionCalculatorFragmentToPaymentSimulationFragment(
                finalAmount.toPlainString()
            )
            findNavController().navigate(action)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentInput.collectLatest {
                binding.currentInput.text = it
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.history.collectLatest { historyList ->
                binding.historyLine1.text = if (historyList.size > 0) historyList[historyList.size - 1] else ""
                binding.historyLine2.text = if (historyList.size > 1) historyList[historyList.size - 2] else ""
                binding.historyLine3.text = if (historyList.size > 2) historyList[historyList.size - 3] else ""
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isPayButtonEnabled.collectLatest { isEnabled ->
                binding.btnPay.isEnabled = isEnabled
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
