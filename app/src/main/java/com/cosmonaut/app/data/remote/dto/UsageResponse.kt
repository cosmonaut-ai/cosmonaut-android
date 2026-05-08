package com.cosmonaut.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UsageResponse(
    @kotlinx.serialization.Transient
    val fetchedAtMs: Long = System.currentTimeMillis(),
    val tier: String = "FREE",
    val username: String? = null,
    @SerialName("is_onboarded")
    val isOnboarded: Boolean = false,
    @SerialName("newsletter_opted_in")
    val newsletterOptedIn: Boolean = false,
    @SerialName("worlds_created")
    val worldsCreated: Int = 0,
    @SerialName("worlds_limit")
    val worldsLimit: Int = 0,
    @SerialName("nodes_used")
    val nodesUsed: Int = 0,
    @SerialName("nodes_limit")
    val nodesLimit: Int = 0,
    @SerialName("audio_narrations_used")
    val audioUsed: Int = 0,
    @SerialName("audio_narrations_limit")
    val audioLimit: Int = 0,
    @SerialName("subscription_status")
    val subscriptionStatus: String? = null,
    @SerialName("period_end")
    val periodEnd: String? = null,
    @SerialName("pending_cancellation")
    val pendingCancellation: Boolean = false,
    @SerialName("cancellation_date")
    val cancellationDate: String? = null,
    @SerialName("pending_tier")
    val pendingTier: String? = null,
    @SerialName("pending_tier_date")
    val pendingTierDate: String? = null,
)
