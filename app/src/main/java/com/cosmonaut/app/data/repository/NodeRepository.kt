package com.cosmonaut.app.data.repository

import com.cosmonaut.app.data.remote.CosmoApiService
import com.cosmonaut.app.data.remote.StreamEvent
import com.cosmonaut.app.data.remote.StreamingService
import com.cosmonaut.app.data.remote.dto.ChooseRequest
import com.cosmonaut.app.data.remote.dto.StoryNodeResponse
import com.cosmonaut.app.data.store.NodeKey
import com.cosmonaut.app.data.store.NodeStore
import com.cosmonaut.app.data.store.firstData
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.store.core5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

/**
 * Repository for story node data, backed by a Store5 cache layer.
 *
 * Mirrors the web's TanStack Query patterns:
 * - `stream(key)` ≈ `useNode(worldId, nodeId)` — returns cached-then-fresh flow
 * - `fresh(key)` ≈ `invalidateQueries + refetch` — forces network fetch
 * - `invalidate(key)` ≈ `queryClient.invalidateQueries(key)` — purges cache entry
 *
 * The Store handles request deduplication, staleness validation, and memory eviction
 * automatically. Non-completed nodes (generating, initialized) bypass the cache
 * via the Validator — they always re-fetch from network.
 */
@Singleton
class NodeRepository @Inject constructor(
    @param:NodeStore private val store: Store<NodeKey, StoryNodeResponse>,
    private val apiService: CosmoApiService,
    private val streamingService: StreamingService,
) {

    /**
     * Stream a node with cache-then-fetch semantics.
     * Emits cached data immediately if valid, then refreshes in background if stale.
     */
    fun stream(worldId: String, nodeId: String, refresh: Boolean = true): Flow<StoreReadResponse<StoryNodeResponse>> =
        store.stream(StoreReadRequest.cached(NodeKey(worldId, nodeId), refresh = refresh))

    /**
     * Get a node, preferring cache for completed nodes.
     * For non-completed nodes (generating, initialized, failed), always hits network.
     */
    suspend fun getNode(worldId: String, nodeId: String): StoryNodeResponse {
        val response = store.stream(StoreReadRequest.cached(NodeKey(worldId, nodeId), refresh = true))
            .firstData()
        return response
    }

    /**
     * Force-fetch a node from network, bypassing and updating the cache.
     * Equivalent to TanStack's `queryClient.invalidateQueries` + immediate refetch.
     */
    suspend fun fetchFresh(worldId: String, nodeId: String): StoryNodeResponse {
        val response = store.stream(StoreReadRequest.fresh(NodeKey(worldId, nodeId)))
            .firstData()
        return response
    }

    /**
     * Invalidate a cached node entry.
     * Next stream/get call for this key will fetch from network.
     * Equivalent to TanStack's `queryClient.invalidateQueries({ queryKey })`.
     */
    suspend fun invalidate(worldId: String, nodeId: String) {
        store.clear(NodeKey(worldId, nodeId))
    }

    @OptIn(ExperimentalStoreApi::class)
    suspend fun clearAll() {
        store.clear()
    }

    /**
     * Fetch all nodes in a world with auto-pagination.
     * Mirrors the web's `getWorldNodes()` which auto-paginates the cursor-based endpoint.
     */
    suspend fun getWorldNodes(worldId: String): List<StoryNodeResponse> {
        val allNodes = mutableListOf<StoryNodeResponse>()
        var cursor: String? = null

        do {
            val page = apiService.getWorldNodes(worldId, cursor)
            allNodes.addAll(page.items)
            cursor = page.nextCursor
        } while (cursor != null)

        return allNodes
    }

    suspend fun chooseOption(
        worldId: String,
        nodeId: String,
        targetId: String?,
        customChoice: String?,
    ): StoryNodeResponse = apiService.chooseOption(
        worldId = worldId,
        nodeId = nodeId,
        request = ChooseRequest(targetId = targetId, customChoice = customChoice),
    )

    suspend fun retryNodeProcessing(worldId: String, nodeId: String): StoryNodeResponse =
        apiService.retryNodeProcessing(worldId, nodeId)

    fun generateNodeText(worldId: String, nodeId: String): Flow<StreamEvent> =
        streamingService.generateNodeText(worldId, nodeId)
}
