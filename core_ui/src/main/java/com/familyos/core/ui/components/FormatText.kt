package com.familyos.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

/** Formats a monetary amount using currencyCode. */
@Composable
fun CurrencyText(amount: Double, currencyCode: String, locale: Locale = Locale.getDefault()) {
    val formatted = remember(amount, currencyCode, locale) {
        val format = NumberFormat.getCurrencyInstance(locale)
        runCatching { format.currency = Currency.getInstance(currencyCode) }
        format.format(amount)
    }
    Text(formatted, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
}

/** Formats epoch-millis as a localized date. */
@Composable
fun DateText(epochMillis: Long, showTime: Boolean = false, zoneId: ZoneId = ZoneId.systemDefault()) {
    val formatted = remember(epochMillis, showTime, zoneId) {
        val formatter = if (showTime) {
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(zoneId)
        } else {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(zoneId)
        }
        formatter.format(Instant.ofEpochMilli(epochMillis))
    }
    Text(formatted, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
