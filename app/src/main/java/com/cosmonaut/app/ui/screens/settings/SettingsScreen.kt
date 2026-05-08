package com.cosmonaut.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.cosmonaut.app.BuildConfig
import com.cosmonaut.app.auth.AuthState
import com.cosmonaut.app.data.remote.dto.UsageResponse
import com.cosmonaut.app.ui.components.CosmoButton
import com.cosmonaut.app.ui.components.CosmoButtonVariant
import com.cosmonaut.app.ui.components.CosmoHaptics
import com.cosmonaut.app.ui.components.SubscriptionPlanCard
import com.cosmonaut.app.ui.components.UsageCard
import com.cosmonaut.app.ui.components.shimmer
import com.cosmonaut.app.ui.theme.CosmoTheme
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SettingsScreen(
    onNavigateToFeedback: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val usage by viewModel.usage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val isSigningOut by viewModel.isSigningOut.collectAsState()
    val context = LocalContext.current

    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SettingsEvent.ShowSnackbar -> snackbarMessage = event.message
            }
        }
    }

    val user = (authState as? AuthState.Authenticated)?.user

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 32.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.titleLarge,
            color = CosmoTheme.colors.foreground,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "Your account, subscription, and preferences",
            style = MaterialTheme.typography.bodyMedium,
            color = CosmoTheme.colors.mutedForeground,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Account Section ─────────────────────────────────────────
        AccountSection(
            email = user?.email,
            username = usage?.username ?: user?.username,
            pictureUrl = user?.picture,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Subscription ────────────────────────────────────────────
        SectionHeader(title = "Subscription", icon = Icons.Outlined.CreditCard)
        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            SubscriptionSkeleton()
            Spacer(modifier = Modifier.height(16.dp))
            UsageSkeleton()
        } else if (usage != null) {
            SubscriptionPlanCard(
                usage = usage!!,
                regionDetector = viewModel.regionDetector,
            )
            Spacer(modifier = Modifier.height(16.dp))
            UsageCard(usage = usage!!)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Email Preferences ───────────────────────────────────────
        EmailPreferencesSection(
            usage = usage,
            isUpdating = viewModel.newsletterUpdating.collectAsState().value,
            onToggle = viewModel::updateNewsletter,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Support & Info ──────────────────────────────────────────
        SupportSection(
            onNavigateToFeedback = onNavigateToFeedback,
            onOpenTerms = {
                val url = "${BuildConfig.WEB_BASE_URL.trimEnd('/')}/terms"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            },
            onOpenPrivacy = {
                val url = "${BuildConfig.WEB_BASE_URL.trimEnd('/')}/privacy"
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            },
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Danger Zone ─────────────────────────────────────────────
        DangerZoneSection(viewModel = viewModel)

        Spacer(modifier = Modifier.height(24.dp))

        // ── Sign Out ────────────────────────────────────────────────
        CosmoButton(
            onClick = viewModel::signOut,
            variant = CosmoButtonVariant.Outline,
            isLoading = isSigningOut,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sign Out")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── App Version ─────────────────────────────────────────────
        Text(
            text = "Cosmonaut v${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = CosmoTheme.colors.mutedForeground,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }

    AnimatedVisibility(
        visible = snackbarMessage != null,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        snackbarMessage?.let { msg ->
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(3000)
                snackbarMessage = null
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CosmoTheme.colors.destructive.copy(alpha = 0.15f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmoTheme.colors.foreground,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
    }
}

// ── Account Section ─────────────────────────────────────────────────

@Composable
private fun AccountSection(email: String?, username: String?, pictureUrl: String?,) {
    SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = null,
                tint = CosmoTheme.colors.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Account",
                style = MaterialTheme.typography.titleMedium,
                color = CosmoTheme.colors.foreground,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (pictureUrl != null) {
                AsyncImage(
                    model = pictureUrl,
                    contentDescription = "Profile picture",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                val initial = (username ?: email ?: "U").first().uppercaseChar()
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CosmoTheme.colors.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initial.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = CosmoTheme.colors.primaryForeground,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (username != null) {
                    Text(
                        text = "@$username",
                        style = MaterialTheme.typography.bodyLarge,
                        color = CosmoTheme.colors.foreground,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (email != null) {
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CosmoTheme.colors.mutedForeground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            val signInMethod = if (pictureUrl != null) "Google" else "Email"
            Surface(
                shape = RoundedCornerShape(50),
                color = CosmoTheme.colors.muted,
                border = BorderStroke(1.dp, CosmoTheme.colors.border),
            ) {
                Text(
                    text = signInMethod,
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmoTheme.colors.mutedForeground,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
    }
}

// ── Email Preferences ───────────────────────────────────────────────

@Composable
private fun EmailPreferencesSection(usage: UsageResponse?, isUpdating: Boolean, onToggle: (Boolean) -> Unit,) {
    val view = LocalView.current

    SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Email,
                contentDescription = null,
                tint = CosmoTheme.colors.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Email Preferences",
                style = MaterialTheme.typography.titleMedium,
                color = CosmoTheme.colors.foreground,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Product Updates Newsletter",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CosmoTheme.colors.foreground,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "Occasional emails when something new ships \u2014 no fluff",
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmoTheme.colors.mutedForeground,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = usage?.newsletterOptedIn ?: false,
                onCheckedChange = {
                    CosmoHaptics.onToggle(view)
                    onToggle(it)
                },
                enabled = usage != null && !isUpdating,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CosmoTheme.colors.primaryForeground,
                    checkedTrackColor = CosmoTheme.colors.primary,
                    uncheckedThumbColor = CosmoTheme.colors.mutedForeground,
                    uncheckedTrackColor = CosmoTheme.colors.muted,
                    uncheckedBorderColor = CosmoTheme.colors.border,
                ),
            )
        }
    }
}

// ── Support & Info ──────────────────────────────────────────────────

@Composable
private fun SupportSection(onNavigateToFeedback: () -> Unit, onOpenTerms: () -> Unit, onOpenPrivacy: () -> Unit,) {
    SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = CosmoTheme.colors.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Support & Info",
                style = MaterialTheme.typography.titleMedium,
                color = CosmoTheme.colors.foreground,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        SettingsListItem(
            icon = Icons.Outlined.Feedback,
            label = "Send Feedback",
            trailingIcon = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            onClick = onNavigateToFeedback,
        )
        HorizontalDivider(color = CosmoTheme.colors.border.copy(alpha = 0.5f))
        SettingsListItem(
            icon = Icons.Outlined.Gavel,
            label = "Terms of Service",
            trailingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
            onClick = onOpenTerms,
        )
        HorizontalDivider(color = CosmoTheme.colors.border.copy(alpha = 0.5f))
        SettingsListItem(
            icon = Icons.Outlined.PrivacyTip,
            label = "Privacy Policy",
            trailingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
            onClick = onOpenPrivacy,
        )
    }
}

@Composable
private fun SettingsListItem(icon: ImageVector, label: String, trailingIcon: ImageVector, onClick: () -> Unit,) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CosmoTheme.colors.mutedForeground,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = CosmoTheme.colors.foreground,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = trailingIcon,
            contentDescription = null,
            tint = CosmoTheme.colors.mutedForeground.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp),
        )
    }
}

// ── Danger Zone ─────────────────────────────────────────────────────

@Composable
private fun DangerZoneSection(viewModel: SettingsViewModel) {
    var showDialog by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CosmoTheme.colors.card,
        border = BorderStroke(1.dp, CosmoTheme.colors.destructive.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = CosmoTheme.colors.destructive,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Danger Zone",
                    style = MaterialTheme.typography.titleMedium,
                    color = CosmoTheme.colors.destructive,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = "Irreversible actions for your account",
                style = MaterialTheme.typography.bodySmall,
                color = CosmoTheme.colors.mutedForeground,
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = CosmoTheme.colors.destructive.copy(alpha = 0.15f))

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Delete account",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CosmoTheme.colors.foreground,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "Permanently delete your account and all data. " +
                            "Your stories will be permanently deleted, " +
                            "even if other users have saved them. " +
                            "This cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmoTheme.colors.mutedForeground,
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                CosmoButton(
                    text = "Delete Account",
                    onClick = { showDialog = true },
                    variant = CosmoButtonVariant.Destructive,
                    modifier = Modifier.width(140.dp),
                )
            }
        }
    }

    if (showDialog) {
        DeleteAccountDialog(
            viewModel = viewModel,
            onDismiss = {
                showDialog = false
                viewModel.clearDeleteError()
            },
        )
    }
}

@Composable
private fun DeleteAccountDialog(viewModel: SettingsViewModel, onDismiss: () -> Unit,) {
    val isDeletingAccount by viewModel.isDeletingAccount.collectAsState()
    val deleteError by viewModel.deleteError.collectAsState()
    var confirmText by remember { mutableStateOf("") }
    val canDelete = confirmText == "DELETE"
    val keyboard = LocalSoftwareKeyboardController.current

    Dialog(onDismissRequest = { if (!isDeletingAccount) onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CosmoTheme.colors.card)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = CosmoTheme.colors.destructive,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Delete Account",
                    style = MaterialTheme.typography.titleMedium,
                    color = CosmoTheme.colors.destructive,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Text(
                text = "This will permanently delete your account " +
                    "and all your stories, even if other users " +
                    "have saved them. Any active subscriptions " +
                    "will be cancelled. This action cannot be undone.",
                style = MaterialTheme.typography.bodySmall,
                color = CosmoTheme.colors.mutedForeground,
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Type DELETE to confirm",
                    style = MaterialTheme.typography.labelMedium,
                    color = CosmoTheme.colors.foreground,
                    fontWeight = FontWeight.Medium,
                )
                BasicTextField(
                    value = confirmText,
                    onValueChange = { confirmText = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = CosmoTheme.colors.foreground,
                        fontFamily = FontFamily.Monospace,
                    ),
                    cursorBrush = SolidColor(CosmoTheme.colors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboard?.hide()
                            if (canDelete && !isDeletingAccount) viewModel.deleteAccount()
                        },
                    ),
                    enabled = !isDeletingAccount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CosmoTheme.colors.background)
                        .padding(12.dp),
                    decorationBox = { innerTextField ->
                        Box {
                            if (confirmText.isEmpty()) {
                                Text(
                                    text = "DELETE",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                    ),
                                    color = CosmoTheme.colors.mutedForeground.copy(alpha = 0.4f),
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }

            AnimatedVisibility(visible = deleteError != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CosmoTheme.colors.destructive.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, CosmoTheme.colors.destructive.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = deleteError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmoTheme.colors.destructive,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CosmoButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    variant = CosmoButtonVariant.Outline,
                    enabled = !isDeletingAccount,
                    modifier = Modifier.weight(1f),
                )
                CosmoButton(
                    text = "Delete My Account",
                    onClick = { viewModel.deleteAccount() },
                    variant = CosmoButtonVariant.Destructive,
                    enabled = canDelete,
                    isLoading = isDeletingAccount,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ── Shared Components ───────────────────────────────────────────────

@Composable
private fun SettingsCard(modifier: Modifier = Modifier, content: @Composable () -> Unit,) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CosmoTheme.colors.card,
        border = BorderStroke(1.dp, CosmoTheme.colors.border),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CosmoTheme.colors.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = CosmoTheme.colors.foreground,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SubscriptionSkeleton() {
    val shimmerColor = CosmoTheme.colors.muted

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CosmoTheme.colors.card,
        border = BorderStroke(1.dp, CosmoTheme.colors.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerColor)
                        .shimmer(),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerColor)
                        .shimmer(),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerColor)
                        .shimmer(),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(50))
                        .background(shimmerColor)
                        .shimmer(),
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(160.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerColor)
                    .shimmer(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerColor)
                    .shimmer(),
            )
        }
    }
}

@Composable
private fun UsageSkeleton() {
    val shimmerColor = CosmoTheme.colors.muted

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CosmoTheme.colors.card,
        border = BorderStroke(1.dp, CosmoTheme.colors.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerColor)
                        .shimmer(),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerColor)
                        .shimmer(),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            UsageBarSkeleton(shimmerColor)
            Spacer(modifier = Modifier.height(16.dp))
            UsageBarSkeleton(shimmerColor)
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerColor.copy(alpha = 0.5f))
                    .shimmer(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = CosmoTheme.colors.border)
            Spacer(modifier = Modifier.height(16.dp))
            UsageBarSkeleton(shimmerColor)
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerColor)
                    .shimmer(),
            )
        }
    }
}

@Composable
private fun UsageBarSkeleton(shimmerColor: androidx.compose.ui.graphics.Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerColor)
                    .shimmer(),
            )
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerColor)
                    .shimmer(),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(shimmerColor)
                .shimmer(),
        )
    }
}
