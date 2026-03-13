package br.com.whatsdireto.ui

import android.content.ActivityNotFoundException
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLEncoder

class WhatsAppViewModel(
    private val historyRepository: HistoryRepository,
    private val quickMessageRepository: QuickMessageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WhatsAppUiState())
    val uiState: StateFlow<WhatsAppUiState> = _uiState.asStateFlow()

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

    fun onFileSelected(context: Context, uri: Uri) {
        val fileName = getFileName(context, uri) ?: "Arquivo selecionado"

        _uiState.update {
            it.copy(
                selectedFileUri = uri,
                selectedFileName = fileName,
                message = "Arquivo anexado: $fileName"
            )
        }
    }

    fun clearAttachment() {
        _uiState.update {
            it.copy(
                selectedFileUri = null,
                selectedFileName = null
            )
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                result = result?.substring(cut!! + 1)
            }
        }
        return result
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

    fun onSendClick(context: Context) {
        val normalized = PhoneMask.toWhatsAppPhone(_uiState.value.phoneInput.text)

        if (normalized == null) {
            showMessage("Número inválido. Digite DDD + telefone.")
            return
        }

        viewModelScope.launch {
            historyRepository.save(normalized)
        }

        // Pega a mensagem
        val messageText = _uiState.value.messageInput.text

        // Verifica se tem arquivo anexado
        val fileUri = _uiState.value.selectedFileUri
        if (fileUri != null) {
            shareFileWithWhatsApp(context, normalized, messageText, fileUri)
        } else {
            WhatsAppLauncher.open(context, normalized, messageText)
        }

        // Limpa os campos após enviar
        clearAllFields()
    }

    private fun shareFileWithWhatsApp(context: Context, phone: String, message: String, fileUri: Uri) {
        try {
            val cleanPhone = phone.replace("[^0-9]".toRegex(), "")

            // Cria um Intent combinado que primeiro abre a conversa e depois compartilha
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                type = context.contentResolver.getType(fileUri) ?: "*/*"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                if (message.isNotBlank()) {
                    putExtra(Intent.EXTRA_TEXT, message)
                }

                // Adiciona o número do WhatsApp
                putExtra("jid", "$cleanPhone@s.whatsapp.net")
                putExtra("phone", cleanPhone)

                setPackage("com.whatsapp")
            }

            // Tenta iniciar diretamente
            context.startActivity(intent)

        } catch (e: ActivityNotFoundException) {
            try {
                // Tenta com WhatsApp Business
                val cleanPhone = phone.replace("[^0-9]".toRegex(), "")

                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = context.contentResolver.getType(fileUri) ?: "*/*"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    if (message.isNotBlank()) {
                        putExtra(Intent.EXTRA_TEXT, message)
                    }

                    putExtra("jid", "$cleanPhone@s.whatsapp.net")
                    putExtra("phone", cleanPhone)

                    setPackage("com.whatsapp.w4b")
                }

                context.startActivity(intent)

            } catch (e2: ActivityNotFoundException) {
                // Se não encontrar WhatsApp, abre no navegador
                val cleanPhone = phone.replace("[^0-9]".toRegex(), "")
                val encodedMessage = URLEncoder.encode(message, "UTF-8")
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage")
                )
                browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            // Se falhar, tenta o método tradicional
            WhatsAppLauncher.open(context, phone, message)
        }
    }

    private fun clearAllFields() {
        _uiState.update {
            it.copy(
                phoneInput = TextFieldValue(""),
                messageInput = TextFieldValue(""),
                selectedFileUri = null,
                selectedFileName = null,
                message = "Conversa aberta com sucesso!"
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

    fun toggleQuickMessages() {
        _uiState.update { it.copy(showQuickMessages = !it.showQuickMessages) }
    }

    private fun showMessage(text: String) {
        _uiState.update { it.copy(message = text) }
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
                }
            }
        }
    }

    // Funções do Tutorial
    fun completeTutorial() {
        _uiState.update {
            it.copy(
                isFirstTimeUser = false,
                currentTutorialStep = 0
            )
        }
    }

    fun nextTutorialStep() {
        val currentStep = _uiState.value.currentTutorialStep
        if (currentStep < 6) {
            _uiState.update {
                it.copy(currentTutorialStep = currentStep + 1)
            }
        } else {
            completeTutorial()
        }
    }

    fun previousTutorialStep() {
        val currentStep = _uiState.value.currentTutorialStep
        if (currentStep > 0) {
            _uiState.update {
                it.copy(currentTutorialStep = currentStep - 1)
            }
        }
    }

    fun skipTutorial() {
        completeTutorial()
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