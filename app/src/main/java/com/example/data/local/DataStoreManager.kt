package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.example.data.model.UserConsentState

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {

    companion object {
        val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val KEY_MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val KEY_MUSIC_VOLUME = floatPreferencesKey("music_volume")
        val KEY_SFX_VOLUME = floatPreferencesKey("sfx_volume")
        val KEY_ADS_REMOVED = booleanPreferencesKey("ads_removed")
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_DARK_MODE = stringPreferencesKey("dark_mode") // "AUTO", "DARK", "LIGHT"

        val KEY_TERMS_ACCEPTED = booleanPreferencesKey("terms_accepted")
        val KEY_PRIVACY_ACCEPTED = booleanPreferencesKey("privacy_accepted")
        val KEY_TERMS_VERSION_ACCEPTED = stringPreferencesKey("terms_version_accepted")
        val KEY_PRIVACY_VERSION_ACCEPTED = stringPreferencesKey("privacy_version_accepted")
        val KEY_ACCEPTED_AT_LOCAL = longPreferencesKey("accepted_at_local")
        val KEY_CONSENT_SYNC_PENDING = booleanPreferencesKey("consent_sync_pending")

        val KEY_CACHED_LAST_USERNAME_CHANGE_AT = longPreferencesKey("cached_last_username_change_at")
        val KEY_CACHED_NEXT_USERNAME_CHANGE_AT = longPreferencesKey("cached_next_username_change_at")
    }

    val cachedLastUsernameChangeAt: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[KEY_CACHED_LAST_USERNAME_CHANGE_AT]
    }

    val cachedNextUsernameChangeAt: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[KEY_CACHED_NEXT_USERNAME_CHANGE_AT]
    }

    suspend fun updateUsernameChangeCache(lastChangeAt: Long?, nextChangeAt: Long?) {
        context.dataStore.edit { prefs ->
            if (lastChangeAt != null) {
                prefs[KEY_CACHED_LAST_USERNAME_CHANGE_AT] = lastChangeAt
            } else {
                prefs.remove(KEY_CACHED_LAST_USERNAME_CHANGE_AT)
            }
            if (nextChangeAt != null) {
                prefs[KEY_CACHED_NEXT_USERNAME_CHANGE_AT] = nextChangeAt
            } else {
                prefs.remove(KEY_CACHED_NEXT_USERNAME_CHANGE_AT)
            }
        }
    }

    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SOUND_ENABLED] ?: true
    }

    val musicEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_MUSIC_ENABLED] ?: true
    }

    val vibrationEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_VIBRATION_ENABLED] ?: true
    }

    val musicVolume: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_MUSIC_VOLUME] ?: 0.5f
    }

    val sfxVolume: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_SFX_VOLUME] ?: 0.8f
    }

    val isAdsRemoved: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ADS_REMOVED] ?: false
    }

    val language: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LANGUAGE] ?: "PT"
    }

    val darkMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_DARK_MODE] ?: "AUTO"
    }

    val userConsentState: Flow<UserConsentState> = context.dataStore.data.map { prefs ->
        UserConsentState(
            termsAccepted = prefs[KEY_TERMS_ACCEPTED] ?: false,
            privacyAccepted = prefs[KEY_PRIVACY_ACCEPTED] ?: false,
            termsVersionAccepted = prefs[KEY_TERMS_VERSION_ACCEPTED] ?: "",
            privacyVersionAccepted = prefs[KEY_PRIVACY_VERSION_ACCEPTED] ?: "",
            acceptedAtLocal = prefs[KEY_ACCEPTED_AT_LOCAL] ?: 0L,
            consentSyncPending = prefs[KEY_CONSENT_SYNC_PENDING] ?: false
        )
    }

    suspend fun saveConsentLocally(
        termsVersion: String,
        privacyVersion: String,
        acceptedAt: Long,
        syncPending: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TERMS_ACCEPTED] = true
            prefs[KEY_PRIVACY_ACCEPTED] = true
            prefs[KEY_TERMS_VERSION_ACCEPTED] = termsVersion
            prefs[KEY_PRIVACY_VERSION_ACCEPTED] = privacyVersion
            prefs[KEY_ACCEPTED_AT_LOCAL] = acceptedAt
            prefs[KEY_CONSENT_SYNC_PENDING] = syncPending
        }
    }

    suspend fun setConsentSyncPending(pending: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CONSENT_SYNC_PENDING] = pending
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_SOUND_ENABLED] = enabled }
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_MUSIC_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_VIBRATION_ENABLED] = enabled }
    }

    suspend fun setMusicVolume(volume: Float) {
        context.dataStore.edit { prefs -> prefs[KEY_MUSIC_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setSfxVolume(volume: Float) {
        context.dataStore.edit { prefs -> prefs[KEY_SFX_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { prefs -> prefs[KEY_LANGUAGE] = language }
    }

    suspend fun setDarkMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[KEY_DARK_MODE] = mode }
    }

    suspend fun clearAllData() {
        context.dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { prefs ->
            prefs[KEY_SOUND_ENABLED] = true
            prefs[KEY_MUSIC_ENABLED] = true
            prefs[KEY_VIBRATION_ENABLED] = true
            prefs[KEY_MUSIC_VOLUME] = 0.5f
            prefs[KEY_SFX_VOLUME] = 0.8f
            prefs[KEY_LANGUAGE] = "PT"
            prefs[KEY_DARK_MODE] = "AUTO"
        }
    }
}
