package com.example.recordatoriosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatoriosapp.data.auth.AuthRepository
import com.example.recordatoriosapp.data.auth.AuthState
import com.example.recordatoriosapp.data.repository.ReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    // Estado de la sesión
    val authState: StateFlow<AuthState> = authRepository.authState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AuthState.Loading
        )

    private val _testSecurityResult = MutableStateFlow<String?>(null)
    val testSecurityResult: StateFlow<String?> = _testSecurityResult.asStateFlow()

    init {
        // Al reabrir la app, verificamos que la sesión no haya sido revocada remotamente
        viewModelScope.launch {
            authRepository.validateCurrentSession()
        }
    }

    fun signIn(email: String, pass: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.signIn(email, pass)
            result.onFailure { onError(it.message ?: "Error desconocido") }
        }
    }

    fun signUp(email: String, pass: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.signUp(email, pass)
            result.onFailure { onError(it.message ?: "Error desconocido") }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            // Regla de seguridad: Borramos datos locales antes de destruir la sesión
            reminderRepository.clearLocalData()
            authRepository.signOut()
        }
    }

    fun testSecurityIsolation(targetUid: String) {
        viewModelScope.launch {
            val result = reminderRepository.testUnauthorizedAccess(targetUid)
            result.onSuccess {
                _testSecurityResult.value = "Fallo de seguridad: Reglas vulneradas, se pudo acceder a datos ajenos."
            }.onFailure {
                _testSecurityResult.value = it.message // Debería ser PERMISSION_DENIED controlado
            }
        }
    }
}
