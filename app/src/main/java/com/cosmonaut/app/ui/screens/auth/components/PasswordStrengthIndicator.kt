package com.cosmonaut.app.ui.screens.auth.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.ui.theme.CosmoTheme

@Composable
fun PasswordStrengthIndicator(rules: PasswordRules, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        RuleRow(label = "At least 8 characters", passed = rules.hasMinLength)
        RuleRow(label = "One lowercase letter", passed = rules.hasLowercase)
        RuleRow(label = "One uppercase letter", passed = rules.hasUppercase)
        RuleRow(label = "One number", passed = rules.hasDigit)
        RuleRow(label = "One special character", passed = rules.hasSymbol)
    }
}

@Composable
private fun RuleRow(label: String, passed: Boolean) {
    val color by animateColorAsState(
        targetValue = if (passed) {
            CosmoTheme.colors.primary
        } else {
            CosmoTheme.colors.mutedForeground
        },
        label = "ruleColor",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = if (passed) Icons.Default.Check else Icons.Default.Close,
            contentDescription = if (passed) "Met" else "Not met",
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
    }
}
