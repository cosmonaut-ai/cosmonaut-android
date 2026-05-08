package com.cosmonaut.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmonaut.app.data.billing.RegionDetector
import com.cosmonaut.app.data.remote.dto.UsageResponse
import com.cosmonaut.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    val regionDetector: RegionDetector,
) : ViewModel() {

    private val _usage = MutableStateFlow<UsageResponse?>(null)
    val usage: StateFlow<UsageResponse?> = _usage.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadUsage()
        viewModelScope.launch { regionDetector.detect() }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                userRepository.invalidate()
                _usage.value = userRepository.fetchFresh()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to refresh usage")
            }
        }
    }

    private fun loadUsage() {
        viewModelScope.launch {
            try {
                _usage.value = userRepository.getUsage()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to load usage")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
