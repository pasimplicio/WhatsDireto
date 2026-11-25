package br.com.whatsdireto.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val QUICK_MESSAGES_KEY = "quick_messages_v2"
private const val MAX_QUICK_MESSAGES = 30

class QuickMessageRepository(private val storage: KeyValueStorage) {

    val defaultMessages: List<String> = listOf(
        "Olá, tudo bem?",
        "Bom dia! Podemos conversar?",
        "Boa tarde! Gostaria de mais informações.",
        "Boa noite! Tudo bem por aí?",
        "Obrigado pelo contato!",
        "Estou a caminho, chego em breve.",
        "Recebi sua mensagem, já respondo."
    )

    private val _messages = MutableStateFlow(loadFromStorage())
    val messages: StateFlow<List<String>> = _messages.asStateFlow()

    fun addOrPromote(message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return
        val current = _messages.value.toMutableList()
        current.removeAll { it == trimmed }
        current.add(0, trimmed)
        persist(current.take(MAX_QUICK_MESSAGES))
    }

    fun update(oldMessage: String, newMessage: String) {
        val trimmed = newMessage.trim()
        if (trimmed.isEmpty()) return
        val current = _messages.value.map { if (it == oldMessage) trimmed else it }.distinct()
        persist(current.take(MAX_QUICK_MESSAGES))
    }

    fun delete(message: String) {
        val updated = _messages.value.filterNot { it == message }
        persist(updated)
    }

    fun resetToDefault() {
        storage.remove(QUICK_MESSAGES_KEY)
        _messages.value = defaultMessages
    }

    private fun persist(items: List<String>) {
        val rows = items.map { listOf(it) }
        storage.putString(QUICK_MESSAGES_KEY, StorageCodec.encodeRows(rows))
        _messages.value = items
    }

    private fun loadFromStorage(): List<String> {
        val raw = storage.getString(QUICK_MESSAGES_KEY) ?: return defaultMessages
        return StorageCodec.decodeRows(raw)
            .mapNotNull { it.firstOrNull()?.trim()?.takeIf(String::isNotEmpty) }
            .distinct()
    }
}
