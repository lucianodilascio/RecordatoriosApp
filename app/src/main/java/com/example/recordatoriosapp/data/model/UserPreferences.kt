package com.example.recordatoriosapp.data.model

/**
 * Modelo que agrupa las preferencias simples del usuario.
 * Define la estructura de las configuraciones que persistiremos con DataStore.
 */
data class UserPreferences(
    val isDarkMode: Boolean = false,
    val showCompleted: Boolean = true,
    val sortOrder: String = "DATE_DESC"
)
