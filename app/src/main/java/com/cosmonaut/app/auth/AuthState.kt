package com.cosmonaut.app.auth

/**
 * Represents a user's identity extracted from Cognito JWT claims.
 * Mirrors the web app's UserInfo interface.
 */
data class UserInfo(
    val sub: String,
    val email: String? = null,
    val name: String? = null,
    val picture: String? = null,
    val username: String? = null,
)

/**
 * Global authentication state observed by UI and navigation.
 */
sealed interface AuthState {
    data object Unknown : AuthState
    data object Loading : AuthState
    data class Authenticated(val user: UserInfo) : AuthState
    data object Unauthenticated : AuthState
}
