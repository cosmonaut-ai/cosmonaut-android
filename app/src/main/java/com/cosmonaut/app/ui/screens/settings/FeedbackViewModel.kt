package com.cosmonaut.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmonaut.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import timber.log.Timber

enum class FeedbackCategory(val label: String) {
    BUG("Bug Report"),
    FEATURE("Feature Request"),
    FEEDBACK("General Feedback"),
    OTHER("Other"),
}

@HiltViewModel
class FeedbackViewModel @Inject constructor(private val userRepository: UserRepository,) : ViewModel() {

    private val _category = MutableStateFlow(FeedbackCategory.FEEDBACK)
    val category: StateFlow<FeedbackCategory> = _category.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _isSubmitted = MutableStateFlow(false)
    val isSubmitted: StateFlow<Boolean> = _isSubmitted.asStateFlow()

    private val _isRateLimited = MutableStateFlow(false)
    val isRateLimited: StateFlow<Boolean> = _isRateLimited.asStateFlow()

    fun setCategory(category: FeedbackCategory) {
        _category.value = category
    }

    fun setMessage(message: String) {
        if (message.length <= MAX_MESSAGE_LENGTH) {
            _message.value = message
            _isRateLimited.value = false
        }
    }

    fun submit() {
        val trimmed = _message.value.trim()
        if (trimmed.length < MIN_MESSAGE_LENGTH || _isSubmitting.value) return

        viewModelScope.launch {
            _isSubmitting.value = true
            _isRateLimited.value = false
            try {
                userRepository.submitFeedback(
                    category = _category.value.name.lowercase(),
                    message = trimmed,
                )
                _isSubmitted.value = true
            } catch (e: HttpException) {
                if (e.code() == RATE_LIMIT_STATUS) {
                    _isRateLimited.value = true
                } else {
                    Timber.e(e, "Failed to submit feedback")
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to submit feedback")
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    companion object {
        const val MIN_MESSAGE_LENGTH = 10
        const val MAX_MESSAGE_LENGTH = 10_000
        private const val RATE_LIMIT_STATUS = 429
    }
}
