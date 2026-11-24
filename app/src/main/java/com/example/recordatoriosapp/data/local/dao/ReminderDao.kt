package com.example.recordatoriosapp.data.local.dao

import androidx.room.*
import com.example.recordatoriosapp.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    // --- Consultas Reactivas para la UI filtradas por USER ID ---

    @Query("SELECT * FROM reminders WHERE userId = :userId AND isDeleted = 0 ORDER BY date DESC")
    fun getAllReminders(userId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE userId = :userId AND isDeleted = 0 AND isCompleted = :isCompleted ORDER BY date DESC")
    fun getRemindersByStatus(userId: String, isCompleted: Boolean): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE userId = :userId AND isDeleted = 0 AND id = :id LIMIT 1")
    fun getReminderById(userId: String, id: String): Flow<ReminderEntity?>


    // --- Operaciones transaccionales y offline filtradas por USER ID ---

    @Query("SELECT * FROM reminders WHERE userId = :userId AND (isPendingSync = 1 OR isDeleted = 1)")
    suspend fun getPendingSyncReminders(userId: String): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>): List<Long>

    @Update
    suspend fun updateReminder(reminder: ReminderEntity): Int

    @Query("UPDATE reminders SET isDeleted = 1, isPendingSync = 1 WHERE userId = :userId AND id = :id")
    suspend fun softDeleteReminder(id: String, userId: String): Int
    
    @Query("DELETE FROM reminders WHERE userId = :userId AND id = :id")
    suspend fun hardDeleteReminder(id: String, userId: String): Int

    // --- Limpieza para el LogOut ---
    
    @Query("DELETE FROM reminders")
    suspend fun clearAllReminders()
}
