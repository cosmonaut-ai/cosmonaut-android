package com.cosmonaut.app.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Typed wrapper around DataStore for Cosmonaut user preferences.
 * Additional preference keys will be added as features are built.
 */
@Singleton
class CosmoPreferences @Inject constructor(private val dataStore: DataStore<Preferences>,) {

    val themeMode: Flow<ThemeMode>
        get() = dataStore.data.map { prefs ->
            when (prefs[Keys.THEME_MODE]) {
                "light" -> ThemeMode.LIGHT
                "dark" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name.lowercase()
        }
    }

    val hasCompletedOnboarding: Flow<Boolean>
        get() = dataStore.data.map { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] ?: false
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    enum class ThemeMode {
        SYSTEM,
        LIGHT,
        DARK
    }
}
