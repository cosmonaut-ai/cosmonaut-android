package com.cosmonaut.app.data.remote

import com.cosmonaut.app.BuildConfig
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

private const val SESSION_TTL_MS = 5 * 60 * 1000L

/**
 * Manages CloudFront signed cookies required by the streaming endpoint.
 *
 * The streaming CloudFront distribution enforces signed-cookie authentication.
 * This manager calls POST /auth/session on the regular API (JWT-authed) to
 * obtain the three CloudFront cookies, caches them for [SESSION_TTL_MS], and
 * provides them as a `Cookie` header value for streaming requests.
 */
@Singleton
class StreamingSessionManager @Inject constructor(
    private val httpClient: OkHttpClient,
) {
    @Volatile private var cachedCookieHeader: String? = null
    @Volatile private var validUntil: Long = 0L

    private val mutex = Mutex()

    suspend fun getCookieHeader(): String {
        val now = System.currentTimeMillis()
        cachedCookieHeader?.let { cached ->
            if (now < validUntil) return cached
        }

        return mutex.withLock {
            cachedCookieHeader?.let { cached ->
                if (System.currentTimeMillis() < validUntil) return@withLock cached
            }
            refreshSession()
        }
    }

    fun invalidate() {
        validUntil = 0L
    }

    private suspend fun refreshSession(): String = withContext(Dispatchers.IO) {
        val url = "${BuildConfig.API_BASE_URL.trimEnd('/')}/auth/session"
        val request = Request.Builder()
            .url(url)
            .post("".toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        try {
            if (!response.isSuccessful) {
                throw IOException("Streaming session refresh failed: ${response.code}")
            }

            val cookieParts = mutableListOf<String>()
            for (header in response.headers("Set-Cookie")) {
                val nameValue = header.substringBefore(";").trim()
                if (nameValue.startsWith("CloudFront-")) {
                    cookieParts.add(nameValue)
                }
            }

            if (cookieParts.size < 3) {
                Timber.w("Expected 3 CloudFront cookies but got ${cookieParts.size}")
                throw IOException("Incomplete CloudFront cookies from /auth/session")
            }

            val cookieHeader = cookieParts.joinToString("; ")
            cachedCookieHeader = cookieHeader
            validUntil = System.currentTimeMillis() + SESSION_TTL_MS
            Timber.d("Streaming session refreshed, valid for ${SESSION_TTL_MS / 1000}s")
            cookieHeader
        } finally {
            response.close()
        }
    }
}
