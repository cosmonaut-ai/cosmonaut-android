package com.cosmonaut.app.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.cosmonaut.app.R
import com.cosmonaut.app.ui.components.CosmoButton
import com.cosmonaut.app.ui.components.CosmoTextField
import com.cosmonaut.app.ui.components.GlassCard
import com.cosmonaut.app.ui.theme.CosmoTheme
import com.cosmonaut.app.ui.theme.OrbitronFontFamily

private const val GLOW_ALPHA = 0.06f
private const val GLOW_RADIUS_FRACTION = 0.5f

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val primaryColor = CosmoTheme.colors.primary

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is OnboardingEvent.NavigateToDashboard -> onComplete()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CosmoTheme.colors.background)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = GLOW_ALPHA),
                            Color.Transparent,
                        ),
                        center = Offset(size.width / 2f, size.height * 0.15f),
                        radius = size.width * GLOW_RADIUS_FRACTION,
                    ),
                    radius = size.width * GLOW_RADIUS_FRACTION,
                    center = Offset(size.width / 2f, size.height * 0.15f),
                )
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(top = 48.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Brand header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmoTheme.colors.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo),
                        contentDescription = "Cosmonaut logo",
                        modifier = Modifier.size(28.dp),
                    )
                }
                Text(
                    text = "Cosmonaut",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = OrbitronFontFamily,
                    ),
                    color = CosmoTheme.colors.foreground,
                )
            }

            Text(
                text = "Complete your profile",
                style = MaterialTheme.typography.bodyMedium,
                color = CosmoTheme.colors.mutedForeground,
                textAlign = TextAlign.Center,
            )

            state.errorMessage?.let { error ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CosmoTheme.colors.destructive.copy(alpha = 0.1f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = error,
                        color = CosmoTheme.colors.destructive,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            GlassCard {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    // Username
                    CosmoTextField(
                        value = state.username,
                        onValueChange = viewModel::updateUsername,
                        label = "What should we call you?",
                        placeholder = "yourname",
                        enabled = !state.isSubmitting,
                        isError = state.usernameStatus == UsernameStatus.TAKEN ||
                            state.usernameStatus == UsernameStatus.INVALID,
                        supportingContent = { UsernameHelperText(state.usernameStatus) },
                        trailingIcon = { UsernameStatusIcon(state.usernameStatus) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Newsletter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Newsletter",
                                style = MaterialTheme.typography.titleSmall,
                                color = CosmoTheme.colors.foreground,
                            )
                            Text(
                                text = "Receive updates about new features",
                                style = MaterialTheme.typography.bodySmall,
                                color = CosmoTheme.colors.mutedForeground,
                            )
                        }
                        Switch(
                            checked = state.newsletterOptIn,
                            onCheckedChange = { viewModel.toggleNewsletter() },
                            enabled = !state.isSubmitting,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = CosmoTheme.colors.primaryForeground,
                                checkedTrackColor = CosmoTheme.colors.primary,
                                uncheckedThumbColor = CosmoTheme.colors.mutedForeground,
                                uncheckedTrackColor = CosmoTheme.colors.muted,
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Age confirmation
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Checkbox(
                            checked = state.ageConfirmed,
                            onCheckedChange = { viewModel.toggleAgeConfirmation() },
                            enabled = !state.isSubmitting,
                            colors = CheckboxDefaults.colors(
                                checkedColor = CosmoTheme.colors.primary,
                                checkmarkColor = CosmoTheme.colors.primaryForeground,
                                uncheckedColor = CosmoTheme.colors.border,
                            ),
                        )
                        Text(
                            text = "I confirm that I am 13 years of age or older",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CosmoTheme.colors.foreground,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    CosmoButton(
                        text = "Complete Setup",
                        onClick = viewModel::submit,
                        enabled = state.canSubmit,
                        isLoading = state.isSubmitting,
                    )
                }
            }
        }
    }
}

@Composable
private fun UsernameHelperText(status: UsernameStatus) {
    val (text, color) = when (status) {
        UsernameStatus.IDLE ->
            "3-30 characters, letters and numbers only" to CosmoTheme.colors.mutedForeground
        UsernameStatus.CHECKING ->
            "Checking availability..." to CosmoTheme.colors.mutedForeground
        UsernameStatus.AVAILABLE ->
            "Username is available!" to CosmoTheme.colors.primary
        UsernameStatus.TAKEN ->
            "Username is already taken" to CosmoTheme.colors.destructive
        UsernameStatus.INVALID ->
            "3-30 characters, letters and numbers only" to CosmoTheme.colors.destructive
    }
    Text(text = text, color = color)
}

@Composable
private fun UsernameStatusIcon(status: UsernameStatus) {
    when (status) {
        UsernameStatus.CHECKING -> CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = CosmoTheme.colors.primary,
        )
        UsernameStatus.AVAILABLE -> Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Available",
            tint = CosmoTheme.colors.primary,
        )
        UsernameStatus.TAKEN -> Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Taken",
            tint = CosmoTheme.colors.destructive,
        )
        else -> {}
    }
}
