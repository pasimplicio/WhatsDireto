package br.com.whatsdireto.data

interface KeyValueStorage {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun remove(key: String)
}

class InMemoryKeyValueStorage : KeyValueStorage {
    private val strings = mutableMapOf<String, String>()
    private val booleans = mutableMapOf<String, Boolean>()

    override fun getString(key: String): String? = strings[key]
    override fun putString(key: String, value: String) { strings[key] = value }
    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = booleans[key] ?: defaultValue
    override fun putBoolean(key: String, value: Boolean) { booleans[key] = value }
    override fun remove(key: String) {
        strings.remove(key)
        booleans.remove(key)
    }
}

/**
 * Safe delimiter-free codec using backslash escaping so characters like '|', newlines,
 * and tabs never corrupt persisted history entries or quick messages.
 */
internal object StorageCodec {
    fun escape(value: String): String = buildString(value.length) {
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }

    fun unescape(value: String): String = buildString(value.length) {
        var i = 0
        while (i < value.length) {
            val ch = value[i]
            if (ch == '\\' && i + 1 < value.length) {
                when (value[i + 1]) {
                    '\\' -> append('\\')
                    'n' -> append('\n')
                    'r' -> append('\r')
                    't' -> append('\t')
                    else -> append(value[i + 1])
                }
                i += 2
            } else {
                append(ch)
                i++
            }
        }
    }

    fun encodeRows(rows: List<List<String>>): String =
        rows.joinToString("\n") { cols -> cols.joinToString("\t") { escape(it) } }

    fun decodeRows(raw: String?): List<List<String>> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split("\n")
            .filter { it.isNotBlank() }
            .map { line -> line.split("\t").map(::unescape) }
    }
}
