package com.cosmonaut.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorldProgressResponse(
    @SerialName("current_node_id")
    val currentNodeId: String? = null,
)
