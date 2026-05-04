package com.cosmonaut.app.data.remote

import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

/**
 * OkHttp interceptor that attaches the JWT bearer token to all API requests.
 * The token provider will be wired up in Stage 2 (Authentication).
 */
@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {

    @Volatile
    private var token: String? = null

    fun setToken(jwt: String?) {
        token = jwt
        Timber.d("Auth token %s", if (jwt != null) "set" else "cleared")
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val currentToken = token

        val request = if (currentToken != null) {
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $currentToken")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(request)
    }
}
