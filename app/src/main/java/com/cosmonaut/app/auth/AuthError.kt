package com.cosmonaut.app.auth

/**
 * Maps Cognito exception messages to user-friendly error strings.
 * Mirrors the web app's formatAuthError() in auth/errors.ts.
 */
object AuthError {

    fun format(error: Throwable): String {
        val message = error.message ?: return "An unexpected error occurred. Please try again."
        return format(message)
    }

    @Suppress("CyclomaticComplexMethod")
    fun format(message: String): String = when {
        message.contains("UserAlreadyAuthenticated", ignoreCase = true) ->
            "You are already signed in."

        message.contains("UsernameExistsException", ignoreCase = true) ||
            message.contains("AccountAlreadyExists", ignoreCase = true) ->
            "An account with this email already exists."

        message.contains("InvalidPasswordException", ignoreCase = true) ->
            "Password does not meet requirements. Please use a stronger password."

        message.contains("NotAuthorizedException", ignoreCase = true) ->
            "Incorrect email or password."

        message.contains("UserNotFoundException", ignoreCase = true) ->
            "No account found with this email."

        message.contains("CodeMismatchException", ignoreCase = true) ->
            "Invalid verification code. Please check and try again."

        message.contains("ExpiredCodeException", ignoreCase = true) ->
            "Verification code has expired. Please request a new one."

        message.contains("LimitExceededException", ignoreCase = true) ->
            "Too many attempts. Please wait a moment and try again."

        message.contains("UserNotConfirmedException", ignoreCase = true) ->
            "Email not verified. Please check your email for a verification code."

        else -> message
    }

    fun isAccountSuspended(error: Throwable): Boolean {
        val message = error.message ?: return false
        return message.contains("NotAuthorizedException", ignoreCase = true) &&
            message.contains("disabled", ignoreCase = true)
    }

    fun isSignUpNotConfirmed(error: Throwable): Boolean {
        val message = error.message ?: return false
        return message.contains("UserNotConfirmedException", ignoreCase = true) ||
            message.contains("not confirmed", ignoreCase = true)
    }
}
