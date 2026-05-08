package com.cosmonaut.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.BuildConfig
import com.cosmonaut.app.data.billing.RegionDetector
import com.cosmonaut.app.ui.theme.CosmoTheme

/**
 * Variants for subscription CTA rendering.
 */
enum class SubscriptionCtaAction {
    UPGRADE,
    MANAGE,
    REACTIVATE,
}

/**
 * Region-aware subscription CTA composable.
 *
 * - US users: Clickable button/link pointing to cosmonaut-ai.com
 * - Non-US users: Plain text only (no links to transactional pages per Play Store policy)
 *
 * This composable encapsulates the US/non-US logic so every call site renders correctly.
 */
@Composable
fun SubscriptionCta(
    action: SubscriptionCtaAction,
    regionDetector: RegionDetector,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val isUsUser by regionDetector.isUsUser.collectAsState()
    val context = LocalContext.current
    val webBaseUrl = BuildConfig.WEB_BASE_URL.trimEnd('/')

    val (label, url, description) = when (action) {
        SubscriptionCtaAction.UPGRADE -> Triple(
            "View Plans",
            "$webBaseUrl/pricing",
            "Subscribe at ${webBaseUrl.removePrefix("https://")}"
        )
        SubscriptionCtaAction.MANAGE -> Triple(
            "Manage Subscription",
            "$webBaseUrl/settings",
            "Manage your subscription at ${webBaseUrl.removePrefix("https://")}"
        )
        SubscriptionCtaAction.REACTIVATE -> Triple(
            "Reactivate Subscription",
            "$webBaseUrl/settings",
            "Reactivate at ${webBaseUrl.removePrefix("https://")}"
        )
    }

    if (isUsUser == true) {
        if (compact) {
            CosmoButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
                variant = if (action == SubscriptionCtaAction.UPGRADE) {
                    CosmoButtonVariant.Primary
                } else {
                    CosmoButtonVariant.Outline
                },
                modifier = modifier,
            ) {
                Icon(
                    imageVector = Icons.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(label)
            }
        } else {
            CosmoButton(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
                variant = if (action == SubscriptionCtaAction.UPGRADE) {
                    CosmoButtonVariant.Primary
                } else {
                    CosmoButtonVariant.Outline
                },
                modifier = modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(label)
            }
        }
    } else {
        if (compact) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = CosmoTheme.colors.mutedForeground,
                modifier = modifier,
            )
        } else {
            Column(
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = CosmoTheme.colors.mutedForeground,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CosmoTheme.colors.mutedForeground,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Open your browser to manage billing",
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmoTheme.colors.mutedForeground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
