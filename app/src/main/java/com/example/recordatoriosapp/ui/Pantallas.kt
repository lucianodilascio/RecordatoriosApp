package com.example.recordatoriosapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.recordatoriosapp.data.Recordatorio
import com.example.recordatoriosapp.viewmodel.ListaUiState
import com.example.recordatoriosapp.viewmodel.RecordatorioViewModel

@Composable
fun RecordatoriosAppNavigation(viewModel: RecordatorioViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "listado") {
        composable("listado") {
            PantallaListado(
                viewModel = viewModel,
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
    onCrearRecordatorio: () -> Unit,
    onRecordatorioClick: (String) -> Unit
) {
    val uiState by viewModel.listaUiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mis Recordatorios") }) },
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
                        Text("No hay recordatorios aún.")
                        Button(onClick = onCrearRecordatorio, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Crear el primero")
                        }
                    }
                }
                is ListaUiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
                        Text(estado.mensaje, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.cargarRecordatorios() }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Reintentar")
                        }
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
                                onClick = { onRecordatorioClick(recordatorio.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun RecordatorioCard(recordatorio: Recordatorio, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = recordatorio.titulo, style = MaterialTheme.typography.titleMedium)
            Text(text = "Categoría: ${recordatorio.categoria.name} | Prioridad: ${recordatorio.prioridad::class.simpleName}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCreacion(viewModel: RecordatorioViewModel, onVolver: () -> Unit) {
    val estado by viewModel.formularioUiState.collectAsStateWithLifecycle()
    var expandido by remember { mutableStateOf(false) }

    // Vuelve a la pantalla principal tras un guardado exitoso
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
                    value = estado.categoria?.name ?: "Seleccionar Categoría",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandido,
                    onDismissRequest = { expandido = false }
                ) {
                    com.example.recordatoriosapp.data.Categoria.values().forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = {
                                viewModel.seleccionarCategoria(cat)
                                expandido = false
                            }
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val prioridades = listOf(
                    com.example.recordatoriosapp.data.Prioridad.Baja,
                    com.example.recordatoriosapp.data.Prioridad.Media,
                    com.example.recordatoriosapp.data.Prioridad.Alta
                )
                prioridades.forEach { prio ->
                    val nombre = prio::class.simpleName ?: ""
                    FilterChip(
                        selected = estado.prioridad == prio,
                        onClick = { viewModel.seleccionarPrioridad(prio) },
                        label = { Text(nombre) }
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
                    Text("Guardar Recordatorio")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalle(id: String, viewModel: RecordatorioViewModel, onVolver: () -> Unit) {
    val uiState by viewModel.listaUiState.collectAsStateWithLifecycle()

    // Buscamos el recordatorio específico dentro del estado de la lista
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
                    text = "No se encontró el recordatorio o los datos están cargando.",
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
                            Text(text = recordatorio.titulo, style = MaterialTheme.typography.headlineSmall)

                            Divider()

                            Text(text = "Descripción:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                            Text(text = recordatorio.descripcion.ifEmpty { "Sin descripción" }, style = MaterialTheme.typography.bodyLarge)

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Categoría: ${recordatorio.categoria.name}", style = MaterialTheme.typography.bodyMedium)
                                val prioridadesNombre = recordatorio.prioridad::class.simpleName ?: ""
                                Text(text = "Prioridad: $prioridadesNombre", style = MaterialTheme.typography.bodyMedium)
                            }
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