package com.example.recordatoriosapp.data.repository

import android.util.Log
import com.example.recordatoriosapp.data.local.dao.ReminderDao
import com.example.recordatoriosapp.data.local.datastore.PreferencesManager
import com.example.recordatoriosapp.data.local.entity.ReminderEntity
import com.example.recordatoriosapp.data.model.Reminder
import com.example.recordatoriosapp.data.model.UserPreferences
import com.example.recordatoriosapp.data.remote.api.ReminderApiService
import com.example.recordatoriosapp.data.repository.mapper.toDomain
import com.example.recordatoriosapp.data.repository.mapper.toDto
import com.example.recordatoriosapp.data.repository.mapper.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Repositorio Local-First.
 * Coordina Room (Fuente de verdad), Retrofit (Sincronización remota) y DataStore (Preferencias).
 */
class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val apiService: ReminderApiService,
    private val preferencesManager: PreferencesManager
) {
    private val TAG = "ReminderRepository"

    // --- Preferencias (Delegadas a DataStore) ---

    val userPreferencesFlow: Flow<UserPreferences> = preferencesManager.userPreferencesFlow

    suspend fun updateDarkMode(isDarkMode: Boolean) = preferencesManager.updateDarkMode(isDarkMode)
    suspend fun updateShowCompleted(showCompleted: Boolean) = preferencesManager.updateShowCompleted(showCompleted)
    suspend fun updateSortOrder(sortOrder: String) = preferencesManager.updateSortOrder(sortOrder)


    // --- Lecturas Locales Reactivas (Fuente de Verdad = Room) ---

    fun getAllReminders(): Flow<List<Reminder>> {
        return reminderDao.getAllReminders().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getRemindersByStatus(isCompleted: Boolean): Flow<List<Reminder>> {
        return reminderDao.getRemindersByStatus(isCompleted).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun getReminderById(id: String): Flow<Reminder?> {
        return reminderDao.getReminderById(id).map { it?.toDomain() }
    }


    // --- Operaciones CRUD Offline-First ---

    suspend fun createReminder(reminder: Reminder) {
        // 1. Guardar localmente marcado como pendiente (offline first)
        val entity = reminder.toEntity(isPendingSync = true)
        reminderDao.insertReminder(entity)
        
        // 2. Intentar sincronizar (fire and forget)
        trySyncSingle(entity)
    }

    suspend fun updateReminder(reminder: Reminder) {
        val entity = reminder.toEntity(isPendingSync = true)
        reminderDao.updateReminder(entity)
        trySyncSingle(entity)
    }

    suspend fun deleteReminder(id: String) {
        // Borrado lógico para ocultarlo de la UI inmediatamente
        reminderDao.softDeleteReminder(id)
        
        // Recuperamos la entidad para intentar enviar el DELETE al servidor
        val entityToDelete = reminderDao.getPendingSyncReminders().find { it.id == id }
        entityToDelete?.let { trySyncSingle(it) }
    }


    // --- Sincronización Remota Controlada ---

    /**
     * Sincronización bidireccional.
     * 1. Sube cambios locales pendientes (Push).
     * 2. Descarga datos del servidor (Pull) y actualiza BD local respetando pendientes.
     */
    suspend fun syncWithRemote() {
        // 1. Subir pendientes
        val pendingEntities = reminderDao.getPendingSyncReminders()
        for (entity in pendingEntities) {
            trySyncSingle(entity)
        }

        // 2. Descargar servidor
        try {
            val response = apiService.getReminders()
            if (response.isSuccessful) {
                response.body()?.let { dtos ->
                    // Evitamos pisar registros locales que AÚN estén pendientes de subir (en caso de fallas recientes)
                    val currentPendingIds = reminderDao.getPendingSyncReminders().map { it.id }.toSet()
                    
                    val newEntities = dtos.mapNotNull { dto ->
                        if (currentPendingIds.contains(dto.id)) null else dto.toEntity(isPendingSync = false)
                    }
                    
                    // Actualiza la BD, los flows de Room se encargarán de notificar a la UI
                    reminderDao.insertReminders(newEntities)
                }
            } else {
                Log.e(TAG, "Error HTTP al sincronizar lista (Pull): ${response.code()}")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Sin conexión al sincronizar lista (Pull): ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado al sincronizar lista (Pull): ${e.message}")
        }
    }

    /**
     * Intenta sincronizar una única entidad hacia el servicio remoto (Push).
     * Si la red falla o hay un error de servidor (500), se captura silenciosamente
     * dejando la entidad en estado "isPendingSync = true".
     */
    private suspend fun trySyncSingle(entity: ReminderEntity) {
        try {
            val response = if (entity.isDeleted) {
                apiService.deleteReminder(entity.id)
            } else {
                // Simplificación: Un POST o PUT unificado para updateReminder
                apiService.updateReminder(entity.id, entity.toDto())
            }

            // Consideramos 404 (Not Found) como éxito para borrados (ya no existe en backend)
            if (response.isSuccessful || (entity.isDeleted && response.code() == 404)) {
                if (entity.isDeleted) {
                    reminderDao.hardDeleteReminder(entity.id)
                } else {
                    // Marcamos como sincronizado localmente
                    reminderDao.updateReminder(entity.copy(isPendingSync = false))
                }
            } else {
                Log.w(TAG, "Error del servidor sincronizando ${entity.id}: HTTP ${response.code()}")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Fallo de red/timeout sincronizando ${entity.id}. Permanece pendiente.")
        } catch (e: Exception) {
            Log.e(TAG, "Error crítico sincronizando ${entity.id}: ${e.message}")
        }
    }
}
