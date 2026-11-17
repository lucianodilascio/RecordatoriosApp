package com.example.recordatoriosapp.data.repository.mapper

import com.example.recordatoriosapp.data.local.entity.ReminderEntity
import com.example.recordatoriosapp.data.model.Reminder
import com.example.recordatoriosapp.data.remote.dto.ReminderDto

/**
 * Funciones de extensión explícitas para transformar modelos entre las
 * distintas capas (Base de datos <-> Dominio <-> Red), separando responsabilidades.
 */

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

fun Reminder.toEntity(isPendingSync: Boolean = false, isDeleted: Boolean = false) = ReminderEntity(
    id = id,
    title = title,
    description = description,
    category = category,
    priority = priority,
    isCompleted = isCompleted,
    date = date,
    isPendingSync = isPendingSync,
    isDeleted = isDeleted
)

// --- DTO <-> Entity ---

fun ReminderDto.toEntity(isPendingSync: Boolean = false) = ReminderEntity(
    id = id,
    title = title,
    description = description,
    category = category,
    priority = priority,
    isCompleted = isCompleted,
    date = date,
    isPendingSync = isPendingSync,
    isDeleted = false // Lo que viene del backend se asume que no está borrado lógicamente localmente
)

fun ReminderEntity.toDto() = ReminderDto(
    id = id,
    title = title,
    description = description,
    category = category,
    priority = priority,
    isCompleted = isCompleted,
    date = date
)
