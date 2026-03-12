package br.com.whatsdireto.domain

object PhoneFormatter {

    /**
     * Regras adotadas:
     * - Remove tudo que não for número.
     * - Se vier com 10 ou 11 dígitos, assume Brasil e prefixa 55.
     * - Se vier com 12 ou 13 dígitos e começar com 55, mantém.
     * - Retorna null se não estiver num formato minimamente válido.
     */
    fun normalize(input: String): String? {
        var digits = input.filter(Char::isDigit)

        if (digits.isBlank()) return null

        digits = digits.trimStart('0')

        return when {
            digits.length in 10..11 -> "55$digits"
            digits.length in 12..13 && digits.startsWith("55") -> digits
            else -> null
        }
    }
}