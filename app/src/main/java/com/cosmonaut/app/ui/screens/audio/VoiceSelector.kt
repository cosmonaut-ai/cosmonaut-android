package com.cosmonaut.app.ui.screens.audio

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.data.remote.dto.VoiceResponse
import com.cosmonaut.app.ui.theme.CosmoTheme

/**
 * Voice selector list with sample playback.
 * Mirrors the web's VoicePicker component with play/pause preview per voice.
 */
@Composable
fun VoiceSelector(
    voices: List<VoiceResponse>,
    selectedVoiceId: String,
    onVoiceSelect: (String) -> Unit,
    onPauseMainAudio: () -> Unit = {},
    onResumeMainAudio: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var playingVoiceId by remember { mutableStateOf<String?>(null) }
    var samplePlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var mainAudioWasPaused by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            samplePlayer?.release()
            samplePlayer = null
            if (mainAudioWasPaused) {
                onResumeMainAudio()
            }
        }
    }

    fun stopSample() {
        samplePlayer?.release()
        samplePlayer = null
        playingVoiceId = null
        if (mainAudioWasPaused) {
            mainAudioWasPaused = false
            onResumeMainAudio()
        }
    }

    fun toggleSample(voice: VoiceResponse) {
        if (playingVoiceId == voice.id) {
            stopSample()
            return
        }

        stopSample()

        onPauseMainAudio()
        mainAudioWasPaused = true

        try {
            val player = MediaPlayer().apply {
                setDataSource(voice.sampleUrl)
                setOnCompletionListener {
                    playingVoiceId = null
                    mainAudioWasPaused = false
                    onResumeMainAudio()
                }
                setOnErrorListener { _, _, _ ->
                    stopSample()
                    true
                }
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
            samplePlayer = player
            playingVoiceId = voice.id
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            stopSample()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        voices.forEach { voice ->
            val isSelected = voice.id == selectedVoiceId
            VoiceItem(
                voice = voice,
                isSelected = isSelected,
                isPlaying = playingVoiceId == voice.id,
                onSelect = {
                    stopSample()
                    onVoiceSelect(voice.id)
                },
                onToggleSample = { toggleSample(voice) },
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun VoiceItem(
    voice: VoiceResponse,
    isSelected: Boolean,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onToggleSample: () -> Unit,
) {
    val bgColor = if (isSelected) {
        CosmoTheme.colors.primary.copy(alpha = 0.08f)
    } else {
        CosmoTheme.colors.background
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Selection indicator
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = CosmoTheme.colors.primary,
                modifier = Modifier.size(16.dp),
            )
        } else {
            Spacer(modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Voice info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = voice.displayName,
                style = MaterialTheme.typography.bodyMedium,
                color = CosmoTheme.colors.foreground,
            )
            Text(
                text = voice.description,
                style = MaterialTheme.typography.bodySmall,
                color = CosmoTheme.colors.mutedForeground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Sample play/pause button
        IconButton(
            onClick = onToggleSample,
            modifier = Modifier.size(32.dp),
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Stop sample" else "Play sample",
                tint = CosmoTheme.colors.mutedForeground,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
