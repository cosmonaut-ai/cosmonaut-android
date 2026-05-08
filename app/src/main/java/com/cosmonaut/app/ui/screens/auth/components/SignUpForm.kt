package com.cosmonaut.app.ui.screens.auth.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.R
import com.cosmonaut.app.ui.components.CosmoButton
import com.cosmonaut.app.ui.components.CosmoButtonVariant
import com.cosmonaut.app.ui.components.CosmoTextField
import com.cosmonaut.app.ui.theme.CosmoTheme

@Composable
fun SignUpForm(
    email: String,
    password: String,
    confirmPassword: String,
    showPassword: Boolean,
    isSubmitting: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onSignUp: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onSwitchToSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rules = remember(password) { PasswordRules.evaluate(password) }
    val passwordsMatch = password == confirmPassword && confirmPassword.isNotEmpty()
    val canSubmit = rules.allPassed && passwordsMatch && email.isNotBlank()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CosmoTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email",
            placeholder = "you@example.com",
            enabled = !isSubmitting,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        CosmoTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Password",
            placeholder = "Create a password",
            enabled = !isSubmitting,
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

        if (password.isNotEmpty()) {
            PasswordStrengthIndicator(rules = rules)
        }

        CosmoTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = "Confirm Password",
            placeholder = "Confirm your password",
            enabled = !isSubmitting,
            visualTransformation = if (showPassword) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            isError = confirmPassword.isNotEmpty() && !passwordsMatch,
            supportingText = if (confirmPassword.isNotEmpty() && !passwordsMatch) {
                "Passwords do not match"
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { if (canSubmit) onSignUp() }),
            modifier = Modifier.fillMaxWidth(),
        )

        CosmoButton(
            text = "Create Account",
            onClick = onSignUp,
            enabled = !isSubmitting && canSubmit,
            isLoading = isSubmitting,
        )

        DividerWithText()

        CosmoButton(
            onClick = onGoogleSignIn,
            variant = CosmoButtonVariant.Outline,
            enabled = !isSubmitting,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_google),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Sign up with Google",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Already have an account?",
                style = MaterialTheme.typography.bodySmall,
                color = CosmoTheme.colors.mutedForeground,
            )
            TextButton(onClick = onSwitchToSignIn, enabled = !isSubmitting) {
                Text("Sign in", color = CosmoTheme.colors.primary)
            }
        }
    }
}
