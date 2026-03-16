package br.com.whatsdireto.ui

import androidx.compose.ui.text.input.TextFieldValue
import br.com.whatsdireto.data.HistoryEntry

data class WhatsAppUiState(
    val phoneInput: TextFieldValue = TextFieldValue(""),
    val messageInput: TextFieldValue = TextFieldValue(""),
    val history: List<HistoryEntry> = emptyList(),
    val filteredHistory: List<HistoryEntry> = emptyList(),
    val quickMessages: List<String> = emptyList(),
    val message: String? = null,
    val showContactPicker: Boolean = false,
    val showQuickMessages: Boolean = false,
    val isSearching: Boolean = false,
    val isFirstTimeUser: Boolean = true,
    val currentTutorialStep: Int = 0
)