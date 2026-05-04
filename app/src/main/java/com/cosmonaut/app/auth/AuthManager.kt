package com.cosmonaut.app.auth

import android.app.Activity
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.auth.options.AuthFetchSessionOptions
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.auth.result.AuthResetPasswordResult
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.AuthSignUpResult
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.amplifyframework.auth.result.step.AuthSignUpStep
import com.amplifyframework.kotlin.core.Amplify
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

@Suppress("TooManyFunctions")
@Singleton
class AuthManager @Inject constructor() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    @Volatile
    private var cachedIdToken: String? = null

    /**
     * Check current auth session on app launch. Safe to call multiple times.
     */
    suspend fun initialize() {
        if (_authState.value is AuthState.Authenticated) return
        _authState.value = AuthState.Loading
        try {
            refreshAuthState()
        } catch (expected: Exception) {
            Timber.d(expected, "No active session found during init")
            clearState()
        }
    }

    /**
     * Refresh auth state from Amplify session. Updates [authState] and caches the ID token.
     */
    private suspend fun refreshAuthState() {
        val session = Amplify.Auth.fetchAuthSession(
            AuthFetchSessionOptions.builder().forceRefresh(false).build(),
        )
        val cognitoSession = session as? AWSCognitoAuthSession
        val tokens = cognitoSession?.userPoolTokensResult?.value

        val idToken = tokens?.idToken
        if (idToken != null) {
            cachedIdToken = idToken
            val user = extractUserFromIdToken(idToken)
            _authState.value = AuthState.Authenticated(user)
            Timber.i("Auth state refreshed — user: %s", user.email)
        } else {
            clearState()
        }
    }

    // ── Email / Password ──────────────────────────────────────────────

    suspend fun signInWithEmail(email: String, password: String): AuthSignInResult {
        val result = Amplify.Auth.signIn(email.trim(), password)
        if (result.isSignedIn) {
            refreshAuthState()
        }
        return result
    }

    suspend fun signUpWithEmail(email: String, password: String): AuthSignUpResult {
        val options = AuthSignUpOptions.builder()
            .userAttribute(AuthUserAttributeKey.email(), email.trim())
            .build()
        return Amplify.Auth.signUp(email.trim(), password, options)
    }

    suspend fun confirmSignUp(email: String, code: String): AuthSignUpResult =
        Amplify.Auth.confirmSignUp(email.trim(), code.trim())

    suspend fun resendSignUpCode(email: String) {
        Amplify.Auth.resendSignUpCode(email.trim())
    }

    // ── Password Reset ────────────────────────────────────────────────

    suspend fun resetPassword(email: String): AuthResetPasswordResult = Amplify.Auth.resetPassword(email.trim())

    suspend fun confirmResetPassword(email: String, newPassword: String, code: String) {
        Amplify.Auth.confirmResetPassword(email.trim(), newPassword, code.trim())
    }

    // ── Social / OAuth ────────────────────────────────────────────────

    suspend fun signInWithGoogle(callingActivity: Activity): AuthSignInResult {
        val result = Amplify.Auth.signInWithSocialWebUI(
            com.amplifyframework.auth.AuthProvider.google(),
            callingActivity,
        )
        if (result.isSignedIn) {
            refreshAuthState()
        }
        return result
    }

    // ── Sign Out ──────────────────────────────────────────────────────

    suspend fun signOut() {
        try {
            Amplify.Auth.signOut()
        } catch (expected: Exception) {
            Timber.w(expected, "Error during sign out")
        } finally {
            clearState()
        }
    }

    // ── Token Access ──────────────────────────────────────────────────

    /**
     * Returns the cached ID token without network calls. Used by [AuthInterceptor].
     */
    fun getCachedIdToken(): String? = cachedIdToken

    /**
     * Fetches a fresh ID token from Amplify, optionally forcing a refresh.
     */
    suspend fun getIdToken(forceRefresh: Boolean = false): String? {
        try {
            val options = AuthFetchSessionOptions.builder()
                .forceRefresh(forceRefresh)
                .build()
            val session = Amplify.Auth.fetchAuthSession(options) as? AWSCognitoAuthSession
            val token = session?.userPoolTokensResult?.value?.idToken
            cachedIdToken = token
            return token
        } catch (expected: Exception) {
            Timber.w(expected, "Failed to get ID token (forceRefresh=%s)", forceRefresh)
            return null
        }
    }

    // ── User Attributes ───────────────────────────────────────────────

    suspend fun fetchUserAttributes(): List<AuthUserAttribute> = Amplify.Auth.fetchUserAttributes()

    // ── Helpers ───────────────────────────────────────────────────────

    private fun clearState() {
        cachedIdToken = null
        _authState.value = AuthState.Unauthenticated
    }

    /**
     * Decode the JWT payload to extract user info.
     * ID tokens are base64-encoded JSON with three dot-separated segments.
     */
    @Suppress("SwallowedException")
    private fun extractUserFromIdToken(idToken: String): UserInfo {
        try {
            val payload = idToken.split(".").getOrNull(1) ?: return UserInfo(sub = "unknown")
            val decoded = String(android.util.Base64.decode(payload, android.util.Base64.URL_SAFE))
            val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
            val claims = json.decodeFromString<JwtClaims>(decoded)
            return UserInfo(
                sub = claims.sub,
                email = claims.email,
                name = claims.givenName,
                picture = claims.picture,
                username = claims.customUsername,
            )
        } catch (ignored: Exception) {
            Timber.w("Failed to parse ID token claims")
            return UserInfo(sub = "unknown")
        }
    }

    fun needsSignUpConfirmation(result: AuthSignInResult): Boolean =
        result.nextStep.signInStep == AuthSignInStep.CONFIRM_SIGN_UP

    fun needsSignUpConfirmation(result: AuthSignUpResult): Boolean =
        result.nextStep.signUpStep == AuthSignUpStep.CONFIRM_SIGN_UP_STEP
}

@kotlinx.serialization.Serializable
private data class JwtClaims(
    val sub: String = "unknown",
    val email: String? = null,
    @kotlinx.serialization.SerialName("given_name")
    val givenName: String? = null,
    val picture: String? = null,
    @kotlinx.serialization.SerialName("custom:username")
    val customUsername: String? = null,
)
