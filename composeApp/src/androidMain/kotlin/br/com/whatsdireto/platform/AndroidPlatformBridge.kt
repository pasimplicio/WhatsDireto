package br.com.whatsdireto.platform

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import br.com.whatsdireto.android.ContactPickerDialog
import br.com.whatsdireto.android.QRCodeScannerOverlay
import br.com.whatsdireto.data.KeyValueStorage
import br.com.whatsdireto.domain.LaunchResult
import br.com.whatsdireto.domain.PhoneNumberUtils
import br.com.whatsdireto.domain.PlatformCapabilities
import br.com.whatsdireto.domain.WhatsAppAppPreference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AndroidKeyValueStorage(context: Context) : KeyValueStorage {
    private val prefs = context.applicationContext
        .getSharedPreferences("whatsdireto_prefs_v2", Context.MODE_PRIVATE)

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        prefs.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}

class AndroidPlatformBridge(context: Context) : PlatformBridge {
    private val appContext = context.applicationContext

    override val capabilities = PlatformCapabilities(
        platformName = "Android",
        supportsContactsPicker = true,
        supportsQrScanner = true,
        supportsWhatsAppVariantPicker = true,
        supportsNativeShare = true
    )

    override val storage: KeyValueStorage = AndroidKeyValueStorage(appContext)

    override fun openWhatsApp(
        normalizedPhone: String,
        message: String,
        preference: WhatsAppAppPreference
    ): LaunchResult {
        val url = PhoneNumberUtils.buildWhatsAppUrl(normalizedPhone, message)
        val uri = Uri.parse(url)

        val packagesToTry: List<String?> = when (preference) {
            WhatsAppAppPreference.STANDARD -> listOf("com.whatsapp", null)
            WhatsAppAppPreference.BUSINESS -> listOf("com.whatsapp.w4b", null)
            WhatsAppAppPreference.AUTO -> listOf("com.whatsapp", "com.whatsapp.w4b", null)
        }

        for (pkg in packagesToTry) {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                if (pkg != null) setPackage(pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                appContext.startActivity(intent)
                return LaunchResult.Success
            } catch (_: ActivityNotFoundException) {
                // Try next target
            } catch (_: SecurityException) {
                // Try next target
            }
        }

        return LaunchResult.Error(
            "Não foi encontrado o WhatsApp nem um navegador para abrir a conversa."
        )
    }

    override fun readClipboard(): String? {
        val manager = appContext.getSystemService(ClipboardManager::class.java) ?: return null
        if (!manager.hasPrimaryClip()) return null
        val clip = manager.primaryClip ?: return null
        if (clip.itemCount == 0) return null

        val desc = manager.primaryClipDescription
        val isText = desc == null ||
            desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
            desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
        if (!isText) return null

        return clip.getItemAt(0).coerceToText(appContext)?.toString()?.takeIf { it.isNotBlank() }
    }

    override fun copyToClipboard(text: String): Boolean {
        return try {
            val manager = appContext.getSystemService(ClipboardManager::class.java) ?: return false
            manager.setPrimaryClip(ClipData.newPlainText("WhatsDireto", text))
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun shareText(text: String): Boolean {
        return try {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(sendIntent, "Compartilhar link do WhatsApp").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(chooser)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun currentTimeMillis(): Long = System.currentTimeMillis()

    override fun formatTimestamp(timestamp: Long): String {
        val absolute = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
        return formatRelativeDateFallback(timestamp, currentTimeMillis(), absolute)
    }
}

@Composable
actual fun PlatformContactsOverlay(
    visible: Boolean,
    onContactPicked: (phone: String, name: String) -> Unit,
    onDismiss: () -> Unit
) {
    if (visible) {
        ContactPickerDialog(
            onContactSelected = onContactPicked,
            onDismiss = onDismiss
        )
    }
}

@Composable
actual fun PlatformQrScannerOverlay(
    visible: Boolean,
    onCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (visible) {
        QRCodeScannerOverlay(
            onQRCodeScanned = onCodeScanned,
            onDismiss = onDismiss
        )
    }
}
