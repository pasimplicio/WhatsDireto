package br.com.whatsdireto.ui

import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.whatsdireto.data.HistoryEntry
import br.com.whatsdireto.data.HistoryRepository
import br.com.whatsdireto.data.QuickMessageRepository
import br.com.whatsdireto.domain.PhoneMask
import br.com.whatsdireto.domain.WhatsAppLauncher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class WhatsAppViewModel(
    private val historyRepository: HistoryRepository,
    private val quickMessageRepository: QuickMessageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WhatsAppUiState())
    val uiState: StateFlow<WhatsAppUiState> = _uiState.asStateFlow()

    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener? = null

    init {
        viewModelScope.launch {
            combine(
                historyRepository.observeHistory(),
                quickMessageRepository.observeMessages()
            ) { history, messages ->
                _uiState.update {
                    it.copy(
                        history = history,
                        quickMessages = messages
                    )
                }
            }.collect {}
        }

        // Inicia monitoramento da área de transferência
        startClipboardMonitoring()
    }

    fun onPhoneChanged(value: TextFieldValue) {
        val formatted = PhoneMask.format(value.text)

        _uiState.update {
            it.copy(
                phoneInput = TextFieldValue(
                    text = formatted,
                    selection = TextRange(formatted.length)
                ),
                message = null
            )
        }

        // Se tiver texto na busca, filtra histórico
        if (formatted.length >= 3) {
            searchHistory(formatted)
        } else {
            clearSearch()
        }
    }

    fun onMessageChanged(value: TextFieldValue) {
        _uiState.update {
            it.copy(
                messageInput = value,
                message = null
            )
        }
    }

    fun pasteFromClipboard(context: Context) {
        val manager = context.getSystemService(ClipboardManager::class.java)
        if (manager == null || !manager.hasPrimaryClip()) {
            showMessage("A área de transferência está vazia.")
            return
        }

        val clip = manager.primaryClip ?: run {
            showMessage("A área de transferência está vazia.")
            return
        }

        val canReadText =
            manager.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == true ||
                    manager.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML) == true

        if (!canReadText || clip.itemCount == 0) {
            showMessage("Nenhum texto válido encontrado para colar.")
            return
        }

        val text = clip.getItemAt(0).coerceToText(context)?.toString().orEmpty()
        if (text.isBlank()) {
            showMessage("Nenhum texto válido encontrado para colar.")
            return
        }

        val formatted = PhoneMask.format(text)

        _uiState.update {
            it.copy(
                phoneInput = TextFieldValue(
                    text = formatted,
                    selection = TextRange(formatted.length)
                ),
                message = "Número colado com sucesso."
            )
        }
    }

    fun onHistorySelected(entry: HistoryEntry) {
        val formatted = PhoneMask.formatForDisplay(entry.phone)

        _uiState.update {
            it.copy(
                phoneInput = TextFieldValue(
                    text = formatted,
                    selection = TextRange(formatted.length)
                ),
                message = null,
                isSearching = false
            )
        }
    }

    fun onQuickMessageSelected(message: String) {
        _uiState.update {
            it.copy(
                messageInput = TextFieldValue(
                    text = message,
                    selection = TextRange(message.length)
                ),
                showQuickMessages = false
            )
        }

        // Salva mensagem usada
        viewModelScope.launch {
            quickMessageRepository.saveMessage(message)
        }
    }

    fun deleteQuickMessage(message: String) {
        viewModelScope.launch {
            quickMessageRepository.deleteMessage(message)
        }
    }

    fun resetQuickMessages() {
        viewModelScope.launch {
            quickMessageRepository.resetToDefault()
        }
    }

    fun onQrCodeScanned(rawValue: String?) {
        val raw = rawValue?.trim().orEmpty()
        if (raw.isBlank()) {
            showMessage("QR code vazio ou inválido.")
            return
        }

        val candidate = extractPhoneCandidate(raw)
        val normalized = candidate?.let(PhoneMask::toWhatsAppPhone)

        if (normalized == null) {
            showMessage("Não foi possível identificar um número de WhatsApp no QR code.")
            return
        }

        val formatted = PhoneMask.formatForDisplay(normalized)

        _uiState.update {
            it.copy(
                phoneInput = TextFieldValue(
                    text = formatted,
                    selection = TextRange(formatted.length)
                ),
                message = "Número lido por QR code.",
                showQRScanner = false
            )
        }
    }

    fun onSendClick(context: Context) {
        val normalized = PhoneMask.toWhatsAppPhone(_uiState.value.phoneInput.text)

        if (normalized == null) {
            showMessage("Número inválido. Digite DDD + telefone.")
            return
        }

        viewModelScope.launch {
            historyRepository.save(normalized)
        }

        // Se tiver mensagem rápida, envia junto (via URL)
        val messageText = _uiState.value.messageInput.text
        WhatsAppLauncher.open(context, normalized, messageText)

        val formatted = PhoneMask.formatForDisplay(normalized)

        _uiState.update {
            it.copy(
                phoneInput = TextFieldValue(
                    text = formatted,
                    selection = TextRange(formatted.length)
                ),
                message = null
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyRepository.clear()
            showMessage("Histórico limpo.")
        }
    }

    fun searchHistory(query: String) {
        viewModelScope.launch {
            val results = historyRepository.searchHistory(query)
            _uiState.update {
                it.copy(
                    filteredHistory = results,
                    isSearching = true
                )
            }
        }
    }

    fun clearSearch() {
        _uiState.update {
            it.copy(
                filteredHistory = emptyList(),
                isSearching = false
            )
        }
    }

    fun consumeMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun historyLabel(entry: HistoryEntry): String {
        return historyRepository.historyLabel(entry)
    }

    fun toggleContactPicker() {
        _uiState.update { it.copy(showContactPicker = !it.showContactPicker) }
    }

    fun toggleQRScanner() {
        _uiState.update { it.copy(showQRScanner = !it.showQRScanner) }
    }

    fun toggleQuickMessages() {
        _uiState.update { it.copy(showQuickMessages = !it.showQuickMessages) }
    }

    private fun showMessage(text: String) {
        _uiState.update { it.copy(message = text) }
    }

    private fun extractPhoneCandidate(raw: String): String? {
        val parsedUri = runCatching { Uri.parse(raw) }.getOrNull()
        val phoneQuery = parsedUri?.getQueryParameter("phone")

        val waMe = Regex("wa\\.me/([0-9+]+)", RegexOption.IGNORE_CASE)
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)

        val directPhone = Regex("phone=([0-9+]+)", RegexOption.IGNORE_CASE)
            .find(raw)
            ?.groupValues
            ?.getOrNull(1)

        val allDigits = PhoneMask.digits(raw)
        val brazilWithCountryCode = Regex("55\\d{10,11}").find(allDigits)?.value

        val candidates = listOfNotNull(
            phoneQuery,
            waMe,
            directPhone,
            raw,
            brazilWithCountryCode,
            allDigits
        ).distinct()

        return candidates.firstOrNull { PhoneMask.toWhatsAppPhone(it) != null }
    }

    private fun startClipboardMonitoring() {
        // Será implementado com um serviço ou observer
        // Por simplicidade, vamos simular com um timer
        viewModelScope.launch {
            while (true) {
                delay(2000)
                // Aqui verificaria o clipboard
                // Mas requer contexto - farei na Activity
            }
        }
    }

    fun checkClipboardForNumber(context: Context) {
        val manager = context.getSystemService(ClipboardManager::class.java)
        if (manager?.hasPrimaryClip() == true) {
            val clip = manager.primaryClip
            val item = clip?.getItemAt(0)
            val text = item?.coerceToText(context).toString()

            val digits = PhoneMask.digits(text)
            if (digits.length in 10..13) {
                val normalized = PhoneMask.toWhatsAppPhone(text)
                if (normalized != null && !_uiState.value.phoneInput.text.contains(digits)) {
                    showMessage("Número detectado na área de transferência!")
                    // Opcionalmente, poderia auto-preenche
                }
            }
        }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WhatsAppViewModel(
                        historyRepository = HistoryRepository(appContext),
                        quickMessageRepository = QuickMessageRepository(appContext)
                    ) as T
                }
            }
        }
    }
}