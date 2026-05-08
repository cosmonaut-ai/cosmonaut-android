package com.cosmonaut.app.data.billing

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber

/**
 * Determines whether the user is in the US for Google Play external billing compliance.
 *
 * Strategy:
 * 1. Primary: Google Play Billing Library's user country code
 * 2. Fallback: Device locale country
 * 3. Default: Non-US (safe default per policy — no links to transactional pages)
 *
 * The result determines whether subscription CTAs render as clickable links (US)
 * or plain text (non-US). Defaulting to non-US is safe because it's the more
 * restrictive behavior.
 */
@Singleton
class RegionDetector @Inject constructor(@ApplicationContext private val context: Context,) {
    private val _isUsUser = MutableStateFlow<Boolean?>(null)
    val isUsUser: StateFlow<Boolean?> = _isUsUser.asStateFlow()

    val isUsUserResolved: Boolean
        get() = _isUsUser.value == true

    suspend fun detect() {
        if (_isUsUser.value != null) return

        val countryCode = getPlayBillingCountry()
            ?: getDeviceLocaleCountry()

        _isUsUser.value = countryCode?.equals("US", ignoreCase = true) == true
        Timber.d("Region detected: country=%s, isUS=%s", countryCode, _isUsUser.value)
    }

    private suspend fun getPlayBillingCountry(): String? = suspendCancellableCoroutine { cont ->
        val client = BillingClient.newBuilder(context)
            .setListener { _, _ -> }
            .enablePendingPurchases()
            .build()

        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                val country = if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    try {
                        @Suppress("DEPRECATION")
                        client.connectionState
                        val method = client.javaClass.getMethod("getUserCountry")
                        method.invoke(client) as? String
                    } catch (e: Exception) {
                        Timber.w(e, "Play Billing getUserCountry unavailable")
                        null
                    }
                } else {
                    Timber.w("Play Billing connection failed: %d", result.responseCode)
                    null
                }
                client.endConnection()
                if (cont.isActive) cont.resume(country)
            }

            override fun onBillingServiceDisconnected() {
                if (cont.isActive) cont.resume(null)
            }
        })

        cont.invokeOnCancellation { client.endConnection() }
    }

    private fun getDeviceLocaleCountry(): String? {
        val locale = context.resources.configuration.locales[0]
        return locale.country.takeIf { it.isNotBlank() }
    }
}
