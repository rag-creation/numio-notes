package com.rr.numio.notes.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "settings")

// Appearance choices, kept on the phone with DataStore
class AppSettings(private val context: Context) {

    private val modeKey = stringPreferencesKey("mode")   // "dark", "light", "system"
    private val themeKey = stringPreferencesKey("theme") // "numio", "bloom", ...

    val mode: Flow<String> = context.settingsStore.data.map { it[modeKey] ?: "dark" }
    val theme: Flow<String> = context.settingsStore.data.map { it[themeKey] ?: "numio" }

    suspend fun setMode(value: String) {
        context.settingsStore.edit { it[modeKey] = value }
    }

    suspend fun setTheme(value: String) {
        context.settingsStore.edit { it[themeKey] = value }
    }
}
