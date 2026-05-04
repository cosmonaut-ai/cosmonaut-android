package com.cosmonaut.app.ui.screens.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.ui.theme.CosmoTheme

private const val VERIFICATION_CODE_LENGTH = 6

@Composable
fun VerifyForm(
    email: String,
    code: String,
    isSubmitting: Boolean,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResendCode: () -> Unit,
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "We sent a verification code to",
            style = MaterialTheme.typography.bodyMedium,
            color = CosmoTheme.colors.mutedForeground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = email,
            style = MaterialTheme.typography.bodyMedium,
            color = CosmoTheme.colors.foreground,
            textAlign = TextAlign.Center,
        )

        OutlinedTextField(
            value = code,
            onValueChange = { newValue ->
                if (newValue.length <= VERIFICATION_CODE_LENGTH && newValue.all { it.isDigit() }) {
                    onCodeChange(newValue)
                }
            },
            label = { Text("Verification Code") },
            singleLine = true,
            enabled = !isSubmitting,
            colors = cosmoTextFieldColors(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { if (code.length == VERIFICATION_CODE_LENGTH) onVerify() },
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        CosmoButton(
            text = "Verify Email",
            onClick = onVerify,
            enabled = !isSubmitting && code.length == VERIFICATION_CODE_LENGTH,
            isLoading = isSubmitting,
        )

        TextButton(onClick = onResendCode, enabled = !isSubmitting) {
            Text("Resend Code", color = CosmoTheme.colors.primary)
        }

        TextButton(onClick = onBackToSignIn, enabled = !isSubmitting) {
            Text("Back to Sign In", color = CosmoTheme.colors.mutedForeground)
        }
    }
}
