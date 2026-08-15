package com.familyos.core.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Supported UI languages and AppCompat locale application.
 */
object AppLocale {

    /** English. */
    const val EN = "en"

    /** Russian. */
    const val RU = "ru"

    /** Serbian (Latin script). */
    const val SR = "sr"

    /** Languages shown in Settings. */
    val supported: List<Pair<String, String>> = listOf(
        EN to "English",
        RU to "Русский",
        SR to "Srpski",
    )

    /**
     * Applies [languageTag] app-wide. Persisted by AppCompat across process restarts.
     */
    fun apply(languageTag: String) {
        val normalized = normalize(languageTag)
        val locales = LocaleListCompat.forLanguageTags(normalized)
        AppCompatDelegate.setApplicationLocales(locales)
    }

    /** Maps stored tags to a supported BCP-47 tag. */
    fun normalize(languageTag: String): String {
        val primary = languageTag.trim().lowercase().substringBefore('-').substringBefore('_')
        return when (primary) {
            RU, "rus" -> RU
            SR, "srp", "sh" -> SR
            EN, "eng" -> EN
            else -> if (languageTag.isBlank()) EN else languageTag
        }
    }
}
