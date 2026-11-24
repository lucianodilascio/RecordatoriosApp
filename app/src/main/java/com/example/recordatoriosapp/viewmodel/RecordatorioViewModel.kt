package com.example.recordatoriosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatoriosapp.data.model.Reminder
import com.example.recordatoriosapp.data.model.UserPreferences
import com.example.recordatoriosapp.data.repository.ReminderRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

// 1. Estados para la pantalla de Listado
sealed interface ListaUiState {
    object Cargando : ListaUiState
    object Vacio : ListaUiState
    data class ConDatos(val recordatorios: List<Reminder>) : ListaUiState
    data class Error(val mensaje: String) : ListaUiState
}

// 2. Estado para el Formulario de Creación
data class FormularioUiState(
    val titulo: String = "",
    val descripcion: String = "",
    val categoria: String? = null,
    val prioridad: String? = null,
    val enviando: Boolean = false,
    val guardadoExitoso: Boolean = false,
    val error: String? = null
) {
    val tituloValido: Boolean get() = titulo.trim().length >= 4
    val descripcionValida: Boolean get() = descripcion.length <= 200
    val esValido: Boolean get() = tituloValido && descripcionValida && categoria != null && prioridad != null
}

// 3. ViewModel
class RecordatorioViewModel(
    private val repository: ReminderRepository,
    private val authRepository: com.example.recordatoriosapp.data.auth.AuthRepository
) : ViewModel() {

    // --- Preferencias del usuario ---
    val userPreferences: StateFlow<UserPreferences> = repository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // --- Listado Reactivo ---
    // Combinamos las preferencias con el listado de Room para aplicar orden y filtros
    val listaUiState: StateFlow<ListaUiState> = combine(
        repository.getAllReminders(),
        userPreferences
    ) { lista, prefs ->
        val filtrados = if (prefs.showCompleted) {
            lista
        } else {
            lista.filter { !it.isCompleted }
        }

        val ordenados = if (prefs.sortOrder == "DATE_DESC") {
            filtrados.sortedByDescending { it.date }
        } else { // ej: PRIORITY_DESC
            // Mapeo simple: HIGH=3, MEDIUM=2, LOW=1
            filtrados.sortedByDescending { 
                when(it.priority) {
                    "HIGH" -> 3
                    "MEDIUM" -> 2
                    else -> 1
                }
            }
        }

        if (ordenados.isEmpty()) {
            ListaUiState.Vacio
        } else {
            ListaUiState.ConDatos(ordenados)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ListaUiState.Cargando
    )

    private val _formularioUiState = MutableStateFlow(FormularioUiState())
    val formularioUiState: StateFlow<FormularioUiState> = _formularioUiState.asStateFlow()

    init {
        // Observar cuando se autentique el usuario para disparar la sincronización remota
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                if (state is com.example.recordatoriosapp.data.auth.AuthState.Authenticated) {
                    sincronizarConServidor()
                }
            }
        }
    }

    fun sincronizarConServidor() {
        viewModelScope.launch {
            _isSyncing.value = true
            repository.syncWithRemote()
            _isSyncing.value = false
        }
    }

    // --- Actualización de Preferencias ---
    fun toggleDarkMode(isDark: Boolean) {
        viewModelScope.launch { repository.updateDarkMode(isDark) }
    }
    
    fun toggleShowCompleted(show: Boolean) {
        viewModelScope.launch { repository.updateShowCompleted(show) }
    }
    
    fun toggleSortOrder() {
        val newOrder = if (userPreferences.value.sortOrder == "DATE_DESC") "PRIORITY_DESC" else "DATE_DESC"
        viewModelScope.launch { repository.updateSortOrder(newOrder) }
    }

    // --- Funciones del Formulario ---
    fun actualizarTitulo(nuevoTitulo: String) {
        _formularioUiState.update { it.copy(titulo = nuevoTitulo) }
    }

    fun actualizarDescripcion(nuevaDesc: String) {
        _formularioUiState.update { it.copy(descripcion = nuevaDesc) }
    }

    fun seleccionarCategoria(categoria: String) {
        _formularioUiState.update { it.copy(categoria = categoria) }
    }

    fun seleccionarPrioridad(prioridad: String) {
        _formularioUiState.update { it.copy(prioridad = prioridad) }
    }

    fun reiniciarFormulario() {
        _formularioUiState.value = FormularioUiState()
    }

    fun guardarRecordatorio() {
        if (!_formularioUiState.value.esValido) return

        viewModelScope.launch {
            _formularioUiState.update { it.copy(enviando = true, error = null) }
            try {
                val estadoActual = _formularioUiState.value
                val nuevoRecordatorio = Reminder(
                    id = UUID.randomUUID().toString(),
                    title = estadoActual.titulo.trim(),
                    description = estadoActual.descripcion.trim(),
                    category = estadoActual.categoria!!,
                    priority = estadoActual.prioridad!!,
                    isCompleted = false,
                    date = System.currentTimeMillis()
                )
                // Se guarda localmente y se intenta enviar (Offline-First)
                repository.createReminder(nuevoRecordatorio)
                _formularioUiState.update { it.copy(enviando = false, guardadoExitoso = true) }
            } catch (e: Exception) {
                _formularioUiState.update { it.copy(enviando = false, error = "Error interno") }
            }
        }
    }

    fun eliminarRecordatorio(id: String) {
        viewModelScope.launch {
            repository.deleteReminder(id)
        }
    }

    fun alternarCompletado(recordatorio: Reminder) {
        viewModelScope.launch {
            val updated = recordatorio.copy(isCompleted = !recordatorio.isCompleted)
            repository.updateReminder(updated)
        }
    }
}
