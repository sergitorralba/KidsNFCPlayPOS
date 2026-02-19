package com.kidsnfcplaypos

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.SharedPreferences
import android.view.View

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView

    private val prefs by lazy {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find the new toolbar and set it as the support action bar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Define top-level destinations for the AppBarConfiguration
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.shopSelectionFragment, R.id.calculatorFragment, R.id.directInputFragment
            )
        )

        // Connect the action bar (our toolbar) with the NavController
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Connect the BottomNavigationView with the NavController
        bottomNav = findViewById(R.id.bottom_nav)
        bottomNav.setupWithNavController(navController)

        updateNavVisibility()
        
        // Listen for preference changes
        prefs.registerOnSharedPreferenceChangeListener(this)
        
        // Listen for navigation changes to enforce visibility rules when returning from Settings
        navController.addOnDestinationChangedListener { _, destination, _ ->
            enforceFeatureRules(destination.id)
        }
    }

    private fun updateNavVisibility() {
        val showShop = prefs.getBoolean("feature_shop", true)
        val showCalculator = prefs.getBoolean("feature_calculator", true)

        bottomNav.menu.findItem(R.id.shopSelectionFragment).isVisible = showShop
        bottomNav.menu.findItem(R.id.calculatorFragment).isVisible = showCalculator
        bottomNav.menu.findItem(R.id.directInputFragment).isVisible = true

        enforceFeatureRules(navController.currentDestination?.id)
    }

    private fun enforceFeatureRules(currentDestId: Int?) {
        val showShop = prefs.getBoolean("feature_shop", true)
        val showCalculator = prefs.getBoolean("feature_calculator", true)

        val isCurrentShopRelated = currentDestId == R.id.shopSelectionFragment || currentDestId == R.id.cartSummaryFragment
        val isCurrentCalculatorRelated = currentDestId == R.id.calculatorFragment || currentDestId == R.id.calculatorSummaryFragment

        if ((isCurrentShopRelated && !showShop) || (isCurrentCalculatorRelated && !showCalculator)) {
            // Post navigation to avoid conflicts during destination changes
            window.decorView.post {
                if (navController.currentDestination?.id != R.id.directInputFragment) {
                    navController.navigate(R.id.directInputFragment)
                }
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "feature_shop" || key == "feature_calculator") {
            updateNavVisibility()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
