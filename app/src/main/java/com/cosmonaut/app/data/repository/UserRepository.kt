package com.cosmonaut.app.data.repository

import com.cosmonaut.app.data.remote.CosmoApiService
import com.cosmonaut.app.data.remote.dto.FeedbackRequest
import com.cosmonaut.app.data.remote.dto.NewsletterRequest
import com.cosmonaut.app.data.remote.dto.SetUsernameRequest
import com.cosmonaut.app.data.remote.dto.UsageResponse
import com.cosmonaut.app.data.remote.dto.UsernameCheckResponse
import com.cosmonaut.app.data.store.UserKey
import com.cosmonaut.app.data.store.UserStore
import com.cosmonaut.app.data.store.firstData
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreReadRequest
import org.mobilenativefoundation.store.store5.StoreReadResponse

/**
 * Repository for authenticated user data (profile, usage, subscription).
 * Backed by Store5, equivalent to the web's `useUser()` TanStack Query.
 *
 * The user store has a 2-minute stale time. Mutations (setUsername, updateNewsletter)
 * invalidate the store so the next read fetches fresh data — matching the web's
 * pattern of `invalidateQueries({ queryKey: queryKeys.user.all })`.
 */
@Singleton
class UserRepository @Inject constructor(
    @param:UserStore private val store: Store<UserKey, UsageResponse>,
    private val apiService: CosmoApiService,
) {

    fun stream(refresh: Boolean = true): Flow<StoreReadResponse<UsageResponse>> =
        store.stream(StoreReadRequest.cached(UserKey, refresh = refresh))

    suspend fun getUsage(): UsageResponse = store.stream(StoreReadRequest.cached(UserKey, refresh = true))
        .firstData()

    suspend fun fetchFresh(): UsageResponse = store.stream(StoreReadRequest.fresh(UserKey))
        .firstData()

    suspend fun checkUsernameAvailability(username: String): UsernameCheckResponse =
        apiService.checkUsernameAvailability(username)

    suspend fun setUsername(username: String): UsageResponse {
        val result = apiService.setUsername(SetUsernameRequest(username))
        invalidate()
        return result
    }

    suspend fun updateNewsletter(optedIn: Boolean) {
        apiService.updateNewsletter(NewsletterRequest(optedIn))
        invalidate()
    }

    suspend fun deleteAccount() {
        apiService.deleteAccount()
        invalidate()
    }

    suspend fun submitFeedback(category: String, message: String) {
        apiService.submitFeedback(FeedbackRequest(category = category, message = message))
    }

    suspend fun invalidate() {
        store.clear(UserKey)
    }
}
