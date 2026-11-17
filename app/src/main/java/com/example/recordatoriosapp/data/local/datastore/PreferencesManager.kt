package com.example.recordatoriosapp.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.recordatoriosapp.data.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Instancia única de DataStore delegada en el Context de la aplicación
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class PreferencesManager(private val context: Context) {

    // Claves tipadas
    private object PreferencesKeys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val SHOW_COMPLETED = booleanPreferencesKey("show_completed")
        val SORT_ORDER = stringPreferencesKey("sort_order")
    }

    /**
     * Flujo que expone el modelo de preferencias para ser observado.
     * Maneja errores recuperables (IOException) y define valores predeterminados
     * en caso de que aún no existan datos guardados.
     */
    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                // Si hay un error de lectura recuperable, emitimos preferencias vacías
                // para que el map posterior aplique los valores por defecto.
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val isDarkMode = preferences[PreferencesKeys.IS_DARK_MODE] ?: false
            val showCompleted = preferences[PreferencesKeys.SHOW_COMPLETED] ?: true
            val sortOrder = preferences[PreferencesKeys.SORT_ORDER] ?: "DATE_DESC"

            UserPreferences(
                isDarkMode = isDarkMode,
                showCompleted = showCompleted,
                sortOrder = sortOrder
            )
        }

    // Funciones suspendidas para actualizar las preferencias (ejecutadas fuera del hilo principal)

    suspend fun updateDarkMode(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] = isDarkMode
        }
    }

    suspend fun updateShowCompleted(showCompleted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_COMPLETED] = showCompleted
        }
    }

    suspend fun updateSortOrder(sortOrder: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORT_ORDER] = sortOrder
        }
    }
}
