package com.example.recordatoriosapp.data.local.dao

import androidx.room.*
import com.example.recordatoriosapp.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    // --- Consultas Reactivas para la UI (usando Flow) ---

    @Query("SELECT * FROM reminders WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    // Filtro requerido por la consigna (ej: por estado de completado)
    @Query("SELECT * FROM reminders WHERE isDeleted = 0 AND isCompleted = :isCompleted ORDER BY date DESC")
    fun getRemindersByStatus(isCompleted: Boolean): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isDeleted = 0 AND id = :id LIMIT 1")
    fun getReminderById(id: String): Flow<ReminderEntity?>


    // --- Operaciones transaccionales y offline ---

    // Sirve al repositorio para saber qué registros debe mandar a Retrofit cuando vuelva internet
    @Query("SELECT * FROM reminders WHERE isPendingSync = 1 OR isDeleted = 1")
    suspend fun getPendingSyncReminders(): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    // Borrado lógico: lo oculta de la UI al poner isDeleted = 1 y lo marca para subir (isPendingSync)
    @Query("UPDATE reminders SET isDeleted = 1, isPendingSync = 1 WHERE id = :id")
    suspend fun softDeleteReminder(id: String)
    
    // Borrado definitivo: ejecutado únicamente tras recibir respuesta HTTP 200/204 del backend
    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun hardDeleteReminder(id: String)
}
