package com.example.recordatoriosapp.data.remote.api

import com.example.recordatoriosapp.data.remote.dto.ReminderDto
import retrofit2.Response
import retrofit2.http.*

/**
 * Interfaz de Retrofit para consumir el servicio remoto.
 * Usa `Response<T>` para permitir la interpretación de códigos HTTP (ej. 404, 500)
 * y manejar los errores desde la capa del Repositorio (y no romper en el ViewModel).
 */
interface ReminderApiService {

    @GET("reminders")
    suspend fun getReminders(): Response<List<ReminderDto>>

    @POST("reminders")
    suspend fun createReminder(@Body reminder: ReminderDto): Response<ReminderDto>

    @PUT("reminders/{id}")
    suspend fun updateReminder(@Path("id") id: String, @Body reminder: ReminderDto): Response<ReminderDto>

    @DELETE("reminders/{id}")
    suspend fun deleteReminder(@Path("id") id: String): Response<Unit>
}
