package com.cosmonaut.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AudioEntryResponse(
    @SerialName("audio_url")
    val audioUrl: String,
    @SerialName("timestamps_url")
    val timestampsUrl: String? = null,
)

@Serializable
data class ChoiceResponse(
    val label: String,
    val outcome: String? = null,
    val target: String? = null,
    @SerialName("is_created")
    val isCreated: Boolean? = null,
    @SerialName("is_explored")
    val isExplored: Boolean? = null,
    @SerialName("is_custom")
    val isCustom: Boolean? = null,
    val creator: String? = null,
    @SerialName("creator_email")
    val creatorEmail: String? = null,
    @SerialName("creator_display_name")
    val creatorDisplayName: String? = null,
)

@Serializable
data class StoryNodeResponse(
    val id: String,
    @SerialName("world_id")
    val worldId: String,
    val title: String? = null,
    val text: String? = null,
    @SerialName("story_summary")
    val storySummary: String? = null,
    val choices: List<ChoiceResponse> = emptyList(),
    @SerialName("parent_id")
    val parentId: String? = null,
    @SerialName("parent_choice")
    val parentChoice: ChoiceResponse? = null,
    val ancestors: List<String> = emptyList(),
    @SerialName("processing_status")
    val processingStatus: String = "pending",
    @SerialName("generation_status")
    val generationStatus: String = "initialized",
    val audio: Map<String, AudioEntryResponse> = emptyMap(),
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String? = null,
) {
    @kotlinx.serialization.Transient
    val fetchedAtMs: Long = System.currentTimeMillis()

    val isCompleted: Boolean get() = generationStatus == "completed"
    val isGenerating: Boolean get() = generationStatus == "generating"
    val isFailed: Boolean get() = generationStatus == "failed"
    val isInitialized: Boolean get() = generationStatus == "initialized"
    val isEnding: Boolean get() = isCompleted && choices.isEmpty()
}
