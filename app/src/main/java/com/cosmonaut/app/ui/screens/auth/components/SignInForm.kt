package com.cosmonaut.app.ui.screens.auth.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.ui.theme.CosmoTheme

private const val DISABLED_FACE_ALPHA = 0.35f
private const val DISABLED_TEXT_ALPHA = 0.5f
private val DEPTH_SIZE = 4.dp
private val DEPTH_PRESSED_TRANSLATE = 2.dp
private val BUTTON_HEIGHT = 48.dp
private val BUTTON_CORNER = 8.dp

@Composable
fun SignInForm(
    email: String,
    password: String,
    showPassword: Boolean,
    isSubmitting: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onSignIn: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onSwitchToSignUp: () -> Unit,
    onForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fieldColors = cosmoTextFieldColors()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            singleLine = true,
            enabled = !isSubmitting,
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Password") },
            singleLine = true,
            enabled = !isSubmitting,
            colors = fieldColors,
            visualTransformation = if (showPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (showPassword) {
                            Icons.Default.VisibilityOff
                        } else {
                            Icons.Default.Visibility
                        },
                        contentDescription = if (showPassword) "Hide password" else "Show password",
                        tint = CosmoTheme.colors.mutedForeground,
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onSignIn() }),
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onForgotPassword, enabled = !isSubmitting) {
                Text(
                    "Forgot password?",
                    color = CosmoTheme.colors.primary.copy(
                        alpha = if (isSubmitting) DISABLED_FACE_ALPHA else 1f,
                    ),
                )
            }
        }

        CosmoButton(
            text = "Sign In",
            onClick = onSignIn,
            enabled = !isSubmitting && email.isNotBlank() && password.isNotBlank(),
            isLoading = isSubmitting,
        )

        DividerWithText()

        OutlinedButton(
            onClick = onGoogleSignIn,
            enabled = !isSubmitting,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            border = ButtonDefaults.outlinedButtonBorder(enabled = !isSubmitting).copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    CosmoTheme.colors.border.copy(alpha = 0.5f),
                ),
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = CosmoTheme.colors.foreground,
            ),
        ) {
            Text("Continue with Google")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Don't have an account?",
                style = MaterialTheme.typography.bodySmall,
                color = CosmoTheme.colors.mutedForeground,
            )
            TextButton(onClick = onSwitchToSignUp, enabled = !isSubmitting) {
                Text("Sign up", color = CosmoTheme.colors.primary)
            }
        }
    }
}

@Composable
fun CosmoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val effectiveEnabled = enabled && !isLoading

    val translateY by animateDpAsState(
        targetValue = if (isPressed && effectiveEnabled) DEPTH_PRESSED_TRANSLATE else 0.dp,
        label = "depthTranslateY",
    )

    val faceColor = CosmoTheme.colors.primary
    val depthColor = CosmoTheme.colors.primaryDepth
    val bgColor = CosmoTheme.colors.background
    val shape = RoundedCornerShape(BUTTON_CORNER)

    val currentFace = if (effectiveEnabled) {
        faceColor
    } else {
        faceColor.copy(alpha = DISABLED_FACE_ALPHA).compositeOver(bgColor)
    }
    val currentDepth = if (effectiveEnabled) {
        depthColor
    } else {
        depthColor.copy(alpha = DISABLED_FACE_ALPHA).compositeOver(bgColor)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BUTTON_HEIGHT + DEPTH_SIZE),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BUTTON_HEIGHT)
                .align(Alignment.BottomCenter)
                .clip(shape)
                .background(currentDepth),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BUTTON_HEIGHT)
                .offset(y = translateY)
                .clip(shape)
                .background(currentFace)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(color = CosmoTheme.colors.primaryForeground),
                    enabled = effectiveEnabled,
                    role = Role.Button,
                    onClick = { if (!isLoading) onClick() },
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = CosmoTheme.colors.primaryForeground,
                )
            } else {
                Text(
                    text = text,
                    color = if (effectiveEnabled) {
                        CosmoTheme.colors.primaryForeground
                    } else {
                        CosmoTheme.colors.primaryForeground.copy(alpha = DISABLED_TEXT_ALPHA)
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
fun DividerWithText(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(CosmoTheme.colors.border.copy(alpha = 0.3f)),
        )
        Text(
            text = "  or  ",
            style = MaterialTheme.typography.bodySmall,
            color = CosmoTheme.colors.mutedForeground,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(CosmoTheme.colors.border.copy(alpha = 0.3f)),
        )
    }
}

@Composable
fun cosmoTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = CosmoTheme.colors.foreground,
    unfocusedTextColor = CosmoTheme.colors.foreground,
    focusedBorderColor = CosmoTheme.colors.primary,
    unfocusedBorderColor = CosmoTheme.colors.border.copy(alpha = 0.5f),
    focusedLabelColor = CosmoTheme.colors.primary,
    unfocusedLabelColor = CosmoTheme.colors.mutedForeground,
    cursorColor = CosmoTheme.colors.primary,
    errorBorderColor = CosmoTheme.colors.destructive,
    errorLabelColor = CosmoTheme.colors.destructive,
    errorCursorColor = CosmoTheme.colors.destructive,
)
