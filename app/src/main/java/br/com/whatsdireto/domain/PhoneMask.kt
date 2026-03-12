package br.com.whatsdireto.domain

object PhoneMask {

    fun digits(input: String): String = input.filter(Char::isDigit)

    fun format(input: String): String {
        val d = digits(input).take(11)

        return when {
            d.isEmpty() -> ""
            d.length <= 2 -> d
            d.length <= 6 -> "(${d.substring(0, 2)}) ${d.substring(2)}"
            d.length <= 10 -> "(${d.substring(0, 2)}) ${d.substring(2, 6)}-${d.substring(6)}"
            else -> "(${d.substring(0, 2)}) ${d.substring(2, 7)}-${d.substring(7)}"
        }
    }

    fun formatForDisplay(input: String): String {
        val d = digits(input)
        return when {
            d.length in 12..13 && d.startsWith("55") -> format(d.drop(2))
            d.length in 10..11 -> format(d)
            else -> format(d)
        }
    }

    fun toWhatsAppPhone(input: String): String? {
        val d = digits(input)

        return when {
            d.length in 10..11 -> "55$d"
            d.length in 12..13 && d.startsWith("55") -> d
            else -> null
        }
    }

    fun isValid(input: String): Boolean = toWhatsAppPhone(input) != null
}