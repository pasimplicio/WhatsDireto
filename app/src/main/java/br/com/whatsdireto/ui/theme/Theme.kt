package br.com.whatsdireto.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val WhatsAppGreen = Color(0xFF25D366)
val WhatsAppDarkGreen = Color(0xFF128C7E)
val WhatsAppHeaderGreen = Color(0xFF075E54)
val WhatsAppBg = Color(0xFFECE5DD)
val ChatBubble = Color(0xFFFFFFFF)
val DoodleLine = Color(0x1A54656F)
val TextPrimary = Color(0xFF111B21)
val TextSecondary = Color(0xFF667781)

private val AppColors = lightColorScheme(
    primary = WhatsAppGreen,
    secondary = WhatsAppDarkGreen,
    background = WhatsAppBg,
    surface = ChatBubble,
    onPrimary = ChatBubble,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

@Composable
fun WhatsDiretoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        typography = AppTypography,
        content = content
    )
}