package com.kidsnfcplaypos.ui.calculator

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.math.BigDecimal
import java.math.MathContext

sealed class CalculatorOperation(val symbol: String) {
    object Add : CalculatorOperation("+")
    object Subtract : CalculatorOperation("-")
    object Multiply : CalculatorOperation("x")
    object Divide : CalculatorOperation("/")
    object Percent : CalculatorOperation("%") // Special handling for percentage
}

class CalculatorViewModel : ViewModel() {

    private val MAX_HISTORY_SIZE = 3
    private val DEFAULT_DISPLAY_VALUE = "0"
    private val MATH_CONTEXT = MathContext(10) // Define precision for BigDecimal operations

    // UI State
    private val _currentInput = MutableStateFlow(DEFAULT_DISPLAY_VALUE)
    val currentInput: StateFlow<String> = _currentInput

    private val _history = MutableStateFlow(List(MAX_HISTORY_SIZE) { "" })
    val history: StateFlow<List<String>> = _history

    private val _isPayButtonEnabled = MutableStateFlow(false)
    val isPayButtonEnabled: StateFlow<Boolean> = _isPayButtonEnabled

    // Internal State
    private var operand1: BigDecimal? = null
    private var operand2: BigDecimal? = null
    private var currentOperation: CalculatorOperation? = null
    private var isNewCalculation = true // Flag to clear input after an operation or equals
    private var hasDecimal = false

    // Last result after an equals operation, used for subsequent calculations
    private var lastResult: BigDecimal? = null

    init {
        updatePayButtonState()
    }

    fun onDigit(digit: Char) {
        if (isNewCalculation) {
            _currentInput.value = DEFAULT_DISPLAY_VALUE
            isNewCalculation = false
            hasDecimal = false
        }
        if (_currentInput.value == DEFAULT_DISPLAY_VALUE && digit != '0') {
            _currentInput.value = digit.toString()
        } else if (_currentInput.value != DEFAULT_DISPLAY_VALUE || digit == '0') {
            // Prevent too many digits for a cleaner display, adjust as needed
            if (_currentInput.value.length < 15) {
                _currentInput.value += digit
            }
        }
        updatePayButtonState()
    }

    fun onDecimal() {
        if (isNewCalculation) {
            _currentInput.value = DEFAULT_DISPLAY_VALUE
            isNewCalculation = false
            hasDecimal = false
        }
        if (!hasDecimal) {
            _currentInput.value += "."
            hasDecimal = true
        }
        updatePayButtonState()
    }

    fun onOperation(operation: CalculatorOperation) {
        val currentNumber = _currentInput.value.toBigDecimalOrNull() ?: BigDecimal.ZERO

        if (operand1 == null || isNewCalculation) {
            operand1 = lastResult ?: currentNumber
            isNewCalculation = false
        } else {
            operand2 = currentNumber
            // Evaluate previous operation if an operation is chained
            evaluateCurrentOperation()
        }
        currentOperation = operation
        addOperationToHistory(formatNumber(operand1 ?: BigDecimal.ZERO) + " " + operation.symbol)
        _currentInput.value = DEFAULT_DISPLAY_VALUE // Clear input for next operand
        hasDecimal = false
        updatePayButtonState()
    }

    fun onEquals() {
        if (currentOperation == null) return

        val currentNumber = _currentInput.value.toBigDecimalOrNull() ?: BigDecimal.ZERO
        operand2 = currentNumber

        val result = performCalculation(operand1, operand2, currentOperation)
        if (result != null) {
            lastResult = result
            addOperationToHistory(formatNumber(operand1 ?: BigDecimal.ZERO) + " " + currentOperation!!.symbol + " " + formatNumber(operand2 ?: BigDecimal.ZERO) + " = " + formatNumber(result))
            _currentInput.value = formatNumber(result)
            operand1 = result
        }

        resetOperationState()
        updatePayButtonState()
    }

    fun onClear() {
        _currentInput.value = DEFAULT_DISPLAY_VALUE
        _history.value = List(MAX_HISTORY_SIZE) { "" }
        resetOperationState()
        lastResult = null
        updatePayButtonState()
    }

    fun onNegate() {
        val currentNumber = _currentInput.value.toBigDecimalOrNull()
        if (currentNumber != null && currentNumber != BigDecimal.ZERO) {
            _currentInput.value = formatNumber(currentNumber.negate())
            isNewCalculation = false // Negating doesn't start a new calculation
        }
        updatePayButtonState()
    }

    fun onPercent() {
        val currentNumber = _currentInput.value.toBigDecimalOrNull()
        if (currentNumber != null && currentNumber != BigDecimal.ZERO) {
            _currentInput.value = formatNumber(currentNumber.divide(BigDecimal(100), MATH_CONTEXT))
            isNewCalculation = false
        }
        updatePayButtonState()
    }

    private fun evaluateCurrentOperation() {
        if (operand1 != null && operand2 != null && currentOperation != null) {
            val result = performCalculation(operand1, operand2, currentOperation)
            if (result != null) {
                operand1 = result
                addOperationToHistory(formatNumber(result) + " " + currentOperation!!.symbol) // Only show partial for chained ops
            }
        }
    }

    private fun performCalculation(op1: BigDecimal?, op2: BigDecimal?, operation: CalculatorOperation?): BigDecimal? {
        if (op1 == null || op2 == null || operation == null) return null

        return when (operation) {
            CalculatorOperation.Add -> op1.add(op2, MATH_CONTEXT)
            CalculatorOperation.Subtract -> op1.subtract(op2, MATH_CONTEXT)
            CalculatorOperation.Multiply -> op1.multiply(op2, MATH_CONTEXT)
            CalculatorOperation.Divide -> {
                if (op2 == BigDecimal.ZERO) {
                    // Handle division by zero
                    return BigDecimal.ZERO // Or throw an error/display "Error"
                }
                op1.divide(op2, MATH_CONTEXT)
            }
            CalculatorOperation.Percent -> op1 // Percent is handled on input
        }
    }

    private fun addOperationToHistory(op: String) {
        val updatedHistory = _history.value.toMutableList()
        if (op.isNotBlank()) {
            if (updatedHistory.size >= MAX_HISTORY_SIZE) {
                updatedHistory.removeAt(0) // Remove oldest
            }
            updatedHistory.add(op)
            _history.value = updatedHistory
        }
    }

    private fun resetOperationState() {
        operand1 = null
        operand2 = null
        currentOperation = null
        isNewCalculation = true
        hasDecimal = false
    }

    private fun updatePayButtonState() {
        _isPayButtonEnabled.value = (_currentInput.value.toBigDecimalOrNull() ?: BigDecimal.ZERO) > BigDecimal.ZERO
    }

    fun getCurrentCalculatedAmount(): BigDecimal {
        return _currentInput.value.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }

    private fun formatNumber(number: BigDecimal): String {
        // Always show two decimal places for monetary display, as it's clearer for kids.
        val symbols = DecimalFormatSymbols(Locale.getDefault())
        val formatter = DecimalFormat("0.00", symbols)
        return formatter.format(number)
    }

}
