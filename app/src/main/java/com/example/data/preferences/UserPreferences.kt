package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gestion_pro_prefs")

class UserPreferences(private val context: Context) {
    private object Keys {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        val CURRENT_USER_ID = longPreferencesKey("current_user_id")
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_MODE] ?: false }
    val isOfflineMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.OFFLINE_MODE] ?: false }
    val currentUserId: Flow<Long?> = context.dataStore.data.map {
        val id = it[Keys.CURRENT_USER_ID] ?: -1L
        if (id > 0) id else null
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setOfflineMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.OFFLINE_MODE] = enabled }
    }

    suspend fun setCurrentUserId(userId: Long?) {
        context.dataStore.edit {
            if (userId != null) it[Keys.CURRENT_USER_ID] = userId
            else it.remove(Keys.CURRENT_USER_ID)
        }
    }
}
