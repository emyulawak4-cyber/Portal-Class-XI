package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.classXiDataStore: DataStore<Preferences> by preferencesDataStore(name = "class_xi_prefs")

enum class ThemeMode(val label: String) {
    AUTO("Otomatis"),
    LIGHT("Terang"),
    DARK("Gelap");

    companion object {
        fun fromRaw(raw: String?): ThemeMode =
            entries.firstOrNull { it.name == raw } ?: AUTO
    }
}

enum class ThemeStyle(val label: String) {
    DEFAULT("Default"),
    GLASS("Liquid Glass");

    companion object {
        fun fromRaw(raw: String?): ThemeStyle =
            entries.firstOrNull { it.name == raw } ?: DEFAULT
    }
}

data class AppPreferences(
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val themeStyle: ThemeStyle = ThemeStyle.DEFAULT,
    val notificationsEnabled: Boolean = true,
    val lastSeenNotificationMs: Long = 0L,
    val activePengurusRole: String = "",
    val guruModeActive: Boolean = false
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_STYLE = stringPreferencesKey("theme_style")
        val NOTIFY_ENABLED = booleanPreferencesKey("notify_enabled")
        val LAST_SEEN_NOTIF_MS = longPreferencesKey("last_seen_notif_ms")
        val ACTIVE_PENGURUS_ROLE = stringPreferencesKey("active_pengurus_role")
        val GURU_MODE = booleanPreferencesKey("guru_mode")
    }

    val preferencesFlow: Flow<AppPreferences> = context.classXiDataStore.data.map { prefs ->
        AppPreferences(
            themeMode = ThemeMode.fromRaw(prefs[Keys.THEME_MODE]),
            themeStyle = ThemeStyle.fromRaw(prefs[Keys.THEME_STYLE]),
            notificationsEnabled = prefs[Keys.NOTIFY_ENABLED] ?: true,
            lastSeenNotificationMs = prefs[Keys.LAST_SEEN_NOTIF_MS] ?: 0L,
            activePengurusRole = prefs[Keys.ACTIVE_PENGURUS_ROLE] ?: "",
            guruModeActive = prefs[Keys.GURU_MODE] ?: false
        )
    }

    suspend fun getLastSeenNotificationMs(): Long {
        return context.classXiDataStore.data.map { it[Keys.LAST_SEEN_NOTIF_MS] ?: 0L }.first()
    }

    suspend fun isNotificationsEnabled(): Boolean {
        return context.classXiDataStore.data.map { it[Keys.NOTIFY_ENABLED] ?: true }.first()
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.classXiDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setThemeStyle(style: ThemeStyle) {
        context.classXiDataStore.edit { it[Keys.THEME_STYLE] = style.name }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.classXiDataStore.edit { it[Keys.NOTIFY_ENABLED] = enabled }
    }

    suspend fun updateLastSeenNotificationMs(timestampMs: Long) {
        context.classXiDataStore.edit { prefs ->
            val current = prefs[Keys.LAST_SEEN_NOTIF_MS] ?: 0L
            if (timestampMs > current) {
                prefs[Keys.LAST_SEEN_NOTIF_MS] = timestampMs
            }
        }
    }

    suspend fun setPengurusRole(role: String) {
        context.classXiDataStore.edit { it[Keys.ACTIVE_PENGURUS_ROLE] = role }
    }

    suspend fun setGuruMode(active: Boolean) {
        context.classXiDataStore.edit { it[Keys.GURU_MODE] = active }
    }
}
