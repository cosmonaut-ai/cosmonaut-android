package com.cosmonaut.app.data.billing

import android.content.Context
import android.provider.Settings
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.GetBillingConfigParams
import com.cosmonaut.app.BuildConfig
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
 * 1. Primary: Google Play Billing Library's BillingConfig country code
 * 2. Fallback: Device locale country
 * 3. Default: Non-US (safe default per policy — no links to transactional pages)
 *
 * The result determines whether subscription CTAs render as clickable links (US)
 * or plain text (non-US). Defaulting to non-US is safe because it's the more
 * restrictive behavior.
 */
@Singleton
class RegionDetector @Inject constructor(@ApplicationContext private val context: Context) {
    private val _isUsUser = MutableStateFlow<Boolean?>(null)
    val isUsUser: StateFlow<Boolean?> = _isUsUser.asStateFlow()

    val isUsUserResolved: Boolean
        get() = _isUsUser.value == true

    suspend fun detect() {
        val debugOverride = getDebugCountryOverride()
        val billingCountry = if (debugOverride != null) null else getPlayBillingCountry()
        val localeCountry = getDeviceLocaleCountry()
        Timber.d(
            "Region signals: debug=%s, billing=%s, locale=%s",
            debugOverride,
            billingCountry,
            localeCountry,
        )

        val countryCode = debugOverride ?: billingCountry ?: localeCountry
        val isUs = countryCode?.equals("US", ignoreCase = true) == true
        _isUsUser.value = isUs
        Timber.d("Region resolved: country=%s, isUS=%s", countryCode, isUs)
    }

    /**
     * Debug-only: read country override from Settings.Global via adb.
     *   adb shell settings put global debug_cosmonaut_country AD   (non-US)
     *   adb shell settings put global debug_cosmonaut_country US   (US)
     *   adb shell settings delete global debug_cosmonaut_country   (clear)
     */
    private fun getDebugCountryOverride(): String? {
        if (!BuildConfig.DEBUG) return null
        return try {
            Settings.Global.getString(context.contentResolver, "debug_cosmonaut_country")
                ?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            Timber.w(e, "Failed to read debug country override")
            null
        }
    }

    private suspend fun getPlayBillingCountry(): String? = suspendCancellableCoroutine { cont ->
        val client = BillingClient.newBuilder(context)
            .setListener { _, _ -> }
            .enablePendingPurchases()
            .build()

        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    val params = GetBillingConfigParams.newBuilder().build()
                    client.getBillingConfigAsync(params) { billingResult, billingConfig ->
                        val country = if (
                            billingResult.responseCode == BillingClient.BillingResponseCode.OK &&
                            billingConfig != null
                        ) {
                            billingConfig.countryCode
                        } else {
                            Timber.w(
                                "getBillingConfigAsync failed: %d %s",
                                billingResult.responseCode,
                                billingResult.debugMessage,
                            )
                            null
                        }
                        client.endConnection()
                        if (cont.isActive) cont.resume(country)
                    }
                } else {
                    Timber.w(
                        "Play Billing connection failed: %d %s",
                        result.responseCode,
                        result.debugMessage,
                    )
                    client.endConnection()
                    if (cont.isActive) cont.resume(null)
                }
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
