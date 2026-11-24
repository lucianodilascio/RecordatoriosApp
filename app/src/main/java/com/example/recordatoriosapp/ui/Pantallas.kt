package com.example.recordatoriosapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.recordatoriosapp.BuildConfig
import com.example.recordatoriosapp.data.auth.AuthState
import com.example.recordatoriosapp.data.model.Reminder
import com.example.recordatoriosapp.viewmodel.AuthViewModel
import com.example.recordatoriosapp.viewmodel.ListaUiState
import com.example.recordatoriosapp.viewmodel.RecordatorioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordatoriosAppNavigation(viewModel: RecordatorioViewModel, authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "listado") {
        composable("listado") {
            PantallaListado(
                viewModel = viewModel,
                authViewModel = authViewModel,
                onCrearRecordatorio = { navController.navigate("creacion") },
                onRecordatorioClick = { id -> navController.navigate("detalle/$id") }
            )
        }
        composable("creacion") {
            PantallaCreacion(
                viewModel = viewModel,
                onVolver = { navController.popBackStack() }
            )
        }
        composable(
            route = "detalle/{id}",
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            PantallaDetalle(id = id, viewModel = viewModel, onVolver = { navController.popBackStack() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListado(
    viewModel: RecordatorioViewModel,
    authViewModel: AuthViewModel,
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (String) -> Unit
) {
    val uiState by viewModel.listaUiState.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val userPrefs by viewModel.userPreferences.collectAsStateWithLifecycle()
    
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val user = (authState as? AuthState.Authenticated)?.user
    val testResult by authViewModel.testSecurityResult.collectAsStateWithLifecycle()
    
    var showPreferencesDialog by remember { mutableStateOf(false) }

    if (showPreferencesDialog) {
        AlertDialog(
            onDismissRequest = { showPreferencesDialog = false },
            title = { Text("Configuración") },
            text = {
                Column {
                    if (user != null) {
                        Text("Sesión: ${user.email ?: user.uid}", style = MaterialTheme.typography.bodySmall)
                        Text("App: v${BuildConfig.VERSION_NAME} - ${BuildConfig.ENVIRONMENT}", style = MaterialTheme.typography.labelSmall)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = userPrefs.isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode(it) }
                        )
                        Text("Tema Oscuro")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = userPrefs.showCompleted,
                            onCheckedChange = { viewModel.toggleShowCompleted(it) }
                        )
                        Text("Mostrar Completados")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { viewModel.toggleSortOrder() }) {
                            Text(if (userPrefs.sortOrder == "DATE_DESC") "Orden: Más recientes" else "Orden: Mayor Prioridad")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    if (BuildConfig.ENABLE_DEBUG_LOGS) {
                        Button(onClick = { authViewModel.testSecurityIsolation("uid_ajeno_prueba") }) {
                            Text("Probar Reglas Firestore")
                        }
                        if (testResult != null) {
                            Text(text = testResult!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Button(
                        onClick = { 
                            authViewModel.signOut()
                            showPreferencesDialog = false 
                        }, 
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Cerrar Sesión")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPreferencesDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Recordatorios") },
                actions = {
                    IconButton(onClick = { viewModel.sincronizarConServidor() }) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                        } else {
                            Text("🔄")
                        }
                    }
                    IconButton(onClick = { showPreferencesDialog = true }) {
                        Text("⚙️")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCrearRecordatorio) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val estado = uiState) {
                is ListaUiState.Cargando -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ListaUiState.Vacio -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
                        Text("No hay recordatorios aquí.")
                        Button(onClick = onCrearRecordatorio, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Crear uno nuevo")
                        }
                    }
                }
                is ListaUiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
                        Text(estado.mensaje, color = MaterialTheme.colorScheme.error)
                    }
                }
                is ListaUiState.ConDatos -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(estado.recordatorios) { recordatorio ->
                            RecordatorioCard(
                                recordatorio = recordatorio,
                                onClick = { onRecordatorioClick(recordatorio.id) },
                                onToggleCompleted = { viewModel.alternarCompletado(recordatorio) }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun RecordatorioCard(
    recordatorio: Reminder, 
    onClick: () -> Unit, 
    onToggleCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (recordatorio.isCompleted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = recordatorio.isCompleted,
                onCheckedChange = { onToggleCompleted() }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recordatorio.title, 
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (recordatorio.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(text = "Cat: ${recordatorio.category} | Prio: ${recordatorio.priority}", style = MaterialTheme.typography.bodyMedium)
                val fechaFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(recordatorio.date))
                Text(text = fechaFormat, style = MaterialTheme.typography.labelSmall)
            }
            if (recordatorio.isPendingSync) {
                Text(
                    text = "⚠️",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCreacion(viewModel: RecordatorioViewModel, onVolver: () -> Unit) {
    val estado by viewModel.formularioUiState.collectAsStateWithLifecycle()
    var expandido by remember { mutableStateOf(false) }

    LaunchedEffect(estado.guardadoExitoso) {
        if (estado.guardadoExitoso) {
            viewModel.reiniciarFormulario()
            onVolver()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Nuevo Recordatorio") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = estado.titulo,
                onValueChange = { viewModel.actualizarTitulo(it) },
                label = { Text("Título (Mín. 4 caracteres)") },
                isError = estado.titulo.isNotEmpty() && !estado.tituloValido,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = estado.descripcion,
                onValueChange = { viewModel.actualizarDescripcion(it) },
                label = { Text("Descripción (Máx. 200 caracteres)") },
                isError = !estado.descripcionValida,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = expandido,
                onExpandedChange = { expandido = !expandido }
            ) {
                OutlinedTextField(
                    value = estado.categoria ?: "Seleccionar Categoría",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandido,
                    onDismissRequest = { expandido = false }
                ) {
                    listOf("Trabajo", "Personal", "Estudios", "Otros").forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                viewModel.seleccionarCategoria(cat)
                                expandido = false
                            }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val prioridades = listOf("LOW", "MEDIUM", "HIGH")
                prioridades.forEach { prio ->
                    FilterChip(
                        selected = estado.prioridad == prio,
                        onClick = { viewModel.seleccionarPrioridad(prio) },
                        label = { Text(prio) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.guardarRecordatorio() },
                enabled = estado.esValido && !estado.enviando,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (estado.enviando) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Guardar Localmente")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalle(id: String, viewModel: RecordatorioViewModel, onVolver: () -> Unit) {
    val uiState by viewModel.listaUiState.collectAsStateWithLifecycle()

    val recordatorio = remember(uiState, id) {
        when (uiState) {
            is ListaUiState.ConDatos -> (uiState as ListaUiState.ConDatos).recordatorios.find { it.id == id }
            else -> null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Recordatorio") },
                navigationIcon = {
                    TextButton(onClick = onVolver) {
                        Text("← Volver", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).padding(16.dp).fillMaxSize()) {
            if (recordatorio == null) {
                Text(
                    text = "No se encontró el recordatorio.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(text = recordatorio.title, style = MaterialTheme.typography.headlineSmall)

                            if (recordatorio.isPendingSync) {
                                Text(
                                    text = "⚠️ Pendiente de sincronización",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Divider()

                            Text(text = "Descripción:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Text(text = recordatorio.description.ifEmpty { "Sin descripción" }, style = MaterialTheme.typography.bodyLarge)

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Cat: ${recordatorio.category}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Prio: ${recordatorio.priority}", style = MaterialTheme.typography.bodyMedium)
                            }
                            
                            val fechaFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(recordatorio.date))
                            Text(text = "Creado: $fechaFormat", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            viewModel.eliminarRecordatorio(recordatorio.id)
                            onVolver()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Eliminar Recordatorio", color = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        }
    }
}
