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

    val hasSeenCarousel: Flow<Boolean>
        get() = dataStore.data.map { prefs ->
            prefs[Keys.CAROUSEL_SEEN] ?: false
        }

    suspend fun setCarouselSeen(seen: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.CAROUSEL_SEEN] = seen
        }
    }

    // ── Story creation defaults ──────────────────────────────────────

    val storyLength: Flow<String>
        get() = dataStore.data.map { prefs ->
            prefs[Keys.STORY_LENGTH] ?: "medium"
        }

    suspend fun setStoryLength(length: String) {
        dataStore.edit { prefs ->
            prefs[Keys.STORY_LENGTH] = length
        }
    }

    val vocabLevel: Flow<String>
        get() = dataStore.data.map { prefs ->
            prefs[Keys.VOCAB_LEVEL] ?: "teen"
        }

    suspend fun setVocabLevel(level: String) {
        dataStore.edit { prefs ->
            prefs[Keys.VOCAB_LEVEL] = level
        }
    }

    val contentFilter: Flow<String>
        get() = dataStore.data.map { prefs ->
            prefs[Keys.CONTENT_FILTER] ?: "none"
        }

    suspend fun setContentFilter(filter: String) {
        dataStore.edit { prefs ->
            prefs[Keys.CONTENT_FILTER] = filter
        }
    }

    // ── Audio narration preferences ─────────────────────────────────

    val audioVoiceId: Flow<String?>
        get() = dataStore.data.map { prefs ->
            prefs[Keys.AUDIO_VOICE_ID]
        }

    suspend fun setAudioVoiceId(voiceId: String) {
        dataStore.edit { prefs ->
            prefs[Keys.AUDIO_VOICE_ID] = voiceId
        }
    }

    val audioPlaybackSpeed: Flow<Float>
        get() = dataStore.data.map { prefs ->
            prefs[Keys.AUDIO_PLAYBACK_SPEED]?.toFloatOrNull() ?: 1f
        }

    suspend fun setAudioPlaybackSpeed(speed: Float) {
        dataStore.edit { prefs ->
            prefs[Keys.AUDIO_PLAYBACK_SPEED] = speed.toString()
        }
    }

    val audioVolume: Flow<Float>
        get() = dataStore.data.map { prefs ->
            prefs[Keys.AUDIO_VOLUME]?.toFloatOrNull() ?: 1f
        }

    suspend fun setAudioVolume(volume: Float) {
        dataStore.edit { prefs ->
            prefs[Keys.AUDIO_VOLUME] = volume.toString()
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val CAROUSEL_SEEN = booleanPreferencesKey("carousel_seen")
        val STORY_LENGTH = stringPreferencesKey("story_length")
        val VOCAB_LEVEL = stringPreferencesKey("vocab_level")
        val CONTENT_FILTER = stringPreferencesKey("content_filter")
        val AUDIO_VOICE_ID = stringPreferencesKey("audio_voice_id")
        val AUDIO_PLAYBACK_SPEED = stringPreferencesKey("audio_playback_speed")
        val AUDIO_VOLUME = stringPreferencesKey("audio_volume")
    }

    enum class ThemeMode {
        SYSTEM,
        LIGHT,
        DARK
    }
}
