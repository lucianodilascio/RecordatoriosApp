package com.example.recordatoriosapp

import android.app.Application
import com.example.recordatoriosapp.data.local.database.ReminderDatabase
import com.example.recordatoriosapp.data.local.datastore.PreferencesManager
import com.example.recordatoriosapp.data.remote.api.RetrofitClient
import com.example.recordatoriosapp.data.repository.ReminderRepository

/**
 * Clase Application para inicializar y proveer las dependencias de la capa de datos
 * como Singletons a lo largo de toda la app.
 */
class RecordatoriosApplication : Application() {

    // Inicializamos con by lazy para que se creen solo cuando se necesiten
    private val database by lazy { ReminderDatabase.getDatabase(this) }
    
    val preferencesManager by lazy { PreferencesManager(this) }
    
    val repository by lazy {
        ReminderRepository(
            reminderDao = database.reminderDao(),
            apiService = RetrofitClient.apiService,
            preferencesManager = preferencesManager
        )
    }
}
