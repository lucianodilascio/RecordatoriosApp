package com.example.recordatoriosapp.data.remote.api

import com.example.recordatoriosapp.data.remote.dto.FirestoreReminderDto
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

/**
 * Data Source Remoto apuntando a Cloud Firestore.
 * Opera estrictamente bajo la colección asilada users/{uid}/reminders/{reminderId}
 */
class FirestoreReminderDataSource(private val firestore: FirebaseFirestore) {

    suspend fun upsertReminder(uid: String, dto: FirestoreReminderDto): Result<Unit> = try {
        firestore.collection("users")
            .document(uid)
            .collection("reminders")
            .document(dto.id)
            .set(dto)
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteReminder(uid: String, reminderId: String): Result<Unit> = try {
        firestore.collection("users")
            .document(uid)
            .collection("reminders")
            .document(reminderId)
            .delete()
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun fetchReminders(uid: String): Result<List<FirestoreReminderDto>> = try {
        val snapshot = firestore.collection("users")
            .document(uid)
            .collection("reminders")
            .get()
            .await()
        
        val list = snapshot.documents.mapNotNull { it.toObject(FirestoreReminderDto::class.java) }
        Result.success(list)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Prueba técnica para demostrar que las Reglas de Seguridad bloquean el acceso
     * a colecciones de otro usuario. Se captura FirebaseFirestoreException.
     */
    suspend fun testUnauthorizedAccess(targetUid: String): Result<Unit> = try {
        firestore.collection("users")
            .document(targetUid)
            .collection("reminders")
            .get()
            .await()
        // Si llega acá, las reglas están mal configuradas
        Result.success(Unit)
    } catch (e: FirebaseFirestoreException) {
        // PERMISSION_DENIED esperado
        Result.failure(Exception("Acceso denegado correctamente por Reglas de Firestore: ${e.message}"))
    } catch (e: Exception) {
        Result.failure(e)
    }
}
