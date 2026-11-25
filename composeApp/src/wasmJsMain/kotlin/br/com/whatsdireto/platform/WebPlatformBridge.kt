package br.com.whatsdireto.platform

import androidx.compose.runtime.Composable
import br.com.whatsdireto.data.KeyValueStorage
import br.com.whatsdireto.domain.LaunchResult
import br.com.whatsdireto.domain.PhoneNumberUtils
import br.com.whatsdireto.domain.PlatformCapabilities
import br.com.whatsdireto.domain.WhatsAppAppPreference
import kotlinx.browser.localStorage
import kotlinx.browser.window

@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

@JsFun(
    """(ts) => {
        const d = new Date(ts);
        const pad = (n) => String(n).padStart(2, '0');
        return pad(d.getDate()) + '/' + pad(d.getMonth() + 1) + '/' + d.getFullYear() + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
    }"""
)
private external fun jsFormatDate(ts: Double): String

@JsFun(
    """(text) => {
        try {
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(text);
                return true;
            }
        } catch (e) {}
        return false;
    }"""
)
private external fun jsCopyToClipboard(text: String): Boolean

@JsFun(
    """(text) => {
        try {
            if (navigator.share) {
                navigator.share({ title: 'WhatsDireto', text: text, url: text });
                return true;
            }
        } catch (e) {}
        return false;
    }"""
)
private external fun jsWebShare(text: String): Boolean

class WebKeyValueStorage : KeyValueStorage {
    private val prefix = "whatsdireto_v2_"

    override fun getString(key: String): String? =
        try { localStorage.getItem(prefix + key) } catch (_: Throwable) { null }

    override fun putString(key: String, value: String) {
        try { localStorage.setItem(prefix + key, value) } catch (_: Throwable) {}
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        when (getString(key)) {
            "1", "true" -> true
            "0", "false" -> false
            else -> defaultValue
        }

    override fun putBoolean(key: String, value: Boolean) {
        putString(key, if (value) "1" else "0")
    }

    override fun remove(key: String) {
        try { localStorage.removeItem(prefix + key) } catch (_: Throwable) {}
    }
}

class WebPlatformBridge : PlatformBridge {
    override val capabilities = PlatformCapabilities(
        platformName = "Web",
        supportsContactsPicker = false,
        supportsQrScanner = false,
        supportsWhatsAppVariantPicker = false,
        supportsNativeShare = true
    )

    override val storage: KeyValueStorage = WebKeyValueStorage()

    override fun openWhatsApp(
        normalizedPhone: String,
        message: String,
        preference: WhatsAppAppPreference
    ): LaunchResult {
        val url = PhoneNumberUtils.buildWhatsAppUrl(normalizedPhone, message)
        return try {
            window.open(url, "_blank", "noopener,noreferrer")
            LaunchResult.Success
        } catch (_: Throwable) {
            LaunchResult.Error("O navegador bloqueou a abertura da nova aba. Copie o link e abra manualmente.")
        }
    }

    override fun readClipboard(): String? {
        // Browsers require async gesture-gated clipboard reads; users can press Ctrl+V / Cmd+V directly
        return null
    }

    override fun copyToClipboard(text: String): Boolean = jsCopyToClipboard(text)

    override fun shareText(text: String): Boolean {
        return if (jsWebShare(text)) true else copyToClipboard(text)
    }

    override fun currentTimeMillis(): Long = jsDateNow().toLong()

    override fun formatTimestamp(timestamp: Long): String {
        val absolute = jsFormatDate(timestamp.toDouble())
        return formatRelativeDateFallback(timestamp, currentTimeMillis(), absolute)
    }
}

@Composable
actual fun PlatformContactsOverlay(
    visible: Boolean,
    onContactPicked: (phone: String, name: String) -> Unit,
    onDismiss: () -> Unit
) {
    // Web uses direct paste/input
}

@Composable
actual fun PlatformQrScannerOverlay(
    visible: Boolean,
    onCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Web uses QR link generation
}
