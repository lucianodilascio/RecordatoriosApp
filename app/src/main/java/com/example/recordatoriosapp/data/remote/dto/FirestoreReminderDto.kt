package com.example.recordatoriosapp.data.remote.dto

/**
 * Data Transfer Object para Firestore.
 * Requiere valores por defecto para que el SDK de Firestore pueda instanciarlo vacío.
 */
data class FirestoreReminderDto(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val priority: String = "",
    val isCompleted: Boolean = false,
    val date: Long = 0L
)
