package com.cosmonaut.app.data.remote

import com.cosmonaut.app.data.remote.dto.ChooseRequest
import com.cosmonaut.app.data.remote.dto.CreateWorldRequest
import com.cosmonaut.app.data.remote.dto.FeedbackRequest
import com.cosmonaut.app.data.remote.dto.GenerateAudioRequest
import com.cosmonaut.app.data.remote.dto.GenerateAudioResponse
import com.cosmonaut.app.data.remote.dto.HealthResponse
import com.cosmonaut.app.data.remote.dto.InviteTokenResponse
import com.cosmonaut.app.data.remote.dto.NewsletterRequest
import com.cosmonaut.app.data.remote.dto.PaginatedNodesResponse
import com.cosmonaut.app.data.remote.dto.PaginatedWorldsResponse
import com.cosmonaut.app.data.remote.dto.SetUsernameRequest
import com.cosmonaut.app.data.remote.dto.StoryNodeResponse
import com.cosmonaut.app.data.remote.dto.UpdateWorldSharingRequest
import com.cosmonaut.app.data.remote.dto.UsageResponse
import com.cosmonaut.app.data.remote.dto.UserInfoResponse
import com.cosmonaut.app.data.remote.dto.UsernameCheckResponse
import com.cosmonaut.app.data.remote.dto.VoiceResponse
import com.cosmonaut.app.data.remote.dto.WorldProgressResponse
import com.cosmonaut.app.data.remote.dto.WorldResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CosmoApiService {

    @GET("health")
    suspend fun healthCheck(): HealthResponse

    // ── Auth ──────────────────────────────────────────────────────────

    @GET("auth/usage")
    suspend fun getUsage(): UsageResponse

    @GET("auth/username/check")
    suspend fun checkUsernameAvailability(@Query("username") username: String,): UsernameCheckResponse

    @POST("auth/username")
    suspend fun setUsername(@Body request: SetUsernameRequest): UsageResponse

    @POST("auth/newsletter")
    suspend fun updateNewsletter(@Body request: NewsletterRequest)

    @DELETE("auth/account")
    suspend fun deleteAccount()

    @POST("auth/feedback")
    suspend fun submitFeedback(@Body request: FeedbackRequest)

    // ── Worlds ────────────────────────────────────────────────────────

    @GET("worlds/")
    suspend fun getWorlds(@Query("cursor") cursor: String? = null): PaginatedWorldsResponse

    @GET("worlds/{worldId}")
    suspend fun getWorld(@Path("worldId") worldId: String, @Query("invite") invite: String? = null,): WorldResponse

    @POST("worlds/")
    suspend fun createWorld(@Body request: CreateWorldRequest): WorldResponse

    @DELETE("worlds/{worldId}")
    suspend fun deleteWorld(@Path("worldId") worldId: String)

    @GET("worlds/{worldId}/progress")
    suspend fun getWorldProgress(@Path("worldId") worldId: String): WorldProgressResponse

    @POST("worlds/{worldId}/sharing")
    suspend fun updateWorldSharing(
        @Path("worldId") worldId: String,
        @Body request: UpdateWorldSharingRequest,
    ): WorldResponse

    // ── Invite Tokens ────────────────────────────────────────────────

    @GET("worlds/{worldId}/invite-token")
    suspend fun getInviteToken(@Path("worldId") worldId: String): InviteTokenResponse?

    @POST("worlds/{worldId}/invite-token")
    suspend fun createInviteToken(@Path("worldId") worldId: String): InviteTokenResponse

    @DELETE("worlds/{worldId}/invite-token")
    suspend fun deleteInviteToken(@Path("worldId") worldId: String)

    // ── User Lookup ──────────────────────────────────────────────────

    @GET("auth/users/batch")
    suspend fun batchLookupUsers(@Query("ids") ids: String): List<UserInfoResponse>

    // ── Nodes ──────────────────────────────────────────────────────────

    @GET("worlds/{worldId}/nodes/")
    suspend fun getWorldNodes(
        @Path("worldId") worldId: String,
        @Query("cursor") cursor: String? = null,
    ): PaginatedNodesResponse

    @GET("worlds/{worldId}/nodes/{nodeId}")
    suspend fun getNode(@Path("worldId") worldId: String, @Path("nodeId") nodeId: String,): StoryNodeResponse

    @POST("worlds/{worldId}/nodes/{nodeId}/choose")
    suspend fun chooseOption(
        @Path("worldId") worldId: String,
        @Path("nodeId") nodeId: String,
        @Body request: ChooseRequest,
    ): StoryNodeResponse

    @POST("worlds/{worldId}/nodes/{nodeId}/retry-processing")
    suspend fun retryNodeProcessing(
        @Path("worldId") worldId: String,
        @Path("nodeId") nodeId: String,
    ): StoryNodeResponse

    // ── Voices / Audio ─────────────────────────────────────────────────

    @GET("voices/")
    suspend fun listVoices(): List<VoiceResponse>

    @POST("worlds/{worldId}/nodes/{nodeId}/audio")
    suspend fun generateNodeAudio(
        @Path("worldId") worldId: String,
        @Path("nodeId") nodeId: String,
        @Body request: GenerateAudioRequest,
    ): GenerateAudioResponse
}
