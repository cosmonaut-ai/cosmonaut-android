package com.cosmonaut.app.ui.screens.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.ui.components.CosmoButton
import com.cosmonaut.app.ui.theme.CosmoTheme

private const val DISABLED_FACE_ALPHA = 0.35f

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
