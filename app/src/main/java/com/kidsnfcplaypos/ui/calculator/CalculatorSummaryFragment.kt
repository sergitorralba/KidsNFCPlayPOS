package com.kidsnfcplaypos.ui.calculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kidsnfcplaypos.databinding.FragmentCalculatorSummaryBinding
import com.kidsnfcplaypos.ui.payment.PaymentEvent
import com.kidsnfcplaypos.ui.payment.PaymentViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

class CalculatorSummaryFragment : Fragment() {

    private var _binding: FragmentCalculatorSummaryBinding? = null
    private val binding get() = _binding!!

    // Share the ViewModel with the CalculatorFragment
    private val viewModel: CalculatorViewModel by activityViewModels()

    private val paymentViewModel: PaymentViewModel by activityViewModels()

    private val currencyFormatter: NumberFormat by lazy {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = java.util.Currency.getInstance("EUR")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalculatorSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupButtons()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        val adapter = TapeAdapter()
        binding.recyclerViewTape.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = adapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.tape.collectLatest { tape: List<TapeEntry> ->
                adapter.submitList(tape)
            }
        }
    }

    private fun setupButtons() {
        binding.buttonCancel.setOnClickListener {
            viewModel.resetAll()
            findNavController().popBackStack()
        }

        binding.buttonPay.setOnClickListener {
            val finalAmount = viewModel.grandTotal.value
            if (finalAmount > BigDecimal.ZERO) {
                val action = CalculatorSummaryFragmentDirections
                    .actionCalculatorSummaryFragmentToPaymentSimulationFragment(
                        finalAmount.toPlainString()
                    )
                findNavController().navigate(action)
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.grandTotal.collectLatest { total: BigDecimal ->
                binding.textTotalAmount.text = currencyFormatter.format(total)
            }
        }

        // Listen for successful payment to reset the calculator
        viewLifecycleOwner.lifecycleScope.launch {
            paymentViewModel.eventFlow.collectLatest { event ->
                // Handled in CalculatorFragment via shouldResetPOS
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- Simple Tape Adapter ---
    private inner class TapeAdapter : RecyclerView.Adapter<TapeAdapter.ViewHolder>() {
        private var tape: List<TapeEntry> = emptyList()

        fun submitList(newTape: List<TapeEntry>) {
            tape = newTape
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(com.kidsnfcplaypos.R.layout.item_tape_entry, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val entry = tape[position]
            holder.expression.text = entry.expression
            holder.result.text = entry.formattedResult
        }

        override fun getItemCount() = tape.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val expression: TextView = view.findViewById(com.kidsnfcplaypos.R.id.textExpression)
            val result: TextView = view.findViewById(com.kidsnfcplaypos.R.id.textResult)
        }
    }
}
