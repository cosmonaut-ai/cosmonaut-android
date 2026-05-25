package com.cosmonaut.app.data.remote

import com.cosmonaut.app.auth.AuthManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import timber.log.Timber

private const val HEADER_AUTH_RETRY = "X-Auth-Retry"
private const val TOKEN_REFRESH_TIMEOUT_MS = 10_000L

/**
 * OkHttp [Authenticator] that handles 401 responses by refreshing the Cognito
 * ID token via Amplify and retrying the request once.
 * Mirrors the web app's fetchWithAuthRetry.ts behavior.
 *
 * Uses [Dispatchers.IO] to avoid blocking the calling thread directly, and
 * applies a timeout to prevent indefinite hangs on slow networks.
 */
@Singleton
class TokenAuthenticator @Inject constructor(private val authManager: AuthManager,) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header(HEADER_AUTH_RETRY) != null) {
            Timber.w("Token refresh retry already attempted — giving up")
            return null
        }

        Timber.d("401 received — attempting token refresh")
        val freshToken = try {
            runBlocking(Dispatchers.IO) {
                withTimeout(TOKEN_REFRESH_TIMEOUT_MS) {
                    authManager.getIdToken(forceRefresh = true)
                }
            }
        } catch (expected: Exception) {
            Timber.w(expected, "Token refresh timed out or failed")
            null
        }

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
