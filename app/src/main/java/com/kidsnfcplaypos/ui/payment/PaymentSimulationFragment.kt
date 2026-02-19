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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.kidsnfcplaypos.R
import com.kidsnfcplaypos.databinding.FragmentPaymentSimulationBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

import com.kidsnfcplaypos.ui.shop.ShopSelectionViewModel
import com.kidsnfcplaypos.ui.calculator.CalculatorViewModel
import com.kidsnfcplaypos.ui.directinput.DirectInputViewModel

class PaymentSimulationFragment : Fragment(), NfcAdapter.ReaderCallback, SoundPool.OnLoadCompleteListener {

    private var _binding: FragmentPaymentSimulationBinding? = null
    private val binding get() = _binding!!

    private val shopViewModel: ShopSelectionViewModel by activityViewModels {
        ShopSelectionViewModel.Factory(requireActivity().application)
    }
    private val calculatorViewModel: CalculatorViewModel by activityViewModels()
    private val directInputViewModel: DirectInputViewModel by activityViewModels()

    private val viewModel: PaymentViewModel by activityViewModels {
        PaymentViewModel.Factory(shopViewModel, calculatorViewModel, directInputViewModel)
    }
    
    private val args: PaymentSimulationFragmentArgs by navArgs()

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var vibrator: Vibrator
    private lateinit var soundPool: SoundPool
    private var beepSoundId: Int = 0
    private var isSoundLoaded = false

    private val currencyFormatter: NumberFormat by lazy {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = java.util.Currency.getInstance("EUR")
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        if (nfcAdapter == null) {
            Toast.makeText(context, "NFC is not available on this device.", Toast.LENGTH_LONG).show()
        }


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
        soundPool.setOnLoadCompleteListener(this)
        beepSoundId = soundPool.load(context, R.raw.beep, 1)
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
        binding.textAmountToPay.text = currencyFormatter.format(amountToPay)
        viewModel.initiatePayment(amountToPay)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        // Keep secret tap on the connection image to force failure for testing
        binding.imageConnection.setOnClickListener {
            viewModel.onSecretTap()
        }

        binding.buttonAccept.setOnClickListener {
            viewModel.onPaymentResultAcknowledged()
        }

        binding.buttonRetry.setOnClickListener {
            viewModel.onPaymentResultAcknowledged()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                binding.textWaitingForNfc.visibility = View.GONE
                binding.layoutProcessing.visibility = View.GONE
                binding.layoutSuccess.visibility = View.GONE
                binding.layoutFailure.visibility = View.GONE

                when (state) {
                    PaymentUiState.Idle, PaymentUiState.NfcTapDetected -> {
                        binding.textWaitingForNfc.visibility = View.VISIBLE
                        binding.textAmountToPay.visibility = View.VISIBLE
                        binding.imageConnection.clearAnimation()
                    }
                    is PaymentUiState.Processing -> {
                        binding.layoutProcessing.visibility = View.VISIBLE
                        binding.textProcessingTimer.text = getString(R.string.payment_processing_timer, state.remainingTimeSeconds)
                        binding.textAmountToPay.visibility = View.VISIBLE
                        startRotationAnimation()
                    }
                    PaymentUiState.Success -> {
                        binding.layoutSuccess.visibility = View.VISIBLE
                        binding.textAmountToPay.visibility = View.GONE
                        binding.imageConnection.clearAnimation()
                    }
                    PaymentUiState.Failure -> {
                        binding.layoutFailure.visibility = View.VISIBLE
                        binding.textAmountToPay.visibility = View.GONE
                        binding.imageConnection.clearAnimation()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.eventFlow.collectLatest { event ->
                when (event) {
                    PaymentEvent.TriggerNfcFeedback -> triggerNfcFeedback()
                    PaymentEvent.PaymentCompleted -> {
                        // After payment, we want to go back to the "clean" state.
                        // If we came from a Summary screen, we should pop all the way back to the main feature screen.
                        val currentDestId = findNavController().currentDestination?.id
                        
                        // We check the UI state to see if it was a success.
                        // If it was successful, we pop up to the main entry points.
                        if (viewModel.uiState.value is PaymentUiState.Success) {
                            findNavController().popBackStack(R.id.shopSelectionFragment, false)
                            findNavController().popBackStack(R.id.calculatorFragment, false)
                            findNavController().popBackStack(R.id.directInputFragment, false)
                        } else {
                            // If it was a failure, just go back one step so they can try again
                            findNavController().popBackStack()
                        }
                    }
                }
            }
        }
    }

    private fun startRotationAnimation() {
        if (binding.imageConnection.animation == null) {
            val rotate = RotateAnimation(
                0f, 360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
            ).apply {
                duration = 2000
                repeatCount = Animation.INFINITE
            }
            binding.imageConnection.startAnimation(rotate)
        }
    }

    private fun triggerNfcFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
        if (isSoundLoaded) {
            soundPool.play(beepSoundId, 1f, 1f, 1, 0, 1f)
        }
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
        viewLifecycleOwner.lifecycleScope.launch {
            if (tag != null) {
                viewModel.onNfcTagDetected(tag)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.imageConnection.clearAnimation()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool.release()
    }

    override fun onLoadComplete(soundPool: SoundPool?, sampleId: Int, status: Int) {
        if (status == 0) {
            isSoundLoaded = true
        }
    }
}
