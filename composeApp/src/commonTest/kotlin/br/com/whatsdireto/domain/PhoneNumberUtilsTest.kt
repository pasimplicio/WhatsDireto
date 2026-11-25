package br.com.whatsdireto.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PhoneNumberUtilsTest {

    @Test
    fun brazilAreaCode55DoesNotDropCountryCode() {
        val brazil = Countries.findByIso("BR")!!
        // Santa Maria (RS) uses DDD 55: 55 99999-8888 (11 local digits)
        val normalized = PhoneNumberUtils.normalizeWithCountry("55999998888", brazil)
        assertEquals("5555999998888", normalized)
        assertTrue(PhoneNumberUtils.isValidWithCountry("55999998888", brazil))
    }

    @Test
    fun brazilNumberAlreadyWithCountryCodeIsNotDuplicated() {
        val brazil = Countries.findByIso("BR")!!
        val normalized = PhoneNumberUtils.normalizeWithCountry("5511999998888", brazil)
        assertEquals("5511999998888", normalized)
    }

    @Test
    fun brazilLocalNumberStartingWith1IsNotMisclassifiedAsUs() {
        val brazil = Countries.findByIso("BR")!!
        val parsed = PhoneNumberUtils.extractFromRawText("(11) 99999-8888", brazil)
        assertNull(parsed.detectedCountry)
        assertEquals("11999998888", parsed.localDigits)
    }

    @Test
    fun explicitInternationalPrefixDetectsLongestCountryCodeFirst() {
        val brazil = Countries.findByIso("BR")!!
        val portugal = PhoneNumberUtils.extractFromRawText("+351 912 345 678", brazil)
        assertEquals("PT", portugal.detectedCountry?.iso)
        assertEquals("912345678", portugal.localDigits)

        val uruguay = PhoneNumberUtils.extractFromRawText("+598 91 234 567", brazil)
        assertEquals("UY", uruguay.detectedCountry?.iso)
        assertEquals("91234567", uruguay.localDigits)
    }

    @Test
    fun parsesWaMeLinkWithEncodedMessage() {
        val brazil = Countries.findByIso("BR")!!
        val parsed = PhoneNumberUtils.extractFromRawText(
            "https://wa.me/5511988887777?text=Ol%C3%A1%20tudo%20bem%3F",
            brazil
        )
        assertEquals("BR", parsed.detectedCountry?.iso)
        assertEquals("11988887777", parsed.localDigits)
        assertEquals("Olá tudo bem?", parsed.extractedMessage)
    }

    @Test
    fun buildsRfc3986UrlWithPercent20Spaces() {
        val url = PhoneNumberUtils.buildWhatsAppUrl("5511999998888", "Olá mundo! Tudo bem?")
        assertEquals("https://wa.me/5511999998888?text=Ol%C3%A1%20mundo%21%20Tudo%20bem%3F", url)
        assertFalse(url.contains("+"))
    }

    @Test
    fun buildsDirectWebAndDesktopUrls() {
        val webUrl = PhoneNumberUtils.buildWhatsAppWebUrl("5511999998888", "Olá Web")
        assertEquals("https://web.whatsapp.com/send?phone=5511999998888&text=Ol%C3%A1%20Web", webUrl)

        val webNoMsg = PhoneNumberUtils.buildWhatsAppWebUrl("5511999998888")
        assertEquals("https://web.whatsapp.com/send?phone=5511999998888", webNoMsg)

        val desktopUrl = PhoneNumberUtils.buildWhatsAppDesktopUrl("5511999998888", "Olá Desktop")
        assertEquals("whatsapp://send?phone=5511999998888&text=Ol%C3%A1%20Desktop", desktopUrl)

        val desktopNoMsg = PhoneNumberUtils.buildWhatsAppDesktopUrl("5511999998888")
        assertEquals("whatsapp://send?phone=5511999998888", desktopNoMsg)
    }

    @Test
    fun generatesValidQrCodeMatrixForWhatsAppLink() {
        val url = PhoneNumberUtils.buildWhatsAppUrl("5511999998888", "Oi")
        val qr = QrCodeMatrix.encode(url)
        assertNotNull(qr)
        assertTrue(qr.size >= 21)
        // Top-left finder pattern center (3,3) must always be dark
        assertTrue(qr[3, 3])
    }
}
