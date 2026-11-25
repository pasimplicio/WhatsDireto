package br.com.whatsdireto

import androidx.compose.ui.window.ComposeUIViewController
import br.com.whatsdireto.platform.IosPlatformBridge
import br.com.whatsdireto.ui.WhatsDiretoRoot
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    val bridge = IosPlatformBridge()
    WhatsDiretoRoot(platformBridge = bridge)
}
