package com.kidsnfcplaypos.ui.payment

import android.nfc.NfcAdapter
import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal

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

class PaymentViewModel : ViewModel() {

    private val PAYMENT_SIMULATION_DURATION_SECONDS = 7
    private val TAP_TIMEOUT_MS = 500L // Time window for consecutive taps

    // UI State exposed to Fragment
    private val _uiState = MutableStateFlow<PaymentUiState>(PaymentUiState.Idle)
    val uiState: StateFlow<PaymentUiState> = _uiState

    // One-time events
    private val _eventFlow = MutableSharedFlow<PaymentEvent>()
    val eventFlow: SharedFlow<PaymentEvent> = _eventFlow

    // --- Reset Logic ---
    // This state persists until consumed by the Shop or Calculator screens
    private val _shouldResetPOS = MutableStateFlow(false)
    val shouldResetPOS: StateFlow<Boolean> = _shouldResetPOS.asStateFlow()

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
                    isForcedFailure = true // Mark for failure but don't stop timer
                }
            } else {
                tapCount = 1
            }
            lastTapTimeMillis = currentTime
        }
    }

    private fun startPaymentSimulation() {
        simulationJob?.cancel() // Ensure no duplicate jobs
        simulationJob = viewModelScope.launch {
            _uiState.value = PaymentUiState.Processing(PAYMENT_SIMULATION_DURATION_SECONDS)
            var remainingTime = PAYMENT_SIMULATION_DURATION_SECONDS
            while (remainingTime > 0) {
                delay(1000)
                remainingTime--
                _uiState.value = PaymentUiState.Processing(remainingTime)
            }
            
            // Simulation finished, decide based on whether it was forced to fail
            if (isForcedFailure) {
                _uiState.value = PaymentUiState.Failure
            } else {
                _uiState.value = PaymentUiState.Success
            }
        }
    }

    fun onPaymentResultAcknowledged() {
        // Called when user clicks "Accept" after success OR clicks something on failure
        viewModelScope.launch {
            if (_uiState.value is PaymentUiState.Success) {
                _shouldResetPOS.value = true
            }
            // PaymentCompleted is emitted for both success and failure to navigate back
            _eventFlow.emit(PaymentEvent.PaymentCompleted)
        }
    }

    fun posResetConsumed() {
        _shouldResetPOS.value = false
    }
}
