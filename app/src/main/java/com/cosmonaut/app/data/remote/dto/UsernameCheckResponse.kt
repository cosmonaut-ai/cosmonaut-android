package com.cosmonaut.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UsernameCheckResponse(val available: Boolean)
