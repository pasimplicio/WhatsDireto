package br.com.whatsdireto.platform

import androidx.compose.runtime.Composable
import br.com.whatsdireto.data.KeyValueStorage
import br.com.whatsdireto.domain.LaunchResult
import br.com.whatsdireto.domain.PlatformCapabilities
import br.com.whatsdireto.domain.WhatsAppAppPreference

interface PlatformBridge {
    val capabilities: PlatformCapabilities
    val storage: KeyValueStorage

    fun openWhatsApp(
        normalizedPhone: String,
        message: String,
        preference: WhatsAppAppPreference
    ): LaunchResult

    fun readClipboard(): String?
    fun copyToClipboard(text: String): Boolean
    fun shareText(text: String): Boolean
    fun currentTimeMillis(): Long
    fun formatTimestamp(timestamp: Long): String
}

internal fun formatRelativeDateFallback(timestamp: Long, nowMillis: Long, absoluteFormatted: String): String {
    val diffSeconds = ((nowMillis - timestamp) / 1000L).coerceAtLeast(0L)
    return when {
        diffSeconds < 60L -> "Agora"
        diffSeconds < 3600L -> "Há ${diffSeconds / 60L} min"
        diffSeconds < 86400L -> {
            val hours = diffSeconds / 3600L
            if (hours == 1L) "Há 1 hora" else "Há $hours horas"
        }
        else -> absoluteFormatted
    }
}

@Composable
expect fun PlatformContactsOverlay(
    visible: Boolean,
    onContactPicked: (phone: String, name: String) -> Unit,
    onDismiss: () -> Unit
)

@Composable
expect fun PlatformQrScannerOverlay(
    visible: Boolean,
    onCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
)
