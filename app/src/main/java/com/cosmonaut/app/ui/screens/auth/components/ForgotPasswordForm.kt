package com.cosmonaut.app.ui.screens.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.ui.components.CosmoButton
import com.cosmonaut.app.ui.theme.CosmoTheme

private const val RESET_CODE_LENGTH = 6

@Composable
fun ForgotPasswordForm(
    isResetMode: Boolean,
    email: String,
    code: String,
    newPassword: String,
    confirmNewPassword: String,
    showPassword: Boolean,
    isSubmitting: Boolean,
    onEmailChange: (String) -> Unit,
    onCodeChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmNewPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onSendCode: () -> Unit,
    onResetPassword: () -> Unit,
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isResetMode) {
        ResetPasswordContent(
            email = email,
            code = code,
            newPassword = newPassword,
            confirmNewPassword = confirmNewPassword,
            showPassword = showPassword,
            isSubmitting = isSubmitting,
            onCodeChange = onCodeChange,
            onNewPasswordChange = onNewPasswordChange,
            onConfirmNewPasswordChange = onConfirmNewPasswordChange,
            onTogglePassword = onTogglePassword,
            onResetPassword = onResetPassword,
            onBackToSignIn = onBackToSignIn,
            modifier = modifier,
        )
    } else {
        SendCodeContent(
            email = email,
            isSubmitting = isSubmitting,
            onEmailChange = onEmailChange,
            onSendCode = onSendCode,
            onBackToSignIn = onBackToSignIn,
            modifier = modifier,
        )
    }
}

@Composable
private fun SendCodeContent(
    email: String,
    isSubmitting: Boolean,
    onEmailChange: (String) -> Unit,
    onSendCode: () -> Unit,
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Enter your email to receive a password reset code.",
            style = MaterialTheme.typography.bodyMedium,
            color = CosmoTheme.colors.mutedForeground,
            textAlign = TextAlign.Center,
        )

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("Email") },
            singleLine = true,
            enabled = !isSubmitting,
            colors = cosmoTextFieldColors(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { if (email.isNotBlank()) onSendCode() }),
            modifier = Modifier.fillMaxWidth(),
        )

        CosmoButton(
            text = "Send Reset Code",
            onClick = onSendCode,
            enabled = !isSubmitting && email.isNotBlank(),
            isLoading = isSubmitting,
        )

        TextButton(onClick = onBackToSignIn, enabled = !isSubmitting) {
            Text("Back to Sign In", color = CosmoTheme.colors.mutedForeground)
        }
    }
}

@Composable
private fun ResetPasswordContent(
    email: String,
    code: String,
    newPassword: String,
    confirmNewPassword: String,
    showPassword: Boolean,
    isSubmitting: Boolean,
    onCodeChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmNewPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onResetPassword: () -> Unit,
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rules = remember(newPassword) { PasswordRules.evaluate(newPassword) }
    val passwordsMatch = newPassword == confirmNewPassword && confirmNewPassword.isNotEmpty()
    val canSubmit = code.length == RESET_CODE_LENGTH && rules.allPassed && passwordsMatch
    val fieldColors = cosmoTextFieldColors()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Enter the code sent to $email and choose a new password.",
            style = MaterialTheme.typography.bodyMedium,
            color = CosmoTheme.colors.mutedForeground,
            textAlign = TextAlign.Center,
        )

        OutlinedTextField(
            value = code,
            onValueChange = { newValue ->
                if (newValue.length <= RESET_CODE_LENGTH && newValue.all { it.isDigit() }) {
                    onCodeChange(newValue)
                }
            },
            label = { Text("Reset Code") },
            singleLine = true,
            enabled = !isSubmitting,
            colors = fieldColors,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = newPassword,
            onValueChange = onNewPasswordChange,
            label = { Text("New Password") },
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
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        if (newPassword.isNotEmpty()) {
            PasswordStrengthIndicator(rules = rules)
        }

        OutlinedTextField(
            value = confirmNewPassword,
            onValueChange = onConfirmNewPasswordChange,
            label = { Text("Confirm New Password") },
            singleLine = true,
            enabled = !isSubmitting,
            colors = fieldColors,
            visualTransformation = if (showPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            isError = confirmNewPassword.isNotEmpty() && !passwordsMatch,
            supportingText = if (confirmNewPassword.isNotEmpty() && !passwordsMatch) {
                { Text("Passwords do not match") }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { if (canSubmit) onResetPassword() }),
            modifier = Modifier.fillMaxWidth(),
        )

        CosmoButton(
            text = "Reset Password",
            onClick = onResetPassword,
            enabled = !isSubmitting && canSubmit,
            isLoading = isSubmitting,
        )

        TextButton(onClick = onBackToSignIn, enabled = !isSubmitting) {
            Text("Back to Sign In", color = CosmoTheme.colors.mutedForeground)
        }
    }
}
