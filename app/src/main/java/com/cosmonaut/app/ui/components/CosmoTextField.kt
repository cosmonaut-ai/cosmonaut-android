package com.cosmonaut.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.ui.theme.CosmoTheme

private val FIELD_SHAPE = RoundedCornerShape(10.dp)
private const val BORDER_ALPHA_UNFOCUSED = 0.5f
private const val BG_ALPHA = 0.3f

@Composable
fun CosmoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    labelTrailing: @Composable (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> CosmoTheme.colors.destructive
            isFocused -> CosmoTheme.colors.ring
            else -> CosmoTheme.colors.border.copy(alpha = BORDER_ALPHA_UNFOCUSED)
        },
        label = "borderColor",
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = if (isError) {
                    CosmoTheme.colors.destructive
                } else {
                    CosmoTheme.colors.foreground
                },
            )
            labelTrailing?.invoke()
        }

        Spacer(modifier = Modifier.height(6.dp))

        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = CosmoTheme.colors.foreground,
            ),
            cursorBrush = SolidColor(CosmoTheme.colors.primary),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(FIELD_SHAPE)
                        .background(CosmoTheme.colors.input.copy(alpha = BG_ALPHA))
                        .border(1.dp, borderColor, FIELD_SHAPE)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CosmoTheme.colors.mutedForeground.copy(alpha = 0.6f),
                                )
                            }
                            innerTextField()
                        }
                        if (trailingIcon != null) {
                            CompositionLocalProvider(
                                LocalContentColor provides CosmoTheme.colors.mutedForeground,
                            ) {
                                trailingIcon()
                            }
                        }
                    }
                }
            },
        )

        if (supportingContent != null) {
            Spacer(modifier = Modifier.height(4.dp))
            supportingContent()
        } else if (supportingText != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = supportingText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) {
                    CosmoTheme.colors.destructive
                } else {
                    CosmoTheme.colors.mutedForeground
                },
            )
        }
    }
}
