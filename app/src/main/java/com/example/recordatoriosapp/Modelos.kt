package com.example.recordatoriosapp

import androidx.compose.ui.graphics.Color

// Sealed classes para controlar los estados y prioridades admitidas
sealed class Prioridad {
    object Alta : Prioridad()
    object Media : Prioridad()
    object Baja : Prioridad()
}

sealed class Estado {
    object Pendiente : Estado()
    object Completado : Estado()
}

// Data class inmutable con atributos obligatorios y opcionales
data class Recordatorio(
    val id: Int,
    val titulo: String,
    val categoria: String,
    val prioridad: Prioridad,
    val estado: Estado,
    val avisoOpcional: String? = null
)

// Map para asociar categorías con colores de etiqueta específicos
val coloresCategoria: Map<String, Color> = mapOf(
    "Trabajo" to Color(0xFF1976D2),
    "Estudio" to Color(0xFF388E3C),
    "Personal" to Color(0xFFF57C00)
)

// List para almacenar los datos simulados que se mostraran en  pantalla
val recordatoriosSimulados: List<Recordatorio> = listOf(
    Recordatorio(1, "Entregar API 1", "Estudio", Prioridad.Alta, Estado.Pendiente, "Armar y subir el PDF a la plataforma"),
    Recordatorio(2, "Publicar propiedades en redes", "Trabajo", Prioridad.Media, Estado.Completado),
    Recordatorio(3, "Jugar unas partidas de CS2", "Personal", Prioridad.Baja, Estado.Pendiente)
)

// Set para aislar categorías únicas sin duplicados
val categoriasUnicas: Set<String> = recordatoriosSimulados.map { it.categoria }.toSet()