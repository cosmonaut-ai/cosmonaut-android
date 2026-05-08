package com.cosmonaut.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.data.billing.RegionDetector
import com.cosmonaut.app.data.remote.dto.UsageResponse
import com.cosmonaut.app.ui.theme.CosmoTheme
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Displays current subscription plan info with tier badge and renewal/reset date.
 * Matches the web's SubscriptionSection component.
 */
@Composable
fun SubscriptionPlanCard(
    usage: UsageResponse,
    regionDetector: RegionDetector,
    modifier: Modifier = Modifier,
) {
    val isFree = usage.tier.uppercase() == "FREE"

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CosmoTheme.colors.card,
        border = BorderStroke(1.dp, CosmoTheme.colors.border),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CreditCard,
                    contentDescription = null,
                    tint = CosmoTheme.colors.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Subscription",
                    style = MaterialTheme.typography.titleMedium,
                    color = CosmoTheme.colors.foreground,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${tierDisplayName(usage.tier)} Plan",
                        style = MaterialTheme.typography.bodyLarge,
                        color = CosmoTheme.colors.foreground,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TierBadge(tier = usage.tier)
                }
            }

            if (usage.periodEnd != null) {
                Spacer(modifier = Modifier.height(4.dp))
                val periodText = when {
                    usage.pendingCancellation -> "Ends on ${formatDate(usage.cancellationDate)}"
                    usage.pendingTier != null -> "Changes to ${tierDisplayName(usage.pendingTier)} on ${formatDate(usage.pendingTierDate)}"
                    isFree -> "Quotas refresh on ${formatDate(usage.periodEnd)}"
                    else -> "Renews on ${formatDate(usage.periodEnd)}"
                }
                Text(
                    text = periodText,
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmoTheme.colors.mutedForeground,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isFree) {
                SubscriptionCta(
                    action = SubscriptionCtaAction.UPGRADE,
                    regionDetector = regionDetector,
                )
            } else {
                SubscriptionCta(
                    action = SubscriptionCtaAction.MANAGE,
                    regionDetector = regionDetector,
                )
            }
        }
    }
}

/**
 * Displays usage stats (worlds, nodes, audio) with progress bars.
 * Matches the web's Usage card in the SubscriptionSection.
 */
@Composable
fun UsageCard(
    usage: UsageResponse,
    modifier: Modifier = Modifier,
) {
    val isFree = usage.tier.uppercase() == "FREE"
    val isExplorer = usage.tier.uppercase() == "EXPLORER"

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CosmoTheme.colors.card,
        border = BorderStroke(1.dp, CosmoTheme.colors.border),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.BarChart,
                    contentDescription = null,
                    tint = CosmoTheme.colors.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Usage",
                    style = MaterialTheme.typography.titleMedium,
                    color = CosmoTheme.colors.foreground,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            UsageBar(
                label = "Stories Created",
                used = usage.worldsCreated,
                limit = usage.worldsLimit,
            )

            Spacer(modifier = Modifier.height(16.dp))

            UsageBar(
                label = "Story Generations",
                used = usage.nodesUsed,
                limit = usage.nodesLimit,
            )

            if (usage.periodEnd != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CosmoTheme.colors.muted.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Usage resets on ${formatDate(usage.periodEnd)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmoTheme.colors.mutedForeground,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = CosmoTheme.colors.border)

            Spacer(modifier = Modifier.height(16.dp))

            UsageBar(
                label = "Audio Narrations",
                used = usage.audioUsed,
                limit = usage.audioLimit,
            )

            if (isFree || isExplorer) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Audio narrations are a one-time allowance and do not reset. Upgrade to Cosmonaut for monthly narrations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = CosmoTheme.colors.mutedForeground,
                )
            }
        }
    }
}

@Composable
private fun TierBadge(tier: String) {
    val (backgroundColor, textColor, borderColor) = when (tier.uppercase()) {
        "EXPLORER" -> Triple(
            CosmoTheme.colors.primary.copy(alpha = 0.15f),
            CosmoTheme.colors.primary,
            CosmoTheme.colors.primary.copy(alpha = 0.3f),
        )
        "COSMONAUT" -> Triple(
            Color(0xFFF59E0B).copy(alpha = 0.15f),
            Color(0xFFF59E0B),
            Color(0xFFF59E0B).copy(alpha = 0.3f),
        )
        else -> Triple(
            CosmoTheme.colors.muted,
            CosmoTheme.colors.mutedForeground,
            CosmoTheme.colors.border,
        )
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Text(
            text = "${tierPrice(tier)}/${tierPriceDetail(tier)}",
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

private fun tierDisplayName(tier: String?): String = when (tier?.uppercase()) {
    "FREE" -> "Free"
    "EXPLORER" -> "Explorer"
    "COSMONAUT" -> "Cosmonaut"
    else -> tier?.replaceFirstChar { it.titlecase() } ?: "Free"
}

private fun tierPrice(tier: String): String = when (tier.uppercase()) {
    "FREE" -> "$0"
    "EXPLORER" -> "$3"
    "COSMONAUT" -> "$25"
    else -> "$0"
}

private fun tierPriceDetail(tier: String): String = when (tier.uppercase()) {
    "FREE" -> "forever"
    else -> "mo"
}

private fun formatDate(isoDate: String?): String {
    if (isoDate == null) return ""
    return try {
        val parsed = ZonedDateTime.parse(isoDate)
        parsed.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    } catch (_: Exception) {
        isoDate.take(10)
    }
}
