package com.cosmonaut.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for the choose endpoint.
 * Exactly one of [targetId] or [customChoice] must be non-null.
 */
@Serializable
data class ChooseRequest(
    @SerialName("target_id")
    val targetId: String? = null,
    @SerialName("custom_choice")
    val customChoice: String? = null,
)
