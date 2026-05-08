package com.cosmonaut.app.ui.screens.storymap

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmonaut.app.analytics.AnalyticsEvent
import com.cosmonaut.app.analytics.CosmoAnalytics
import com.cosmonaut.app.data.repository.NodeRepository
import com.cosmonaut.app.data.repository.WorldRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

sealed interface StoryMapUiState {
    data object Loading : StoryMapUiState
    data class Success(val graphData: GraphData, val hasCurrentNode: Boolean) : StoryMapUiState
    data class Empty(val rootNodeId: String?) : StoryMapUiState
    data class Error(val message: String) : StoryMapUiState
}

@HiltViewModel
class StoryMapViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val nodeRepository: NodeRepository,
    private val worldRepository: WorldRepository,
    private val analytics: CosmoAnalytics,
) : ViewModel() {

    val worldId: String = checkNotNull(savedStateHandle["worldId"])

    /**
     * The node ID that was active when the user opened the map.
     * Navigation Compose serializes nullable String as the literal "null", so we
     * normalize that back to actual null.
     */
    private val currentNodeId: String? = savedStateHandle.get<String>("currentNodeId")
        ?.takeIf { it != "null" && it.isNotBlank() }

    private val _uiState = MutableStateFlow<StoryMapUiState>(StoryMapUiState.Loading)
    val uiState: StateFlow<StoryMapUiState> = _uiState.asStateFlow()

    init {
        analytics.trackEvent(AnalyticsEvent.MapViewed(worldId = worldId))
        loadNodes()
    }

    fun loadNodes() {
        viewModelScope.launch {
            _uiState.value = StoryMapUiState.Loading
            try {
                val nodes = nodeRepository.getWorldNodes(worldId)

                if (nodes.isEmpty()) {
                    val rootNodeId = try {
                        worldRepository.getWorld(worldId).rootNodeId
                    } catch (e: Exception) {
                        null
                    }
                    _uiState.value = StoryMapUiState.Empty(rootNodeId)
                    return@launch
                }

                val graphData = GraphLayoutEngine.layout(nodes, currentNodeId)
                _uiState.value = StoryMapUiState.Success(
                    graphData = graphData,
                    hasCurrentNode = currentNodeId != null,
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to load story map nodes")
                _uiState.value = StoryMapUiState.Error(
                    message = e.message ?: "Failed to load story map",
                )
            }
        }
    }
}
