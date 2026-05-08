package com.cosmonaut.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FeedbackRequest(val category: String, val message: String,)
