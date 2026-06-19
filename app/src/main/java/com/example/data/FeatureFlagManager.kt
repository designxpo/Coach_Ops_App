package com.example.data

import android.content.Context
import android.content.SharedPreferences

class FeatureFlagManager private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("feature_flags", Context.MODE_PRIVATE)

    fun isEnabled(feature: AppFeature): Boolean =
        prefs.getBoolean(feature.key, feature.defaultEnabled)

    fun setEnabled(feature: AppFeature, enabled: Boolean) =
        prefs.edit().putBoolean(feature.key, enabled).apply()

    fun allFlags(): Map<AppFeature, Boolean> =
        AppFeature.entries.associateWith { isEnabled(it) }

    companion object {
        @Volatile private var instance: FeatureFlagManager? = null

        fun getInstance(context: Context): FeatureFlagManager =
            instance ?: synchronized(this) {
                instance ?: FeatureFlagManager(context.applicationContext).also { instance = it }
            }
    }
}
