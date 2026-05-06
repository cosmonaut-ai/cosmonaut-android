package com.cosmonaut.app.ui.screens.audio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.audio.AudioPlaybackState
import com.cosmonaut.app.audio.AudioTrackInfo
import com.cosmonaut.app.ui.theme.CosmoTheme

/**
 * Spotify-style compact mini-player bar.
 * Positioned between page content and bottom navigation bar.
 * Tapping the bar expands to the full player bottom sheet.
 */
@Composable
fun MiniPlayer(
    isVisible: Boolean,
    playbackState: AudioPlaybackState,
    trackInfo: AudioTrackInfo?,
    isGenerating: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    onClose: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CosmoTheme.colors.card)
                .clickable(enabled = !isGenerating, onClick = onExpand),
        ) {
            // Progress bar at top of mini-player
            val progress = if (playbackState.durationMs > 0) {
                playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()
            } else {
                0f
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = CosmoTheme.colors.primary,
                trackColor = CosmoTheme.colors.border,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = CosmoTheme.colors.primary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = trackInfo?.nodeTitle ?: "Story Narration",
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmoTheme.colors.foreground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Generating…",
                            style = MaterialTheme.typography.labelSmall,
                            color = CosmoTheme.colors.mutedForeground,
                        )
                    }
                } else {
                    // Skip back
                    IconButton(
                        onClick = onSkipBack,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay5,
                            contentDescription = "Skip back 5 seconds",
                            tint = CosmoTheme.colors.foreground,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    // Play/Pause button
                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) {
                                Icons.Filled.Pause
                            } else {
                                Icons.Filled.PlayArrow
                            },
                            contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                            tint = CosmoTheme.colors.foreground,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    // Skip forward
                    IconButton(
                        onClick = onSkipForward,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forward5,
                            contentDescription = "Skip forward 5 seconds",
                            tint = CosmoTheme.colors.foreground,
                            modifier = Modifier.size(18.dp),
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Track title
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = trackInfo?.nodeTitle ?: "Story Narration",
                            style = MaterialTheme.typography.bodySmall,
                            color = CosmoTheme.colors.foreground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (playbackState.durationMs > 0) {
                            Text(
                                text = "${formatTime(
                                    playbackState.currentPositionMs
                                )} / ${formatTime(playbackState.durationMs)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CosmoTheme.colors.mutedForeground,
                            )
                        }
                    }
                }

                // Close button (always available)
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close player",
                        tint = CosmoTheme.colors.mutedForeground,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

internal fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}
