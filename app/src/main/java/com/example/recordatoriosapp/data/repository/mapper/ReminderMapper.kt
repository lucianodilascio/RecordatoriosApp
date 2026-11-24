package com.example.recordatoriosapp.data.repository.mapper

import com.example.recordatoriosapp.data.local.entity.ReminderEntity
import com.example.recordatoriosapp.data.model.Reminder
import com.example.recordatoriosapp.data.remote.dto.FirestoreReminderDto

// --- Entity <-> Domain ---

fun ReminderEntity.toDomain() = Reminder(
    id = id,
    title = title,
    description = description,
    category = category,
    priority = priority,
    isCompleted = isCompleted,
    date = date,
    isPendingSync = isPendingSync
)

fun Reminder.toEntity(userId: String, isPendingSync: Boolean = false, isDeleted: Boolean = false) = ReminderEntity(
    id = id,
    userId = userId, // Añadido para multi-usuario
    title = title,
    description = description,
    category = category,
    priority = priority,
    isCompleted = isCompleted,
    date = date,
    isPendingSync = isPendingSync,
    isDeleted = isDeleted
)

// --- Firestore DTO <-> Entity ---

fun FirestoreReminderDto.toEntity(userId: String, isPendingSync: Boolean = false) = ReminderEntity(
    id = id,
    userId = userId,
    title = title,
    description = description,
    category = category,
    priority = priority,
    isCompleted = isCompleted,
    date = date,
    isPendingSync = isPendingSync,
    isDeleted = false // Lo que baja de Firestore nunca viene "borrado logicamente"
)

fun ReminderEntity.toFirestoreDto() = FirestoreReminderDto(
    id = id,
    title = title,
    description = description,
    category = category,
    priority = priority,
    isCompleted = isCompleted,
    date = date
)
