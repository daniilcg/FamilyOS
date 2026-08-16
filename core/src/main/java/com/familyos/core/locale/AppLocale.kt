package com.familyos.core.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * Supported UI languages and AppCompat locale application.
 */
object AppLocale {

    const val EN = "en"
    const val RU = "ru"
    const val SR = "sr"
    const val DE = "de"
    const val FR = "fr"
    const val ES = "es"
    const val UK = "uk"

    /**
     * Languages shown in Settings (original set + Russian + Serbian).
     */
    val supported: List<Pair<String, String>> = listOf(
        EN to "English",
        RU to "Русский",
        SR to "Srpski",
        DE to "Deutsch",
        FR to "Français",
        ES to "Español",
        UK to "Українська",
    )

    /** Applies [languageTag] app-wide (persisted by AppCompat). Must run on the main thread. */
    fun apply(languageTag: String) {
        val normalized = normalize(languageTag)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(normalized))
    }

    /** Maps stored / system tags onto a supported BCP-47 primary tag. */
    fun normalize(languageTag: String): String {
        if (languageTag.isBlank()) return EN
        val primary = languageTag.trim().lowercase()
            .substringBefore('-')
            .substringBefore('_')
        return when (primary) {
            RU, "rus" -> RU
            SR, "srp", "sh" -> SR
            DE, "deu", "ger" -> DE
            FR, "fra", "fre" -> FR
            ES, "spa" -> ES
            UK, "ukr" -> UK
            EN, "eng" -> EN
            else -> if (supported.any { it.first == primary }) primary else EN
        }
    }

    /**
     * Active UI language. Prefers AppCompat application locales (what Settings sets),
     * then falls back to the process default.
     */
    fun currentLanguage(): String {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (!appLocales.isEmpty) {
            val tag = appLocales.toLanguageTags()
            if (tag.isNotBlank()) return normalize(tag)
        }
        return normalize(Locale.getDefault().toLanguageTag())
    }
}
