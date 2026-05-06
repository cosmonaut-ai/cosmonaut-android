package com.cosmonaut.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaginatedWorldsResponse(
    val items: List<WorldResponse> = emptyList(),
    @SerialName("next_cursor")
    val nextCursor: String? = null,
)
