package com.cosmonaut.app.ui.screens.audio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.audio.AudioPlaybackState
import com.cosmonaut.app.audio.AudioTrackInfo
import com.cosmonaut.app.data.remote.dto.VoiceResponse
import com.cosmonaut.app.ui.theme.CosmoTheme

val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandedPlayer(
    playbackState: AudioPlaybackState,
    trackInfo: AudioTrackInfo?,
    voices: List<VoiceResponse>,
    selectedVoiceId: String?,
    isGenerating: Boolean,
    onTogglePlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onVoiceSelect: (String) -> Unit,
    onPauseMainAudio: () -> Unit,
    onResumeMainAudio: () -> Unit,
    onClose: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CosmoTheme.colors.card,
        contentColor = CosmoTheme.colors.cardForeground,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trackInfo?.nodeTitle ?: "Story Narration",
                        style = MaterialTheme.typography.titleMedium,
                        color = CosmoTheme.colors.foreground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Cosmonaut",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmoTheme.colors.mutedForeground,
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close player",
                        tint = CosmoTheme.colors.mutedForeground,
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Seek slider
            val seekProgress = if (playbackState.durationMs > 0) {
                playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()
            } else {
                0f
            }

            Slider(
                value = seekProgress,
                onValueChange = onSeek,
                enabled = !isGenerating,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = CosmoTheme.colors.primary,
                    activeTrackColor = CosmoTheme.colors.primary,
                    inactiveTrackColor = CosmoTheme.colors.muted,
                ),
            )

            // Time labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (isGenerating) "0:00" else formatTime(playbackState.currentPositionMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmoTheme.colors.mutedForeground,
                )
                Text(
                    text = if (isGenerating) {
                        "Generating…"
                    } else {
                        formatTime(playbackState.durationMs)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmoTheme.colors.mutedForeground,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Play/Pause button or spinner
            if (isGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = CosmoTheme.colors.primary,
                    strokeWidth = 3.dp,
                )
            } else {
                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(64.dp),
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) {
                            Icons.Filled.Pause
                        } else {
                            Icons.Filled.PlayArrow
                        },
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = CosmoTheme.colors.primary,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Volume control
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val volumeIcon = when {
                    playbackState.volume == 0f -> Icons.AutoMirrored.Filled.VolumeMute
                    playbackState.volume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                    else -> Icons.AutoMirrored.Filled.VolumeUp
                }
                Icon(
                    imageVector = volumeIcon,
                    contentDescription = "Volume",
                    tint = CosmoTheme.colors.mutedForeground,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = playbackState.volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = CosmoTheme.colors.mutedForeground,
                        activeTrackColor = CosmoTheme.colors.mutedForeground,
                        inactiveTrackColor = CosmoTheme.colors.muted,
                    ),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playback speed selector
            Text(
                text = "Playback Speed",
                style = MaterialTheme.typography.labelMedium,
                color = CosmoTheme.colors.mutedForeground,
                modifier = Modifier.align(Alignment.Start),
            )
            Spacer(modifier = Modifier.height(8.dp))
            SpeedSelector(
                currentSpeed = playbackState.playbackSpeed,
                onSpeedSelected = onSpeedChange,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Voice picker
            if (voices.isNotEmpty() && selectedVoiceId != null) {
                Text(
                    text = "Voice",
                    style = MaterialTheme.typography.labelMedium,
                    color = CosmoTheme.colors.mutedForeground,
                    modifier = Modifier.align(Alignment.Start),
                )
                Spacer(modifier = Modifier.height(8.dp))
                VoiceSelector(
                    voices = voices,
                    selectedVoiceId = selectedVoiceId,
                    onVoiceSelect = onVoiceSelect,
                    onPauseMainAudio = onPauseMainAudio,
                    onResumeMainAudio = onResumeMainAudio,
                )
            }
        }
    }
}

@Composable
private fun SpeedSelector(currentSpeed: Float, onSpeedSelected: (Float) -> Unit,) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        SPEED_OPTIONS.forEach { speed ->
            val isSelected = speed == currentSpeed
            val textColor = if (isSelected) CosmoTheme.colors.primary else CosmoTheme.colors.mutedForeground
            val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

            Text(
                text = formatSpeed(speed),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = fontWeight),
                color = textColor,
                modifier = Modifier
                    .clickable { onSpeedSelected(speed) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

private fun formatSpeed(speed: Float): String =
    if (speed == speed.toLong().toFloat()) "${speed.toInt()}x" else "${speed}x"
