package com.cosmonaut.app.ui.screens.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmonaut.app.analytics.AnalyticsEvent
import com.cosmonaut.app.analytics.CosmoAnalytics
import com.cosmonaut.app.auth.AuthError
import com.cosmonaut.app.auth.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

enum class LoginView { SIGN_IN, SIGN_UP, VERIFY, FORGOT, RESET }

sealed interface LoginEvent {
    data object NavigateToDashboard : LoginEvent
    data class ShowMessage(val message: String) : LoginEvent
}

data class LoginUiState(
    val view: LoginView = LoginView.SIGN_IN,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val verificationCode: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val resetCode: String = "",
    val showPassword: Boolean = false,
    val isSubmitting: Boolean = false,
    val isSuspended: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val analytics: CosmoAnalytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    // ── Field Updates ─────────────────────────────────────────────────

    fun updateEmail(value: String) = _uiState.update { it.copy(email = value, errorMessage = null) }

    fun updatePassword(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }

    fun updateConfirmPassword(value: String) = _uiState.update { it.copy(confirmPassword = value, errorMessage = null) }

    fun updateVerificationCode(value: String) =
        _uiState.update { it.copy(verificationCode = value, errorMessage = null) }

    fun updateNewPassword(value: String) = _uiState.update { it.copy(newPassword = value, errorMessage = null) }

    fun updateConfirmNewPassword(value: String) =
        _uiState.update { it.copy(confirmNewPassword = value, errorMessage = null) }

    fun updateResetCode(value: String) = _uiState.update { it.copy(resetCode = value, errorMessage = null) }

    fun togglePasswordVisibility() = _uiState.update { it.copy(showPassword = !it.showPassword) }

    // ── View Navigation ───────────────────────────────────────────────

    fun switchToSignUp() = _uiState.update {
        it.copy(view = LoginView.SIGN_UP, errorMessage = null, successMessage = null)
    }

    fun switchToSignIn() = _uiState.update {
        it.copy(view = LoginView.SIGN_IN, errorMessage = null, successMessage = null)
    }

    fun switchToForgotPassword() = _uiState.update {
        it.copy(view = LoginView.FORGOT, errorMessage = null, successMessage = null)
    }

    // ── Sign In ───────────────────────────────────────────────────────

    fun signIn() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) return
        launchAuth {
            val result = authManager.signInWithEmail(state.email, state.password)
            if (result.isSignedIn) {
                analytics.trackEvent(AnalyticsEvent.Login(method = "email"))
                _events.emit(LoginEvent.NavigateToDashboard)
            } else if (authManager.needsSignUpConfirmation(result)) {
                _uiState.update {
                    it.copy(
                        view = LoginView.VERIFY,
                        successMessage = "Please verify your email to continue.",
                    )
                }
            }
        }
    }

    // ── Sign Up ───────────────────────────────────────────────────────

    fun signUp() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) return
        launchAuth {
            val result = authManager.signUpWithEmail(state.email, state.password)
            analytics.trackEvent(AnalyticsEvent.SignUp(method = "email"))
            if (authManager.needsSignUpConfirmation(result)) {
                _uiState.update {
                    it.copy(
                        view = LoginView.VERIFY,
                        successMessage = "Account created! Check your email for a verification code.",
                    )
                }
            } else if (result.isSignUpComplete) {
                val signInResult = authManager.signInWithEmail(state.email, state.password)
                if (signInResult.isSignedIn) {
                    analytics.trackEvent(AnalyticsEvent.Login(method = "email"))
                    _events.emit(LoginEvent.NavigateToDashboard)
                }
            }
        }
    }

    // ── Verify ────────────────────────────────────────────────────────

    fun verify() {
        val state = _uiState.value
        launchAuth {
            authManager.confirmSignUp(state.email, state.verificationCode)
            analytics.trackEvent(AnalyticsEvent.EmailVerified)
            val signInResult = authManager.signInWithEmail(state.email, state.password)
            if (signInResult.isSignedIn) {
                analytics.trackEvent(AnalyticsEvent.Login(method = "email"))
                _events.emit(LoginEvent.NavigateToDashboard)
            }
        }
    }

    fun resendCode() {
        val state = _uiState.value
        if (state.email.isBlank()) return
        launchAuth {
            authManager.resendSignUpCode(state.email)
            _events.emit(LoginEvent.ShowMessage("Verification code resent to ${state.email}"))
        }
    }

    // ── Forgot / Reset Password ───────────────────────────────────────

    fun sendResetCode() {
        val state = _uiState.value
        if (state.email.isBlank()) return
        launchAuth {
            authManager.resetPassword(state.email)
            _uiState.update {
                it.copy(
                    view = LoginView.RESET,
                    successMessage = "Reset code sent to ${state.email}",
                )
            }
        }
    }

    fun resetPassword() {
        val state = _uiState.value
        launchAuth {
            authManager.confirmResetPassword(state.email, state.newPassword, state.resetCode)
            analytics.trackEvent(AnalyticsEvent.PasswordReset)
            _uiState.update {
                it.copy(
                    view = LoginView.SIGN_IN,
                    password = "",
                    newPassword = "",
                    confirmNewPassword = "",
                    resetCode = "",
                    successMessage = "Password reset successfully. Please sign in.",
                )
            }
        }
    }

    // ── Google Sign In ────────────────────────────────────────────────

    fun signInWithGoogle(activity: Activity) {
        analytics.trackEvent(AnalyticsEvent.Login(method = "google"))
        launchAuth {
            val result = authManager.signInWithGoogle(activity)
            if (result.isSignedIn) {
                _events.emit(LoginEvent.NavigateToDashboard)
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private fun launchAuth(block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            try {
                block()
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                Timber.w(error, "Auth operation failed")
                val currentView = _uiState.value.view
                val action = when (currentView) {
                    LoginView.SIGN_IN -> "sign_in"
                    LoginView.SIGN_UP -> "sign_up"
                    else -> "auth"
                }
                analytics.trackEvent(AnalyticsEvent.AuthFailed(method = "email", action = action))
                if (AuthError.isAccountSuspended(error)) {
                    _uiState.update { it.copy(isSuspended = true) }
                } else {
                    _uiState.update { it.copy(errorMessage = AuthError.format(error)) }

                    if (AuthError.isSignUpNotConfirmed(error)) {
                        _uiState.update {
                            it.copy(
                                view = LoginView.VERIFY,
                                successMessage = "Please verify your email first.",
                            )
                        }
                    }
                }
            } finally {
                _uiState.update { it.copy(isSubmitting = false) }
            }
        }
    }
}
