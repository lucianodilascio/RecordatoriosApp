package com.example.recordatoriosapp.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemoryRecordatorioRepository : RecordatorioRepository {


    private val _recordatorios = MutableStateFlow<List<Recordatorio>>(emptyList())

    override fun obtenerRecordatorios(): Flow<List<Recordatorio>> {
        return _recordatorios
    }

    override fun obtenerRecordatorio(id: String): Flow<Recordatorio?> {
        return _recordatorios.map { lista ->
            lista.find { it.id == id }
        }
    }

    override suspend fun guardarRecordatorio(recordatorio: Recordatorio) {
        _recordatorios.update { listaActual ->
            val existe = listaActual.any { it.id == recordatorio.id }
            if (existe) {

                listaActual.map { if (it.id == recordatorio.id) recordatorio else it }
            } else {

                listaActual + recordatorio
            }
        }
    }

    override suspend fun eliminarRecordatorio(id: String) {
        _recordatorios.update { listaActual ->
            listaActual.filterNot { it.id == id }
        }
    }
}