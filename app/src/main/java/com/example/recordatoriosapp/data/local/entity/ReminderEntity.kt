package com.example.recordatoriosapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de base de datos para Room.
 * Representa la tabla donde persistiremos localmente los recordatorios.
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String, // Identificador estable (ej. UUID) proporcionado desde la creación
    val title: String,
    val description: String,
    val category: String,
    val priority: String, // "LOW", "MEDIUM", "HIGH"
    val isCompleted: Boolean,
    val date: Long, // Unix timestamp en milisegundos
    
    // Campos necesarios para la lógica offline (sincronización con el repositorio)
    val isPendingSync: Boolean = false, 
    val isDeleted: Boolean = false // Borrado lógico para recordarlo y borrarlo en backend
)
