package com.cosmonaut.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * Typed API error carrying HTTP status, human-readable detail, and optional error code.
 * Mirrors the web app's ApiError class with the same semantic helper properties.
 *
 * Backend envelope: `{ "error": { "code": "...", "message": "..." } }`
 * Legacy fallback: `{ "detail": "..." }`
 */
class ApiError(val status: Int, val detail: String, val code: String? = null,) : Exception(detail) {

    val isQuotaExceeded: Boolean
        get() {
            if (code != null) return code == "QUOTA_EXCEEDED"
            return status == 429 && !detail.contains("rate limit", ignoreCase = true)
        }

    val isRateLimited: Boolean
        get() {
            if (code != null) return code == "RATE_LIMITED"
            return status == 429 && detail.contains("rate limit", ignoreCase = true)
        }

    val isForbidden: Boolean
        get() {
            if (code != null) return code == "FORBIDDEN" || code == "SESSION_ACCESS_DENIED"
            return status == 403
        }

    val isNotFound: Boolean
        get() {
            if (code != null) return code == "NOT_FOUND"
            return status == 404
        }

    val isUnauthorized: Boolean get() = status == 401

    val isNodeAlreadyProcessed: Boolean
        get() = Regex(
            "Cannot generate text for node .+ with status (completed|generating)",
            RegexOption.IGNORE_CASE,
        ).containsMatchIn(detail)

    val isNodeProcessingConflict: Boolean
        get() {
            if (code != null) return code == "CONFLICT"
            return status == 409
        }

    val isSessionNotFound: Boolean
        get() {
            if (code != null) return code == "SESSION_NOT_FOUND"
            return false
        }

    val isWrongSession: Boolean
        get() {
            if (code != null) return code == "WRONG_SESSION_FOR_NODE"
            return false
        }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /**
         * Parse an error response body into an [ApiError].
         * Handles both the structured envelope and legacy `detail` format.
         */
        fun fromResponseBody(status: Int, body: String?): ApiError {
            if (body.isNullOrBlank()) return ApiError(status, "Request failed with status $status")

            return try {
                val envelope = json.decodeFromString<ErrorEnvelope>(body)
                if (envelope.error != null) {
                    ApiError(status, envelope.error.message, envelope.error.code)
                } else if (envelope.detail != null) {
                    ApiError(status, envelope.detail)
                } else {
                    ApiError(status, body)
                }
            } catch (_: Exception) {
                ApiError(status, body)
            }
        }

        /**
         * Create an [ApiError] from an SSE error event.
         */
        fun fromStreamEvent(data: String): ApiError {
            val status = when {
                data.startsWith("Quota exceeded", ignoreCase = true) -> 429
                data.contains("Cannot generate text for node", ignoreCase = true) -> 400
                else -> 500
            }
            return ApiError(status, data)
        }
    }
}

/**
 * Converts any caught [Exception] to a typed [ApiError] when possible.
 * Handles both direct [ApiError] instances and Retrofit's [HttpException].
 */
fun Exception.asApiError(): ApiError? = when (this) {
    is ApiError -> this
    is HttpException -> ApiError.fromResponseBody(code(), response()?.errorBody()?.string())
    else -> null
}

@Serializable
private data class ErrorEnvelope(val error: ErrorBody? = null, val detail: String? = null,)

@Serializable
private data class ErrorBody(
    val code: String? = null,
    val message: String = "",
    @SerialName("detail")
    val detailField: String? = null,
)
