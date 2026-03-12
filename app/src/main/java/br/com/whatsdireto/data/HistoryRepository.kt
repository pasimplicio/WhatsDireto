package br.com.whatsdireto.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import br.com.whatsdireto.domain.PhoneMask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val DATASTORE_NAME = "whats_direto_prefs"
private const val HISTORY_LIMIT = 20

private val Context.dataStore by preferencesDataStore(name = DATASTORE_NAME)

data class HistoryEntry(
    val phone: String,
    val timestamp: Long,
    val formattedDate: String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        .format(Date(timestamp))
)

class HistoryRepository(private val context: Context) {

    private val historyKey = stringPreferencesKey("history_numbers_with_time")

    fun observeHistory(): Flow<List<HistoryEntry>> {
        return context.dataStore.data.map { prefs ->
            prefs[historyKey]
                ?.split("|||")
                ?.map { entry ->
                    val parts = entry.split("|")
                    if (parts.size == 2) {
                        HistoryEntry(
                            phone = parts[0],
                            timestamp = parts[1].toLongOrNull() ?: System.currentTimeMillis()
                        )
                    } else {
                        HistoryEntry(
                            phone = entry,
                            timestamp = System.currentTimeMillis()
                        )
                    }
                }
                ?.sortedByDescending { it.timestamp }
                ?: emptyList()
        }
    }

    suspend fun save(normalizedPhone: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[historyKey]
                ?.split("|||")
                ?.map { entry ->
                    val parts = entry.split("|")
                    if (parts.size == 2) {
                        parts[0] to parts[1].toLongOrNull()
                    } else {
                        entry to null
                    }
                }
                ?.associate { it.first to it.second }
                ?.toMutableMap()
                ?: mutableMapOf()

            // Adiciona/atualiza com timestamp atual
            current[normalizedPhone] = System.currentTimeMillis()

            // Ordena por timestamp (mais recente primeiro) e limita
            val limited = current.entries
                .sortedByDescending { it.value }
                .take(HISTORY_LIMIT)
                .associate { it.key to it.value }

            // Salva no formato: telefone|timestamp|||telefone|timestamp
            prefs[historyKey] = limited.entries
                .joinToString("|||") { "${it.key}|${it.value}" }
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(historyKey)
        }
    }

    suspend fun searchHistory(query: String): List<HistoryEntry> {
        val all = currentHistory()
        if (query.isBlank()) return all

        val normalizedQuery = PhoneMask.digits(query)
        return all.filter {
            it.phone.contains(normalizedQuery) ||
                    PhoneMask.formatForDisplay(it.phone).contains(query, ignoreCase = true)
        }
    }

    suspend fun currentHistory(): List<HistoryEntry> {
        var result: List<HistoryEntry> = emptyList()
        observeHistory().collect { list ->
            result = list
            return@collect
        }
        return result
    }

    fun historyLabel(entry: HistoryEntry): String {
        val formatted = PhoneMask.formatForDisplay(entry.phone)
        return "$formatted • ${entry.formattedDate}"
    }
}