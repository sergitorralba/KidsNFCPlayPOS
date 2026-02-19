package com.kidsnfcplaypos.ui.payment

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

import com.kidsnfcplaypos.ui.shop.ShopSelectionViewModel
import com.kidsnfcplaypos.ui.calculator.CalculatorViewModel
import com.kidsnfcplaypos.ui.directinput.DirectInputViewModel

sealed class PaymentUiState {
    object Idle : PaymentUiState() // Waiting for NFC or amount
    data class Processing(val remainingTimeSeconds: Int) : PaymentUiState() // 7-second simulation
    object Success : PaymentUiState()
    object Failure : PaymentUiState()
    object NfcTapDetected : PaymentUiState() // Intermediate state after first NFC tap
}

sealed class PaymentEvent {
    object TriggerNfcFeedback : PaymentEvent() // One-time event for vibration/beep
    object PaymentCompleted : PaymentEvent() // One-time event to navigate away
}

class PaymentViewModel(
    private val shopViewModel: ShopSelectionViewModel,
    private val calculatorViewModel: CalculatorViewModel,
    private val directInputViewModel: DirectInputViewModel
) : ViewModel() {

    private val PAYMENT_SIMULATION_DURATION_SECONDS = 7
    private val TAP_TIMEOUT_MS = 500L // Time window for consecutive taps

    // UI State exposed to Fragment
    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val uiState: StateFlow<PaymentUiState> = _uiState

    // One-time events
    private val _eventFlow = MutableSharedFlow<PaymentEvent>()
    val eventFlow: SharedFlow<PaymentEvent> = _eventFlow

    // Internal state for simulation
    private var paymentAmount: BigDecimal = BigDecimal.ZERO
    private var simulationJob: Job? = null
    private var nfcDetected = false
    private var tapCount = 0
    private var lastTapTimeMillis = 0L
    private var isForcedFailure = false

    // For NFC ReaderMode (to be handled in Fragment due to NfcAdapter dependency)
    val readerFlags = NfcAdapter.FLAG_READER_NFC_A or
                      NfcAdapter.FLAG_READER_NFC_B or
                      NfcAdapter.FLAG_READER_NFC_F or
                      NfcAdapter.FLAG_READER_NFC_V or
                      NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

    fun initiatePayment(amount: BigDecimal) {
        Log.d("PaymentVM", "Initiating payment for: $amount")
        paymentAmount = amount
        _uiState.value = PaymentUiState.Idle
        nfcDetected = false
        tapCount = 0
        lastTapTimeMillis = 0L
        isForcedFailure = false
        simulationJob?.cancel() // Cancel any ongoing simulation
    }

    fun onNfcTagDetected(tag: Tag) {
        if (!nfcDetected) {
            nfcDetected = true
            viewModelScope.launch { _eventFlow.emit(PaymentEvent.TriggerNfcFeedback) }
            _uiState.value = PaymentUiState.NfcTapDetected
            startPaymentSimulation()
        }
    }

    fun onSecretTap() {
        if (_uiState.value is PaymentUiState.Processing) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTapTimeMillis < TAP_TIMEOUT_MS) {
                tapCount++
                if (tapCount >= 3) {
                    Log.d("PaymentVM", "Forced failure triggered!")
                    isForcedFailure = true 
                }
            } else {
                tapCount = 1
            }
            lastTapTimeMillis = currentTime
        }
    }

    private fun startPaymentSimulation() {
        simulationJob?.cancel() 
        simulationJob = viewModelScope.launch {
            Log.d("PaymentVM", "Starting simulation...")
            _uiState.value = PaymentUiState.Processing(PAYMENT_SIMULATION_DURATION_SECONDS)
            var remainingTime = PAYMENT_SIMULATION_DURATION_SECONDS
            while (remainingTime > 0) {
                delay(1000)
                remainingTime--
                _uiState.value = PaymentUiState.Processing(remainingTime)
            }
            
            if (isForcedFailure) {
                Log.d("PaymentVM", "Simulation finished with FAILURE")
                _uiState.value = PaymentUiState.Failure
            } else {
                Log.d("PaymentVM", "Simulation finished with SUCCESS. Resetting values.")
                _uiState.value = PaymentUiState.Success
                
                // RESET EVERYTHING IMMEDIATELY ON SUCCESS
                // This ensures that even if the user hits the back button or kills the app,
                // the state is already cleared.
                shopViewModel.clearCart()
                calculatorViewModel.resetAll()
                directInputViewModel.resetInput()
            }
        }
    }

    fun onPaymentResultAcknowledged() {
        Log.d("PaymentVM", "Payment result acknowledged by user.")
        viewModelScope.launch {
            _eventFlow.emit(PaymentEvent.PaymentCompleted)
        }
    }

    class Factory(
        private val shopViewModel: ShopSelectionViewModel,
        private val calculatorViewModel: CalculatorViewModel,
        private val directInputViewModel: DirectInputViewModel
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PaymentViewModel(shopViewModel, calculatorViewModel, directInputViewModel) as T
        }
    }
}
