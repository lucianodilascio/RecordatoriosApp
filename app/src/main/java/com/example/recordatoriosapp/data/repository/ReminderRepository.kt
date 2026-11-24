package com.example.recordatoriosapp.data.repository

import android.util.Log
import com.example.recordatoriosapp.data.auth.AuthRepository
import com.example.recordatoriosapp.data.auth.AuthState
import com.example.recordatoriosapp.data.local.dao.ReminderDao
import com.example.recordatoriosapp.data.local.datastore.PreferencesManager
import com.example.recordatoriosapp.data.local.entity.ReminderEntity
import com.example.recordatoriosapp.data.model.Reminder
import com.example.recordatoriosapp.data.model.UserPreferences
import com.example.recordatoriosapp.data.remote.api.FirestoreReminderDataSource
import com.example.recordatoriosapp.data.repository.mapper.toDomain
import com.example.recordatoriosapp.data.repository.mapper.toEntity
import com.example.recordatoriosapp.data.repository.mapper.toFirestoreDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Repositorio Local-First orquestador.
 * Inyecta AuthRepository para aislar el UID de la Vista, garantizando que el ViewModel
 * no se entere de los detalles de red/autenticación y la firma de los métodos quede intacta.
 */
class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val firestoreDataSource: FirestoreReminderDataSource,
    private val preferencesManager: PreferencesManager,
    private val authRepository: AuthRepository
) {
    private val TAG = "ReminderRepository"

    val userPreferencesFlow: Flow<UserPreferences> = preferencesManager.userPreferencesFlow
    suspend fun updateDarkMode(isDarkMode: Boolean) = preferencesManager.updateDarkMode(isDarkMode)
    suspend fun updateShowCompleted(showCompleted: Boolean) = preferencesManager.updateShowCompleted(showCompleted)
    suspend fun updateSortOrder(sortOrder: String) = preferencesManager.updateSortOrder(sortOrder)

    // --- Lecturas Locales Reactivas (Fuente de Verdad = Room + AuthState) ---

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getAllReminders(): Flow<List<Reminder>> {
        return authRepository.authState.flatMapLatest { authState ->
            if (authState is AuthState.Authenticated) {
                reminderDao.getAllReminders(authState.user.uid).map { entities -> 
                    entities.map { it.toDomain() } 
                }
            } else {
                flowOf(emptyList()) // Si no hay sesión, emitimos lista vacía sin crashear
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getRemindersByStatus(isCompleted: Boolean): Flow<List<Reminder>> {
        return authRepository.authState.flatMapLatest { authState ->
            if (authState is AuthState.Authenticated) {
                reminderDao.getRemindersByStatus(authState.user.uid, isCompleted).map { entities -> 
                    entities.map { it.toDomain() } 
                }
            } else {
                flowOf(emptyList())
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getReminderById(id: String): Flow<Reminder?> {
        return authRepository.authState.flatMapLatest { authState ->
            if (authState is AuthState.Authenticated) {
                reminderDao.getReminderById(authState.user.uid, id).map { it?.toDomain() }
            } else {
                flowOf(null)
            }
        }
    }

    // --- Operaciones CRUD Offline-First ---

    suspend fun createReminder(reminder: Reminder) {
        val uid = authRepository.getCurrentUser()?.uid ?: return
        
        val entity = reminder.toEntity(userId = uid, isPendingSync = true)
        reminderDao.insertReminder(entity)
        trySyncSingle(uid, entity)
    }

    suspend fun updateReminder(reminder: Reminder) {
        val uid = authRepository.getCurrentUser()?.uid ?: return

        val entity = reminder.toEntity(userId = uid, isPendingSync = true)
        reminderDao.updateReminder(entity)
        trySyncSingle(uid, entity)
    }

    suspend fun deleteReminder(id: String) {
        val uid = authRepository.getCurrentUser()?.uid ?: return

        reminderDao.softDeleteReminder(id, uid)
        val entityToDelete = reminderDao.getPendingSyncReminders(uid).find { it.id == id }
        entityToDelete?.let { trySyncSingle(uid, it) }
    }

    // --- Sincronización Remota (Push-before-Pull) ---

    suspend fun syncWithRemote() {
        val uid = authRepository.getCurrentUser()?.uid ?: return

        // 1. Subir pendientes
        val pendingEntities = reminderDao.getPendingSyncReminders(uid)
        for (entity in pendingEntities) {
            trySyncSingle(uid, entity)
        }

        // 2. Descargar servidor
        val result = firestoreDataSource.fetchReminders(uid)
        result.onSuccess { dtos ->
            // Protegemos contra pisar registros locales pendientes
            val currentPendingIds = reminderDao.getPendingSyncReminders(uid).map { it.id }.toSet()
            
            val newEntities = dtos.mapNotNull { dto ->
                if (currentPendingIds.contains(dto.id)) null else dto.toEntity(uid, isPendingSync = false)
            }
            reminderDao.insertReminders(newEntities)
        }.onFailure { e ->
            Log.e(TAG, "Error al sincronizar lista (Pull) en Firestore: ${e.message}")
        }
    }

    private suspend fun trySyncSingle(uid: String, entity: ReminderEntity) {
        if (entity.isDeleted) {
            val result = firestoreDataSource.deleteReminder(uid, entity.id)
            result.onSuccess {
                reminderDao.hardDeleteReminder(entity.id, uid)
            }.onFailure {
                Log.w(TAG, "Delete pendiente de red para ${entity.id}")
            }
        } else {
            val result = firestoreDataSource.upsertReminder(uid, entity.toFirestoreDto())
            result.onSuccess {
                reminderDao.updateReminder(entity.copy(isPendingSync = false))
            }.onFailure {
                Log.w(TAG, "Upsert pendiente de red para ${entity.id}")
            }
        }
    }

    // --- Limpieza Local y Testing ---
    
    suspend fun clearLocalData() {
        reminderDao.clearAllReminders()
    }

    suspend fun testUnauthorizedAccess(targetUid: String): Result<Unit> {
        return firestoreDataSource.testUnauthorizedAccess(targetUid)
    }
}
