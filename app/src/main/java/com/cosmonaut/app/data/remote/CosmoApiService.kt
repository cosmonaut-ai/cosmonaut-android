package com.cosmonaut.app.data.remote

import com.cosmonaut.app.data.remote.dto.HealthResponse
import retrofit2.http.GET

/**
 * Retrofit API service for the Cosmonaut backend.
 * Endpoints will be added as features are implemented in subsequent stages.
 */
interface CosmoApiService {

    @GET("health")
    suspend fun healthCheck(): HealthResponse
}
