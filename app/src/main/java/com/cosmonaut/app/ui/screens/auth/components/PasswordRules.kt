package com.cosmonaut.app.ui.screens.auth.components

private const val MIN_PASSWORD_LENGTH = 8

data class PasswordRules(
    val hasMinLength: Boolean,
    val hasLowercase: Boolean,
    val hasUppercase: Boolean,
    val hasDigit: Boolean,
    val hasSymbol: Boolean,
) {
    val allPassed: Boolean
        get() = hasMinLength && hasLowercase && hasUppercase && hasDigit && hasSymbol

    companion object {
        fun evaluate(password: String): PasswordRules = PasswordRules(
            hasMinLength = password.length >= MIN_PASSWORD_LENGTH,
            hasLowercase = password.any { it.isLowerCase() },
            hasUppercase = password.any { it.isUpperCase() },
            hasDigit = password.any { it.isDigit() },
            hasSymbol = password.any { !it.isLetterOrDigit() },
        )
    }
}
