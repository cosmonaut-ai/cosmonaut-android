package com.cosmonaut.app.audio

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Playback state exposed to UI. Mirrors the web's useAudioPlayer reactive state.
 */
data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playbackSpeed: Float = 1f,
    val volume: Float = 1f,
    val isBuffering: Boolean = false,
    val hasEnded: Boolean = false,
)

/**
 * Metadata about what's currently loaded in the player.
 */
data class AudioTrackInfo(
    val worldId: String,
    val nodeId: String,
    val voiceId: String,
    val audioUrl: String,
    val nodeTitle: String? = null,
)

private const val POSITION_UPDATE_INTERVAL_MS = 250L

/**
 * Application-wide singleton managing audio playback state.
 *
 * Architecture:
 * - Owns a reference to the ExoPlayer instance (attached via the AudioPlaybackService).
 * - Exposes playback state as Kotlin StateFlows for Compose consumption.
 * - Manages the mini-player visibility, track info, and voice/speed preferences.
 * - Persists user preferences (selected voice, playback speed) via DataStore.
 *
 * Mirrors the web's `useAudioPlayer` hook but with MediaSession integration for
 * lock screen/notification controls and proper audio focus handling.
 */
@Singleton
class AudioPlayerManager @Inject constructor(@param:ApplicationContext private val context: Context,) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var player: ExoPlayer? = null
    private var positionUpdateJob: kotlinx.coroutines.Job? = null
    private var pendingTrackInfo: AudioTrackInfo? = null

    private val _playbackState = MutableStateFlow(AudioPlaybackState())
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private val _trackInfo = MutableStateFlow<AudioTrackInfo?>(null)
    val trackInfo: StateFlow<AudioTrackInfo?> = _trackInfo.asStateFlow()

    private val _isPlayerVisible = MutableStateFlow(false)
    val isPlayerVisible: StateFlow<Boolean> = _isPlayerVisible.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updateState { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startPositionUpdates() else stopPositionUpdates()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val isBuffering = playbackState == Player.STATE_BUFFERING
            val hasEnded = playbackState == Player.STATE_ENDED
            updateState { it.copy(isBuffering = isBuffering, hasEnded = hasEnded) }

            if (playbackState == Player.STATE_READY) {
                player?.let { p ->
                    updateState { it.copy(durationMs = p.duration.coerceAtLeast(0)) }
                }
            }
        }
    }

    fun attachPlayer(exoPlayer: ExoPlayer) {
        player = exoPlayer
        exoPlayer.addListener(playerListener)

        pendingTrackInfo?.let { track ->
            pendingTrackInfo = null
            playAudio(track)
        }
    }

    fun detachPlayer() {
        player?.removeListener(playerListener)
        player = null
        stopPositionUpdates()
    }

    /**
     * Connect to the MediaSessionService to ensure it's running.
     * Called when the user first activates audio narration.
     */
    fun ensureServiceStarted() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, AudioPlaybackService::class.java),
        )
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            { Timber.d("MediaController connected") },
            MoreExecutors.directExecutor(),
        )
    }

    fun setGenerating(generating: Boolean) {
        _isGenerating.value = generating
    }

    /**
     * Immediately show the player in generating state (before audio is ready).
     * Sets track title and visibility so the user sees immediate feedback.
     */
    fun showPlayerForTrack(nodeTitle: String?) {
        _trackInfo.value = AudioTrackInfo(
            worldId = "",
            nodeId = "",
            voiceId = "",
            audioUrl = "",
            nodeTitle = nodeTitle,
        )
        _isPlayerVisible.value = true
        _isGenerating.value = true
    }

    /**
     * Stop current playback and return to generating state.
     * Used when the user changes voice — player stays visible but shows loading.
     */
    fun stopPlaybackForRegeneration() {
        player?.stop()
        player?.clearMediaItems()
        pendingTrackInfo = null
        _isGenerating.value = true
        updateState {
            AudioPlaybackState(
                playbackSpeed = it.playbackSpeed,
                volume = it.volume,
            )
        }
    }

    /**
     * Load and play audio for a story node narration.
     * If the player isn't ready yet (service still connecting), queues the track
     * for playback once attachPlayer is called.
     */
    fun playAudio(trackInfo: AudioTrackInfo) {
        val p = player
        if (p == null) {
            pendingTrackInfo = trackInfo
            _trackInfo.value = trackInfo
            _isPlayerVisible.value = true
            return
        }
        _trackInfo.value = trackInfo
        _isPlayerVisible.value = true
        _isGenerating.value = false

        val mediaItem = MediaItem.Builder()
            .setUri(trackInfo.audioUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(trackInfo.nodeTitle ?: "Story Narration")
                    .setArtist("Cosmonaut")
                    .build(),
            )
            .build()

        p.setMediaItem(mediaItem)
        p.playbackParameters = p.playbackParameters.withSpeed(_playbackState.value.playbackSpeed)
        p.prepare()
        p.play()
    }

    fun togglePlayPause() {
        val p = player ?: return
        if (_playbackState.value.hasEnded) {
            p.seekTo(0)
            p.play()
        } else if (p.isPlaying) {
            p.pause()
        } else {
            p.play()
        }
    }

    fun seekTo(positionMs: Long) {
        player?.seekTo(positionMs)
        updateState { it.copy(currentPositionMs = positionMs, hasEnded = false) }
    }

    fun seekToFraction(fraction: Float) {
        val durationMs = _playbackState.value.durationMs
        if (durationMs > 0) {
            seekTo((fraction * durationMs).toLong())
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        player?.let { p ->
            p.playbackParameters = p.playbackParameters.withSpeed(speed)
        }
        updateState { it.copy(playbackSpeed = speed) }
    }

    fun setVolume(volume: Float) {
        player?.volume = volume
        updateState { it.copy(volume = volume) }
    }

    fun skipForward(ms: Long = 5000L) {
        val p = player ?: return
        val target = (p.currentPosition + ms).coerceAtMost(p.duration.coerceAtLeast(0))
        seekTo(target)
    }

    fun skipBack(ms: Long = 5000L) {
        val p = player ?: return
        val target = (p.currentPosition - ms).coerceAtLeast(0)
        seekTo(target)
    }

    fun pause() {
        player?.pause()
    }

    fun resume() {
        player?.play()
    }

    fun closePlayer() {
        pendingTrackInfo = null
        player?.stop()
        player?.clearMediaItems()
        _isPlayerVisible.value = false
        _trackInfo.value = null
        _isGenerating.value = false
        updateState { AudioPlaybackState(playbackSpeed = it.playbackSpeed, volume = it.volume) }
    }

    /**
     * Reset player state when navigating to a different node.
     * Stops playback and hides the mini-player.
     */
    fun resetForNodeChange() {
        closePlayer()
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = scope.launch {
            while (true) {
                player?.let { p ->
                    updateState { it.copy(currentPositionMs = p.currentPosition.coerceAtLeast(0)) }
                }
                delay(POSITION_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    private inline fun updateState(transform: (AudioPlaybackState) -> AudioPlaybackState) {
        _playbackState.value = transform(_playbackState.value)
    }
}
