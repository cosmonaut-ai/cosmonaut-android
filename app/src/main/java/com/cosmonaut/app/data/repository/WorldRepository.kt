package com.cosmonaut.app.data.repository

import com.cosmonaut.app.data.remote.CosmoApiService
import com.cosmonaut.app.data.remote.dto.CreateWorldRequest
import com.cosmonaut.app.data.remote.dto.PaginatedWorldsResponse
import com.cosmonaut.app.data.remote.dto.WorldProgressResponse
import com.cosmonaut.app.data.remote.dto.WorldResponse
import com.cosmonaut.app.data.store.WorldDetailStore
import com.cosmonaut.app.data.store.WorldKey
import com.cosmonaut.app.data.store.WorldListKey
import com.cosmonaut.app.data.store.WorldListStore
import com.cosmonaut.app.data.store.WorldProgressKey
import com.cosmonaut.app.data.store.WorldProgressStore
import com.cosmonaut.app.data.store.firstData
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

/**
 * Repository for world data, backed by Store5 cache layers.
 *
 * Mirrors the web's TanStack Query patterns:
 * - `streamWorld(id)` ≈ `useWorld(worldId)` — single world with cache
 * - `streamWorldList()` ≈ `useWorlds()` — paginated list
 * - `streamProgress(id)` ≈ `useWorldProgress(worldId)`
 * - Mutations (create, delete) invalidate relevant stores after completion.
 */
@Singleton
class WorldRepository @Inject constructor(
    @param:WorldDetailStore private val worldStore: Store<WorldKey, WorldResponse>,
    @param:WorldListStore private val listStore: Store<WorldListKey, PaginatedWorldsResponse>,
    @param:WorldProgressStore private val progressStore: Store<WorldProgressKey, WorldProgressResponse>,
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

    fun streamWorldList(cursor: String? = null): Flow<StoreReadResponse<PaginatedWorldsResponse>> =
        listStore.stream(StoreReadRequest.cached(WorldListKey(cursor), refresh = true))

    fun streamProgress(worldId: String): Flow<StoreReadResponse<WorldProgressResponse>> =
        progressStore.stream(StoreReadRequest.cached(WorldProgressKey(worldId), refresh = true))

    suspend fun getWorld(worldId: String, invite: String? = null): WorldResponse =
        worldStore.stream(StoreReadRequest.cached(WorldKey(worldId, invite), refresh = true))
            .firstData()

    suspend fun getWorlds(cursor: String? = null): PaginatedWorldsResponse =
        listStore.stream(StoreReadRequest.cached(WorldListKey(cursor), refresh = true))
            .firstData()

    suspend fun getWorldProgress(worldId: String): WorldProgressResponse =
        progressStore.stream(StoreReadRequest.cached(WorldProgressKey(worldId), refresh = true))
            .firstData()

    suspend fun createWorld(request: CreateWorldRequest): WorldResponse {
        val world = apiService.createWorld(request)
        invalidateWorldList()
        return world
    }

    suspend fun deleteWorld(worldId: String) {
        apiService.deleteWorld(worldId)
        worldStore.clear(WorldKey(worldId))
        invalidateWorldList()
    }

    suspend fun invalidateWorld(worldId: String) {
        worldStore.clear(WorldKey(worldId))
    }

    suspend fun invalidateWorldList() {
        listStore.clear(WorldListKey(cursor = null))
    }

    suspend fun invalidateProgress(worldId: String) {
        progressStore.clear(WorldProgressKey(worldId))
    }
}
