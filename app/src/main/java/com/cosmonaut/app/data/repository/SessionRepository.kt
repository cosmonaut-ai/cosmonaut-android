package com.cosmonaut.app.data.repository

import com.cosmonaut.app.data.remote.CosmoApiService
import com.cosmonaut.app.data.remote.dto.CreateWorldSessionRequest
import com.cosmonaut.app.data.remote.dto.PaginatedSessionsResponse
import com.cosmonaut.app.data.remote.dto.SessionLinkHandoffResponse
import com.cosmonaut.app.data.remote.dto.WorldSessionResponse
import com.cosmonaut.app.data.store.SessionDetailStore
import com.cosmonaut.app.data.store.SessionKey
import com.cosmonaut.app.data.store.SessionListKey
import com.cosmonaut.app.data.store.SessionListStore
import com.cosmonaut.app.data.store.firstData
import javax.inject.Inject
import javax.inject.Singleton
import org.mobilenativefoundation.store.core5.ExperimentalStoreApi
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest

/**
 * Repository for the current user's playthrough sessions.
 *
 * Sessions own dashboard membership, last-visited progress, node graph access,
 * and the playthrough-specific APIs under /sessions.
 */
@Singleton
class SessionRepository @Inject constructor(
    @param:SessionDetailStore private val sessionStore: Store<SessionKey, WorldSessionResponse>,
    @param:SessionListStore private val sessionListStore: Store<SessionListKey, PaginatedSessionsResponse>,
    private val apiService: CosmoApiService,
) {

    suspend fun getSessions(cursor: String? = null, fresh: Boolean = false): PaginatedSessionsResponse {
        val request = if (fresh) {
            StoreReadRequest.fresh(SessionListKey(cursor))
        } else {
            StoreReadRequest.cached(SessionListKey(cursor), refresh = true)
        }
        return sessionListStore.stream(request).firstData()
    }

    suspend fun getSession(sessionId: String, fresh: Boolean = false): WorldSessionResponse {
        val request = if (fresh) {
            StoreReadRequest.fresh(SessionKey(sessionId))
        } else {
            StoreReadRequest.cached(SessionKey(sessionId), refresh = true)
        }
        return sessionStore.stream(request).firstData()
    }

    suspend fun createWorldSession(worldId: String, inviteToken: String? = null): WorldSessionResponse {
        val session = apiService.createWorldSession(
            worldId = worldId,
            request = CreateWorldSessionRequest(inviteToken = inviteToken),
        )
        sessionStore.clear(SessionKey(session.id))
        sessionListStore.clear(SessionListKey(cursor = null))
        return session
    }

    suspend fun getSessionHandoff(sessionId: String): SessionLinkHandoffResponse =
        apiService.getSessionHandoff(sessionId)

    suspend fun deleteSession(sessionId: String) {
        apiService.deleteSession(sessionId)
        sessionStore.clear(SessionKey(sessionId))
        sessionListStore.clear(SessionListKey(cursor = null))
    }

    suspend fun invalidateSession(sessionId: String) {
        sessionStore.clear(SessionKey(sessionId))
    }

    suspend fun invalidateSessionList() {
        sessionListStore.clear(SessionListKey(cursor = null))
    }

    @OptIn(ExperimentalStoreApi::class)
    suspend fun clearAll() {
        sessionStore.clear()
        sessionListStore.clear()
    }
}
