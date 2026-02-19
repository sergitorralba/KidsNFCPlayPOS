package com.kidsnfcplaypos.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kidsnfcplaypos.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val prefs by lazy {
        requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load current state
        binding.switchRestaurant.isChecked = prefs.getBoolean("feature_restaurant", true)
        binding.switchCalculator.isChecked = prefs.getBoolean("feature_calculator", true)
        binding.switchDirectInput.isChecked = prefs.getBoolean("feature_direct_input", true)

        // Save on change
        binding.switchRestaurant.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("feature_restaurant", isChecked).apply()
        }

        binding.switchCalculator.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("feature_calculator", isChecked).apply()
        }

        binding.switchDirectInput.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("feature_direct_input", isChecked).apply()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
