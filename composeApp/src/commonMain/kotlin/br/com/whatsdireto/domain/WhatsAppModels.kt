package br.com.whatsdireto.domain

enum class WhatsAppAppPreference(val label: String) {
    AUTO("Automático"),
    STANDARD("WhatsApp"),
    BUSINESS("Business")
}

enum class AppThemeMode(val label: String) {
    SYSTEM("Automático"),
    LIGHT("Claro"),
    DARK("Escuro")
}

data class HistoryEntry(
    val phone: String,
    val timestamp: Long,
    val label: String = "",
    val isPinned: Boolean = false
)

sealed interface LaunchResult {
    data object Success : LaunchResult
    data class Error(val message: String) : LaunchResult
}

data class PlatformCapabilities(
    val platformName: String,
    val supportsContactsPicker: Boolean,
    val supportsQrScanner: Boolean,
    val supportsWhatsAppVariantPicker: Boolean,
    val supportsNativeShare: Boolean
)
