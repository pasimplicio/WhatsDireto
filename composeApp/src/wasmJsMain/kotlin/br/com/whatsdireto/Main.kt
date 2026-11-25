package br.com.whatsdireto

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import br.com.whatsdireto.platform.WebPlatformBridge
import br.com.whatsdireto.ui.WhatsDiretoRoot
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val bridge = WebPlatformBridge()
    ComposeViewport(document.body!!) {
        WhatsDiretoRoot(platformBridge = bridge)
    }
}
