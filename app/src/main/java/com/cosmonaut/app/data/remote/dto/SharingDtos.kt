package com.cosmonaut.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateWorldSharingRequest(
    val visibility: String? = null,
    @SerialName("shared_with")
    val sharedWith: List<String>? = null,
)

@Serializable
data class InviteTokenResponse(
    val token: String,
    @SerialName("root_world_id")
    val rootWorldId: String,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("expires_at")
    val expiresAt: String,
    @SerialName("use_count")
    val useCount: Int,
    @SerialName("invite_url")
    val inviteUrl: String,
)

@Serializable
data class UserInfoResponse(
    val id: String,
    @SerialName("display_name")
    val displayName: String,
)
