package com.cosmonaut.app.ui.screens.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmonaut.app.analytics.AnalyticsEvent
import com.cosmonaut.app.analytics.CosmoAnalytics
import com.cosmonaut.app.audio.AudioPlayerManager
import com.cosmonaut.app.audio.AudioTrackInfo
import com.cosmonaut.app.data.local.CosmoPreferences
import com.cosmonaut.app.data.remote.asApiError
import com.cosmonaut.app.data.remote.dto.AudioEntryResponse
import com.cosmonaut.app.data.remote.dto.VoiceResponse
import com.cosmonaut.app.data.repository.AudioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

private const val DEFAULT_VOICE_ID = "theo"
private const val MAX_NARRATION_CHARS = 3000

sealed interface AudioNarrationEvent {
    data class Error(val message: String) : AudioNarrationEvent
    data object QuotaExceeded : AudioNarrationEvent
    data class RateLimited(val message: String) : AudioNarrationEvent
}

data class NarrationAvailability(
    val isEnabled: Boolean = false,
    val disabledMessage: String? = "Narration available once story is generated",
)

/**
 * ViewModel managing the audio narration feature for the story reader.
 *
 * Mirrors the web's AudioNarration.svelte component logic:
 * - Manages voice selection with persistence
 * - Handles audio generation (request + error handling)
 * - Coordinates with AudioPlayerManager for playback
 * - Tracks per-node audio cache (voice → URL mapping)
 *
 * This ViewModel is scoped to the Activity (not per-screen) so the audio
 * state persists across node navigation, matching the web behavior where
 * the player bar persists until explicitly closed.
 */
@HiltViewModel
class AudioNarrationViewModel @Inject constructor(
    private val audioRepository: AudioRepository,
    private val playerManager: AudioPlayerManager,
    private val preferences: CosmoPreferences,
    private val analytics: CosmoAnalytics,
) : ViewModel() {

    private val _voices = MutableStateFlow<List<VoiceResponse>>(emptyList())
    val voices: StateFlow<List<VoiceResponse>> = _voices.asStateFlow()

    private val _selectedVoiceId = MutableStateFlow<String?>(null)
    val selectedVoiceId: StateFlow<String?> = _selectedVoiceId.asStateFlow()

    private val _events = MutableStateFlow<AudioNarrationEvent?>(null)
    val events: StateFlow<AudioNarrationEvent?> = _events.asStateFlow()

    // Current node context
    private var currentSessionId: String? = null
    private var currentWorldId: String? = null
    private var currentNodeId: String? = null
    private var currentNodeTitle: String? = null
    private var currentNodeTextLength: Int = 0
    private var currentNodeCompleted: Boolean = false

    // Per-node audio URL cache (voice_id → audio_url)
    private var nodeAudioCache: Map<String, AudioEntryResponse> = emptyMap()

    private val _narrationAvailability = MutableStateFlow(NarrationAvailability())
    val narrationAvailability: StateFlow<NarrationAvailability> = _narrationAvailability.asStateFlow()

    val playbackState = playerManager.playbackState
    val trackInfo = playerManager.trackInfo
    val isPlayerVisible = playerManager.isPlayerVisible
    val isGenerating = playerManager.isGenerating

    init {
        loadVoices()
        loadPersistedPreferences()
    }

    private fun updateNarrationAvailability() {
        val isEnabled = currentNodeCompleted && currentNodeTextLength <= MAX_NARRATION_CHARS
        val message = when {
            currentNodeTextLength > MAX_NARRATION_CHARS ->
                "Too long for narration ($currentNodeTextLength / $MAX_NARRATION_CHARS chars)"
            !currentNodeCompleted ->
                "Narration available once story is generated"
            else -> null
        }
        _narrationAvailability.value = NarrationAvailability(isEnabled, message)
    }

    /**
     * Update the current node context. Called by StoryReaderScreen when node data loads.
     */
    fun setNodeContext(
        sessionId: String,
        worldId: String,
        nodeId: String,
        nodeTitle: String?,
        nodeTextLength: Int,
        isCompleted: Boolean,
        audioEntries: Map<String, AudioEntryResponse>,
    ) {
        val nodeChanged = currentNodeId != nodeId

        currentSessionId = sessionId
        currentWorldId = worldId
        currentNodeId = nodeId
        currentNodeTitle = nodeTitle
        currentNodeTextLength = nodeTextLength
        currentNodeCompleted = isCompleted
        nodeAudioCache = audioEntries

        updateNarrationAvailability()

        if (nodeChanged) {
            playerManager.resetForNodeChange()
        }
    }

    /**
     * Toggle audio narration (speaker icon in toolbar).
     * If the player is already open (generating or playing), does nothing.
     */
    fun toggleNarration() {
        if (playerManager.isPlayerVisible.value) return
        activateNarration()
    }

    fun togglePlayPause() = playerManager.togglePlayPause()

    fun skipForward() = playerManager.skipForward()

    fun skipBack() = playerManager.skipBack()

    fun pauseForSample() = playerManager.pause()

    fun resumeAfterSample() = playerManager.resume()

    fun seekToFraction(fraction: Float) = playerManager.seekToFraction(fraction)

    fun setVolume(volume: Float) {
        playerManager.setVolume(volume)
        viewModelScope.launch { preferences.setAudioVolume(volume) }
    }

    fun setPlaybackSpeed(speed: Float) {
        playerManager.setPlaybackSpeed(speed)
        viewModelScope.launch { preferences.setAudioPlaybackSpeed(speed) }
    }

    fun closePlayer() = playerManager.closePlayer()

    fun consumeEvent() {
        _events.value = null
    }

    fun selectVoice(voiceId: String) {
        _selectedVoiceId.value = voiceId
        viewModelScope.launch { preferences.setAudioVoiceId(voiceId) }

        // Stop current playback and transition to generating/loading state
        playerManager.stopPlaybackForRegeneration()

        val existingAudio = nodeAudioCache[voiceId]
        if (existingAudio != null) {
            playAudioUrl(existingAudio.audioUrl)
        } else if (currentNodeCompleted) {
            generateForVoice(voiceId)
        }
    }

    private fun activateNarration() {
        val voiceId = resolveEffectiveVoiceId() ?: return
        playerManager.ensureServiceStarted()

        val worldId = currentWorldId
        val nodeId = currentNodeId
        if (worldId != null && nodeId != null) {
            analytics.trackEvent(AnalyticsEvent.NarrationStarted(worldId = worldId, nodeId = nodeId))
        }

        playerManager.showPlayerForTrack(currentNodeTitle)

        val existingAudio = nodeAudioCache[voiceId]
        if (existingAudio != null) {
            playAudioUrl(existingAudio.audioUrl)
        } else if (currentNodeCompleted) {
            generateForVoice(voiceId)
        }
    }

    private fun generateForVoice(voiceId: String) {
        val sessionId = currentSessionId ?: return
        val nodeId = currentNodeId ?: return

        playerManager.setGenerating(true)

        viewModelScope.launch {
            try {
                val result = audioRepository.generateNodeAudio(sessionId, nodeId, voiceId)

                // Update local cache
                nodeAudioCache = nodeAudioCache + (
                    voiceId to AudioEntryResponse(
                        audioUrl = result.audioUrl,
                        timestampsUrl = result.timestampsUrl,
                    )
                    )

                playerManager.setGenerating(false)
                playAudioUrl(result.audioUrl)
            } catch (e: Exception) {
                playerManager.setGenerating(false)
                handleGenerationError(e)
            }
        }
    }

    private fun playAudioUrl(url: String) {
        val worldId = currentWorldId ?: return
        val nodeId = currentNodeId ?: return
        val voiceId = resolveEffectiveVoiceId() ?: return

        playerManager.playAudio(
            AudioTrackInfo(
                worldId = worldId,
                nodeId = nodeId,
                voiceId = voiceId,
                audioUrl = url,
                nodeTitle = currentNodeTitle,
            ),
        )
    }

    private fun handleGenerationError(error: Exception) {
        val apiError = error.asApiError()
        when {
            apiError?.isRateLimited == true -> {
                playerManager.closePlayer()
                _events.value = AudioNarrationEvent.RateLimited(
                    "You're generating audio too quickly. Please wait a moment.",
                )
            }
            apiError?.isQuotaExceeded == true -> {
                playerManager.closePlayer()
                _events.value = AudioNarrationEvent.QuotaExceeded
            }
            else -> {
                playerManager.closePlayer()
                Timber.e(error, "Audio generation failed")
                _events.value = AudioNarrationEvent.Error(
                    error.message ?: "Audio generation failed. Please try again.",
                )
            }
        }
    }

    private fun resolveEffectiveVoiceId(): String? {
        val voices = _voices.value
        val selected = _selectedVoiceId.value

        if (selected != null && voices.any { it.id == selected }) return selected
        if (voices.any { it.id == DEFAULT_VOICE_ID }) return DEFAULT_VOICE_ID
        return voices.firstOrNull()?.id
    }

    private fun loadPersistedPreferences() {
        viewModelScope.launch {
            val voiceId = preferences.audioVoiceId.first()
            if (voiceId != null) _selectedVoiceId.value = voiceId

            val speed = preferences.audioPlaybackSpeed.first()
            playerManager.setPlaybackSpeed(speed)

            val volume = preferences.audioVolume.first()
            playerManager.setVolume(volume)
        }
    }

    private fun loadVoices() {
        viewModelScope.launch {
            try {
                val voices = audioRepository.getVoices()
                _voices.value = voices
                if (_selectedVoiceId.value == null) {
                    _selectedVoiceId.value = resolveEffectiveVoiceId()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load voices")
            }
        }
    }
}
