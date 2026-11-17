package com.example.recordatoriosapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatoriosapp.RecordatoriosApplication
import com.example.recordatoriosapp.ui.theme.RecordatoriosAppTheme
import com.example.recordatoriosapp.viewmodel.RecordatorioViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtenemos el repositorio desde nuestra clase Application
        val app = application as RecordatoriosApplication
        val repository = app.repository

        setContent {
            // Instanciamos el ViewModel con el Factory inyectando el repositorio Local-First
            val viewModel: RecordatorioViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return RecordatorioViewModel(repository) as T
                    }
                }
            )

            // Obtenemos las preferencias de forma reactiva para aplicar el tema
            val userPrefs by viewModel.userPreferences.collectAsState()
            val useDarkTheme = userPrefs.isDarkMode || isSystemInDarkTheme()

            RecordatoriosAppTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RecordatoriosAppNavigation(viewModel = viewModel)
                }
            }
        }
    }
}
