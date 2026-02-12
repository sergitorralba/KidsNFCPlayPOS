package com.kidsnfcplaypos.ui.payment

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.kidsnfcplaypos.R
import com.kidsnfcplaypos.databinding.FragmentPaymentSimulationBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal

class PaymentSimulationFragment : Fragment(), NfcAdapter.ReaderCallback {

    private var _binding: FragmentPaymentSimulationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PaymentViewModel by viewModels()
    private val args: PaymentSimulationFragmentArgs by navArgs()

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var vibrator: Vibrator
    private lateinit var soundPool: SoundPool
    private var beepSoundId: Int = 0

    override fun onAttach(context: Context) {
        super.onAttach(context)

        nfcAdapter = NfcAdapter.getDefaultAdapter(context)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            SoundPool.Builder().setMaxStreams(1).build()
        } else {
            @Suppress("DEPRECATION")
            SoundPool(1, AudioManager.STREAM_MUSIC, 0)
        }
        beepSoundId = soundPool.load(context, R.raw.beep, 1) // Assuming you have a beep.wav in res/raw
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentSimulationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val amountToPay = BigDecimal(args.amountToPay)
        binding.textAmountToPay.text = String.format("$%.2f", amountToPay) // Format for display
        viewModel.initiatePayment(amountToPay)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.imageWalkingCard.setOnClickListener {
            viewModel.onSecretTap()
        }

        binding.buttonAccept.setOnClickListener {
            viewModel.onPaymentResultAcknowledged()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                // Reset visibility of all state-specific layouts
                binding.textWaitingForNfc.visibility = View.GONE
                binding.layoutProcessing.visibility = View.GONE
                binding.layoutSuccess.visibility = View.GONE
                binding.layoutFailure.visibility = View.GONE

                // Update UI based on current state
                when (state) {
                    PaymentUiState.Idle, PaymentUiState.NfcTapDetected -> {
                        binding.textWaitingForNfc.visibility = View.VISIBLE
                        // Only show amount when waiting for NFC if not already processing
                        binding.textAmountToPay.visibility = View.VISIBLE
                    }
                    is PaymentUiState.Processing -> {
                        binding.layoutProcessing.visibility = View.VISIBLE
                        binding.textProcessingTimer.text = getString(R.string.payment_processing_timer, state.remainingTimeSeconds)
                        binding.textAmountToPay.visibility = View.VISIBLE // Keep amount visible
                    }
                    PaymentUiState.Success -> {
                        binding.layoutSuccess.visibility = View.VISIBLE
                        binding.textAmountToPay.visibility = View.GONE // Hide amount on success screen
                    }
                    PaymentUiState.Failure -> {
                        binding.layoutFailure.visibility = View.VISIBLE
                        binding.textAmountToPay.visibility = View.GONE // Hide amount on failure screen
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.eventFlow.collectLatest { event ->
                when (event) {
                    PaymentEvent.TriggerNfcFeedback -> triggerNfcFeedback()
                    PaymentEvent.PaymentCompleted -> findNavController().popBackStack()
                }
            }
        }
    }

    private fun triggerNfcFeedback() {
        // Vibrate
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
        // Beep
        soundPool.play(beepSoundId, 1f, 1f, 1, 0, 1f)
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableReaderMode(requireActivity(), this, viewModel.readerFlags, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableReaderMode(requireActivity())
    }

    override fun onTagDiscovered(tag: Tag?) {
        // Run on UI thread as it updates ViewModel/UI
        viewLifecycleOwner.lifecycleScope.launch {
            if (tag != null) {
                viewModel.onNfcTagDetected(tag)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        soundPool.release()
    }
}
