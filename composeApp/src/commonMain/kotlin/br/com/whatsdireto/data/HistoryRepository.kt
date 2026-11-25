package br.com.whatsdireto.data

import br.com.whatsdireto.domain.HistoryEntry
import br.com.whatsdireto.domain.PhoneNumberUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val HISTORY_STORAGE_KEY = "history_v2"
private const val HISTORY_LIMIT = 50

class HistoryRepository(
    private val storage: KeyValueStorage,
    private val nowProvider: () -> Long
) {
    private val _history = MutableStateFlow(loadFromStorage())
    val history: StateFlow<List<HistoryEntry>> = _history.asStateFlow()

    fun save(normalizedPhone: String, label: String? = null) {
        val clean = PhoneNumberUtils.digits(normalizedPhone)
        if (clean.isEmpty()) return

        val current = _history.value.toMutableList()
        val existing = current.firstOrNull { it.phone == clean }
        current.removeAll { it.phone == clean }

        val resolvedLabel = when {
            label != null -> label.trim()
            existing != null -> existing.label
            else -> ""
        }

        current.add(
            HistoryEntry(
                phone = clean,
                timestamp = nowProvider(),
                label = resolvedLabel,
                isPinned = existing?.isPinned ?: false
            )
        )

        persist(sortAndLimit(current))
    }

    fun togglePin(phone: String) {
        val updated = _history.value.map { entry ->
            if (entry.phone == phone) entry.copy(isPinned = !entry.isPinned) else entry
        }
        persist(sortAndLimit(updated))
    }

    fun updateLabel(phone: String, newLabel: String) {
        val updated = _history.value.map { entry ->
            if (entry.phone == phone) entry.copy(label = newLabel.trim()) else entry
        }
        persist(sortAndLimit(updated))
    }

    fun delete(phone: String): HistoryEntry? {
        val target = _history.value.firstOrNull { it.phone == phone } ?: return null
        val updated = _history.value.filterNot { it.phone == phone }
        persist(updated)
        return target
    }

    fun restore(entry: HistoryEntry) {
        val current = _history.value.filterNot { it.phone == entry.phone } + entry
        persist(sortAndLimit(current))
    }

    fun clear() {
        storage.remove(HISTORY_STORAGE_KEY)
        _history.value = emptyList()
    }

    private fun sortAndLimit(items: List<HistoryEntry>): List<HistoryEntry> {
        val pinned = items.filter { it.isPinned }.sortedByDescending { it.timestamp }
        val regular = items.filterNot { it.isPinned }.sortedByDescending { it.timestamp }
        return (pinned + regular).take(HISTORY_LIMIT)
    }

    private fun persist(items: List<HistoryEntry>) {
        val rows = items.map { entry ->
            listOf(
                entry.phone,
                entry.timestamp.toString(),
                entry.label,
                if (entry.isPinned) "1" else "0"
            )
        }
        storage.putString(HISTORY_STORAGE_KEY, StorageCodec.encodeRows(rows))
        _history.value = items
    }

    private fun loadFromStorage(): List<HistoryEntry> {
        val rows = StorageCodec.decodeRows(storage.getString(HISTORY_STORAGE_KEY))
        val parsed = rows.mapNotNull { cols ->
            val phone = cols.getOrNull(0)?.let(PhoneNumberUtils::digits).orEmpty()
            if (phone.isEmpty()) return@mapNotNull null
            val ts = cols.getOrNull(1)?.toLongOrNull() ?: nowProvider()
            val label = cols.getOrNull(2).orEmpty()
            val pinned = cols.getOrNull(3) == "1"
            HistoryEntry(phone = phone, timestamp = ts, label = label, isPinned = pinned)
        }
        return sortAndLimit(parsed)
    }
}
