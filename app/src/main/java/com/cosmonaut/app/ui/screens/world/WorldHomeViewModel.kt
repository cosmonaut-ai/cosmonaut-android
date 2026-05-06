package com.cosmonaut.app.ui.screens.world

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmonaut.app.data.remote.dto.WorldResponse
import com.cosmonaut.app.data.repository.WorldRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

private const val POLL_INTERVAL_MS = 2000L
private const val MAX_POLL_ATTEMPTS = 120

sealed interface WorldHomeUiState {
    data object Loading : WorldHomeUiState
    data class Generating(val world: WorldResponse) : WorldHomeUiState
    data class Failed(val world: WorldResponse) : WorldHomeUiState
    data class Ready(val world: WorldResponse, val currentNodeId: String?) : WorldHomeUiState
    data class Error(val message: String) : WorldHomeUiState
}

sealed interface WorldHomeEvent {
    data class NavigateToStoryNode(val worldId: String, val nodeId: String) : WorldHomeEvent
    data class ShowMessage(val message: String) : WorldHomeEvent
    data object NavigateBack : WorldHomeEvent
}

@HiltViewModel
class WorldHomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val worldRepository: WorldRepository,
) : ViewModel() {

    private val worldId: String = checkNotNull(savedStateHandle["worldId"])

    private val _uiState = MutableStateFlow<WorldHomeUiState>(WorldHomeUiState.Loading)
    val uiState: StateFlow<WorldHomeUiState> = _uiState.asStateFlow()

    private val _events = Channel<WorldHomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var pollingJob: Job? = null

    init {
        loadWorld()
    }

    fun loadWorld() {
        viewModelScope.launch {
            _uiState.value = WorldHomeUiState.Loading
            try {
                val world = worldRepository.getWorld(worldId)
                handleWorldState(world)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to load world $worldId")
                _uiState.value = WorldHomeUiState.Error("Failed to load story. Please try again.")
            }
        }
    }

    fun onContinueStory() {
        val state = _uiState.value
        if (state !is WorldHomeUiState.Ready) return

        viewModelScope.launch {
            try {
                val progress = worldRepository.getWorldProgress(worldId)
                val nodeId = progress.currentNodeId ?: state.world.rootNodeId
                if (nodeId != null) {
                    _events.send(WorldHomeEvent.NavigateToStoryNode(worldId, nodeId))
                } else {
                    _events.send(WorldHomeEvent.ShowMessage("No story nodes available yet."))
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to get progress for world $worldId")
                state.world.rootNodeId?.let { rootId ->
                    _events.send(WorldHomeEvent.NavigateToStoryNode(worldId, rootId))
                }
            }
        }
    }

    fun retryGeneration() {
        loadWorld()
    }

    fun deleteWorld() {
        viewModelScope.launch {
            try {
                worldRepository.deleteWorld(worldId)
                _events.send(WorldHomeEvent.NavigateBack)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to delete world $worldId")
                _events.send(WorldHomeEvent.ShowMessage("Failed to delete. Please try again."))
            }
        }
    }

    private fun handleWorldState(world: WorldResponse) {
        when {
            world.isCompleted -> {
                pollingJob?.cancel()
                viewModelScope.launch {
                    val progress = try {
                        worldRepository.getWorldProgress(worldId)
                    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                        Timber.e(e, "Failed to get progress")
                        null
                    }
                    _uiState.value = WorldHomeUiState.Ready(
                        world = world,
                        currentNodeId = progress?.currentNodeId,
                    )
                }
            }
            world.isFailed -> {
                pollingJob?.cancel()
                _uiState.value = WorldHomeUiState.Failed(world)
            }
            world.isGenerating -> {
                _uiState.value = WorldHomeUiState.Generating(world)
                startPolling()
            }
        }
    }

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var attempts = 0
            while (attempts < MAX_POLL_ATTEMPTS) {
                delay(POLL_INTERVAL_MS)
                attempts++
                try {
                    val world = worldRepository.getWorld(worldId)
                    if (!world.isGenerating) {
                        handleWorldState(world)
                        return@launch
                    }
                    _uiState.update {
                        if (it is WorldHomeUiState.Generating) {
                            WorldHomeUiState.Generating(world)
                        } else {
                            it
                        }
                    }
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    Timber.e(e, "Polling error for world $worldId")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
