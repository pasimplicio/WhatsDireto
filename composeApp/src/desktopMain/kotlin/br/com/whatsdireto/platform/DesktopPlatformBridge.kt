package br.com.whatsdireto.platform

import androidx.compose.runtime.Composable
import br.com.whatsdireto.data.KeyValueStorage
import br.com.whatsdireto.domain.LaunchResult
import br.com.whatsdireto.domain.PhoneNumberUtils
import br.com.whatsdireto.domain.PlatformCapabilities
import br.com.whatsdireto.domain.WhatsAppAppPreference
import br.com.whatsdireto.domain.WhatsAppLaunchTarget
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.prefs.Preferences

class DesktopKeyValueStorage : KeyValueStorage {
    private val prefs: Preferences = Preferences.userRoot().node("br/com/whatsdireto/v2")

    override fun getString(key: String): String? = prefs.get(key, null)

    override fun putString(key: String, value: String) {
        prefs.put(key, value)
        prefs.flush()
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        prefs.getBoolean(key, defaultValue)

    override fun putBoolean(key: String, value: Boolean) {
        prefs.putBoolean(key, value)
        prefs.flush()
    }

    override fun remove(key: String) {
        prefs.remove(key)
        prefs.flush()
    }
}

class DesktopPlatformBridge : PlatformBridge {
    override val capabilities = PlatformCapabilities(
        platformName = "Desktop",
        supportsContactsPicker = false,
        supportsQrScanner = false,
        supportsWhatsAppVariantPicker = false,
        supportsNativeShare = false,
        supportsWebAndDesktopButtons = true
    )

    override val storage: KeyValueStorage = DesktopKeyValueStorage()

    override fun openWhatsApp(
        normalizedPhone: String,
        message: String,
        preference: WhatsAppAppPreference,
        target: WhatsAppLaunchTarget
    ): LaunchResult {
        val url = when (target) {
            WhatsAppLaunchTarget.WEB -> PhoneNumberUtils.buildWhatsAppWebUrl(normalizedPhone, message)
            WhatsAppLaunchTarget.DESKTOP -> PhoneNumberUtils.buildWhatsAppDesktopUrl(normalizedPhone, message)
            WhatsAppLaunchTarget.DEFAULT -> PhoneNumberUtils.buildWhatsAppUrl(normalizedPhone, message)
        }
        return try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
                LaunchResult.Success
            } else {
                copyToClipboard(url)
                LaunchResult.Error("Navegador indisponível. O link foi copiado para a área de transferência.")
            }
        } catch (e: Exception) {
            LaunchResult.Error("Não foi possível abrir o navegador: ${e.message ?: "erro desconhecido"}")
        }
    }

    override fun readClipboard(): String? {
        return try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                (clipboard.getData(DataFlavor.stringFlavor) as? String)?.takeIf { it.isNotBlank() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun copyToClipboard(text: String): Boolean {
        return try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(text), null)
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun shareText(text: String): Boolean = copyToClipboard(text)

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
    // Desktop does not use a mobile contacts picker
}

@Composable
actual fun PlatformQrScannerOverlay(
    visible: Boolean,
    onCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Desktop does not use a mobile camera QR scanner
}
