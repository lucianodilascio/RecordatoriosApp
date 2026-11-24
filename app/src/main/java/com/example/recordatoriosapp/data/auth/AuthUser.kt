package com.example.recordatoriosapp.data.auth

/**
 * Modelo de dominio para la sesión del usuario.
 * No expone clases del SDK de Firebase (FirebaseUser) a las capas superiores.
 */
data class AuthUser(
    val uid: String,
    val email: String?
)
