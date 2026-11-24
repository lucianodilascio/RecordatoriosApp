package com.example.recordatoriosapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recordatoriosapp.RecordatoriosApplication
import com.example.recordatoriosapp.data.auth.AuthState
import com.example.recordatoriosapp.ui.screens.AuthScreen
import com.example.recordatoriosapp.ui.theme.RecordatoriosAppTheme
import com.example.recordatoriosapp.viewmodel.AuthViewModel
import com.example.recordatoriosapp.viewmodel.RecordatorioViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Obtenemos el repositorio desde nuestra clase Application
        val app = application as RecordatoriosApplication
        val repository = app.repository
        val authRepository = app.authRepository

        setContent {
            val authViewModel: AuthViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return AuthViewModel(authRepository, repository) as T
                    }
                }
            )

            // Instanciamos el ViewModel con el Factory inyectando el repositorio Local-First
            val viewModel: RecordatorioViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return RecordatorioViewModel(repository, authRepository) as T
                    }
                }
            )

            val authState by authViewModel.authState.collectAsState()
            val userPrefs by viewModel.userPreferences.collectAsState()
            val useDarkTheme = userPrefs.isDarkMode || isSystemInDarkTheme()

            RecordatoriosAppTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (authState) {
                        is AuthState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        is AuthState.Unauthenticated, is AuthState.SessionInvalid -> {
                            AuthScreen(viewModel = authViewModel)
                        }
                        is AuthState.Authenticated -> {
                            RecordatoriosAppNavigation(viewModel = viewModel, authViewModel = authViewModel)
                        }
                        is AuthState.Error -> {
                            AuthScreen(viewModel = authViewModel)
                        }
                    }
                }
            }
        }
    }
}
