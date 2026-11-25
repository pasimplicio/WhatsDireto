package br.com.whatsdireto.domain

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

data class ParsedPhoneInput(
    val localDigits: String,
    val detectedCountry: Country?,
    val extractedMessage: String? = null
)

object PhoneNumberUtils {
    private const val E164_MIN_LENGTH = 7
    private const val E164_MAX_LENGTH = 15

    fun digits(input: String): String = input.filter(Char::isDigit)

    /**
     * Sanitizes user-typed local digits according to the selected country's maximum length.
     */
    fun sanitizeLocalInput(raw: String, country: Country): String {
        val clean = digits(raw)
        val maxLen = (country.maxLocalDigits + country.code.length).coerceAtMost(E164_MAX_LENGTH)
        return clean.take(maxLen)
    }

    /**
     * Parses pasted text, QR code payloads, wa.me / api.whatsapp.com URLs, tel: links,
     * vCards, or raw phone numbers without misidentifying regional area codes (like Brazil DDD 55).
     */
    fun extractFromRawText(raw: String, currentCountry: Country): ParsedPhoneInput {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ParsedPhoneInput("", null)

        // 1. Handle WhatsApp URLs: wa.me/5511999999999?text=Oi or api.whatsapp.com/send?phone=...&text=...
        val lower = trimmed.lowercase()
        if (lower.contains("wa.me/") || lower.contains("whatsapp.com/send")) {
            val extractedMessage = extractQueryParam(trimmed, "text")?.let(::decodeUrlComponent)
            val phoneParam = extractQueryParam(trimmed, "phone")
                ?: trimmed.substringAfter("wa.me/", "")
                    .substringBefore("?")
                    .substringBefore("#")
                    .substringBefore("/")
            val urlDigits = digits(phoneParam)
            if (urlDigits.isNotEmpty()) {
                return resolveExplicitInternational(urlDigits, currentCountry, extractedMessage)
            }
        }

        // 2. Handle vCard TEL lines
        val candidateLine = if (trimmed.contains("BEGIN:VCARD", ignoreCase = true)) {
            trimmed.lines()
                .firstOrNull { it.uppercase().startsWith("TEL") }
                ?.substringAfter(":", "")
                ?.trim()
                ?: trimmed
        } else {
            trimmed
        }

        val hasExplicitInternationalPrefix =
            candidateLine.startsWith("+") ||
                candidateLine.startsWith("00") ||
                candidateLine.lowercase().startsWith("tel:+")

        val allDigits = digits(candidateLine)
        if (allDigits.isEmpty()) return ParsedPhoneInput("", null)

        val normalizedDigits = if (candidateLine.startsWith("00") && allDigits.length > 2) {
            allDigits.drop(2)
        } else {
            allDigits
        }

        if (hasExplicitInternationalPrefix) {
            return resolveExplicitInternational(normalizedDigits, currentCountry, null)
        }

        // 3. No explicit '+' or '00':
        // First check if the digits already fit as a local number for the currently selected country.
        // This guarantees Brazil DDD 55 (11 digits: 55 99999-8888) stays local in Brazil,
        // and Brazil DDD 11 (11 digits: 11 99999-9999) is never misidentified as US (+1).
        val strippedLocalLeadingZero = stripNationalLeadingZero(normalizedDigits, currentCountry)
        if (strippedLocalLeadingZero.length in currentCountry.minLocalDigits..currentCountry.maxLocalDigits) {
            return ParsedPhoneInput(strippedLocalLeadingZero, null)
        }

        // 4. If digits include the current country's dial code (e.g. 5511999999999 -> 13 digits)
        if (normalizedDigits.startsWith(currentCountry.code)) {
            val remainder = stripNationalLeadingZero(
                normalizedDigits.drop(currentCountry.code.length),
                currentCountry
            )
            if (remainder.length in currentCountry.minLocalDigits..currentCountry.maxLocalDigits) {
                return ParsedPhoneInput(remainder, currentCountry)
            }
        }

        // 5. Try matching another country ONLY when remainder fits that country's valid local range
        val matched = Countries.byCodeLongestFirst.firstOrNull { country ->
            if (!normalizedDigits.startsWith(country.code)) return@firstOrNull false
            val remainder = stripNationalLeadingZero(
                normalizedDigits.drop(country.code.length),
                country
            )
            remainder.length in country.minLocalDigits..country.maxLocalDigits
        }

        if (matched != null) {
            val remainder = stripNationalLeadingZero(
                normalizedDigits.drop(matched.code.length),
                matched
            )
            return ParsedPhoneInput(remainder, matched)
        }

        return ParsedPhoneInput(strippedLocalLeadingZero.take(currentCountry.maxLocalDigits), null)
    }

    private fun resolveExplicitInternational(
        internationalDigits: String,
        fallbackCountry: Country,
        extractedMessage: String?
    ): ParsedPhoneInput {
        val matched = Countries.byCodeLongestFirst.firstOrNull { country ->
            if (!internationalDigits.startsWith(country.code)) return@firstOrNull false
            val remainder = internationalDigits.drop(country.code.length)
            remainder.length in country.minLocalDigits..country.maxLocalDigits
        } ?: Countries.byCodeLongestFirst.firstOrNull { country ->
            internationalDigits.startsWith(country.code) &&
                internationalDigits.length > country.code.length + 5
        }

        return if (matched != null) {
            val local = stripNationalLeadingZero(
                internationalDigits.drop(matched.code.length),
                matched
            )
            ParsedPhoneInput(local, matched, extractedMessage)
        } else {
            ParsedPhoneInput(
                internationalDigits.take(fallbackCountry.maxLocalDigits),
                null,
                extractedMessage
            )
        }
    }

    private fun stripNationalLeadingZero(localDigits: String, country: Country): String {
        if (!localDigits.startsWith("0")) return localDigits
        val withoutZero = localDigits.drop(1)
        // Countries where users commonly type a trunk prefix 0 (Brazil 011..., UK 07..., DE 015..., etc.)
        return if (withoutZero.length in country.minLocalDigits..country.maxLocalDigits) {
            withoutZero
        } else {
            localDigits
        }
    }

    /**
     * Normalizes local input + selected country into E.164 digits (without leading '+').
     * Correctly handles numbers whose area code matches the country dial code (e.g. Brazil DDD 55).
     */
    fun normalizeWithCountry(input: String, country: Country): String? {
        val rawDigits = digits(input)
        if (rawDigits.isEmpty()) return null

        val cleanLocal = stripNationalLeadingZero(rawDigits, country)

        val normalized = if (
            cleanLocal.startsWith(country.code) &&
            cleanLocal.length in (country.code.length + country.minLocalDigits)..(country.code.length + country.maxLocalDigits)
        ) {
            cleanLocal
        } else {
            country.code + cleanLocal
        }

        return normalized.takeIf { it.length in E164_MIN_LENGTH..E164_MAX_LENGTH }
    }

    fun isValidWithCountry(input: String, country: Country): Boolean {
        val normalized = normalizeWithCountry(input, country) ?: return false
        val localPart = normalized.removePrefix(country.code)
        return localPart.length in country.minLocalDigits..country.maxLocalDigits &&
            normalized.length in E164_MIN_LENGTH..E164_MAX_LENGTH
    }

    fun validationHint(input: String, country: Country): String? {
        val rawDigits = digits(input)
        if (rawDigits.isEmpty()) return null
        if (isValidWithCountry(rawDigits, country)) return null

        val localLen = stripNationalLeadingZero(rawDigits, country).let {
            if (it.startsWith(country.code) && it.length > country.maxLocalDigits) {
                it.removePrefix(country.code).length
            } else {
                it.length
            }
        }

        return when {
            localLen < country.minLocalDigits -> {
                val missing = country.minLocalDigits - localLen
                "Faltam pelo menos $missing ${if (missing == 1) "dígito" else "dígitos"} para ${country.name}"
            }
            localLen > country.maxLocalDigits -> {
                "Número longo demais para ${country.name} (máx. ${country.maxLocalDigits} dígitos)"
            }
            else -> "Número inválido para ${country.name}"
        }
    }

    fun formatLocal(localInput: String, country: Country): String {
        val d = digits(localInput)
        if (d.isEmpty()) return ""

        return when (country.iso) {
            "BR" -> formatBrazil(d)
            "US", "CA" -> formatNorthAmerica(d)
            "PT", "ES" -> formatGroupsOfThree(d)
            else -> formatGenericInternational(d)
        }
    }

    private fun formatBrazil(d: String): String = when {
        d.length <= 2 -> d
        d.length <= 6 -> "(${d.substring(0, 2)}) ${d.substring(2)}"
        d.length <= 10 -> "(${d.substring(0, 2)}) ${d.substring(2, 6)}-${d.substring(6)}"
        d.length == 11 -> "(${d.substring(0, 2)}) ${d.substring(2, 7)}-${d.substring(7)}"
        else -> "(${d.substring(0, 2)}) ${d.substring(2, 7)}-${d.substring(7, 11)} ${d.substring(11)}"
    }

    private fun formatNorthAmerica(d: String): String = when {
        d.length <= 3 -> d
        d.length <= 6 -> "(${d.substring(0, 3)}) ${d.substring(3)}"
        d.length <= 10 -> "(${d.substring(0, 3)}) ${d.substring(3, 6)}-${d.substring(6)}"
        else -> "(${d.substring(0, 3)}) ${d.substring(3, 6)}-${d.substring(6, 10)} ${d.substring(10)}"
    }

    private fun formatGroupsOfThree(d: String): String =
        d.chunked(3).joinToString(" ")

    private fun formatGenericInternational(d: String): String = when {
        d.length <= 4 -> d
        d.length <= 7 -> "${d.substring(0, 3)} ${d.substring(3)}"
        d.length <= 10 -> "${d.substring(0, 3)} ${d.substring(3, 6)} ${d.substring(6)}"
        else -> d.chunked(3).joinToString(" ")
    }

    fun resolveCountryFromFullNumber(fullNormalizedNumber: String): Country? {
        val d = digits(fullNormalizedNumber)
        return Countries.byCodeLongestFirst.firstOrNull { country ->
            d.startsWith(country.code) &&
                d.length in (country.code.length + country.minLocalDigits)..(country.code.length + country.maxLocalDigits)
        } ?: Countries.byCodeLongestFirst.firstOrNull { country ->
            d.startsWith(country.code) && d.length > country.code.length + 5
        }
    }

    fun formatFullForDisplay(fullNormalizedNumber: String): String {
        val d = digits(fullNormalizedNumber)
        val country = resolveCountryFromFullNumber(d)
        return if (country != null) {
            val local = d.removePrefix(country.code)
            "${country.flag} ${country.dialCode} ${formatLocal(local, country)}"
        } else {
            "+$d"
        }
    }

    /**
     * Builds a standards-compliant wa.me link with RFC-3986 percent encoding (%20 for spaces).
     */
    fun buildWhatsAppUrl(normalizedPhone: String, message: String = ""): String {
        val cleanPhone = digits(normalizedPhone)
        val trimmedMessage = message.trim()
        return if (trimmedMessage.isNotEmpty()) {
            "https://wa.me/$cleanPhone?text=${encodeUrlComponent(trimmedMessage)}"
        } else {
            "https://wa.me/$cleanPhone"
        }
    }

    /**
     * RFC-3986 percent-encoding (multiplatform, UTF-8 safe, encodes spaces as %20).
     */
    fun encodeUrlComponent(value: String): String {
        val bytes = value.encodeToByteArray()
        val hex = "0123456789ABCDEF"
        return buildString(bytes.size * 2) {
            for (b in bytes) {
                val c = b.toInt() and 0xFF
                val isUnreserved =
                    (c in 'A'.code..'Z'.code) ||
                        (c in 'a'.code..'z'.code) ||
                        (c in '0'.code..'9'.code) ||
                        c == '-'.code || c == '_'.code || c == '.'.code || c == '~'.code
                if (isUnreserved) {
                    append(c.toChar())
                } else {
                    append('%')
                    append(hex[(c ushr 4) and 0x0F])
                    append(hex[c and 0x0F])
                }
            }
        }
    }

    fun decodeUrlComponent(value: String): String {
        val out = ByteArray(value.length)
        var outIndex = 0
        var i = 0
        while (i < value.length) {
            val ch = value[i]
            when {
                ch == '+' -> {
                    out[outIndex++] = ' '.code.toByte()
                    i++
                }
                ch == '%' && i + 2 < value.length -> {
                    val hi = value[i + 1].digitToIntOrNull(16)
                    val lo = value[i + 2].digitToIntOrNull(16)
                    if (hi != null && lo != null) {
                        out[outIndex++] = ((hi shl 4) or lo).toByte()
                        i += 3
                    } else {
                        out[outIndex++] = ch.code.toByte()
                        i++
                    }
                }
                else -> {
                    val encodedChar = ch.toString().encodeToByteArray()
                    for (b in encodedChar) {
                        if (outIndex < out.size) out[outIndex++] = b
                    }
                    i++
                }
            }
        }
        return out.copyOf(outIndex).decodeToString()
    }

    private fun extractQueryParam(url: String, param: String): String? {
        val query = url.substringAfter("?", "").substringBefore("#")
        if (query.isEmpty()) return null
        return query.split("&")
            .firstOrNull { it.startsWith("$param=", ignoreCase = true) }
            ?.substringAfter("=")
    }
}

/**
 * Formats raw digits in real time while preserving exact cursor placement during mid-string edits.
 */
class PhoneVisualTransformation(private val country: Country) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val formatted = PhoneNumberUtils.formatLocal(raw, country)

        val digitToTransformed = IntArray(raw.length + 1)
        val transformedToDigit = IntArray(formatted.length + 1)

        var rawIdx = 0
        for (tIdx in formatted.indices) {
            transformedToDigit[tIdx] = rawIdx
            if (formatted[tIdx].isDigit() && rawIdx < raw.length) {
                digitToTransformed[rawIdx] = tIdx
                rawIdx++
            }
        }
        while (rawIdx <= raw.length) {
            digitToTransformed[rawIdx] = formatted.length
            rawIdx++
        }
        transformedToDigit[formatted.length] = raw.length

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, raw.length)
                return digitToTransformed[clamped]
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formatted.length)
                return transformedToDigit[clamped]
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}
