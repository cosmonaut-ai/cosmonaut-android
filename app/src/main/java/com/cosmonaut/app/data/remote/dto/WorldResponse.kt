package com.cosmonaut.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CharacterResponse(
    val name: String? = null,
    val description: String? = null,
    val relationships: List<String>? = null,
)

@Serializable
data class LocationResponse(
    val name: String? = null,
    val description: String? = null,
    val connections: List<String>? = null,
)

@Serializable
data class WorldResponse(
    val id: String,
    @SerialName("session_id")
    val sessionId: String? = null,
    val title: String? = null,
    val description: String? = null,
    val genre: String? = null,
    val score: String? = null,
    @SerialName("generation_status")
    val generationStatus: String = "initialized",
    @SerialName("author_id")
    val authorId: String? = null,
    @SerialName("root_node_id")
    val rootNodeId: String? = null,
    val visibility: String? = null,
    @SerialName("shared_with")
    val sharedWith: List<String>? = null,
    @SerialName("world_prompt")
    val worldPrompt: String? = null,
    val setting: String? = null,
    @SerialName("narrative_context")
    val narrativeContext: String? = null,
    val characters: List<CharacterResponse>? = null,
    val locations: List<LocationResponse>? = null,
    @SerialName("potential_endings")
    val potentialEndings: List<String>? = null,
    @SerialName("narrator_profile")
    val narratorProfile: String? = null,
    @SerialName("node_text_length")
    val nodeTextLength: Int? = null,
    @SerialName("story_max_nodes")
    val storyMaxNodes: Int? = null,
    @SerialName("world_length")
    val worldLength: String? = null,
    @SerialName("vocab_level")
    val vocabLevel: String = "teen",
    @SerialName("content_filter")
    val contentFilter: String = "none",
    @SerialName("max_choices")
    val maxChoices: Int? = null,
    @SerialName("world_image_url")
    val worldImageUrl: String? = null,
    @SerialName("world_image_alt_text")
    val worldImageAltText: String? = null,
    @SerialName("world_image_width")
    val worldImageWidth: String? = null,
    @SerialName("world_image_height")
    val worldImageHeight: String? = null,
    @SerialName("world_image_size")
    val worldImageSize: String? = null,
    @SerialName("image_generation_status")
    val imageGenerationStatus: String? = null,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = "",
) {
    @kotlinx.serialization.Transient
    val fetchedAtMs: Long = System.currentTimeMillis()

    val isCompleted: Boolean get() = generationStatus == "completed"
    val isGenerating: Boolean
        get() = generationStatus in listOf(
            "generating_lore",
            "generating_narrator_profile",
            "initialized",
        )
    val isFailed: Boolean get() = generationStatus == "failed"

    val generationStatusDisplay: String
        get() = when (generationStatus) {
            "initialized" -> "Initializing..."
            "generating_lore" -> "Generating world..."
            "generating_narrator_profile" -> "Creating narrator..."
            "completed" -> "Completed"
            "failed" -> "Failed"
            else -> generationStatus
        }
}
