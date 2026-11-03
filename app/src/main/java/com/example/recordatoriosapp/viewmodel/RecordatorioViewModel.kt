package com.example.recordatoriosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recordatoriosapp.data.Categoria
import com.example.recordatoriosapp.data.Prioridad
import com.example.recordatoriosapp.data.Recordatorio
import com.example.recordatoriosapp.data.RecordatorioRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

// 1. Estados para la pantalla de Listado (Excluyentes según rúbrica)
sealed interface ListaUiState {
    object Cargando : ListaUiState
    object Vacio : ListaUiState
    data class ConDatos(val recordatorios: List<Recordatorio>) : ListaUiState
    data class Error(val mensaje: String) : ListaUiState
}

// 2. Estado para el Formulario de Creación
data class FormularioUiState(
    val titulo: String = "",
    val descripcion: String = "",
    val categoria: Categoria? = null,
    val prioridad: Prioridad? = null,
    val enviando: Boolean = false,
    val guardadoExitoso: Boolean = false,
    val error: String? = null
) {
    // Validaciones exigidas por TECLAB
    val tituloValido: Boolean get() = titulo.trim().length >= 4
    val descripcionValida: Boolean get() = descripcion.length <= 200
    val esValido: Boolean get() = tituloValido && descripcionValida && categoria != null && prioridad != null
}

// 3. ViewModel
class RecordatorioViewModel(
    private val repository: RecordatorioRepository
) : ViewModel() {

    private val _listaUiState = MutableStateFlow<ListaUiState>(ListaUiState.Cargando)
    val listaUiState: StateFlow<ListaUiState> = _listaUiState.asStateFlow()

    private val _formularioUiState = MutableStateFlow(FormularioUiState())
    val formularioUiState: StateFlow<FormularioUiState> = _formularioUiState.asStateFlow()

    init {
        cargarRecordatorios()
    }

    fun cargarRecordatorios() {
        viewModelScope.launch {
            _listaUiState.value = ListaUiState.Cargando
            try {
                delay(1000) // Simulación asincrónica
                repository.obtenerRecordatorios().collect { lista ->
                    if (lista.isEmpty()) {
                        _listaUiState.value = ListaUiState.Vacio
                    } else {
                        _listaUiState.value = ListaUiState.ConDatos(lista)
                    }
                }
            } catch (e: Exception) {
                _listaUiState.value = ListaUiState.Error("Error al cargar datos")
            }
        }
    }

    // Funciones de actualización del formulario
    fun actualizarTitulo(nuevoTitulo: String) {
        _formularioUiState.update { it.copy(titulo = nuevoTitulo) }
    }

    fun actualizarDescripcion(nuevaDesc: String) {
        _formularioUiState.update { it.copy(descripcion = nuevaDesc) }
    }

    fun seleccionarCategoria(categoria: Categoria) {
        _formularioUiState.update { it.copy(categoria = categoria) }
    }

    fun seleccionarPrioridad(prioridad: Prioridad) {
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
                delay(1500) // Simulación de guardado
                val estadoActual = _formularioUiState.value
                val nuevoRecordatorio = Recordatorio(
                    id = UUID.randomUUID().toString(),
                    titulo = estadoActual.titulo.trim(),
                    descripcion = estadoActual.descripcion.trim(),
                    categoria = estadoActual.categoria!!,
                    prioridad = estadoActual.prioridad!!
                )
                repository.guardarRecordatorio(nuevoRecordatorio)
                _formularioUiState.update { it.copy(enviando = false, guardadoExitoso = true) }
            } catch (e: Exception) {
                _formularioUiState.update { it.copy(enviando = false, error = "Error al guardar") }
            }
        }
    }

    fun eliminarRecordatorio(id: String) {
        viewModelScope.launch {
            try {
                repository.eliminarRecordatorio(id)
            } catch (e: Exception) {
                // Manejo de error opcional si falla la eliminación
            }
        }
    }
}