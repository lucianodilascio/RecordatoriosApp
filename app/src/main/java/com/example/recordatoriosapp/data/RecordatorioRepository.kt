package com.example.recordatoriosapp.data

import kotlinx.coroutines.flow.Flow

interface RecordatorioRepository {
    fun obtenerRecordatorios(): Flow<List<Recordatorio>>
    fun obtenerRecordatorio(id: String): Flow<Recordatorio?>
    suspend fun guardarRecordatorio(recordatorio: Recordatorio)
    suspend fun eliminarRecordatorio(id: String)
}