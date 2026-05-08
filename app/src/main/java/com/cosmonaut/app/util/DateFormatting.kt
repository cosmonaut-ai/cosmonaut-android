package com.cosmonaut.app.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private val LONG_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US)
private val SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d", Locale.US)

/**
 * Formats a date string for display (e.g. "January 15, 2025").
 * Returns "N/A" when the date is null or empty.
 */
fun formatDate(dateStr: String?): String {
    if (dateStr.isNullOrEmpty()) return "N/A"
    return try {
        val instant = Instant.parse(dateStr)
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        localDate.format(LONG_DATE_FORMATTER)
    } catch (_: Exception) {
        "N/A"
    }
}

/**
 * Formats a reset date with relative messaging:
 * "Resets tomorrow.", "Resets in 3 days.", "Resets Jan 15".
 * Returns null when the date is null or empty.
 */
fun formatResetDate(dateStr: String?): String? {
    if (dateStr.isNullOrEmpty()) return null
    return try {
        val instant = Instant.parse(dateStr)
        val resetDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        val diffDays = ChronoUnit.DAYS.between(today, resetDate).toInt()

        when {
            diffDays <= 0 -> "Resets soon."
            diffDays == 1 -> "Resets tomorrow."
            diffDays <= 7 -> "Resets in $diffDays days."
            else -> "Resets ${resetDate.format(SHORT_DATE_FORMATTER)}."
        }
    } catch (_: Exception) {
        null
    }
}
