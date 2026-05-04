package com.cosmonaut.app.util

import timber.log.Timber

/**
 * Centralized logging facade. Delegates to Timber for all log output.
 * Timber's DebugTree is planted in CosmoApp for debug builds only.
 * In release builds, a crash-reporting tree (Sentry) will be planted in Stage 11.
 */
object CosmoLogger {

    fun d(message: String, vararg args: Any?) {
        Timber.d(message, *args)
    }

    fun i(message: String, vararg args: Any?) {
        Timber.i(message, *args)
    }

    fun w(message: String, vararg args: Any?) {
        Timber.w(message, *args)
    }

    fun w(throwable: Throwable, message: String, vararg args: Any?) {
        Timber.w(throwable, message, *args)
    }

    fun e(message: String, vararg args: Any?) {
        Timber.e(message, *args)
    }

    fun e(throwable: Throwable, message: String, vararg args: Any?) {
        Timber.e(throwable, message, *args)
    }
}
