package com.cosmonaut.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmonaut.app.analytics.AnalyticsEvent
import com.cosmonaut.app.analytics.CosmoAnalytics
import com.cosmonaut.app.data.billing.RegionDetector
import com.cosmonaut.app.data.remote.dto.UsageResponse
import com.cosmonaut.app.data.remote.dto.WorldResponse
import com.cosmonaut.app.data.remote.dto.WorldSessionSummaryResponse
import com.cosmonaut.app.data.repository.SessionRepository
import com.cosmonaut.app.data.repository.UserRepository
import com.cosmonaut.app.data.repository.WorldRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class HomeUiState(
    val sessions: ImmutableList<WorldSessionSummaryResponse> = persistentListOf(),
    val featuredWorlds: ImmutableList<WorldResponse> = persistentListOf(),
    val isLoading: Boolean = true,
    val isLoadingFeatured: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val nextCursor: String? = null,
    val hasMore: Boolean = true,
    val sessionToDelete: WorldSessionSummaryResponse? = null,
    val worldsAtLimit: Boolean = false,
    val usage: UsageResponse? = null,
)

sealed interface HomeEvent {
    data class NavigateToWorld(val worldId: String) : HomeEvent
    data class NavigateToSession(val sessionId: String) : HomeEvent
    data class NavigateToStoryNode(val sessionId: String, val nodeId: String) : HomeEvent
    data class ShowMessage(val message: String) : HomeEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val worldRepository: WorldRepository,
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val analytics: CosmoAnalytics,
    val regionDetector: RegionDetector,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadSessions()
        loadFeaturedWorlds()
        checkUsageLimits()
        viewModelScope.launch { regionDetector.detect() }
    }

    fun loadSessions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = sessionRepository.getSessions()
                _uiState.update {
                    it.copy(
                        sessions = response.items.toImmutableList(),
                        isLoading = false,
                        nextCursor = response.nextCursor,
                        hasMore = response.nextCursor != null,
                    )
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to load sessions")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load your stories. Please try again.",
                    )
                }
            }
        }
    }

    private fun loadFeaturedWorlds() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFeatured = true) }
            try {
                val featured = worldRepository.getFeaturedWorlds()
                _uiState.update {
                    it.copy(
                        featuredWorlds = featured.toImmutableList(),
                        isLoadingFeatured = false,
                    )
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to load featured worlds")
                _uiState.update { it.copy(isLoadingFeatured = false) }
            }
        }
    }

    fun loadMore() {
        val cursor = _uiState.value.nextCursor ?: return
        if (_uiState.value.isLoadingMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                val response = sessionRepository.getSessions(cursor)
                _uiState.update {
                    it.copy(
                        sessions = (it.sessions + response.items).toImmutableList(),
                        isLoadingMore = false,
                        nextCursor = response.nextCursor,
                        hasMore = response.nextCursor != null,
                    )
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to load more sessions")
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                val response = sessionRepository.getSessions(fresh = true)
                _uiState.update {
                    it.copy(
                        sessions = response.items.toImmutableList(),
                        isRefreshing = false,
                        nextCursor = response.nextCursor,
                        hasMore = response.nextCursor != null,
                        error = null,
                    )
                }
                checkUsageLimits()
                loadFeaturedWorlds()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to refresh worlds")
                _uiState.update { it.copy(isRefreshing = false) }
                _events.send(HomeEvent.ShowMessage("Failed to refresh. Please try again."))
            }
        }
    }

    fun onSessionClick(session: WorldSessionSummaryResponse) {
        viewModelScope.launch {
            _events.send(HomeEvent.NavigateToSession(session.id))
        }
    }

    fun onPlayClick(session: WorldSessionSummaryResponse) {
        viewModelScope.launch {
            val nodeId = session.lastVisitedNodeId ?: session.world.rootNodeId
            if (nodeId != null) {
                _events.send(HomeEvent.NavigateToStoryNode(session.id, nodeId))
            } else {
                _events.send(HomeEvent.NavigateToSession(session.id))
            }
        }
    }

    fun requestDelete(session: WorldSessionSummaryResponse) {
        _uiState.update { it.copy(sessionToDelete = session) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(sessionToDelete = null) }
    }

    fun confirmDelete() {
        val session = _uiState.value.sessionToDelete ?: return
        _uiState.update { it.copy(sessionToDelete = null) }

        viewModelScope.launch {
            try {
                sessionRepository.deleteSession(session.id)
                _uiState.update { state ->
                    state.copy(sessions = state.sessions.filter { it.id != session.id }.toImmutableList())
                }
                _events.send(HomeEvent.ShowMessage("\"${session.world.title ?: "Story"}\" removed"))
                checkUsageLimits()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to delete world")
                _events.send(HomeEvent.ShowMessage("Failed to delete. Please try again."))
            }
        }
    }

    /**
     * Re-fetch usage data from the server. Called on pull-to-refresh and on app
     * foreground resume to detect tier changes made on the web (post-upgrade handling).
     */
    fun refreshUsage() {
        viewModelScope.launch {
            try {
                userRepository.invalidate()
                val usage = userRepository.fetchFresh()
                _uiState.update {
                    it.copy(
                        worldsAtLimit = usage.worldsCreated >= usage.worldsLimit,
                        usage = usage,
                    )
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to refresh usage")
            }
        }
    }

    fun trackFeaturedWorldClick(worldId: String) {
        analytics.trackEvent(AnalyticsEvent.FeaturedWorldClicked(worldId = worldId))
    }

    private fun checkUsageLimits() {
        viewModelScope.launch {
            try {
                val usage = userRepository.getUsage()
                _uiState.update {
                    it.copy(
                        worldsAtLimit = usage.worldsCreated >= usage.worldsLimit,
                        usage = usage,
                    )
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to check usage limits")
            }
        }
    }
}
