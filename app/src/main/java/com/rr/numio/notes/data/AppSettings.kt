package com.rr.numio.notes.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "settings")

// Appearance choices and notebook names, kept on the phone with DataStore
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

    // Sorting: "edited", "created" or "title"; reverse; unfinished to-do lists on top
    private val sortKey = stringPreferencesKey("sort")
    private val reverseKey = booleanPreferencesKey("sort_reverse")
    private val todosFirstKey = booleanPreferencesKey("todos_first")
    val sort: Flow<SortChoice> = context.settingsStore.data.map {
        SortChoice(it[sortKey] ?: "edited", it[reverseKey] ?: false, it[todosFirstKey] ?: false)
    }

    suspend fun setSort(choice: SortChoice) {
        context.settingsStore.edit {
            it[sortKey] = choice.by
            it[reverseKey] = choice.reverse
            it[todosFirstKey] = choice.todosFirst
        }
    }

    // App lock (fingerprint / screen lock)
    private val lockKey = booleanPreferencesKey("app_lock")
    val appLock: Flow<Boolean> = context.settingsStore.data.map { it[lockKey] ?: false }

    suspend fun setAppLock(on: Boolean) {
        context.settingsStore.edit { it[lockKey] = on }
    }

    // Notebooks you created (they show even while empty)
    private val notebooksKey = stringSetPreferencesKey("notebooks")
    val notebooks: Flow<Set<String>> = context.settingsStore.data.map { it[notebooksKey] ?: emptySet() }

    suspend fun addNotebook(name: String) {
        context.settingsStore.edit { it[notebooksKey] = (it[notebooksKey] ?: emptySet()) + name }
    }

    suspend fun removeNotebook(name: String) {
        context.settingsStore.edit { it[notebooksKey] = (it[notebooksKey] ?: emptySet()) - name }
    }
}

data class SortChoice(val by: String = "edited", val reverse: Boolean = false, val todosFirst: Boolean = false)
