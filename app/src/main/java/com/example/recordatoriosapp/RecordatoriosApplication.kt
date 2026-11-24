package com.example.recordatoriosapp

import android.app.Application
import com.example.recordatoriosapp.data.auth.AuthRepository
import com.example.recordatoriosapp.data.local.database.ReminderDatabase
import com.example.recordatoriosapp.data.local.datastore.PreferencesManager
import com.example.recordatoriosapp.data.remote.api.FirestoreReminderDataSource
import com.example.recordatoriosapp.data.repository.ReminderRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Contenedor de inyección manual de dependencias.
 */
class RecordatoriosApplication : Application() {

    private val database by lazy { ReminderDatabase.getDatabase(this) }
    
    val preferencesManager by lazy { PreferencesManager(this) }
    
    val authRepository by lazy { 
        AuthRepository(FirebaseAuth.getInstance()) 
    }
    
    private val firestoreDataSource by lazy { 
        FirestoreReminderDataSource(FirebaseFirestore.getInstance()) 
    }
    
    val repository by lazy {
        ReminderRepository(
            reminderDao = database.reminderDao(),
            firestoreDataSource = firestoreDataSource,
            preferencesManager = preferencesManager,
            authRepository = authRepository
        )
    }
}
