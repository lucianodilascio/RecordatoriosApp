package com.example.recordatoriosapp.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.FirebaseNetworkException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AuthRepository(private val auth: FirebaseAuth) {

    /**
     * Observación reactiva del estado de autenticación usando callbackFlow.
     */
    val authState: Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                trySend(AuthState.Authenticated(AuthUser(user.uid, user.email)))
            } else {
                trySend(AuthState.Unauthenticated)
            }
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun getCurrentUser(): AuthUser? {
        return auth.currentUser?.let { AuthUser(it.uid, it.email) }
    }

    suspend fun signIn(email: String, pass: String): Result<AuthUser> = try {
        val result = auth.signInWithEmailAndPassword(email, pass).await()
        val user = result.user ?: throw Exception("Usuario nulo tras inicio de sesión")
        Result.success(AuthUser(user.uid, user.email))
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        Result.failure(Exception("Credenciales inválidas o cuenta no existe."))
    } catch (e: FirebaseAuthInvalidUserException) {
        Result.failure(Exception("La cuenta ha sido deshabilitada."))
    } catch (e: FirebaseNetworkException) {
        Result.failure(Exception("Error de red. Verifica tu conexión."))
    } catch (e: Exception) {
        Result.failure(Exception("Error al iniciar sesión: ${e.localizedMessage}"))
    }

    suspend fun signUp(email: String, pass: String): Result<AuthUser> = try {
        val result = auth.createUserWithEmailAndPassword(email, pass).await()
        val user = result.user ?: throw Exception("Usuario nulo tras registro")
        Result.success(AuthUser(user.uid, user.email))
    } catch (e: FirebaseAuthUserCollisionException) {
        Result.failure(Exception("El correo ya está registrado."))
    } catch (e: FirebaseNetworkException) {
        Result.failure(Exception("Error de red. Verifica tu conexión."))
    } catch (e: Exception) {
        Result.failure(Exception("Error al registrarse: ${e.localizedMessage}"))
    }

    suspend fun signOut() {
        auth.signOut()
    }

    /**
     * Verifica que la sesión actual siga siendo válida (que no haya sido revocada o borrada en el servidor).
     */
    suspend fun validateCurrentSession(): AuthState {
        val user = auth.currentUser ?: return AuthState.Unauthenticated
        return try {
            user.reload().await()
            AuthState.Authenticated(AuthUser(user.uid, user.email))
        } catch (e: FirebaseAuthInvalidUserException) {
            // El token fue revocado o la cuenta eliminada
            auth.signOut()
            AuthState.SessionInvalid("Tu sesión expiró o la cuenta fue modificada.")
        } catch (e: FirebaseNetworkException) {
            // No hay red, pero la sesión local sigue siendo válida
            AuthState.Authenticated(AuthUser(user.uid, user.email))
        } catch (e: Exception) {
            AuthState.Error("No se pudo validar la sesión.")
        }
    }
}
