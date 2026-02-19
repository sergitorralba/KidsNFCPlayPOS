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
        binding.switchShop.isChecked = prefs.getBoolean("feature_shop", true)
        binding.switchCalculator.isChecked = prefs.getBoolean("feature_calculator", true)

        // Save on change
        binding.switchShop.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("feature_shop", isChecked).apply()
        }

        binding.switchCalculator.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("feature_calculator", isChecked).apply()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
