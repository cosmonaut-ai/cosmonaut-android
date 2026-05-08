package com.cosmonaut.app.ui.screens.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmonaut.app.BuildConfig
import com.cosmonaut.app.data.remote.dto.InviteTokenResponse
import com.cosmonaut.app.data.remote.dto.UserInfoResponse
import com.cosmonaut.app.data.remote.dto.WorldResponse
import com.cosmonaut.app.data.repository.WorldRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

private const val AUTOSAVE_DEBOUNCE_MS = 500L
private const val SAVED_FEEDBACK_MS = 2000L
private const val MS_PER_HOUR = 3_600_000L

data class SharedUser(
    val id: String,
    val displayName: String,
)

data class ShareUiState(
    val isLoading: Boolean = true,
    val worldId: String = "",
    val worldTitle: String = "",
    val isOwner: Boolean = true,
    val visibility: String = "private",
    val sharedUsers: List<SharedUser> = emptyList(),
    val inviteToken: InviteTokenResponse? = null,
    val isLoadingInviteToken: Boolean = false,
    val isCreatingToken: Boolean = false,
    val isDeletingToken: Boolean = false,
    val isSaving: Boolean = false,
    val justSaved: Boolean = false,
    val userToRemove: SharedUser? = null,
    val showPrivateConfirm: Boolean = false,
    val pendingVisibility: String? = null,
)

sealed interface ShareEvent {
    data class ShowMessage(val message: String) : ShareEvent
    data class WorldUpdated(val world: WorldResponse) : ShareEvent
}

@HiltViewModel
class ShareBottomSheetViewModel @Inject constructor(
    private val worldRepository: WorldRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ShareEvent>()
    val events = _events.asSharedFlow()

    private var autosaveJob: Job? = null
    private var savedFeedbackJob: Job? = null

    private var originalVisibility: String = "private"
    private var originalSharedWith: List<String> = emptyList()

    fun initialize(world: WorldResponse, currentUserId: String?) {
        val isOwner = currentUserId != null && world.authorId == currentUserId
        originalVisibility = world.visibility ?: "private"
        originalSharedWith = world.sharedWith ?: emptyList()

        _uiState.update {
            it.copy(
                isLoading = false,
                worldId = world.id,
                worldTitle = world.title ?: "Untitled Story",
                isOwner = isOwner,
                visibility = originalVisibility,
                sharedUsers = emptyList(),
            )
        }

        resolveUsers(originalSharedWith)

        if (isOwner && originalVisibility == "private") {
            loadInviteToken(world.id)
        }
    }

    private fun resolveUsers(userIds: List<String>) {
        if (userIds.isEmpty()) {
            _uiState.update { it.copy(sharedUsers = emptyList()) }
            return
        }
        viewModelScope.launch {
            val users = try {
                worldRepository.batchLookupUsers(userIds).map { info ->
                    SharedUser(id = info.id, displayName = info.displayName)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to resolve shared users")
                userIds.map { SharedUser(id = it, displayName = it.take(8)) }
            }
            _uiState.update { it.copy(sharedUsers = users) }
        }
    }

    private fun loadInviteToken(worldId: String) {
        _uiState.update { it.copy(isLoadingInviteToken = true) }
        viewModelScope.launch {
            try {
                val token = worldRepository.getInviteToken(worldId)
                _uiState.update { it.copy(inviteToken = token, isLoadingInviteToken = false) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load invite token")
                _uiState.update { it.copy(isLoadingInviteToken = false) }
            }
        }
    }

    fun onVisibilityChange(newVisibility: String) {
        val state = _uiState.value
        if (!state.isOwner) return

        if (newVisibility == "private" && state.visibility != "private") {
            _uiState.update {
                it.copy(pendingVisibility = newVisibility, showPrivateConfirm = true)
            }
        } else {
            _uiState.update { it.copy(visibility = newVisibility) }
            if (newVisibility == "private" && state.inviteToken == null && !state.isLoadingInviteToken) {
                loadInviteToken(state.worldId)
            }
            scheduleSave()
        }
    }

    fun confirmPrivateSwitch() {
        val pending = _uiState.value.pendingVisibility ?: return
        _uiState.update {
            it.copy(
                visibility = pending,
                pendingVisibility = null,
                showPrivateConfirm = false,
            )
        }
        loadInviteToken(_uiState.value.worldId)
        scheduleSave()
    }

    fun cancelPrivateSwitch() {
        _uiState.update { it.copy(pendingVisibility = null, showPrivateConfirm = false) }
    }

    fun confirmRemoveUser(user: SharedUser) {
        _uiState.update { it.copy(userToRemove = user) }
    }

    fun dismissRemoveUser() {
        _uiState.update { it.copy(userToRemove = null) }
    }

    fun executeRemoveUser() {
        val user = _uiState.value.userToRemove ?: return
        _uiState.update {
            it.copy(
                sharedUsers = it.sharedUsers.filter { u -> u.id != user.id },
                userToRemove = null,
            )
        }
        scheduleSave()
    }

    fun createInviteToken() {
        val worldId = _uiState.value.worldId
        _uiState.update { it.copy(isCreatingToken = true) }
        viewModelScope.launch {
            try {
                val token = worldRepository.createInviteToken(worldId)
                _uiState.update { it.copy(inviteToken = token, isCreatingToken = false) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to create invite token")
                _uiState.update { it.copy(isCreatingToken = false) }
                _events.emit(ShareEvent.ShowMessage("Failed to create invite link"))
            }
        }
    }

    fun deleteInviteToken() {
        val worldId = _uiState.value.worldId
        _uiState.update { it.copy(isDeletingToken = true) }
        viewModelScope.launch {
            try {
                worldRepository.deleteInviteToken(worldId)
                _uiState.update { it.copy(inviteToken = null, isDeletingToken = false) }
                _events.emit(ShareEvent.ShowMessage("Invite link deleted"))
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete invite token")
                _uiState.update { it.copy(isDeletingToken = false) }
                _events.emit(ShareEvent.ShowMessage("Failed to delete invite link"))
            }
        }
    }

    fun getWorldLink(): String {
        val baseUrl = BuildConfig.WEB_BASE_URL.trimEnd('/')
        return "$baseUrl/worlds/${_uiState.value.worldId}"
    }

    fun getExpiryText(token: InviteTokenResponse): String {
        return try {
            val expires = java.time.Instant.parse(token.expiresAt)
            val now = java.time.Instant.now()
            val hoursLeft = maxOf(0, (expires.toEpochMilli() - now.toEpochMilli()) / MS_PER_HOUR)
            if (hoursLeft <= 1L) "Expires in less than an hour"
            else "Expires in $hoursLeft hours"
        } catch (e: Exception) {
            "Expires soon"
        }
    }

    private fun hasUnsavedChanges(): Boolean {
        val state = _uiState.value
        if (state.visibility != originalVisibility) return true
        val currentIds = state.sharedUsers.map { it.id }
        if (currentIds.size != originalSharedWith.size) return true
        return !currentIds.zip(originalSharedWith).all { (a, b) -> a == b }
    }

    private fun scheduleSave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(AUTOSAVE_DEBOUNCE_MS)
            if (!hasUnsavedChanges()) return@launch
            triggerSave()
        }
    }

    private fun triggerSave() {
        val state = _uiState.value
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val updated = worldRepository.updateWorldSharing(
                    worldId = state.worldId,
                    visibility = state.visibility,
                    sharedWith = state.sharedUsers.map { it.id },
                )
                originalVisibility = updated.visibility ?: "private"
                originalSharedWith = updated.sharedWith ?: emptyList()

                _uiState.update { it.copy(isSaving = false) }
                markSaved()
                _events.emit(ShareEvent.WorldUpdated(updated))
            } catch (e: Exception) {
                Timber.e(e, "Failed to update sharing settings")
                _uiState.update { it.copy(isSaving = false) }
                _events.emit(ShareEvent.ShowMessage("Failed to update sharing settings"))
            }
        }
    }

    private fun markSaved() {
        _uiState.update { it.copy(justSaved = true) }
        savedFeedbackJob?.cancel()
        savedFeedbackJob = viewModelScope.launch {
            delay(SAVED_FEEDBACK_MS)
            _uiState.update { it.copy(justSaved = false) }
        }
    }

    fun reset() {
        autosaveJob?.cancel()
        savedFeedbackJob?.cancel()
        _uiState.value = ShareUiState()
    }
}
