package com.cosmonaut.app.ui.screens.story.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmonaut.app.ui.theme.CosmoTheme

/**
 * Renders story text with paragraph breaks and italic emphasis.
 * Splits on double newlines for paragraphs; transforms `*text*` to italic spans.
 * Optionally shows a blinking cursor after the last paragraph (during streaming).
 */
@Composable
fun StoryText(text: String, showCursor: Boolean = false, modifier: Modifier = Modifier,) {
    val paragraphs = remember(text) { text.split("\n\n").filter { it.isNotBlank() } }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        paragraphs.forEachIndexed { index, paragraph ->
            val isLast = index == paragraphs.lastIndex

            if (isLast && showCursor) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = formatParagraph(paragraph),
                        style = storyTextStyle(),
                        color = CosmoTheme.colors.foreground,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    BlinkingCursor()
                }
            } else {
                Text(
                    text = formatParagraph(paragraph),
                    style = storyTextStyle(),
                    color = CosmoTheme.colors.foreground,
                )
            }
        }

        if (paragraphs.isEmpty() && showCursor) {
            BlinkingCursor()
        }
    }
}

/**
 * Formats a paragraph by converting `*text*` patterns to italic spans.
 */
private fun formatParagraph(text: String): AnnotatedString = buildAnnotatedString {
    var remaining = text.trim()

    while (remaining.isNotEmpty()) {
        val startIdx = remaining.indexOf('*')
        if (startIdx == -1) {
            append(remaining)
            break
        }

        val endIdx = remaining.indexOf('*', startIdx + 1)
        if (endIdx == -1) {
            append(remaining)
            break
        }

        append(remaining.substring(0, startIdx))

        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
            append(remaining.substring(startIdx + 1, endIdx))
        }

        remaining = remaining.substring(endIdx + 1)
    }
}

@Composable
private fun storyTextStyle() = MaterialTheme.typography.bodyLarge.copy(
    lineHeight = 28.sp,
    letterSpacing = 0.15.sp,
)
