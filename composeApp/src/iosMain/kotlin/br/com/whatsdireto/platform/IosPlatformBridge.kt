package br.com.whatsdireto.platform

import androidx.compose.runtime.Composable
import br.com.whatsdireto.data.KeyValueStorage
import br.com.whatsdireto.domain.LaunchResult
import br.com.whatsdireto.domain.PhoneNumberUtils
import br.com.whatsdireto.domain.PlatformCapabilities
import br.com.whatsdireto.domain.WhatsAppAppPreference
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSURL
import platform.Foundation.NSUserDefaults
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard

class IosKeyValueStorage : KeyValueStorage {
    private val defaults = NSUserDefaults.standardUserDefaults
    private val prefix = "whatsdireto_v2_"

    override fun getString(key: String): String? = defaults.stringForKey(prefix + key)

    override fun putString(key: String, value: String) {
        defaults.setObject(value, forKey = prefix + key)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val fullKey = prefix + key
        return if (defaults.objectForKey(fullKey) != null) {
            defaults.boolForKey(fullKey)
        } else {
            defaultValue
        }
    }

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, forKey = prefix + key)
    }

    override fun remove(key: String) {
        defaults.removeObjectForKey(prefix + key)
    }
}

class IosPlatformBridge : PlatformBridge {
    override val capabilities = PlatformCapabilities(
        platformName = "iOS",
        supportsContactsPicker = false,
        supportsQrScanner = false,
        supportsWhatsAppVariantPicker = false,
        supportsNativeShare = true
    )

    override val storage: KeyValueStorage = IosKeyValueStorage()

    override fun openWhatsApp(
        normalizedPhone: String,
        message: String,
        preference: WhatsAppAppPreference
    ): LaunchResult {
        val urlString = PhoneNumberUtils.buildWhatsAppUrl(normalizedPhone, message)
        val nsUrl = NSURL.URLWithString(urlString)
            ?: return LaunchResult.Error("Link inválido para o WhatsApp.")

        UIApplication.sharedApplication.openURL(
            url = nsUrl,
            options = emptyMap<Any?, Any>(),
            completionHandler = null
        )
        return LaunchResult.Success
    }

    override fun readClipboard(): String? =
        UIPasteboard.generalPasteboard.string?.takeIf { it.isNotBlank() }

    override fun copyToClipboard(text: String): Boolean {
        UIPasteboard.generalPasteboard.string = text
        return true
    }

    override fun shareText(text: String): Boolean {
        val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return false
        val activity = UIActivityViewController(
            activityItems = listOf(text),
            applicationActivities = null
        )
        rootController.presentViewController(activity, animated = true, completion = null)
        return true
    }

    override fun currentTimeMillis(): Long =
        (NSDate().timeIntervalSince1970 * 1000.0).toLong()

    override fun formatTimestamp(timestamp: Long): String {
        val formatter = NSDateFormatter().apply {
            dateFormat = "dd/MM/yyyy HH:mm"
        }
        val date = NSDate.dateWithTimeIntervalSince1970(timestamp.toDouble() / 1000.0)
        val absolute = formatter.stringFromDate(date)
        return formatRelativeDateFallback(timestamp, currentTimeMillis(), absolute)
    }
}

@Composable
actual fun PlatformContactsOverlay(
    visible: Boolean,
    onContactPicked: (phone: String, name: String) -> Unit,
    onDismiss: () -> Unit
) {
    // Future native CNContactPickerViewController hook
}

@Composable
actual fun PlatformQrScannerOverlay(
    visible: Boolean,
    onCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Future native AVFoundation QR hook
}
