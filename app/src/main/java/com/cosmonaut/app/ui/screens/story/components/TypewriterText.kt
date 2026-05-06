package com.cosmonaut.app.ui.screens.story.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import com.cosmonaut.app.ui.theme.CosmoTheme
import kotlinx.coroutines.delay

private val FLAVOR_PHRASES = listOf(
    "Weaving the threads of fate...",
    "Charting unknown territories...",
    "The stars align...",
    "Crafting your destiny...",
    "Consulting the cosmos...",
    "Unfurling the narrative...",
    "A new chapter begins...",
    "The story takes shape...",
)

private const val CHAR_DELAY_MS = 35L
private const val PHRASE_PAUSE_MS = 1500L
private const val DELETE_DELAY_MS = 20L

/**
 * Rotating typewriter flavor text shown while waiting for the first streaming token.
 * Displays one phrase at a time, typing it out character-by-character, pausing,
 * then deleting before showing the next phrase.
 */
@Composable
fun TypewriterText(modifier: Modifier = Modifier) {
    var displayText by remember { mutableStateOf("") }
    var phraseIndex by remember { mutableIntStateOf((FLAVOR_PHRASES.indices).random()) }

    LaunchedEffect(Unit) {
        while (true) {
            val phrase = FLAVOR_PHRASES[phraseIndex % FLAVOR_PHRASES.size]

            for (i in 1..phrase.length) {
                displayText = phrase.substring(0, i)
                delay(CHAR_DELAY_MS)
            }

            delay(PHRASE_PAUSE_MS)

            for (i in phrase.length downTo 0) {
                displayText = phrase.substring(0, i)
                delay(DELETE_DELAY_MS)
            }

            delay(200L)
            phraseIndex++
        }
    }

    Text(
        text = displayText,
        style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
        color = CosmoTheme.colors.mutedForeground,
        modifier = modifier,
    )
}
