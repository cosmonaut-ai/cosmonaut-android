package com.cosmonaut.app.ui.screens.world

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmonaut.app.auth.AuthManager
import com.cosmonaut.app.auth.AuthState
import com.cosmonaut.app.data.remote.asApiError
import com.cosmonaut.app.data.remote.dto.WorldResponse
import com.cosmonaut.app.data.remote.dto.WorldSessionResponse
import com.cosmonaut.app.data.repository.SessionRepository
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
private const val IMAGE_POLL_INTERVAL_MS = 4000L
private const val IMAGE_MAX_POLL_ATTEMPTS = 60

sealed interface WorldHomeUiState {
    data object Loading : WorldHomeUiState
    data class Generating(val world: WorldResponse) : WorldHomeUiState
    data class Failed(val world: WorldResponse, val sessionId: String?) : WorldHomeUiState
    data class Ready(
        val world: WorldResponse,
        val sessionId: String?,
        val currentNodeId: String?,
    ) : WorldHomeUiState
    data class Error(val message: String) : WorldHomeUiState
}

sealed interface WorldHomeEvent {
    data class NavigateToWorld(val worldId: String) : WorldHomeEvent
    data class NavigateToStoryNode(val sessionId: String, val nodeId: String) : WorldHomeEvent
    data class NavigateToMap(val sessionId: String) : WorldHomeEvent
    data class ShowMessage(val message: String) : WorldHomeEvent
    data object NavigateBack : WorldHomeEvent
}

@HiltViewModel
class WorldHomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val worldRepository: WorldRepository,
    private val sessionRepository: SessionRepository,
    private val authManager: AuthManager,
) : ViewModel() {

    private val worldId: String? = savedStateHandle["worldId"]
    private val routeSessionId: String? = savedStateHandle["sessionId"]
    private val invite: String? = savedStateHandle["invite"]

    val currentUserId: String?
        get() = (authManager.authState.value as? AuthState.Authenticated)?.user?.sub

    private val _uiState = MutableStateFlow<WorldHomeUiState>(WorldHomeUiState.Loading)
    val uiState: StateFlow<WorldHomeUiState> = _uiState.asStateFlow()

    private val _events = Channel<WorldHomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var pollingJob: Job? = null
    private var imagePollingJob: Job? = null

    init {
        loadWorld()
    }

    fun loadWorld() {
        viewModelScope.launch {
            _uiState.value = WorldHomeUiState.Loading
            try {
                routeSessionId?.let { sessionId ->
                    val session = sessionRepository.getSession(sessionId, fresh = true)
                    handleWorldState(
                        world = session.world,
                        sessionId = session.id,
                        currentNodeId = session.lastVisitedNodeId,
                    )
                    return@launch
                }

                val rootWorldId = worldId
                if (rootWorldId == null) {
                    _uiState.value = WorldHomeUiState.Error("Story link is missing an identifier.")
                    return@launch
                }

                val world = worldRepository.getWorld(rootWorldId, invite)
                handleWorldState(world = world, sessionId = null, currentNodeId = null)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to load world/session")
                if (routeSessionId != null && e.asApiError()?.isForbidden == true) {
                    try {
                        val handoff = sessionRepository.getSessionHandoff(routeSessionId)
                        _events.send(WorldHomeEvent.NavigateToWorld(handoff.rootWorldId))
                        return@launch
                    } catch (@Suppress("TooGenericExceptionCaught") handoffError: Exception) {
                        Timber.e(handoffError, "Failed to resolve session handoff")
                    }
                }
                _uiState.value = WorldHomeUiState.Error("Failed to load story. Please try again.")
            }
        }
    }

    fun onContinueStory() {
        val state = _uiState.value
        if (state !is WorldHomeUiState.Ready) return

        viewModelScope.launch {
            try {
                val session = ensureSession(state)
                val nodeId = session.lastVisitedNodeId ?: session.world.rootNodeId
                if (nodeId != null) {
                    _events.send(WorldHomeEvent.NavigateToStoryNode(session.id, nodeId))
                } else {
                    _events.send(WorldHomeEvent.ShowMessage("No story nodes available yet."))
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to start or resume story")
                _events.send(WorldHomeEvent.ShowMessage("Failed to start story. Please try again."))
            }
        }
    }

    fun onViewMap() {
        val state = _uiState.value
        if (state !is WorldHomeUiState.Ready) return

        viewModelScope.launch {
            try {
                val session = ensureSession(state)
                _events.send(WorldHomeEvent.NavigateToMap(session.id))
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to open map")
                _events.send(WorldHomeEvent.ShowMessage("Failed to open story map. Please try again."))
            }
        }
    }

    fun retryGeneration() {
        loadWorld()
    }

    fun onWorldUpdated(world: WorldResponse) {
        val current = _uiState.value
        if (current is WorldHomeUiState.Ready) {
            _uiState.value = current.copy(world = world)
        }
    }

    fun deleteWorld() {
        viewModelScope.launch {
            try {
                val sessionId = when (val state = _uiState.value) {
                    is WorldHomeUiState.Failed -> state.sessionId
                    is WorldHomeUiState.Ready -> state.sessionId
                    else -> null
                }
                if (sessionId == null) {
                    _events.send(WorldHomeEvent.ShowMessage("Open this story from your library to remove it."))
                    return@launch
                }

                sessionRepository.deleteSession(sessionId)
                _events.send(WorldHomeEvent.NavigateBack)
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to remove session")
                _events.send(WorldHomeEvent.ShowMessage("Failed to delete. Please try again."))
            }
        }
    }

    private suspend fun ensureSession(state: WorldHomeUiState.Ready): WorldSessionResponse {
        state.sessionId?.let { sessionId ->
            val session = sessionRepository.getSession(sessionId, fresh = true)
            _uiState.value = state.copy(
                world = session.world,
                sessionId = session.id,
                currentNodeId = session.lastVisitedNodeId,
            )
            return session
        }

        val session = sessionRepository.createWorldSession(state.world.id, inviteToken = invite)
        _uiState.value = state.copy(
            world = session.world,
            sessionId = session.id,
            currentNodeId = session.lastVisitedNodeId,
        )
        return session
    }

    private fun handleWorldState(world: WorldResponse, sessionId: String?, currentNodeId: String?) {
        when {
            world.isCompleted -> {
                pollingJob?.cancel()
                _uiState.value = WorldHomeUiState.Ready(
                    world = world,
                    sessionId = sessionId,
                    currentNodeId = currentNodeId,
                )
                if (world.isImageGenerating) {
                    startImagePolling(sessionId)
                }
            }
            world.isFailed -> {
                pollingJob?.cancel()
                imagePollingJob?.cancel()
                _uiState.value = WorldHomeUiState.Failed(world, sessionId)
            }
            world.isGenerating -> {
                _uiState.value = WorldHomeUiState.Generating(world)
                startPolling(sessionId)
            }
        }
    }

    private fun startPolling(sessionId: String?) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var attempts = 0
            while (attempts < MAX_POLL_ATTEMPTS) {
                delay(POLL_INTERVAL_MS)
                attempts++
                try {
                    if (sessionId != null) {
                        val session = sessionRepository.getSession(sessionId, fresh = true)
                        if (!session.world.isGenerating) {
                            handleWorldState(session.world, session.id, session.lastVisitedNodeId)
                            return@launch
                        }
                        updateGeneratingWorld(session.world)
                    } else {
                        val rootWorldId = worldId ?: return@launch
                        val world = worldRepository.getWorld(rootWorldId)
                        if (!world.isGenerating) {
                            handleWorldState(world, null, null)
                            return@launch
                        }
                        updateGeneratingWorld(world)
                    }
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    Timber.e(e, "Polling error for world/session")
                }
            }
        }
    }

    private fun updateGeneratingWorld(world: WorldResponse) {
        _uiState.update {
            if (it is WorldHomeUiState.Generating) {
                WorldHomeUiState.Generating(world)
            } else {
                it
            }
        }
    }

    private fun startImagePolling(sessionId: String?) {
        imagePollingJob?.cancel()
        imagePollingJob = viewModelScope.launch {
            var attempts = 0
            while (attempts < IMAGE_MAX_POLL_ATTEMPTS) {
                delay(IMAGE_POLL_INTERVAL_MS)
                attempts++
                try {
                    val world = if (sessionId != null) {
                        sessionRepository.invalidateSession(sessionId)
                        sessionRepository.getSession(sessionId).world
                    } else {
                        val rootWorldId = worldId ?: return@launch
                        worldRepository.invalidateWorld(rootWorldId)
                        worldRepository.getWorld(rootWorldId)
                    }
                    if (!world.isImageGenerating) {
                        _uiState.update {
                            if (it is WorldHomeUiState.Ready) it.copy(world = world) else it
                        }
                        return@launch
                    }
                } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                    Timber.e(e, "Image polling error for world/session")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
        imagePollingJob?.cancel()
    }
}
