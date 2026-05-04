package com.cosmonaut.app.data.remote

import com.cosmonaut.app.data.remote.dto.HealthResponse
import com.cosmonaut.app.data.remote.dto.NewsletterRequest
import com.cosmonaut.app.data.remote.dto.SetUsernameRequest
import com.cosmonaut.app.data.remote.dto.UsageResponse
import com.cosmonaut.app.data.remote.dto.UsernameCheckResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
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
}
