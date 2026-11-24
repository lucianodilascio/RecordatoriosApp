package com.example.recordatoriosapp.data.auth

/**
 * Representa los distintos estados de la sesión del usuario.
 */
sealed interface AuthState {
    object Loading : AuthState
    data class Authenticated(val user: AuthUser) : AuthState
    object Unauthenticated : AuthState
    data class Error(val message: String) : AuthState
    data class SessionInvalid(val message: String) : AuthState
}
