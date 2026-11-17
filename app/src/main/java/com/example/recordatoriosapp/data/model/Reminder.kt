package com.example.recordatoriosapp.data.model

/**
 * Modelo de Dominio/Presentación.
 * Representa la información que utilizará el ViewModel y la UI, independiente de
 * cómo se almacene en la base de datos local o en el servicio remoto.
 */
data class Reminder(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val priority: String, // Ej: "LOW", "MEDIUM", "HIGH"
    val isCompleted: Boolean,
    val date: Long,
    val isPendingSync: Boolean = false // Permite a la UI mostrar un indicador de sincronización
)
