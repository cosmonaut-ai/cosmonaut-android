package com.cosmonaut.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateWorldRequest(
    @SerialName("world_prompt")
    val worldPrompt: String,
    val visibility: String = "private",
    @SerialName("world_length")
    val worldLength: String = "medium",
    @SerialName("vocab_level")
    val vocabLevel: String = "teen",
    @SerialName("content_filter")
    val contentFilter: String = "none",
)
