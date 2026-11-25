package br.com.whatsdireto

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import br.com.whatsdireto.platform.DesktopPlatformBridge
import br.com.whatsdireto.ui.WhatsDiretoRoot
import java.awt.Dimension

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(1020.dp, 740.dp),
        position = WindowPosition(Alignment.Center)
    )
    val bridge = DesktopPlatformBridge()

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "WhatsDireto"
    ) {
        window.minimumSize = Dimension(420, 620)
        WhatsDiretoRoot(platformBridge = bridge)
    }
}
