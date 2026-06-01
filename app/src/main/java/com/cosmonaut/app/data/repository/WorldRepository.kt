package com.cosmonaut.app.data.repository

import com.cosmonaut.app.data.remote.CosmoApiService
import com.cosmonaut.app.data.remote.dto.CreateWorldResponse
import com.cosmonaut.app.data.remote.dto.CreateWorldRequest
import com.cosmonaut.app.data.remote.dto.InviteTokenResponse
import com.cosmonaut.app.data.remote.dto.UpdateWorldSharingRequest
import com.cosmonaut.app.data.remote.dto.UserInfoResponse
import com.cosmonaut.app.data.remote.dto.WorldResponse
import com.cosmonaut.app.data.store.WorldDetailStore
import com.cosmonaut.app.data.store.WorldKey
import com.cosmonaut.app.data.store.firstData
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.store.core5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

/**
 * Repository for world data, backed by Store5 cache layers.
 *
 * Mirrors the web's TanStack Query patterns:
 * - `streamWorld(id)` ≈ `useWorld(worldId)` — single world with cache
 * Root-world metadata remains separate from playthrough sessions.
 * Dashboard and story progress data live in SessionRepository.
 */
@Singleton
class WorldRepository @Inject constructor(
    @param:WorldDetailStore private val worldStore: Store<WorldKey, WorldResponse>,
    private val apiService: CosmoApiService,
) {

    fun streamWorld(
        worldId: String,
        invite: String? = null,
        refresh: Boolean = true,
    ): Flow<StoreReadResponse<WorldResponse>> =
        worldStore.stream(StoreReadRequest.cached(WorldKey(worldId, invite), refresh = refresh))

    fun streamWorldFresh(worldId: String): Flow<StoreReadResponse<WorldResponse>> =
        worldStore.stream(StoreReadRequest.fresh(WorldKey(worldId)))

    suspend fun getWorld(worldId: String, invite: String? = null): WorldResponse =
        worldStore.stream(StoreReadRequest.cached(WorldKey(worldId, invite), refresh = true))
            .firstData()

    suspend fun getFeaturedWorlds(): List<WorldResponse> = apiService.getFeaturedWorlds()

    suspend fun createWorld(request: CreateWorldRequest): CreateWorldResponse = apiService.createWorld(request)

    suspend fun invalidateWorld(worldId: String) {
        worldStore.clear(WorldKey(worldId))
    }

    @OptIn(ExperimentalStoreApi::class)
    suspend fun clearAll() {
        worldStore.clear()
    }

    // ── Sharing ──────────────────────────────────────────────────────

    suspend fun updateWorldSharing(
        worldId: String,
        visibility: String? = null,
        sharedWith: List<String>? = null,
    ): WorldResponse {
        val request = UpdateWorldSharingRequest(
            visibility = visibility,
            sharedWith = sharedWith,
        )
        val updated = apiService.updateWorldSharing(worldId, request)
        worldStore.clear(WorldKey(worldId))
        return updated
    }

    suspend fun getInviteToken(worldId: String): InviteTokenResponse? = try {
        apiService.getInviteToken(worldId)
    } catch (_: kotlinx.serialization.SerializationException) {
        null
    }

    suspend fun createInviteToken(worldId: String): InviteTokenResponse = apiService.createInviteToken(worldId)

    suspend fun deleteInviteToken(worldId: String) = apiService.deleteInviteToken(worldId)

    suspend fun batchLookupUsers(ids: List<String>): List<UserInfoResponse> {
        if (ids.isEmpty()) return emptyList()
        return apiService.batchLookupUsers(ids.joinToString(","))
    }
}
