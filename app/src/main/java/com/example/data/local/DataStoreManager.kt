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
