package com.cosmonaut.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaginatedNodesResponse(
    val items: List<StoryNodeResponse>,
    @SerialName("next_cursor")
    val nextCursor: String? = null,
)
