package br.com.whatsdireto.ui

import br.com.whatsdireto.domain.AppThemeMode
import br.com.whatsdireto.domain.Countries
import br.com.whatsdireto.domain.Country
import br.com.whatsdireto.domain.HistoryEntry
import br.com.whatsdireto.domain.PhoneNumberUtils
import br.com.whatsdireto.domain.WhatsAppAppPreference

enum class HistoryFilter { ALL, PINNED }

data class FeedbackEvent(
    val id: Long,
    val message: String,
    val actionLabel: String? = null,
    val undoDeletedEntry: HistoryEntry? = null
)

data class WhatsAppUiState(
    val phoneDigits: String = "",
    val messageText: String = "",
    val selectedCountry: Country = Countries.default,
    val whatsAppPreference: WhatsAppAppPreference = WhatsAppAppPreference.AUTO,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val history: List<HistoryEntry> = emptyList(),
    val historySearchQuery: String = "",
    val historyFilter: HistoryFilter = HistoryFilter.ALL,
    val quickMessages: List<String> = emptyList(),
    val showCountryPicker: Boolean = false,
    val showContactPicker: Boolean = false,
    val showQrScanner: Boolean = false,
    val showQuickMessages: Boolean = false,
    val showQrGenerator: Boolean = false,
    val editingHistoryEntry: HistoryEntry? = null,
    val showTutorial: Boolean = false,
    val tutorialStep: Int = 0,
    val feedback: FeedbackEvent? = null
) {
    val isPhoneValid: Boolean
        get() = PhoneNumberUtils.isValidWithCountry(phoneDigits, selectedCountry)

    val validationHint: String?
        get() = PhoneNumberUtils.validationHint(phoneDigits, selectedCountry)

    val normalizedPhone: String?
        get() = if (isPhoneValid) {
            PhoneNumberUtils.normalizeWithCountry(phoneDigits, selectedCountry)
        } else {
            null
        }

    val previewUrl: String?
        get() = normalizedPhone?.let { PhoneNumberUtils.buildWhatsAppUrl(it, messageText) }

    val filteredHistory: List<HistoryEntry>
        get() {
            val base = when (historyFilter) {
                HistoryFilter.ALL -> history
                HistoryFilter.PINNED -> history.filter { it.isPinned }
            }
            val query = historySearchQuery.trim()
            if (query.isEmpty()) return base
            val digitsQuery = PhoneNumberUtils.digits(query)
            return base.filter { entry ->
                entry.label.contains(query, ignoreCase = true) ||
                    PhoneNumberUtils.formatFullForDisplay(entry.phone).contains(query, ignoreCase = true) ||
                    (digitsQuery.isNotEmpty() && entry.phone.contains(digitsQuery))
            }
        }

    val pinnedCount: Int
        get() = history.count { it.isPinned }
}
