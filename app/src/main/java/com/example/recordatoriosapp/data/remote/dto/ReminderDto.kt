package com.example.recordatoriosapp.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Object (DTO)
 * Representa exactamente la estructura del JSON que envía/recibe la API remota.
 */
data class ReminderDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("category") val category: String,
    @SerializedName("priority") val priority: String,
    @SerializedName("isCompleted") val isCompleted: Boolean,
    @SerializedName("date") val date: Long
)
