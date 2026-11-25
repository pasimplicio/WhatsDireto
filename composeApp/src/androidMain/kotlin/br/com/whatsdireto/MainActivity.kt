package br.com.whatsdireto

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import br.com.whatsdireto.platform.AndroidPlatformBridge
import br.com.whatsdireto.ui.WhatsDiretoRoot

class MainActivity : ComponentActivity() {
    private val incomingSharedText = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        incomingSharedText.value = extractIncomingText(intent)
        val bridge = AndroidPlatformBridge(applicationContext)

        setContent {
            WhatsDiretoRoot(
                platformBridge = bridge,
                incomingText = incomingSharedText.value,
                onIncomingTextConsumed = { incomingSharedText.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingSharedText.value = extractIncomingText(intent)
    }

    private fun extractIncomingText(intent: Intent?): String? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }
            Intent.ACTION_DIAL, Intent.ACTION_VIEW -> intent.dataString?.takeIf { it.isNotBlank() }
            else -> null
        }
    }
}
