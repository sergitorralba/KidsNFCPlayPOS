package com.kidsnfcplaypos.util

import android.app.Activity
import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kidsnfcplaypos.R

object LanguageDialogHelper {
    fun showLanguageSelectionDialog(context: Context, activity: Activity?) {
        val languages = arrayOf("English", "Español", "Nederlands", "Català")
        val localeTags = arrayOf("en", "es", "nl", "ca")

        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.select_language_title))
            .setItems(languages) { _, which ->
                val selectedLocaleTag = localeTags[which]
                LocaleManager.saveLocale(context, selectedLocaleTag)
                activity?.recreate()
            }
            .show()
    }
}
