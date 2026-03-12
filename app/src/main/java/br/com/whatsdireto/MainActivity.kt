package br.com.whatsdireto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import br.com.whatsdireto.ui.WhatsAppScreen
import br.com.whatsdireto.ui.theme.WhatsDiretoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WhatsDiretoTheme {
                WhatsAppScreen()
            }
        }
    }
}