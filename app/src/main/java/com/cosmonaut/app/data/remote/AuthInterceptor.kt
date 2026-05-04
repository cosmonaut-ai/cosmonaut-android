package com.cosmonaut.app.data.remote

import com.cosmonaut.app.auth.AuthManager
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that attaches the cached JWT bearer token to all API requests.
 * Uses [AuthManager.getCachedIdToken] for non-blocking token access.
 * Token refresh on 401 is handled by [TokenAuthenticator].
 */
@Singleton
class AuthInterceptor @Inject constructor(private val authManager: dagger.Lazy<AuthManager>,) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val token = authManager.get().getCachedIdToken()

        val request = if (token != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(request)
    }
}
