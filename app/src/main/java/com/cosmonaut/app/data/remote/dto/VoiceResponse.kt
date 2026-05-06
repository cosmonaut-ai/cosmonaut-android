package com.cosmonaut.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoiceResponse(
    val id: String,
    @SerialName("display_name")
    val displayName: String,
    val description: String,
    @SerialName("sample_url")
    val sampleUrl: String,
)

@Serializable
data class GenerateAudioRequest(
    @SerialName("voice_id")
    val voiceId: String,
)

@Serializable
data class GenerateAudioResponse(
    @SerialName("audio_url")
    val audioUrl: String,
    @SerialName("timestamps_url")
    val timestampsUrl: String? = null,
)
