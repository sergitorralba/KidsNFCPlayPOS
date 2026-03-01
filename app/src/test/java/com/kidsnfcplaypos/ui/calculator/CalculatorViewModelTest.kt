package com.kidsnfcplaypos.ui.calculator

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class CalculatorViewModelTest {

    private lateinit var viewModel: CalculatorViewModel

    @Before
    fun setup() {
        java.util.Locale.setDefault(java.util.Locale.US)
        viewModel = CalculatorViewModel()
    }

    @Test
    fun `onDigit updates currentDisplay correctly`() {
        viewModel.onDigit('5')
        viewModel.onDigit('0')
        viewModel.onDigit('0')
        assertEquals("5.00", viewModel.currentDisplay.value)
    }

    @Test
    fun `onDigit respects length limit of 9`() {
        repeat(10) { viewModel.onDigit('1') }
        // 9 ones -> 1111111.11
        assertEquals("1111111.11", viewModel.currentDisplay.value)
    }

    @Test
    fun `onTapeOperator with plus adds to grandTotal`() {
        viewModel.onDigit('5')
        viewModel.onDigit('0')
        viewModel.onDigit('0') // 5.00
        viewModel.onTapeOperator("+")

        assertBigDecimalEquals(BigDecimal("5.00"), viewModel.grandTotal.value)
        assertEquals(1, viewModel.tape.value.size)
        assertEquals("+ 5.00", viewModel.tape.value[0].expression)
    }

    @Test
    fun `onTapeOperator with minus subtracts from grandTotal`() {
        viewModel.onDigit('1')
        viewModel.onDigit('0')
        viewModel.onDigit('0')
        viewModel.onDigit('0') // 10.00
        viewModel.onTapeOperator("+")

        viewModel.onDigit('2')
        viewModel.onDigit('0')
        viewModel.onDigit('0') // 2.00
        viewModel.onTapeOperator("-")

        assertBigDecimalEquals(BigDecimal("8.00"), viewModel.grandTotal.value)
        assertEquals(2, viewModel.tape.value.size)
    }

    @Test
    fun `complex expression multiplication works correctly`() {
        viewModel.onDigit('2')
        viewModel.onDigit('0')
        viewModel.onDigit('0') // 2.00
        viewModel.onExpressionOperator("x")
        
        viewModel.onDigit('3')
        viewModel.onDigit('0')
        viewModel.onDigit('0') // 3.00
        viewModel.onTapeOperator("+")

        // 2.00 * 3.00 = 6.00
        assertBigDecimalEquals(BigDecimal("6.00"), viewModel.grandTotal.value)
    }

    @Test
    fun `percentage calculation works correctly`() {
        // First add some money to the total
        viewModel.onDigit('1')
        viewModel.onDigit('0')
        viewModel.onDigit('0')
        viewModel.onDigit('0') // 10.00
        viewModel.onTapeOperator("+")

        // Now calculate 50%
        viewModel.onDigit('5')
        viewModel.onDigit('0')
        viewModel.onDigit('0') // 5.00 interpreted as 5.00%
        viewModel.onPercent()
        viewModel.onTapeOperator("+")

        // 10.00 + 50% of 10.00 = 10.00 + 0.50 = 10.50
        assertBigDecimalEquals(BigDecimal("10.50"), viewModel.grandTotal.value)
    }

    @Test
    fun `onClear resets current input`() {
        viewModel.onDigit('5')
        viewModel.onClear()
        assertEquals("0.00", viewModel.currentDisplay.value)
        assertBigDecimalEquals(BigDecimal.ZERO, viewModel.grandTotal.value)
    }

    @Test
    fun `onClear twice resets everything`() {
        viewModel.onDigit('5')
        viewModel.onTapeOperator("+")
        viewModel.onClear() 
        viewModel.onClear()
        assertEquals(0, viewModel.tape.value.size)
        assertBigDecimalEquals(BigDecimal.ZERO, viewModel.grandTotal.value)
    }

    private fun assertBigDecimalEquals(expected: BigDecimal, actual: BigDecimal) {
        assertEquals(0, expected.compareTo(actual))
    }
}
