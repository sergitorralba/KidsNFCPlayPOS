package com.kidsnfcplaypos.ui.calculator

import android.util.Log
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal
import java.math.MathContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class TapeEntry(
    val expression: String,
    val result: BigDecimal,
    val formattedResult: String
)

class CalculatorViewModel : ViewModel() {

    private val MATH_CONTEXT = MathContext(10)

    // --- State ---
    private val _rawDigits = MutableStateFlow("") 
    private val _activeExpression = MutableStateFlow("") 
    val activeExpression: StateFlow<String> = _activeExpression

    private val _currentDisplay = MutableStateFlow("0.00") 
    val currentDisplay: StateFlow<String> = _currentDisplay

    private val _tape = MutableStateFlow<List<TapeEntry>>(emptyList())
    val tape: StateFlow<List<TapeEntry>> = _tape

    private val _grandTotal = MutableStateFlow(BigDecimal.ZERO)
    val grandTotal: StateFlow<BigDecimal> = _grandTotal

    private val currencyFormatter = DecimalFormat.getCurrencyInstance(Locale.getDefault()).apply {
        currency = java.util.Currency.getInstance("EUR")
    }

    private val displayFormatter = DecimalFormat("0.00", DecimalFormatSymbols(Locale.getDefault()))

    fun onDigit(digit: Char) {
        if (_rawDigits.value.length < 9) {
            _rawDigits.value += digit
            updateDisplay()
        }
    }

    private fun updateDisplay() {
        val raw = _rawDigits.value
        val expr = _activeExpression.value
        
        _currentDisplay.value = when {
            expr.endsWith("% ") -> {
                // If we have "5.00 % ", show the 5.00%
                val percentValue = expr.split(" ")[0]
                "$percentValue%"
            }
            raw.isEmpty() -> "0.00"
            else -> {
                val decimal = BigDecimal(raw).movePointLeft(2)
                displayFormatter.format(decimal)
            }
        }
    }

    fun onDecimal() {
        // No-op for now in cash register mode
    }

    fun onExpressionOperator(symbol: String) {
        val currentAmount = getCurrentAmount()
        if (currentAmount != BigDecimal.ZERO || _rawDigits.value.isNotEmpty()) {
            _activeExpression.value = "${formatNumber(currentAmount)} $symbol "
            _rawDigits.value = "" 
            updateDisplay()
        }
    }

    fun onPercent() {
        // The user types the number first using X.XX logic, then hits %
        // Example: "5 0 0 %" -> currentAmount is 5.00, activeExpression becomes "5.00 % "
        if (_rawDigits.value.isNotEmpty()) {
            val currentAmount = getCurrentAmount()
            _activeExpression.value = "${formatNumber(currentAmount)} % "
            _rawDigits.value = ""
            updateDisplay()
        }
    }

    fun onTapeOperator(sign: String) {
        val rawInput = _rawDigits.value
        val expr = _activeExpression.value
        
        if (rawInput.isEmpty() && expr.isEmpty()) return

        try {
            val lineValue: BigDecimal
            val tapeExpression: String

            if (expr.contains("%")) {
                // Percentage logic: X% of Total
                val percentValue = expr.split(" ")[0].toBigDecimalOrNull() ?: BigDecimal.ZERO
                // lineValue = total * (percent / 100)
                lineValue = _grandTotal.value.multiply(percentValue).divide(BigDecimal(100), MATH_CONTEXT)
                tapeExpression = "$sign (${formatNumber(percentValue)}% of ${currencyFormatter.format(_grandTotal.value)})"
            } else if (expr.isNotEmpty()) {
                // Complex expression logic (x, /)
                val currentAmount = getCurrentAmount()
                lineValue = evaluateExpression(expr, currentAmount)
                tapeExpression = "$sign ($expr ${formatNumber(currentAmount)})"
            } else {
                // Simple number logic
                lineValue = getCurrentAmount()
                tapeExpression = "$sign ${formatNumber(lineValue)}"
            }

            val finalValue = if (sign == "-") lineValue.negate() else lineValue
            
            _tape.value += TapeEntry(tapeExpression, finalValue, formatNumber(finalValue))
            _grandTotal.value = _grandTotal.value.add(finalValue)
            
            _rawDigits.value = ""
            _activeExpression.value = ""
            updateDisplay()
        } catch (e: Exception) {
            Log.e("CalculatorVM", "Error in onTapeOperator", e)
            _rawDigits.value = ""
            _activeExpression.value = ""
            updateDisplay()
        }
    }

    private fun getCurrentAmount(): BigDecimal {
        return if (_rawDigits.value.isEmpty()) BigDecimal.ZERO else BigDecimal(_rawDigits.value).movePointLeft(2)
    }

    private fun evaluateExpression(expression: String, currentAmount: BigDecimal): BigDecimal {
        val parts = expression.trim().split(" ")
        if (parts.size < 2) return currentAmount

        val op1 = parts[0].toBigDecimalOrNull() ?: BigDecimal.ZERO
        val op = parts[1]
        
        return when (op) {
            "x" -> op1.multiply(currentAmount, MATH_CONTEXT)
            "/" -> if (currentAmount != BigDecimal.ZERO) op1.divide(currentAmount, MATH_CONTEXT) else op1
            else -> currentAmount
        }
    }

    fun onClear() {
        if (_rawDigits.value.isEmpty() && _activeExpression.value.isEmpty()) {
            resetAll()
        } else {
            _rawDigits.value = ""
            _activeExpression.value = ""
            updateDisplay()
        }
    }

    fun resetAll() {
        Log.d("CalculatorVM", "resetAll called! Current tape size: ${_tape.value.size}")
        _tape.value = emptyList()
        _grandTotal.value = BigDecimal.ZERO
        _rawDigits.value = ""
        _activeExpression.value = ""
        updateDisplay()
    }

    private fun formatNumber(number: BigDecimal): String {
        return displayFormatter.format(number)
    }
}
