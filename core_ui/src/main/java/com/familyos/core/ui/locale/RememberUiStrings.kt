package com.familyos.core.ui.locale

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.familyos.core.locale.AppLocale
import com.familyos.core.locale.UiStrings
import com.familyos.core.locale.UiStringsCatalog

/**
 * Returns [UiStrings] for the active app language and recomposes when locale changes.
 */
@Composable
fun rememberUiStrings(): UiStrings {
    LocalConfiguration.current // recompose on locale change
    return UiStringsCatalog.forLang(AppLocale.currentLanguage())
}
