package com.cosmonaut.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmonaut.app.data.billing.RegionDetector
import com.cosmonaut.app.data.remote.dto.UsageResponse
import com.cosmonaut.app.data.remote.dto.WorldProgressResponse
import com.cosmonaut.app.data.remote.dto.WorldResponse
import com.cosmonaut.app.data.repository.UserRepository
import com.cosmonaut.app.data.repository.WorldRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

data class HomeUiState(
    val worlds: List<WorldResponse> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val nextCursor: String? = null,
    val hasMore: Boolean = true,
    val worldToDelete: WorldResponse? = null,
    val worldsAtLimit: Boolean = false,
    val usage: UsageResponse? = null,
)

sealed interface HomeEvent {
    data class NavigateToWorld(val worldId: String) : HomeEvent
    data class NavigateToStoryNode(val worldId: String, val nodeId: String) : HomeEvent
    data class ShowMessage(val message: String) : HomeEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val worldRepository: WorldRepository,
    private val userRepository: UserRepository,
    val regionDetector: RegionDetector,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadWorlds()
        checkUsageLimits()
        viewModelScope.launch { regionDetector.detect() }
    }

    fun loadWorlds() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val response = worldRepository.getWorlds()
                _uiState.update {
                    it.copy(
                        worlds = response.items,
                        isLoading = false,
                        nextCursor = response.nextCursor,
                        hasMore = response.nextCursor != null,
                    )
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to load worlds")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load your stories. Please try again.",
                    )
                }
            }
        }
    }

    fun loadMore() {
        val cursor = _uiState.value.nextCursor ?: return
        if (_uiState.value.isLoadingMore) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            try {
                val response = worldRepository.getWorlds(cursor)
                _uiState.update {
                    it.copy(
                        worlds = it.worlds + response.items,
                        isLoadingMore = false,
                        nextCursor = response.nextCursor,
                        hasMore = response.nextCursor != null,
                    )
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to load more worlds")
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                val response = worldRepository.getWorlds()
                _uiState.update {
                    it.copy(
                        worlds = response.items,
                        isRefreshing = false,
                        nextCursor = response.nextCursor,
                        hasMore = response.nextCursor != null,
                        error = null,
                    )
                }
                checkUsageLimits()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to refresh worlds")
                _uiState.update { it.copy(isRefreshing = false) }
                _events.send(HomeEvent.ShowMessage("Failed to refresh. Please try again."))
            }
        }
    }

    fun onWorldClick(world: WorldResponse) {
        viewModelScope.launch {
            _events.send(HomeEvent.NavigateToWorld(world.id))
        }
    }

    fun onPlayClick(world: WorldResponse) {
        viewModelScope.launch {
            try {
                val progress: WorldProgressResponse = worldRepository.getWorldProgress(world.id)
                val nodeId = progress.currentNodeId ?: world.rootNodeId
                if (nodeId != null) {
                    _events.send(HomeEvent.NavigateToStoryNode(world.id, nodeId))
                } else {
                    _events.send(HomeEvent.NavigateToWorld(world.id))
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Timber.e(e, "Failed to get world progress")
                _events.send(HomeEvent.NavigateToWorld(world.id))
            }
        }
    }

    fun requestDelete(world: WorldResponse) {
        _uiState.update { it.copy(worldToDelete = world) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(worldToDelete = null) }
    }

    fun confirmDelete() {
        val world = _uiState.value.worldToDelete ?: return
        _uiState.update { it.copy(worldToDelete = null) }

        viewModelScope.launch {
            try {
                worldRepository.deleteWorld(world.id)
                _uiState.update { state ->
                    state.copy(worlds = state.worlds.filter { it.id != world.id })
                }
                _events.send(HomeEvent.ShowMessage("\"${world.title ?: "Story"}\" deleted"))
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
