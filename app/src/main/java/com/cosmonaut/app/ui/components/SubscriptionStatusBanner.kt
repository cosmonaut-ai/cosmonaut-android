package com.cosmonaut.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.cosmonaut.app.data.billing.RegionDetector
import com.cosmonaut.app.data.remote.dto.UsageResponse
import com.cosmonaut.app.ui.theme.CosmoTheme
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Displays subscription status warnings matching the web's SubscriptionStatusBanner.
 *
 * Shows banners for:
 * - Pending cancellation (plan ending on date)
 * - Pending tier change (downgrade scheduled)
 * - Past due payment
 * - Paused subscription
 * - Unpaid/failed subscription
 */
@Composable
fun SubscriptionStatusBanner(
    usage: UsageResponse,
    regionDetector: RegionDetector,
    modifier: Modifier = Modifier,
) {
    val hasBanner = usage.pendingCancellation ||
        usage.pendingTier != null ||
        usage.subscriptionStatus in listOf("past_due", "paused", "unpaid")

    AnimatedVisibility(
        visible = hasBanner,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier,
    ) {
        when {
            usage.pendingCancellation && usage.cancellationDate != null -> {
                BannerCard(
                    icon = Icons.Outlined.Warning,
                    iconTint = amberColor,
                    backgroundColor = amberColor.copy(alpha = 0.1f),
                    borderColor = amberColor.copy(alpha = 0.3f),
                ) {
                    Text(
                        text = "Your ${tierDisplayName(usage.tier)} plan will end on ${formatDate(usage.cancellationDate)}. " +
                            "You'll be downgraded to the Free plan after that.",
                        style = MaterialTheme.typography.bodySmall,
                        color = amber200,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SubscriptionCta(
                        action = SubscriptionCtaAction.REACTIVATE,
                        regionDetector = regionDetector,
                        compact = true,
                    )
                }
            }

            usage.pendingTier != null -> {
                BannerCard(
                    icon = Icons.Outlined.ArrowDownward,
                    iconTint = blueColor,
                    backgroundColor = blueColor.copy(alpha = 0.1f),
                    borderColor = blueColor.copy(alpha = 0.3f),
                ) {
                    Text(
                        text = "Your plan will change to ${tierDisplayName(usage.pendingTier)} on " +
                            "${formatDate(usage.pendingTierDate)}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmoTheme.colors.foreground,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SubscriptionCta(
                        action = SubscriptionCtaAction.MANAGE,
                        regionDetector = regionDetector,
                        compact = true,
                    )
                }
            }

            usage.subscriptionStatus == "past_due" -> {
                BannerCard(
                    icon = Icons.Outlined.Warning,
                    iconTint = amberColor,
                    backgroundColor = amberColor.copy(alpha = 0.1f),
                    borderColor = amberColor.copy(alpha = 0.3f),
                ) {
                    Text(
                        text = "There's an issue with your payment. Please update your payment method to avoid losing access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = amber200,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SubscriptionCta(
                        action = SubscriptionCtaAction.MANAGE,
                        regionDetector = regionDetector,
                        compact = true,
                    )
                }
            }

            usage.subscriptionStatus == "paused" -> {
                BannerCard(
                    icon = Icons.Outlined.Info,
                    iconTint = blueColor,
                    backgroundColor = blueColor.copy(alpha = 0.1f),
                    borderColor = blueColor.copy(alpha = 0.3f),
                ) {
                    Text(
                        text = "Your ${tierDisplayName(usage.tier)} subscription is currently paused.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmoTheme.colors.foreground,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SubscriptionCta(
                        action = SubscriptionCtaAction.MANAGE,
                        regionDetector = regionDetector,
                        compact = true,
                    )
                }
            }

            usage.subscriptionStatus == "unpaid" -> {
                BannerCard(
                    icon = Icons.Outlined.Warning,
                    iconTint = CosmoTheme.colors.destructive,
                    backgroundColor = CosmoTheme.colors.destructive.copy(alpha = 0.1f),
                    borderColor = CosmoTheme.colors.destructive.copy(alpha = 0.3f),
                ) {
                    Text(
                        text = "Your subscription payment failed and your account has been downgraded to the Free plan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = CosmoTheme.colors.foreground,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SubscriptionCta(
                        action = SubscriptionCtaAction.UPGRADE,
                        regionDetector = regionDetector,
                        compact = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun BannerCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    backgroundColor: Color,
    borderColor: Color,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = iconTint,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                content()
            }
        }
    }
}

private val amberColor = Color(0xFFF59E0B)
private val amber200 = Color(0xFFFDE68A)
private val blueColor = Color(0xFF3B82F6)

private fun tierDisplayName(tier: String?): String = when (tier?.uppercase()) {
    "FREE" -> "Free"
    "EXPLORER" -> "Explorer"
    "COSMONAUT" -> "Cosmonaut"
    else -> tier?.replaceFirstChar { it.titlecase() } ?: "Free"
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
