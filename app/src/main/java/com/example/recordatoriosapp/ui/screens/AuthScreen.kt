package com.example.recordatoriosapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.recordatoriosapp.data.auth.AuthState
import com.example.recordatoriosapp.viewmodel.AuthViewModel

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoginMode by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Limpiar error al tipear
    LaunchedEffect(email, password) {
        errorMessage = null
    }

    // Volver isLoading a false cuando cambie el estado
    LaunchedEffect(authState) {
        isLoading = false
    }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isLoginMode) "Iniciar Sesión" else "Registrarse",
                style = MaterialTheme.typography.headlineMedium
            )
            
            if (authState is AuthState.SessionInvalid) {
                Text(
                    text = (authState as AuthState.SessionInvalid).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña (mín 6 caracteres)") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            }

            val isValid = email.contains("@") && password.length >= 6

            Button(
                onClick = {
                    isLoading = true
                    if (isLoginMode) {
                        viewModel.signIn(email, password) { err ->
                            isLoading = false
                            errorMessage = err
                        }
                    } else {
                        viewModel.signUp(email, password) { err ->
                            isLoading = false
                            errorMessage = err
                        }
                    }
                },
                enabled = isValid && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(if (isLoginMode) "Entrar" else "Crear Cuenta")
                }
            }

            TextButton(
                onClick = { 
                    isLoginMode = !isLoginMode
                    errorMessage = null 
                }
            ) {
                Text(if (isLoginMode) "¿No tienes cuenta? Regístrate" else "¿Ya tienes cuenta? Inicia Sesión")
            }
        }
    }
}
