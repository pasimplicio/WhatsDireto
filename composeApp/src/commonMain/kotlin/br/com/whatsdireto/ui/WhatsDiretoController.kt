package br.com.whatsdireto.ui

import br.com.whatsdireto.data.HistoryRepository
import br.com.whatsdireto.data.QuickMessageRepository
import br.com.whatsdireto.data.SettingsRepository
import br.com.whatsdireto.domain.AppThemeMode
import br.com.whatsdireto.domain.Country
import br.com.whatsdireto.domain.HistoryEntry
import br.com.whatsdireto.domain.LaunchResult
import br.com.whatsdireto.domain.PhoneNumberUtils
import br.com.whatsdireto.domain.WhatsAppAppPreference
import br.com.whatsdireto.platform.PlatformBridge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val TUTORIAL_TOTAL_STEPS = 5

class WhatsDiretoController(
    private val platform: PlatformBridge,
    private val historyRepository: HistoryRepository,
    private val quickMessageRepository: QuickMessageRepository,
    private val settingsRepository: SettingsRepository,
    scope: CoroutineScope
) {
    private var nextFeedbackId = 1L

    private val _uiState = MutableStateFlow(
        WhatsAppUiState(
            selectedCountry = settingsRepository.getSelectedCountry(),
            whatsAppPreference = settingsRepository.getWhatsAppPreference(),
            themeMode = settingsRepository.getThemeMode(),
            history = historyRepository.history.value,
            quickMessages = quickMessageRepository.messages.value,
            showTutorial = !settingsRepository.hasSeenTutorial(),
            tutorialStep = 0
        )
    )
    val uiState: StateFlow<WhatsAppUiState> = _uiState.asStateFlow()

    val capabilities get() = platform.capabilities

    init {
        scope.launch {
            combine(
                historyRepository.history,
                quickMessageRepository.messages
            ) { history, messages ->
                history to messages
            }.collect { (history, messages) ->
                _uiState.update {
                    it.copy(
                        history = history,
                        quickMessages = messages
                    )
                }
            }
        }
    }

    fun onPhoneDigitsChanged(rawInput: String) {
        val currentCountry = _uiState.value.selectedCountry
        // If user pasted a link or '+' international number straight into the input field
        if (rawInput.contains("wa.me", ignoreCase = true) ||
            rawInput.contains("whatsapp.com", ignoreCase = true) ||
            rawInput.trim().startsWith("+")
        ) {
            applyRawExternalText(rawInput, sourceFeedback = null)
            return
        }

        val sanitized = PhoneNumberUtils.sanitizeLocalInput(rawInput, currentCountry)
        _uiState.update { it.copy(phoneDigits = sanitized) }
    }

    fun clearPhoneInput() {
        _uiState.update { it.copy(phoneDigits = "") }
    }

    fun onMessageChanged(newText: String) {
        _uiState.update { it.copy(messageText = newText) }
    }

    fun onCountrySelected(country: Country) {
        settingsRepository.setSelectedCountry(country)
        _uiState.update {
            it.copy(
                selectedCountry = country,
                showCountryPicker = false
            )
        }
    }

    fun onWhatsAppPreferenceChanged(pref: WhatsAppAppPreference) {
        settingsRepository.setWhatsAppPreference(pref)
        _uiState.update { it.copy(whatsAppPreference = pref) }
    }

    fun cycleThemeMode() {
        val next = when (_uiState.value.themeMode) {
            AppThemeMode.SYSTEM -> AppThemeMode.LIGHT
            AppThemeMode.LIGHT -> AppThemeMode.DARK
            AppThemeMode.DARK -> AppThemeMode.SYSTEM
        }
        settingsRepository.setThemeMode(next)
        _uiState.update { it.copy(themeMode = next) }
        emitFeedback("Tema: ${next.label}")
    }

    fun pasteFromClipboard() {
        val text = platform.readClipboard()
        if (text.isNullOrBlank()) {
            emitFeedback("Nenhum texto encontrado na área de transferência.")
            return
        }
        applyRawExternalText(text, sourceFeedback = "Número colado")
    }

    fun onIncomingExternalText(text: String) {
        applyRawExternalText(text, sourceFeedback = "Número importado")
    }

    fun onContactPicked(rawPhone: String, contactName: String) {
        applyRawExternalText(rawPhone, sourceFeedback = "Contato carregado: $contactName")
    }

    fun onQrCodeScanned(payload: String) {
        applyRawExternalText(payload, sourceFeedback = "QR Code lido com sucesso")
    }

    private fun applyRawExternalText(rawText: String, sourceFeedback: String?) {
        val currentCountry = _uiState.value.selectedCountry
        val parsed = PhoneNumberUtils.extractFromRawText(rawText, currentCountry)
        if (parsed.localDigits.isEmpty()) {
            if (sourceFeedback != null) {
                emitFeedback("Não foi possível identificar um número válido.")
            }
            return
        }

        val targetCountry = parsed.detectedCountry ?: currentCountry
        if (parsed.detectedCountry != null && parsed.detectedCountry != currentCountry) {
            settingsRepository.setSelectedCountry(parsed.detectedCountry)
        }

        _uiState.update { state ->
            state.copy(
                selectedCountry = targetCountry,
                phoneDigits = parsed.localDigits,
                messageText = parsed.extractedMessage ?: state.messageText
            )
        }

        if (sourceFeedback != null) {
            val countryNote = if (parsed.detectedCountry != null && parsed.detectedCountry != currentCountry) {
                " (${parsed.detectedCountry.name})"
            } else {
                ""
            }
            emitFeedback("$sourceFeedback$countryNote")
        }
    }

    fun openConversation() {
        val state = _uiState.value
        val normalized = state.normalizedPhone
        if (normalized == null) {
            emitFeedback(state.validationHint ?: "Digite um número válido para continuar.")
            return
        }

        when (val result = platform.openWhatsApp(normalized, state.messageText, state.whatsAppPreference)) {
            LaunchResult.Success -> {
                historyRepository.save(normalized)
                emitFeedback("Abrindo conversa no WhatsApp...")
            }
            is LaunchResult.Error -> emitFeedback(result.message)
        }
    }

    fun openHistoryEntryDirectly(entry: HistoryEntry) {
        val state = _uiState.value
        when (val result = platform.openWhatsApp(entry.phone, "", state.whatsAppPreference)) {
            LaunchResult.Success -> {
                historyRepository.save(entry.phone)
                emitFeedback("Abrindo conversa...")
            }
            is LaunchResult.Error -> emitFeedback(result.message)
        }
    }

    fun copyCurrentLink() {
        val url = _uiState.value.previewUrl
        if (url == null) {
            emitFeedback("Digite um número válido para copiar o link.")
            return
        }
        if (platform.copyToClipboard(url)) {
            emitFeedback("Link wa.me copiado!")
        } else {
            emitFeedback("Não foi possível copiar o link.")
        }
    }

    fun shareCurrentLink() {
        val url = _uiState.value.previewUrl
        if (url == null) {
            emitFeedback("Digite um número válido para compartilhar.")
            return
        }
        if (!platform.shareText(url)) {
            emitFeedback("Não foi possível compartilhar o link.")
        }
    }

    fun copyHistoryLink(entry: HistoryEntry) {
        val url = PhoneNumberUtils.buildWhatsAppUrl(entry.phone)
        if (platform.copyToClipboard(url)) {
            emitFeedback("Link copiado!")
        }
    }

    fun onHistorySelected(entry: HistoryEntry) {
        val detectedCountry = PhoneNumberUtils.resolveCountryFromFullNumber(entry.phone)
            ?: _uiState.value.selectedCountry
        val localDigits = if (entry.phone.startsWith(detectedCountry.code)) {
            entry.phone.removePrefix(detectedCountry.code)
        } else {
            entry.phone
        }

        settingsRepository.setSelectedCountry(detectedCountry)
        _uiState.update {
            it.copy(
                selectedCountry = detectedCountry,
                phoneDigits = localDigits
            )
        }
    }

    fun toggleHistoryPin(entry: HistoryEntry) {
        historyRepository.togglePin(entry.phone)
        emitFeedback(if (entry.isPinned) "Removido dos fixados" else "Número fixado no topo")
    }

    fun startEditingHistoryLabel(entry: HistoryEntry) {
        _uiState.update { it.copy(editingHistoryEntry = entry) }
    }

    fun dismissEditingHistoryLabel() {
        _uiState.update { it.copy(editingHistoryEntry = null) }
    }

    fun saveHistoryLabel(phone: String, label: String) {
        historyRepository.updateLabel(phone, label)
        _uiState.update { it.copy(editingHistoryEntry = null) }
        emitFeedback(if (label.isBlank()) "Etiqueta removida" else "Etiqueta salva")
    }

    fun deleteHistoryEntry(entry: HistoryEntry) {
        val removed = historyRepository.delete(entry.phone) ?: return
        emitFeedback(
            message = "Número removido do histórico",
            actionLabel = "Desfazer",
            undoDeletedEntry = removed
        )
    }

    fun undoDeleteHistoryEntry(entry: HistoryEntry) {
        historyRepository.restore(entry)
    }

    fun clearHistory() {
        historyRepository.clear()
        _uiState.update { it.copy(historySearchQuery = "", historyFilter = HistoryFilter.ALL) }
        emitFeedback("Histórico limpo.")
    }

    fun onHistorySearchChanged(query: String) {
        _uiState.update { it.copy(historySearchQuery = query) }
    }

    fun onHistoryFilterChanged(filter: HistoryFilter) {
        _uiState.update { it.copy(historyFilter = filter) }
    }

    fun onQuickMessageSelected(message: String) {
        _uiState.update {
            it.copy(
                messageText = message,
                showQuickMessages = false
            )
        }
        quickMessageRepository.addOrPromote(message)
    }

    fun saveCurrentMessageAsQuickTemplate() {
        val text = _uiState.value.messageText.trim()
        if (text.isEmpty()) return
        quickMessageRepository.addOrPromote(text)
        emitFeedback("Mensagem salva nos modelos rápidos!")
    }

    fun addQuickMessage(message: String) {
        if (message.isBlank()) return
        quickMessageRepository.addOrPromote(message)
        emitFeedback("Modelo adicionado.")
    }

    fun updateQuickMessage(oldMessage: String, newMessage: String) {
        if (newMessage.isBlank()) return
        quickMessageRepository.update(oldMessage, newMessage)
        emitFeedback("Modelo atualizado.")
    }

    fun deleteQuickMessage(message: String) {
        quickMessageRepository.delete(message)
    }

    fun resetQuickMessages() {
        quickMessageRepository.resetToDefault()
        emitFeedback("Modelos padrão restaurados.")
    }

    fun setShowCountryPicker(show: Boolean) {
        _uiState.update { it.copy(showCountryPicker = show) }
    }

    fun setShowContactPicker(show: Boolean) {
        _uiState.update { it.copy(showContactPicker = show) }
    }

    fun setShowQrScanner(show: Boolean) {
        _uiState.update { it.copy(showQrScanner = show) }
    }

    fun setShowQuickMessages(show: Boolean) {
        _uiState.update { it.copy(showQuickMessages = show) }
    }

    fun setShowQrGenerator(show: Boolean) {
        _uiState.update { it.copy(showQrGenerator = show) }
    }

    fun openTutorial() {
        _uiState.update {
            it.copy(
                showTutorial = true,
                tutorialStep = 0
            )
        }
    }

    fun nextTutorialStep() {
        val current = _uiState.value.tutorialStep
        if (current + 1 < TUTORIAL_TOTAL_STEPS) {
            _uiState.update { it.copy(tutorialStep = current + 1) }
        } else {
            completeTutorial()
        }
    }

    fun previousTutorialStep() {
        val current = _uiState.value.tutorialStep
        if (current > 0) {
            _uiState.update { it.copy(tutorialStep = current - 1) }
        }
    }

    fun completeTutorial() {
        settingsRepository.setTutorialSeen(true)
        _uiState.update {
            it.copy(
                showTutorial = false,
                tutorialStep = 0
            )
        }
    }

    fun formatTimestamp(timestamp: Long): String = platform.formatTimestamp(timestamp)

    fun consumeFeedback() {
        _uiState.update { it.copy(feedback = null) }
    }

    private fun emitFeedback(
        message: String,
        actionLabel: String? = null,
        undoDeletedEntry: HistoryEntry? = null
    ) {
        _uiState.update {
            it.copy(
                feedback = FeedbackEvent(
                    id = nextFeedbackId++,
                    message = message,
                    actionLabel = actionLabel,
                    undoDeletedEntry = undoDeletedEntry
                )
            )
        }
    }
}
