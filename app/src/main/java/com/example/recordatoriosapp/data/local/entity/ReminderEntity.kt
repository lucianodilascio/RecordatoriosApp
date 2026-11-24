package com.example.recordatoriosapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de base de datos para Room.
 * Modificada para aislar los datos por usuario (soporte multi-cuenta).
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val userId: String, // Clave para aislar la información de cada cuenta
    val title: String,
    val description: String,
    val category: String,
    val priority: String,
    val isCompleted: Boolean,
    val date: Long,
    
    // Campos necesarios para la lógica offline/sync
    val isPendingSync: Boolean = false, 
    val isDeleted: Boolean = false
)
