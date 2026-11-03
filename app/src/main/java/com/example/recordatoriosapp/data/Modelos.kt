package com.example.recordatoriosapp.data

import androidx.compose.ui.graphics.Color

// Enum para validar estrictamente las categorías en el formulario
enum class Categoria {
    Trabajo, Estudio, Personal
}

sealed class Prioridad {
    object Alta : Prioridad()
    object Media : Prioridad()
    object Baja : Prioridad()
}

sealed class Estado {
    object Pendiente : Estado()
    object Completado : Estado()
}

// Data class actualizada con id (String) y descripcion obligatoria
data class Recordatorio(
    val id: String = java.util.UUID.randomUUID().toString(),
    val titulo: String,
    val descripcion: String,
    val categoria: Categoria,
    val prioridad: Prioridad,
    val estado: Estado = Estado.Pendiente
)

// Map para asociar categorías con colores
val coloresCategoria: Map<Categoria, Color> = mapOf(
    Categoria.Trabajo to Color(0xFF1976D2),
    Categoria.Estudio to Color(0xFF388E3C),
    Categoria.Personal to Color(0xFFF57C00)
)

// Lista de datos simulados actualizada
val recordatoriosSimulados: List<Recordatorio> = listOf(
    Recordatorio("1", "Entregar API 1", "Armar y subir el PDF a la plataforma", Categoria.Estudio, Prioridad.Alta, Estado.Pendiente),
    Recordatorio("2", "Publicar propiedades en redes", "Armar posteos en Canva para la inmobiliaria", Categoria.Trabajo, Prioridad.Media, Estado.Completado),
    Recordatorio("3", "Jugar unas partidas de CS2", "Buscar partidas en MDQServers con el equipo", Categoria.Personal, Prioridad.Baja, Estado.Pendiente)
)