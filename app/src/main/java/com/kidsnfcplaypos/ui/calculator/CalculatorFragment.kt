package com.kidsnfcplaypos.ui.calculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.kidsnfcplaypos.databinding.FragmentCalculatorBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal

class CalculatorFragment : Fragment() {

    private var _binding: FragmentCalculatorBinding? = null
    private val binding get() = _binding!!

    // Shared ViewModel with SummaryFragment
    private val viewModel: CalculatorViewModel by activityViewModels()

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

        // Tape Operators (Add or Subtract to the tape)
        binding.btnPlus.setOnClickListener { viewModel.onTapeOperator("+") }
        binding.btnSubtract.setOnClickListener { viewModel.onTapeOperator("-") }

        // Expression Operators (Building internal expression like x or /)
        binding.btnMultiply.setOnClickListener { viewModel.onExpressionOperator("x") }
        binding.btnDivide.setOnClickListener { viewModel.onExpressionOperator("/") }
        binding.btnPercent.setOnClickListener { viewModel.onPercent() }

        // Other buttons
        binding.btnClear.setOnClickListener { viewModel.onClear() }
        binding.btnDecimal.setOnClickListener { viewModel.onDecimal() }
        
        // Total button navigates to Summary
        binding.btnPay.setOnClickListener {
            val action = CalculatorFragmentDirections.actionCalculatorFragmentToCalculatorSummaryFragment()
            findNavController().navigate(action)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentDisplay.collectLatest { displayValue ->
                binding.currentInput.text = displayValue
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeExpression.collectLatest { expression ->
                binding.textActiveOperation.text = expression
                binding.textActiveOperation.visibility = if (expression.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.grandTotal.collectLatest { total ->
                // The big TOTAL button is enabled if we have anything on the tape
                // or a current total that is not zero
                binding.btnPay.isEnabled = total != BigDecimal.ZERO || viewModel.tape.value.isNotEmpty()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
