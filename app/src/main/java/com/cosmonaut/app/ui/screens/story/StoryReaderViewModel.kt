package com.cosmonaut.app.ui.screens.story

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmonaut.app.analytics.AnalyticsEvent
import com.cosmonaut.app.analytics.CosmoAnalytics
import com.cosmonaut.app.auth.AuthManager
import com.cosmonaut.app.auth.AuthState
import com.cosmonaut.app.data.billing.RegionDetector
import com.cosmonaut.app.data.remote.ApiError
import com.cosmonaut.app.data.remote.StreamEvent
import com.cosmonaut.app.data.remote.asApiError
import com.cosmonaut.app.data.remote.dto.ChoiceResponse
import com.cosmonaut.app.data.remote.dto.StoryNodeResponse
import com.cosmonaut.app.data.remote.dto.UsageResponse
import com.cosmonaut.app.data.remote.dto.WorldResponse
import com.cosmonaut.app.data.repository.NodeRepository
import com.cosmonaut.app.data.repository.UserRepository
import com.cosmonaut.app.data.repository.WorldRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import timber.log.Timber

private const val POLL_INTERVAL_MS = 2000L
private const val MAX_POLL_ATTEMPTS = 120

sealed interface StoryReaderUiState {
    data object Loading : StoryReaderUiState

    data class Streaming(val text: String, val parentChoice: ChoiceResponse?, val isWaitingForFirstToken: Boolean,) :
        StoryReaderUiState

    data class RemoteGenerating(val parentChoice: ChoiceResponse?) : StoryReaderUiState

    data class Content(val node: StoryNodeResponse, val isEnding: Boolean) :
        StoryReaderUiState

    data class Failed(val canRetry: Boolean) : StoryReaderUiState

    data class WrongSession(val worldId: String) : StoryReaderUiState

    data class Error(val message: String) : StoryReaderUiState
}

sealed interface StoryReaderEvent {
    data class NavigateToNode(val worldId: String, val nodeId: String) : StoryReaderEvent
    data class NavigateToParent(val worldId: String, val nodeId: String) : StoryReaderEvent
    data object NavigateToDashboard : StoryReaderEvent
    data class ShowMessage(val message: String) : StoryReaderEvent
    data object ShowQuotaPrompt : StoryReaderEvent
}

@HiltViewModel
class StoryReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val nodeRepository: NodeRepository,
    private val userRepository: UserRepository,
    private val worldRepository: WorldRepository,
    private val authManager: AuthManager,
    private val analytics: CosmoAnalytics,
    val regionDetector: RegionDetector,
) : ViewModel() {

    val worldId: String = checkNotNull(savedStateHandle["worldId"])
    val nodeId: String = checkNotNull(savedStateHandle["nodeId"])

    val currentUserId: String?
        get() = (authManager.authState.value as? AuthState.Authenticated)?.user?.sub

    private val _worldForShare = MutableStateFlow<WorldResponse?>(null)
    val worldForShare: StateFlow<WorldResponse?> = _worldForShare.asStateFlow()

    private val _usage = MutableStateFlow<UsageResponse?>(null)
    val usage: StateFlow<UsageResponse?> = _usage.asStateFlow()

    private val _uiState = MutableStateFlow<StoryReaderUiState>(StoryReaderUiState.Loading)
    val uiState: StateFlow<StoryReaderUiState> = _uiState.asStateFlow()

    private val _hasParent = MutableStateFlow(false)
    val hasParent: StateFlow<Boolean> = _hasParent.asStateFlow()

    private val _events = Channel<StoryReaderEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val _isChoiceInProgress = MutableStateFlow(false)
    val isChoiceInProgress: StateFlow<Boolean> = _isChoiceInProgress.asStateFlow()

    private var streamingJob: Job? = null
    private var pollingJob: Job? = null
    private var currentNode: StoryNodeResponse? = null

    init {
        loadNode()
        loadUsage()
    }

    fun retryGeneration() {
        loadNode()
    }

    fun onChoiceSelected(targetId: String) {
        val node = currentNode ?: return
        if (_uiState.value is StoryReaderUiState.Streaming) return
        if (_isChoiceInProgress.value) return
        _isChoiceInProgress.value = true

        if (node.parentId == null) {
            analytics.trackEvent(AnalyticsEvent.StoryStarted(worldId = worldId))
        }
        analytics.trackEvent(AnalyticsEvent.StoryChoiceMade(worldId = worldId, choiceType = "preset"))

        viewModelScope.launch {
            try {
                val newNode = nodeRepository.chooseOption(
                    worldId = worldId,
                    nodeId = node.id,
                    targetId = targetId,
                    customChoice = null,
                )

                nodeRepository.invalidate(worldId, node.id)

                _events.send(StoryReaderEvent.NavigateToNode(worldId, newNode.id))
                refreshParentState(node)
            } catch (e: Exception) {
                handleChoiceError(e)
            } finally {
                _isChoiceInProgress.value = false
            }
        }
    }

    fun onCustomChoice(text: String) {
        val node = currentNode ?: return
        if (_uiState.value is StoryReaderUiState.Streaming) return
        if (text.isBlank()) return
        if (_isChoiceInProgress.value) return
        _isChoiceInProgress.value = true

        analytics.trackEvent(AnalyticsEvent.StoryChoiceMade(worldId = worldId, choiceType = "custom"))

        viewModelScope.launch {
            try {
                val newNode = nodeRepository.chooseOption(
                    worldId = worldId,
                    nodeId = node.id,
                    targetId = null,
                    customChoice = text.trim(),
                )

                nodeRepository.invalidate(worldId, node.id)

                _events.send(StoryReaderEvent.NavigateToNode(worldId, newNode.id))
                refreshParentState(node)
            } catch (e: Exception) {
                handleChoiceError(e)
            } finally {
                _isChoiceInProgress.value = false
            }
        }
    }

    /**
     * After navigating to a child node, refresh this (parent) node's data
     * so that choices reflect updated is_explored flags when navigating back.
     */
    private fun refreshParentState(parentNode: StoryNodeResponse) {
        viewModelScope.launch {
            try {
                val freshParent = nodeRepository.fetchFresh(worldId, parentNode.id)
                currentNode = freshParent
                _uiState.value = StoryReaderUiState.Content(
                    node = freshParent,
                    isEnding = freshParent.isEnding,
                )
            } catch (_: Exception) {
                _uiState.value = StoryReaderUiState.Content(
                    node = parentNode,
                    isEnding = parentNode.isEnding,
                )
            }
        }
    }

    fun navigateToParent() {
        val parentId = currentNode?.parentId ?: return
        cancelStreaming()
        cancelPolling()
        viewModelScope.launch {
            _events.send(StoryReaderEvent.NavigateToParent(worldId, parentId))
        }
    }

    fun navigateToDashboard() {
        cancelStreaming()
        cancelPolling()
        viewModelScope.launch { _events.send(StoryReaderEvent.NavigateToDashboard) }
    }

    fun loadWorldForShare() {
        viewModelScope.launch {
            try {
                _worldForShare.value = worldRepository.getWorld(worldId)
            } catch (e: Exception) {
                Timber.e(e, "Failed to load world for share")
                _events.send(StoryReaderEvent.ShowMessage("Failed to load sharing info"))
            }
        }
    }

    private fun loadNode() {
        cancelStreaming()
        cancelPolling()

        viewModelScope.launch {
            _uiState.value = StoryReaderUiState.Loading
            try {
                val node = nodeRepository.getNode(worldId, nodeId)
                currentNode = node
                _hasParent.value = node.parentId != null
                handleNodeState(node)
            } catch (e: Exception) {
                handleLoadError(e)
            }
        }
    }

    private fun handleNodeState(node: StoryNodeResponse) {
        when {
            node.isInitialized -> startStreaming(node)
            node.isGenerating -> startPolling(node)
            node.isCompleted -> {
                if (node.isEnding) {
                    analytics.trackEvent(
                        AnalyticsEvent.StoryEnded(worldId = worldId, pathLength = computePathLength(node.id)),
                    )
                }
                _uiState.value = StoryReaderUiState.Content(
                    node = node,
                    isEnding = node.isEnding,
                )
            }
            node.isFailed -> {
                _uiState.value = StoryReaderUiState.Failed(canRetry = true)
            }
        }
    }

    private fun computePathLength(nodeId: String): Int {
        if (nodeId == "0") return 0
        var depth = 0
        var remaining = nodeId.removePrefix("0")
        while (remaining.isNotEmpty()) {
            depth++
            val firstChar = remaining[0]
            if (firstChar.isDigit()) {
                val prefixLen = firstChar.digitToInt()
                remaining = remaining.drop(1 + prefixLen)
            } else {
                remaining = remaining.drop(1)
            }
        }
        return depth
    }

    private fun startStreaming(node: StoryNodeResponse) {
        _uiState.value = StoryReaderUiState.Streaming(
            text = "",
            parentChoice = node.parentChoice,
            isWaitingForFirstToken = true,
        )

        streamingJob?.cancel()
        streamingJob = viewModelScope.launch {
            nodeRepository.generateNodeText(worldId, node.id)
                .catch { e -> handleStreamingError(e, node.id) }
                .collect { event ->
                    when (event) {
                        is StreamEvent.Token -> {
                            _uiState.value = StoryReaderUiState.Streaming(
                                text = event.text,
                                parentChoice = node.parentChoice,
                                isWaitingForFirstToken = false,
                            )
                        }
                        is StreamEvent.Done -> {
                            currentNode = event.completedNode
                            _uiState.value = StoryReaderUiState.Content(
                                node = event.completedNode,
                                isEnding = event.completedNode.isEnding,
                            )
                        }
                        is StreamEvent.PreGenerated -> {
                            currentNode = event.node
                            _uiState.value = StoryReaderUiState.Content(
                                node = event.node,
                                isEnding = event.node.isEnding,
                            )
                        }
                        is StreamEvent.Error -> {
                            handleStreamingApiError(event.error, node.id)
                        }
                    }
                }
        }
    }

    private fun startPolling(node: StoryNodeResponse) {
        _uiState.value = StoryReaderUiState.RemoteGenerating(parentChoice = node.parentChoice)

        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var attempts = 0
            while (attempts < MAX_POLL_ATTEMPTS) {
                delay(POLL_INTERVAL_MS)
                attempts++
                try {
                    val freshNode = nodeRepository.getNode(worldId, node.id)
                    if (!freshNode.isGenerating) {
                        currentNode = freshNode
                        handleNodeState(freshNode)
                        return@launch
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Polling error for node ${node.id}")
                }
            }
            _uiState.value = StoryReaderUiState.Failed(canRetry = true)
        }
    }

    private suspend fun handleStreamingError(error: Throwable, nodeId: String) {
        if (error is ApiError) {
            handleStreamingApiError(error, nodeId)
            return
        }

        Timber.e(error, "Streaming failed for node $nodeId")

        try {
            val freshNode = nodeRepository.getNode(worldId, nodeId)
            currentNode = freshNode
            when {
                freshNode.isGenerating -> startPolling(freshNode)
                freshNode.isCompleted && freshNode.text != null -> {
                    _uiState.value = StoryReaderUiState.Content(
                        node = freshNode,
                        isEnding = freshNode.isEnding,
                    )
                }
                else -> {
                    _uiState.value = StoryReaderUiState.Failed(canRetry = true)
                }
            }
        } catch (_: Exception) {
            _events.send(StoryReaderEvent.ShowMessage("Failed to generate text. Please try again."))
            _uiState.value = StoryReaderUiState.Failed(canRetry = true)
        }
    }

    private suspend fun handleStreamingApiError(error: ApiError, nodeId: String) {
        when {
            error.isRateLimited -> {
                _events.send(
                    StoryReaderEvent.ShowMessage("Slow down — you're making requests too quickly."),
                )
                _uiState.value = StoryReaderUiState.Failed(canRetry = true)
            }
            error.isQuotaExceeded -> {
                val parentId = currentNode?.parentId
                if (parentId != null) {
                    _events.send(StoryReaderEvent.NavigateToParent(worldId, parentId))
                }
                _events.send(StoryReaderEvent.ShowQuotaPrompt)
            }
            error.isNodeAlreadyProcessed -> {
                try {
                    val freshNode = nodeRepository.getNode(worldId, nodeId)
                    currentNode = freshNode
                    handleNodeState(freshNode)
                } catch (_: Exception) {
                    _uiState.value = StoryReaderUiState.Failed(canRetry = true)
                }
            }
            else -> {
                _uiState.value = StoryReaderUiState.Failed(canRetry = true)
            }
        }
    }

    private fun handleLoadError(error: Exception) {
        if (error is ApiError) {
            when {
                error.isWrongSession -> {
                    _uiState.value = StoryReaderUiState.WrongSession(worldId)
                }
                error.isNotFound -> {
                    _uiState.value = StoryReaderUiState.Error("Story node not found.")
                }
                else -> {
                    _uiState.value = StoryReaderUiState.Error(
                        error.detail.ifBlank { "Failed to load story." },
                    )
                }
            }
        } else {
            Timber.e(error, "Failed to load node")
            _uiState.value = StoryReaderUiState.Error("Failed to load story. Please try again.")
        }
    }

    private suspend fun handleChoiceError(error: Exception) {
        val apiError = error.asApiError()
        if (apiError?.isQuotaExceeded == true) {
            _events.send(StoryReaderEvent.ShowQuotaPrompt)
            loadNode()
        } else if (apiError?.isNodeProcessingConflict == true) {
            try {
                nodeRepository.retryNodeProcessing(worldId, currentNode!!.id)
                _events.send(
                    StoryReaderEvent.ShowMessage(
                        "Story node busy — a background task was re-queued. Please try again.",
                    ),
                )
            } catch (_: Exception) {
                _events.send(
                    StoryReaderEvent.ShowMessage(
                        "Story node encountered a processing issue. Please wait and try again.",
                    ),
                )
            }
            loadNode()
        } else {
            val message = apiError?.detail ?: "Failed to make choice."
            _events.send(StoryReaderEvent.ShowMessage(message))
            loadNode()
        }
    }

    private fun loadUsage() {
        viewModelScope.launch {
            try {
                val usage = userRepository.getUsage()
                _usage.value = usage
            } catch (_: Exception) {
                Timber.d("Failed to load usage — will default to not at quota")
            }
        }
    }

    private fun cancelStreaming() {
        streamingJob?.cancel()
        streamingJob = null
    }

    private fun cancelPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        cancelStreaming()
        cancelPolling()
    }
}
