package com.cosmonaut.app.audio

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Media3 foreground service providing background audio playback with
 * lock screen and notification controls via MediaSession.
 *
 * The service is automatically started/stopped by Media3 when a MediaSession
 * is active and has media to play. It handles audio focus, duck-on-interruption,
 * and auto-pause on headphone disconnect via the audio attributes configuration.
 */
@AndroidEntryPoint
class AudioPlaybackService : MediaSessionService() {

    @Inject
    lateinit var playerManager: AudioPlayerManager

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()

        mediaSession = MediaSession.Builder(this, player).build()
        playerManager.attachPlayer(player)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            playerManager.detachPlayer()
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
