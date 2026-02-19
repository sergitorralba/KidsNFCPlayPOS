package com.kidsnfcplaypos

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.kidsnfcplaypos.util.LocaleManager

class KidsNFCPlayPOSApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Ensure the saved locale is applied on startup
        val savedLocale = LocaleManager.getLocale(this)
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(savedLocale)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}
