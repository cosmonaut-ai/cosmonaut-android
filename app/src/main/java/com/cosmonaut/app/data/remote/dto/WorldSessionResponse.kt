package com.cosmonaut.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorldSessionSummaryResponse(
    val id: String,
    @SerialName("root_world_id")
    val rootWorldId: String,
    val role: String,
    @SerialName("last_visited_node_id")
    val lastVisitedNodeId: String? = null,
    @SerialName("visited_node_count")
    val visitedNodeCount: Int = 0,
    @SerialName("joined_at")
    val joinedAt: String? = null,
    @SerialName("last_accessed_at")
    val lastAccessedAt: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    val world: WorldResponse,
)

@Serializable
data class WorldSessionResponse(
    val id: String,
    @SerialName("root_world_id")
    val rootWorldId: String,
    val role: String,
    @SerialName("last_visited_node_id")
    val lastVisitedNodeId: String? = null,
    @SerialName("visited_node_count")
    val visitedNodeCount: Int = 0,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    val world: WorldResponse,
) {
    @kotlinx.serialization.Transient
    val fetchedAtMs: Long = System.currentTimeMillis()
}

@Serializable
data class CreateWorldSessionRequest(
    @SerialName("invite_token")
    val inviteToken: String? = null,
)

@Serializable
data class CreateWorldResponse(
    val world: WorldResponse,
    val session: WorldSessionResponse,
)

@Serializable
data class PaginatedSessionsResponse(
    val items: List<WorldSessionSummaryResponse> = emptyList(),
    @SerialName("next_cursor")
    val nextCursor: String? = null,
)

@Serializable
data class SessionLinkHandoffResponse(
    @SerialName("root_world_id")
    val rootWorldId: String,
    val title: String? = null,
    val description: String? = null,
    @SerialName("world_image_url")
    val worldImageUrl: String? = null,
    @SerialName("world_image_alt_text")
    val worldImageAltText: String? = null,
)
