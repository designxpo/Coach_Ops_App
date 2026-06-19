package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.CoachRepository
import com.example.data.FeatureFlagManager
import com.example.data.UserPreferences

class CoachViewModelFactory(
    private val repository: CoachRepository,
    private val userPreferences: UserPreferences,
    private val featureFlagManager: FeatureFlagManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, userPreferences, featureFlagManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
