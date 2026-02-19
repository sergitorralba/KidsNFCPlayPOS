package com.kidsnfcplaypos.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kidsnfcplaypos.R
import com.kidsnfcplaypos.databinding.FragmentSplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hide UI elements we don't want on the splash screen
        requireActivity().findViewById<View>(R.id.toolbar)?.visibility = View.GONE
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)?.visibility = View.GONE

        // Wait for 2 seconds then navigate to direct input
        viewLifecycleOwner.lifecycleScope.launch {
            delay(2000)
            if (isAdded) {
                findNavController().navigate(R.id.action_splashFragment_to_directInputFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Restore UI elements for other screens
        requireActivity().findViewById<View>(R.id.toolbar)?.visibility = View.VISIBLE
        requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)?.visibility = View.VISIBLE
        _binding = null
    }
}
