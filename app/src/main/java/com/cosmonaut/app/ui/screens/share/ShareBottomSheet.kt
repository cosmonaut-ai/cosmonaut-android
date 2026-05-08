@file:OptIn(ExperimentalMaterial3Api::class)

package com.cosmonaut.app.ui.screens.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.data.remote.dto.InviteTokenResponse
import com.cosmonaut.app.data.remote.dto.WorldResponse
import androidx.compose.ui.window.Dialog
import com.cosmonaut.app.ui.components.CosmoButton
import com.cosmonaut.app.ui.components.CosmoButtonVariant
import com.cosmonaut.app.ui.components.CosmoIconButton
import com.cosmonaut.app.ui.components.GlassCard
import com.cosmonaut.app.ui.components.VisibilitySelector
import com.cosmonaut.app.ui.components.shimmer
import com.cosmonaut.app.ui.theme.CosmoTheme

private const val COPY_ICON_BG_ALPHA = 0.15f

@Composable
fun ShareBottomSheet(
    world: WorldResponse,
    currentUserId: String?,
    viewModel: ShareBottomSheetViewModel,
    onDismiss: () -> Unit,
    onWorldUpdate: (WorldResponse) -> Unit = {},
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(world.id) {
        viewModel.initialize(world, currentUserId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ShareEvent.ShowMessage -> {
                    android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_SHORT).show()
                }
                is ShareEvent.WorldUpdated -> onWorldUpdate(event.world)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.reset()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = CosmoTheme.colors.background,
        contentColor = CosmoTheme.colors.foreground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 32.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(CosmoTheme.colors.mutedForeground.copy(alpha = 0.3f)),
            )
        },
    ) {
        ShareBottomSheetContent(
            state = state,
            viewModel = viewModel,
            context = context,
        )
    }

    if (state.showPrivateConfirm) {
        PrivateConfirmDialog(
            onConfirm = viewModel::confirmPrivateSwitch,
            onDismiss = viewModel::cancelPrivateSwitch,
        )
    }

    state.userToRemove?.let { user ->
        RemoveUserDialog(
            user = user,
            onConfirm = viewModel::executeRemoveUser,
            onDismiss = viewModel::dismissRemoveUser,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShareBottomSheetContent(
    state: ShareUiState,
    viewModel: ShareBottomSheetViewModel,
    context: Context,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Title row with save status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = null,
                    tint = CosmoTheme.colors.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Share \"${state.worldTitle}\"",
                    style = MaterialTheme.typography.titleMedium,
                    color = CosmoTheme.colors.foreground,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            SaveStatusIndicator(
                isSaving = state.isSaving,
                justSaved = state.justSaved,
            )
        }

        // Visibility selector
        if (state.isOwner) {
            VisibilitySelector(
                selectedVisibility = state.visibility,
                onVisibilityChange = viewModel::onVisibilityChange,
                label = "General access",
            )
        }

        // Link sharing section
        when (state.visibility) {
            "public", "unlisted" -> {
                CopyLinkRow(
                    link = viewModel.getWorldLink(),
                    description = "Anyone with this link can view this story",
                    context = context,
                )

                NativeShareButton(
                    worldTitle = state.worldTitle,
                    link = viewModel.getWorldLink(),
                    context = context,
                )
            }
            "private" -> {
                if (state.isOwner) {
                    InviteLinkSection(
                        inviteToken = state.inviteToken,
                        isLoading = state.isLoadingInviteToken,
                        isCreating = state.isCreatingToken,
                        isDeleting = state.isDeletingToken,
                        expiryText = state.inviteToken?.let { viewModel.getExpiryText(it) },
                        onCreateToken = viewModel::createInviteToken,
                        onDeleteToken = viewModel::deleteInviteToken,
                        context = context,
                    )
                }
            }
        }

        // Shared users
        if (state.isOwner && state.visibility == "private" && state.sharedUsers.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "People with access (${state.sharedUsers.size})",
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmoTheme.colors.mutedForeground,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    state.sharedUsers.forEach { user ->
                        SharedUserChip(
                            user = user,
                            enabled = !state.isSaving,
                            onRemove = { viewModel.confirmRemoveUser(user) },
                        )
                    }
                }
            }
        }

        // Non-owner notice
        if (!state.isOwner) {
            GlassCard(cornerRadius = 12.dp) {
                Text(
                    text = "Only the owner of this story can manage sharing settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmoTheme.colors.mutedForeground,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun SaveStatusIndicator(isSaving: Boolean, justSaved: Boolean) {
    if (isSaving) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 1.5.dp,
                color = CosmoTheme.colors.mutedForeground,
            )
            Text(
                text = "Saving\u2026",
                style = MaterialTheme.typography.labelSmall,
                color = CosmoTheme.colors.mutedForeground,
            )
        }
    } else if (justSaved) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = Color(0xFF22C55E),
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = "Saved",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF22C55E),
            )
        }
    }
}

@Composable
private fun CopyLinkRow(link: String, description: String, context: Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CosmoTheme.colors.card.copy(alpha = 0.6f))
            .clickable { copyToClipboard(context, link, "Link copied") }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(CosmoTheme.colors.primary.copy(alpha = COPY_ICON_BG_ALPHA)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Link,
                contentDescription = null,
                tint = CosmoTheme.colors.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Copy link",
                style = MaterialTheme.typography.bodyMedium,
                color = CosmoTheme.colors.foreground,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = CosmoTheme.colors.mutedForeground,
            )
        }
        Icon(
            imageVector = Icons.Outlined.ContentCopy,
            contentDescription = "Copy",
            tint = CosmoTheme.colors.mutedForeground,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun NativeShareButton(worldTitle: String, link: String, context: Context) {
    CosmoButton(
        onClick = {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Check out \"$worldTitle\" on Cosmonaut")
                putExtra(Intent.EXTRA_TEXT, "Check out \"$worldTitle\" on Cosmonaut!\n\n$link")
            }
            context.startActivity(Intent.createChooser(intent, "Share story"))
        },
        variant = CosmoButtonVariant.Outline,
    ) {
        Icon(
            imageVector = Icons.Outlined.Share,
            contentDescription = null,
            tint = CosmoTheme.colors.outlineForeground,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "Share via\u2026",
            color = CosmoTheme.colors.outlineForeground,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun InviteLinkSection(
    inviteToken: InviteTokenResponse?,
    isLoading: Boolean,
    isCreating: Boolean,
    isDeleting: Boolean,
    expiryText: String?,
    onCreateToken: () -> Unit,
    onDeleteToken: () -> Unit,
    context: Context,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Invite link",
            style = MaterialTheme.typography.bodyMedium,
            color = CosmoTheme.colors.foreground,
            fontWeight = FontWeight.Medium,
        )

        when {
            isLoading -> {
                GlassCard(cornerRadius = 12.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmer(),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.35f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmer(),
                        )
                    }
                }
            }

            inviteToken != null -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GlassCard(
                        cornerRadius = 12.dp,
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = inviteToken.inviteUrl,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CosmoTheme.colors.mutedForeground,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                expiryText?.let {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CosmoTheme.colors.mutedForeground,
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        copyToClipboard(context, inviteToken.inviteUrl, "Invite link copied")
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy invite link",
                                    tint = CosmoTheme.colors.foreground,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = "Copy",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CosmoTheme.colors.foreground,
                                )
                            }
                        }
                    }

                    CosmoIconButton(
                        icon = Icons.Outlined.Delete,
                        contentDescription = "Delete invite link",
                        onClick = onDeleteToken,
                        variant = CosmoButtonVariant.Destructive,
                    )
                }
            }

            else -> {
                CosmoButton(
                    onClick = onCreateToken,
                    variant = CosmoButtonVariant.Outline,
                    isLoading = isCreating,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = CosmoTheme.colors.outlineForeground,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Create Invite Link",
                        color = CosmoTheme.colors.outlineForeground,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = "Invite links are valid for 24 hours. Anyone with the link can join this story.",
                    style = MaterialTheme.typography.labelSmall,
                    color = CosmoTheme.colors.mutedForeground,
                )
            }
        }
    }
}

@Composable
private fun SharedUserChip(user: SharedUser, enabled: Boolean, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(CosmoTheme.colors.secondary)
            .padding(start = 10.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "@${user.displayName}",
            style = MaterialTheme.typography.labelSmall,
            color = CosmoTheme.colors.secondaryForeground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable(enabled = enabled, onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Remove ${user.displayName}",
                tint = CosmoTheme.colors.secondaryForeground,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun PrivateConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CosmoTheme.colors.card)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Switch to private?",
                style = MaterialTheme.typography.titleMedium,
                color = CosmoTheme.colors.foreground,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Switching to private will remove access for all users " +
                    "who haven\u2019t been explicitly invited. This cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = CosmoTheme.colors.mutedForeground,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CosmoButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    variant = CosmoButtonVariant.Outline,
                    modifier = Modifier.weight(1f),
                )
                CosmoButton(
                    text = "Switch to Private",
                    onClick = onConfirm,
                    variant = CosmoButtonVariant.Destructive,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RemoveUserDialog(user: SharedUser, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CosmoTheme.colors.card)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Remove access?",
                style = MaterialTheme.typography.titleMedium,
                color = CosmoTheme.colors.foreground,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${user.displayName} will no longer be able to view this story.",
                style = MaterialTheme.typography.bodyMedium,
                color = CosmoTheme.colors.mutedForeground,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CosmoButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    variant = CosmoButtonVariant.Outline,
                    modifier = Modifier.weight(1f),
                )
                CosmoButton(
                    text = "Remove",
                    onClick = onConfirm,
                    variant = CosmoButtonVariant.Destructive,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String, toastMessage: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("Cosmonaut Link", text))
    triggerHaptic(context)
    android.widget.Toast.makeText(context, toastMessage, android.widget.Toast.LENGTH_SHORT).show()
}

@Suppress("DEPRECATION")
private fun triggerHaptic(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE),
            )
        } else {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    } catch (_: SecurityException) {
        // Permission not granted; silently skip haptic feedback
    }
}
