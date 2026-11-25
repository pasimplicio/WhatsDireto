package br.com.whatsdireto.domain

data class Country(
    val iso: String,
    val code: String,
    val name: String,
    val flag: String,
    val dialCode: String = "+$code",
    val exampleLocal: String = "11 99999-9999",
    val minLocalDigits: Int = 8,
    val maxLocalDigits: Int = 12
)

object Countries {
    val all: List<Country> = listOf(
        Country("BR", "55", "Brasil", "🇧🇷", "+55", "11 99999-9999", minLocalDigits = 10, maxLocalDigits = 11),
        Country("US", "1", "Estados Unidos", "🇺🇸", "+1", "(415) 555-0199", minLocalDigits = 10, maxLocalDigits = 10),
        Country("PT", "351", "Portugal", "🇵🇹", "+351", "912 345 678", minLocalDigits = 9, maxLocalDigits = 9),
        Country("AR", "54", "Argentina", "🇦🇷", "+54", "9 11 2345-6789", minLocalDigits = 10, maxLocalDigits = 11),
        Country("ES", "34", "Espanha", "🇪🇸", "+34", "612 345 678", minLocalDigits = 9, maxLocalDigits = 9),
        Country("GB", "44", "Reino Unido", "🇬🇧", "+44", "7700 900123", minLocalDigits = 9, maxLocalDigits = 10),
        Country("DE", "49", "Alemanha", "🇩🇪", "+49", "151 23456789", minLocalDigits = 9, maxLocalDigits = 12),
        Country("FR", "33", "França", "🇫🇷", "+33", "6 12 34 56 78", minLocalDigits = 9, maxLocalDigits = 9),
        Country("IT", "39", "Itália", "🇮🇹", "+39", "312 345 6789", minLocalDigits = 9, maxLocalDigits = 11),
        Country("MX", "52", "México", "🇲🇽", "+52", "55 1234 5678", minLocalDigits = 10, maxLocalDigits = 10),
        Country("CL", "56", "Chile", "🇨🇱", "+56", "9 8765 4321", minLocalDigits = 9, maxLocalDigits = 9),
        Country("CO", "57", "Colômbia", "🇨🇴", "+57", "310 123 4567", minLocalDigits = 10, maxLocalDigits = 10),
        Country("PE", "51", "Peru", "🇵🇪", "+51", "987 654 321", minLocalDigits = 9, maxLocalDigits = 9),
        Country("UY", "598", "Uruguai", "🇺🇾", "+598", "91 234 567", minLocalDigits = 8, maxLocalDigits = 9),
        Country("PY", "595", "Paraguai", "🇵🇾", "+595", "981 123 456", minLocalDigits = 9, maxLocalDigits = 9),
        Country("BO", "591", "Bolívia", "🇧🇴", "+591", "7123 4567", minLocalDigits = 8, maxLocalDigits = 8),
        Country("EC", "593", "Equador", "🇪🇨", "+593", "99 123 4567", minLocalDigits = 9, maxLocalDigits = 9),
        Country("VE", "58", "Venezuela", "🇻🇪", "+58", "412 1234567", minLocalDigits = 10, maxLocalDigits = 10),
        Country("CA", "1", "Canadá", "🇨🇦", "+1", "(416) 555-0199", minLocalDigits = 10, maxLocalDigits = 10),
        Country("JP", "81", "Japão", "🇯🇵", "+81", "90 1234 5678", minLocalDigits = 9, maxLocalDigits = 10),
        Country("CN", "86", "China", "🇨🇳", "+86", "138 0013 8000", minLocalDigits = 11, maxLocalDigits = 11),
        Country("KR", "82", "Coreia do Sul", "🇰🇷", "+82", "10 1234 5678", minLocalDigits = 9, maxLocalDigits = 10),
        Country("IN", "91", "Índia", "🇮🇳", "+91", "98765 43210", minLocalDigits = 10, maxLocalDigits = 10),
        Country("AU", "61", "Austrália", "🇦🇺", "+61", "412 345 678", minLocalDigits = 9, maxLocalDigits = 9),
        Country("IE", "353", "Irlanda", "🇮🇪", "+353", "85 123 4567", minLocalDigits = 9, maxLocalDigits = 9),
        Country("CH", "41", "Suíça", "🇨🇭", "+41", "79 123 45 67", minLocalDigits = 9, maxLocalDigits = 9),
        Country("NL", "31", "Países Baixos", "🇳🇱", "+31", "6 12345678", minLocalDigits = 9, maxLocalDigits = 9),
        Country("BE", "32", "Bélgica", "🇧🇪", "+32", "470 12 34 56", minLocalDigits = 9, maxLocalDigits = 9),
        Country("SE", "46", "Suécia", "🇸🇪", "+46", "70 123 45 67", minLocalDigits = 8, maxLocalDigits = 10),
        Country("PL", "48", "Polônia", "🇵🇱", "+48", "501 234 567", minLocalDigits = 9, maxLocalDigits = 9),
        Country("AE", "971", "Emirados Árabes", "🇦🇪", "+971", "50 123 4567", minLocalDigits = 9, maxLocalDigits = 9),
        Country("IL", "972", "Israel", "🇮🇱", "+972", "50 123 4567", minLocalDigits = 9, maxLocalDigits = 9),
        Country("ZA", "27", "África do Sul", "🇿🇦", "+27", "82 123 4567", minLocalDigits = 9, maxLocalDigits = 9),
        Country("AO", "244", "Angola", "🇦🇴", "+244", "923 123 456", minLocalDigits = 9, maxLocalDigits = 9),
        Country("MZ", "258", "Moçambique", "🇲🇿", "+258", "82 123 4567", minLocalDigits = 9, maxLocalDigits = 9),
        Country("CV", "238", "Cabo Verde", "🇨🇻", "+238", "991 12 34", minLocalDigits = 7, maxLocalDigits = 7),
        Country("GW", "245", "Guiné-Bissau", "🇬🇼", "+245", "955 123 456", minLocalDigits = 7, maxLocalDigits = 9),
        Country("ST", "239", "São Tomé e Príncipe", "🇸🇹", "+239", "990 1234", minLocalDigits = 7, maxLocalDigits = 7),
        Country("SN", "221", "Senegal", "🇸🇳", "+221", "77 123 45 67", minLocalDigits = 9, maxLocalDigits = 9),
        Country("TL", "670", "Timor-Leste", "🇹🇱", "+670", "7712 3456", minLocalDigits = 7, maxLocalDigits = 8)
    ).sortedBy { it.name }

    val default: Country = all.first { it.iso == "BR" }

    val popular: List<Country> = listOf("BR", "US", "PT", "AR", "ES", "GB")
        .mapNotNull { iso -> all.firstOrNull { it.iso == iso } }

    /**
     * Ordered by dial code length descending so longer prefixes (e.g. +351, +598)
     * are always matched before shorter prefixes (e.g. +1, +55).
     * When dial codes share the same prefix (e.g. US/CA +1), prioritizes US.
     */
    val byCodeLongestFirst: List<Country> = all.sortedWith(
        compareByDescending<Country> { it.code.length }
            .thenBy { if (it.iso == "BR" || it.iso == "US") 0 else 1 }
            .thenBy { it.name }
    )

    fun findByIso(iso: String): Country? = all.firstOrNull { it.iso.equals(iso, ignoreCase = true) }

    fun search(query: String): List<Country> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return all
        val digitsQuery = trimmed.filter(Char::isDigit)
        return all.filter { country ->
            country.name.contains(trimmed, ignoreCase = true) ||
                country.iso.contains(trimmed, ignoreCase = true) ||
                country.dialCode.contains(trimmed, ignoreCase = true) ||
                (digitsQuery.isNotEmpty() && country.code.startsWith(digitsQuery))
        }
    }
}
