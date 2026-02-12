package com.kidsnfcplaypos.ui.directinput

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class DirectInputViewModel : ViewModel() {

    private val _inputDigits = MutableStateFlow(MutableList(5) { '_' }) // 3 for integer, 2 for decimal
    private var currentDigitIndex = 0

    private val _formattedAmountDisplay = MutableStateFlow("___.__")
    val formattedAmountDisplay: StateFlow<String> = _formattedAmountDisplay

    private val _currentAmount = MutableStateFlow(BigDecimal.ZERO)
    val currentAmount: StateFlow<BigDecimal> = _currentAmount

    private val _isPaymentEnabled = MutableStateFlow(false)
    val isPaymentEnabled: StateFlow<Boolean> = _isPaymentEnabled

    init {
        updateDisplayAndAmountState()
    }

    fun onDigitPressed(digit: Char) {
        if (currentDigitIndex < _inputDigits.value.size) {
            val updatedList = _inputDigits.value.toMutableList()
            updatedList[currentDigitIndex] = digit
            _inputDigits.value = updatedList
            currentDigitIndex++
            updateDisplayAndAmountState()
        }
    }

    fun onDeletePressed() {
        if (currentDigitIndex > 0) {
            currentDigitIndex--
            val updatedList = _inputDigits.value.toMutableList()
            updatedList[currentDigitIndex] = '_'
            _inputDigits.value = updatedList
            updateDisplayAndAmountState()
        }
    }

    private fun updateDisplayAndAmountState() {
        val integerPart = _inputDigits.value.subList(0, 3).joinToString("")
        val decimalPart = _inputDigits.value.subList(3, 5).joinToString("")
        _formattedAmountDisplay.value = "$integerPart.$decimalPart"

        // Calculate actual amount
        val rawAmountString = _inputDigits.value.joinToString("").replace('_', '0')
        _currentAmount.value = try {
            BigDecimal(rawAmountString).movePointLeft(2) // _ _ _ . _ _ means 2 decimal places
        } catch (e: NumberFormatException) {
            BigDecimal.ZERO
        }

        // Validate payment enabled state
        _isPaymentEnabled.value = (currentDigitIndex > 0 && _currentAmount.value > BigDecimal.ZERO)
    }

    fun resetInput() {
        _inputDigits.value = MutableList(5) { '_' }
        currentDigitIndex = 0
        updateDisplayAndAmountState()
    }
}
