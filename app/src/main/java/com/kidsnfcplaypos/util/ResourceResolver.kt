package com.kidsnfcplaypos.util

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

class ResourceResolver(private val context: Context) {

    private val stringIdCache = mutableMapOf<String, Int>()

    @StringRes
    fun getStringId(name: String): Int {
        return stringIdCache.getOrPut(name) {
            context.resources.getIdentifier(name, "string", context.packageName)
        }
    }

    /**
     * Resolves a string resource by name, ensuring the current AppCompat locale is applied.
     * This fixes issues on older Android versions (like on your LG G7) where the 
     * application context resources don't update automatically.
     */
    fun getString(name: String, vararg formatArgs: Any): String {
        val stringId = getStringId(name)
        if (stringId == 0) return "[MISSING: $name]"

        // Create a configuration-wrapped context using the currently selected app locale
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val targetContext = if (!appLocales.isEmpty) {
            val config = context.resources.configuration
            config.setLocale(appLocales.get(0))
            context.createConfigurationContext(config)
        } else {
            context
        }

        return targetContext.getString(stringId, *formatArgs)
    }
}
