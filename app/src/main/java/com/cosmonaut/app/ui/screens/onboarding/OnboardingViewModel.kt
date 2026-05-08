package com.cosmonaut.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmonaut.app.analytics.AnalyticsEvent
import com.cosmonaut.app.analytics.CosmoAnalytics
import com.cosmonaut.app.data.local.CosmoPreferences
import com.cosmonaut.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

private const val USERNAME_MIN_LENGTH = 3
private const val USERNAME_MAX_LENGTH = 30
private const val USERNAME_CHECK_DEBOUNCE_MS = 300L

enum class UsernameStatus { IDLE, CHECKING, AVAILABLE, TAKEN, INVALID }

data class OnboardingUiState(
    val username: String = "",
    val usernameStatus: UsernameStatus = UsernameStatus.IDLE,
    val newsletterOptIn: Boolean = false,
    val ageConfirmed: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
) {
    val canSubmit: Boolean
        get() = usernameStatus == UsernameStatus.AVAILABLE && ageConfirmed && !isSubmitting
}

sealed interface OnboardingEvent {
    data object NavigateToDashboard : OnboardingEvent
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val preferences: CosmoPreferences,
    private val analytics: CosmoAnalytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<OnboardingEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<OnboardingEvent> = _events.asSharedFlow()

    private var usernameCheckJob: Job? = null
    private val usernameRegex = Regex("^[A-Za-z0-9]+$")

    init {
        checkIfAlreadyOnboarded()
    }

    private fun checkIfAlreadyOnboarded() {
        viewModelScope.launch {
            try {
                val usage = userRepository.getUsage()
                if (usage.isOnboarded) {
                    preferences.setOnboardingCompleted(true)
                    _events.emit(OnboardingEvent.NavigateToDashboard)
                }
            } catch (expected: Exception) {
                Timber.d(expected, "Usage check failed — showing onboarding")
            }
        }
    }

    fun updateUsername(value: String) {
        _uiState.update {
            it.copy(
                username = value,
                usernameStatus = UsernameStatus.IDLE,
                errorMessage = null,
            )
        }
        validateAndCheckUsername(value)
    }

    fun toggleNewsletter() {
        _uiState.update { it.copy(newsletterOptIn = !it.newsletterOptIn) }
    }

    fun toggleAgeConfirmation() {
        _uiState.update { it.copy(ageConfirmed = !it.ageConfirmed) }
    }

    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                userRepository.setUsername(state.username.trim())

                if (state.newsletterOptIn) {
                    try {
                        userRepository.updateNewsletter(true)
                    } catch (expected: Exception) {
                        Timber.w(expected, "Newsletter opt-in failed — non-blocking")
                    }
                }

                preferences.setOnboardingCompleted(true)
                analytics.trackEvent(AnalyticsEvent.OnboardingCompleted(newsletterOptIn = state.newsletterOptIn))
                _events.emit(OnboardingEvent.NavigateToDashboard)
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                Timber.w(error, "Onboarding submission failed")
                val message = error.message ?: "Failed to complete onboarding."
                if (message.contains("already taken", ignoreCase = true)) {
                    _uiState.update { it.copy(usernameStatus = UsernameStatus.TAKEN) }
                } else {
                    _uiState.update { it.copy(errorMessage = message) }
                }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }

    private fun validateAndCheckUsername(value: String) {
        usernameCheckJob?.cancel()

        val trimmed = value.trim()
        if (trimmed.length < USERNAME_MIN_LENGTH) {
            if (trimmed.isNotEmpty()) {
                _uiState.update { it.copy(usernameStatus = UsernameStatus.INVALID) }
            }
            return
        }
        if (trimmed.length > USERNAME_MAX_LENGTH || !usernameRegex.matches(trimmed)) {
            _uiState.update { it.copy(usernameStatus = UsernameStatus.INVALID) }
            return
        }

        _uiState.update { it.copy(usernameStatus = UsernameStatus.CHECKING) }
        usernameCheckJob = viewModelScope.launch {
            delay(USERNAME_CHECK_DEBOUNCE_MS)
            try {
                val response = userRepository.checkUsernameAvailability(trimmed)
                _uiState.update {
                    it.copy(
                        usernameStatus = if (response.available) {
                            UsernameStatus.AVAILABLE
                        } else {
                            UsernameStatus.TAKEN
                        },
                    )
                }
            } catch (expected: Exception) {
                Timber.w(expected, "Username check failed")
                _uiState.update { it.copy(usernameStatus = UsernameStatus.IDLE) }
            }
        }
    }
}
