package com.kidsnfcplaypos.ui.directinput

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

class DirectInputViewModel : ViewModel() {

    private val _digitString = MutableStateFlow("")
    private val maxLength = 9 // Max digits, e.g., for 9,999,999.99

    // Derived state for the actual BigDecimal amount
    val currentAmount: StateFlow<BigDecimal> = _digitString.map { digits ->
        if (digits.isEmpty()) {
            BigDecimal.ZERO
        } else {
            BigDecimal(digits).movePointLeft(2)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BigDecimal.ZERO)

    // Derived state for the formatted display string
    val formattedAmountDisplay: StateFlow<String> = _digitString.map { digits ->
        val decimalAmount = if (digits.isEmpty()) {
            BigDecimal.ZERO
        } else {
            BigDecimal(digits).movePointLeft(2)
        }
        
        // Use standard NumberFormat for locale-aware decimal/thousand separators
        val formatter = NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        
        formatter.format(decimalAmount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0.00")

    // Derived state to enable/disable the payment button
    val isPaymentEnabled: StateFlow<Boolean> = currentAmount.map { it > BigDecimal.ZERO }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)


    fun onDigitPressed(digit: Char) {
        if (_digitString.value.length < maxLength) {
            _digitString.value += digit
        }
    }

    fun onDeletePressed() {
        _digitString.value = _digitString.value.dropLast(1)
    }

    fun resetInput() {
        _digitString.value = ""
    }
}
