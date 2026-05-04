package com.cosmonaut.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NewsletterRequest(
    @SerialName("opted_in")
    val optedIn: Boolean,
)
