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
import kotlinx.coroutines.launch
import java.math.BigDecimal

sealed class PaymentUiState {
    object Idle : PaymentUiState() // Waiting for NFC or amount
    data class Processing(val remainingTimeSeconds: Int) : PaymentUiState() // 10-second simulation
    object Success : PaymentUiState()
    object Failure : PaymentUiState()
    object NfcTapDetected : PaymentUiState() // Intermediate state after first NFC tap
}

sealed class PaymentEvent {
    object TriggerNfcFeedback : PaymentEvent() // One-time event for vibration/beep
    object PaymentCompleted : PaymentEvent() // One-time event to navigate away
}

class PaymentViewModel : ViewModel() {

    private val PAYMENT_SIMULATION_DURATION_SECONDS = 10
    private val FAILURE_SCREEN_DURATION_SECONDS = 5
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
                    forcePaymentFailure()
                    return
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
            // If not forced failure, then it's a success
            if (_uiState.value !is PaymentUiState.Failure) {
                _uiState.value = PaymentUiState.Success
            }
        }
    }

    private fun forcePaymentFailure() {
        simulationJob?.cancel() // Stop ongoing simulation
        _uiState.value = PaymentUiState.Failure
        viewModelScope.launch {
            delay(FAILURE_SCREEN_DURATION_SECONDS * 1000L)
            _eventFlow.emit(PaymentEvent.PaymentCompleted) // Signal to navigate away
        }
    }

    fun onPaymentResultAcknowledged() {
        // Called when user clicks "Accept" after success or after failure screen timeout
        viewModelScope.launch { _eventFlow.emit(PaymentEvent.PaymentCompleted) }
    }
}
