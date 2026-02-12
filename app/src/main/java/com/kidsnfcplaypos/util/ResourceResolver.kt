package com.kidsnfcplaypos.util

import android.content.Context
import androidx.annotation.StringRes

class ResourceResolver(private val context: Context) {

    private val stringIdCache = mutableMapOf<String, Int>()

    @StringRes
    fun getStringId(name: String): Int {
        return stringIdCache.getOrPut(name) {
            context.resources.getIdentifier(name, "string", context.packageName)
        }
    }

    fun getString(name: String, vararg formatArgs: Any): String {
        val stringId = getStringId(name)
        return if (stringId != 0) {
            context.getString(stringId, *formatArgs)
        } else {
            "[MISSING: $name]"
        }
    }
}
