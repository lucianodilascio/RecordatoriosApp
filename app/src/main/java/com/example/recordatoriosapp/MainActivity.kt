package com.example.recordatoriosapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.recordatoriosapp.ui.theme.RecordatoriosAppTheme
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecordatoriosAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ListaRecordatorios(recordatoriosSimulados)
                }
            }
        }
    }
}

@Composable
fun ListaRecordatorios(recordatorios: List<Recordatorio>) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Mis Recordatorios",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        items(recordatorios) { recordatorio ->
            RecordatorioCard(recordatorio)
        }
    }
}

@Composable
fun RecordatorioCard(recordatorio: Recordatorio) {
    val colorEtiqueta = coloresCategoria[recordatorio.categoria] ?: Color.Gray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp) // Un poco más de separación entre tarjetas
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) // Volvemos al tono de contraste
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = recordatorio.titulo,
                fontWeight = FontWeight.ExtraBold, // Título más pesado
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp)) // Más aire debajo del título

            Text(
                text = "Categoría: ${recordatorio.categoria}",
                color = colorEtiqueta,
                fontWeight = FontWeight.Bold // Resaltamos el color de la categoría
            )
            Spacer(modifier = Modifier.height(4.dp))

            Text(text = "Prioridad: ${recordatorio.prioridad.javaClass.simpleName}")
            Text(text = "Estado: ${recordatorio.estado.javaClass.simpleName}")

            recordatorio.avisoOpcional?.let { aviso ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aviso: $aviso",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}