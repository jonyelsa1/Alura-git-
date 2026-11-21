package dev.lanzador.tablet.ui

import androidx.compose.runtime.Stable

/** Lo que una pantalla de inicio puede abrir encima de sí misma. */
@Stable
class HomeNav(
    val openDrawer: (keyboard: Boolean, letter: String?) -> Unit = { _, _ -> },
    val openSettings: () -> Unit = {},
    val openStylePicker: () -> Unit = {},
    val addTask: () -> Unit = {},
)
