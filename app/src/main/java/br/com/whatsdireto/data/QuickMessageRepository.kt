package br.com.whatsdireto.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "quick_messages")

class QuickMessageRepository(private val context: Context) {

    private val messagesKey = stringPreferencesKey("saved_messages")

    val defaultMessages = listOf(
        "Olá, tudo bem?",
        "Oi, vi seu número no WhatsApp",
        "Bom dia! 🌅",
        "Boa tarde! ☀️",
        "Boa noite! 🌙",
        "Podemos conversar?",
        "Obrigado pelo contato!",
        "Estou interessado no seu produto"
    )

    fun observeMessages(): Flow<List<String>> {
        return context.dataStore.data.map { prefs ->
            prefs[messagesKey]
                ?.split("|")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.distinct()
                ?: defaultMessages
        }
    }

    suspend fun saveMessage(message: String) {
        if (message.isBlank()) return

        context.dataStore.edit { prefs ->
            val current = prefs[messagesKey]
                ?.split("|")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.toMutableList()
                ?: defaultMessages.toMutableList()

            current.removeAll { it == message }
            current.add(0, message)

            val limited = current.take(20) // Mantém até 20 mensagens
            prefs[messagesKey] = limited.joinToString("|")
        }
    }

    suspend fun deleteMessage(message: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[messagesKey]
                ?.split("|")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?.toMutableList()
                ?: defaultMessages.toMutableList()

            current.removeAll { it == message }
            prefs[messagesKey] = current.joinToString("|")
        }
    }

    suspend fun resetToDefault() {
        context.dataStore.edit { prefs ->
            prefs.remove(messagesKey)
        }
    }
}