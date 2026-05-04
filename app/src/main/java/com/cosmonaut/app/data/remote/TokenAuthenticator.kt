package com.cosmonaut.app.data.remote

import com.cosmonaut.app.auth.AuthManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber

private const val HEADER_AUTH_RETRY = "X-Auth-Retry"

/**
 * OkHttp [Authenticator] that handles 401 responses by refreshing the Cognito
 * ID token via Amplify and retrying the request once.
 * Mirrors the web app's fetchWithAuthRetry.ts behavior.
 */
@Singleton
class TokenAuthenticator @Inject constructor(private val authManager: AuthManager,) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header(HEADER_AUTH_RETRY) != null) {
            Timber.w("Token refresh retry already attempted — giving up")
            return null
        }

        Timber.d("401 received — attempting token refresh")
        val freshToken = runBlocking { authManager.getIdToken(forceRefresh = true) }

        return if (freshToken != null) {
            response.request.newBuilder()
                .header("Authorization", "Bearer $freshToken")
                .header(HEADER_AUTH_RETRY, "true")
                .build()
        } else {
            Timber.w("Token refresh failed — cannot retry request")
            null
        }
    }
}
