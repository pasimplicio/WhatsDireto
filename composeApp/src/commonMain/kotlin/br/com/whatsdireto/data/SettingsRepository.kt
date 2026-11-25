package br.com.whatsdireto.data

import br.com.whatsdireto.domain.AppThemeMode
import br.com.whatsdireto.domain.Countries
import br.com.whatsdireto.domain.Country
import br.com.whatsdireto.domain.WhatsAppAppPreference

private const val KEY_TUTORIAL_SEEN = "tutorial_seen_v2"
private const val KEY_THEME_MODE = "theme_mode_v2"
private const val KEY_COUNTRY_ISO = "country_iso_v2"
private const val KEY_WHATSAPP_PREF = "whatsapp_pref_v2"

class SettingsRepository(private val storage: KeyValueStorage) {

    fun hasSeenTutorial(): Boolean = storage.getBoolean(KEY_TUTORIAL_SEEN, false)

    fun setTutorialSeen(seen: Boolean) {
        storage.putBoolean(KEY_TUTORIAL_SEEN, seen)
    }

    fun getThemeMode(): AppThemeMode {
        val saved = storage.getString(KEY_THEME_MODE)
        return AppThemeMode.entries.firstOrNull { it.name == saved } ?: AppThemeMode.SYSTEM
    }

    fun setThemeMode(mode: AppThemeMode) {
        storage.putString(KEY_THEME_MODE, mode.name)
    }

    fun getSelectedCountry(): Country {
        val iso = storage.getString(KEY_COUNTRY_ISO)
        return iso?.let(Countries::findByIso) ?: Countries.default
    }

    fun setSelectedCountry(country: Country) {
        storage.putString(KEY_COUNTRY_ISO, country.iso)
    }

    fun getWhatsAppPreference(): WhatsAppAppPreference {
        val saved = storage.getString(KEY_WHATSAPP_PREF)
        return WhatsAppAppPreference.entries.firstOrNull { it.name == saved }
            ?: WhatsAppAppPreference.AUTO
    }

    fun setWhatsAppPreference(pref: WhatsAppAppPreference) {
        storage.putString(KEY_WHATSAPP_PREF, pref.name)
    }
}
