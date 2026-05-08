package com.cosmonaut.app.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.imageLoader
import com.cosmonaut.app.auth.AuthManager
import com.cosmonaut.app.auth.AuthState
import com.cosmonaut.app.data.billing.RegionDetector
import com.cosmonaut.app.data.local.CosmoPreferences
import com.cosmonaut.app.data.remote.dto.UsageResponse
import com.cosmonaut.app.data.repository.NodeRepository
import com.cosmonaut.app.data.repository.UserRepository
import com.cosmonaut.app.data.repository.WorldRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber

sealed interface SettingsEvent {
    data class ShowSnackbar(val message: String) : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val worldRepository: WorldRepository,
    private val nodeRepository: NodeRepository,
    private val authManager: AuthManager,
    private val preferences: CosmoPreferences,
    val regionDetector: RegionDetector,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _usage = MutableStateFlow<UsageResponse?>(null)
    val usage: StateFlow<UsageResponse?> = _usage.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _newsletterUpdating = MutableStateFlow(false)
    val newsletterUpdating: StateFlow<Boolean> = _newsletterUpdating.asStateFlow()

    private val _isDeletingAccount = MutableStateFlow(false)
    val isDeletingAccount: StateFlow<Boolean> = _isDeletingAccount.asStateFlow()

    private val _deleteError = MutableStateFlow<String?>(null)
    val deleteError: StateFlow<String?> = _deleteError.asStateFlow()

    private val _isSigningOut = MutableStateFlow(false)
    val isSigningOut: StateFlow<Boolean> = _isSigningOut.asStateFlow()

    val authState: StateFlow<AuthState> = authManager.authState

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

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

    fun updateNewsletter(optedIn: Boolean) {
        viewModelScope.launch {
            _newsletterUpdating.value = true
            try {
                userRepository.updateNewsletter(optedIn)
                _usage.value = userRepository.fetchFresh()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to update newsletter preference")
                _events.send(SettingsEvent.ShowSnackbar("Failed to update newsletter preference"))
            } finally {
                _newsletterUpdating.value = false
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _isDeletingAccount.value = true
            _deleteError.value = null
            try {
                userRepository.deleteAccount()
                clearLocalCaches()
                authManager.signOut()
                preferences.clear()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to delete account")
                _deleteError.value = e.message ?: "Account deletion failed."
            } finally {
                _isDeletingAccount.value = false
            }
        }
    }

    fun clearDeleteError() {
        _deleteError.value = null
    }

    fun signOut() {
        viewModelScope.launch {
            _isSigningOut.value = true
            try {
                clearLocalCaches()
                authManager.signOut()
                preferences.clearUserData()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Error during sign out")
            }
        }
    }

    private suspend fun clearLocalCaches() {
        worldRepository.clearAll()
        nodeRepository.clearAll()
        userRepository.invalidate()
        appContext.imageLoader.memoryCache?.clear()
        appContext.imageLoader.diskCache?.clear()
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
