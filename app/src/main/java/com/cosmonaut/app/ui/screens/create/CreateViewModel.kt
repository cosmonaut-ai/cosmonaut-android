package com.cosmonaut.app.ui.screens.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmonaut.app.data.local.CosmoPreferences
import com.cosmonaut.app.data.remote.dto.CreateWorldRequest
import com.cosmonaut.app.data.repository.UserRepository
import com.cosmonaut.app.data.repository.WorldRepository
import com.cosmonaut.app.util.PromptLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

private const val MAX_PROMPT_LENGTH = 2000

data class CreateUiState(
    val prompt: String = "",
    val visibility: String = "private",
    val storyLength: String = "medium",
    val vocabLevel: String = "teen",
    val contentFilter: String = "none",
    val isSubmitting: Boolean = false,
    val promptError: String? = null,
    val showMoreSettings: Boolean = false,
    val worldsAtLimit: Boolean = false,
)

sealed interface CreateEvent {
    data class NavigateToWorld(val worldId: String) : CreateEvent
    data class ShowMessage(val message: String) : CreateEvent
}

@HiltViewModel
class CreateViewModel @Inject constructor(
    private val worldRepository: WorldRepository,
    private val userRepository: UserRepository,
    private val preferences: CosmoPreferences,
    private val promptLoader: PromptLoader,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateUiState())
    val uiState: StateFlow<CreateUiState> = _uiState.asStateFlow()

    private val _events = Channel<CreateEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadSavedPreferences()
        checkUsageLimits()
    }

    fun updatePrompt(prompt: String) {
        if (prompt.length <= MAX_PROMPT_LENGTH) {
            _uiState.update { it.copy(prompt = prompt, promptError = null) }
        }
    }

    fun updateVisibility(visibility: String) {
        _uiState.update { it.copy(visibility = visibility) }
    }

    fun updateStoryLength(length: String) {
        _uiState.update { it.copy(storyLength = length) }
        viewModelScope.launch { preferences.setStoryLength(length) }
    }

    fun updateVocabLevel(level: String) {
        _uiState.update { it.copy(vocabLevel = level) }
        viewModelScope.launch { preferences.setVocabLevel(level) }
    }

    fun updateContentFilter(filter: String) {
        _uiState.update { it.copy(contentFilter = filter) }
        viewModelScope.launch { preferences.setContentFilter(filter) }
    }

    fun toggleMoreSettings() {
        _uiState.update { it.copy(showMoreSettings = !it.showMoreSettings) }
    }

    fun loadRandomPrompt() {
        val prompt = promptLoader.getRandomPrompt()
        _uiState.update { it.copy(prompt = prompt, promptError = null) }
    }

    fun createWorld() {
        val state = _uiState.value
        if (state.prompt.isBlank()) {
            _uiState.update { it.copy(promptError = "Please enter a story prompt") }
            return
        }
        if (state.isSubmitting) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, promptError = null) }
            try {
                val request = CreateWorldRequest(
                    worldPrompt = state.prompt.trim(),
                    visibility = state.visibility,
                    worldLength = state.storyLength,
                    vocabLevel = state.vocabLevel,
                    contentFilter = state.contentFilter,
                )
                val world = worldRepository.createWorld(request)
                _uiState.update { it.copy(isSubmitting = false, prompt = "") }
                _events.send(CreateEvent.NavigateToWorld(world.id))
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to create world")
                _uiState.update { it.copy(isSubmitting = false) }
                val isQuotaError = e.message?.contains("429") == true ||
                    e.message?.contains("quota", ignoreCase = true) == true
                val message = if (isQuotaError) {
                    "You've reached your world creation limit. " +
                        "Upgrade your plan to create more stories."
                } else {
                    "Failed to create story. Please try again."
                }
                _events.send(CreateEvent.ShowMessage(message))
            }
        }
    }

    private fun loadSavedPreferences() {
        viewModelScope.launch {
            val length = preferences.storyLength.first()
            val vocab = preferences.vocabLevel.first()
            val filter = preferences.contentFilter.first()
            _uiState.update {
                it.copy(
                    storyLength = length,
                    vocabLevel = vocab,
                    contentFilter = filter,
                )
            }
        }
    }

    private fun checkUsageLimits() {
        viewModelScope.launch {
            try {
                val usage = userRepository.getUsage()
                _uiState.update {
                    it.copy(worldsAtLimit = usage.worldsCreated >= usage.worldsLimit)
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to check usage limits")
            }
        }
    }
}
