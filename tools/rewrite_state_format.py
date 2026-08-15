from pathlib import Path

FQ = "androidx.compose.ui.modifier." + "M" + "odifier"

Path(r"d:/Projects/Develop/FamilyOS/core_ui/src/main/java/com/familyos/core/ui/components/StateViews.kt").write_text(
    f"""package com.familyos.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Empty-state placeholder with optional action. */
@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: {FQ} = {FQ},
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {{
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {{
        Icon(
            imageVector = Icons.Outlined.Inbox,
            contentDescription = null,
            modifier = {FQ}.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = {FQ}.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(modifier = {FQ}.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {{
            Spacer(modifier = {FQ}.height(20.dp))
            Button(onClick = onAction) {{ Text(actionLabel) }}
        }}
    }}
}}

/** Full-screen loading indicator with optional label. */
@Composable
fun LoadingState(
    modifier: {FQ} = {FQ},
    message: String = "Loading…",
) {{
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {{
        CircularProgressIndicator()
        Spacer(modifier = {FQ}.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }}
}}

/** Error-state placeholder with retry action. */
@Composable
fun ErrorState(
    message: String,
    modifier: {FQ} = {FQ},
    retryLabel: String = "Retry",
    onRetry: (() -> Unit)? = null,
) {{
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {{
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            modifier = {FQ}.size(56.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(modifier = {FQ}.height(16.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {{
            Spacer(modifier = {FQ}.height(20.dp))
            Button(onClick = onRetry) {{ Text(retryLabel) }}
        }}
    }}
}}
""",
    encoding="utf-8",
    newline="\n",
)

Path(r"d:/Projects/Develop/FamilyOS/core_ui/src/main/java/com/familyos/core/ui/components/FormatText.kt").write_text(
    f"""package com.familyos.core.ui.components

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
fun CurrencyText(
    amount: Double,
    currencyCode: String,
    modifier: {FQ} = {FQ},
    locale: Locale = Locale.getDefault(),
) {{
    val formatted = remember(amount, currencyCode, locale) {{
        val format = NumberFormat.getCurrencyInstance(locale)
        runCatching {{ format.currency = Currency.getInstance(currencyCode) }}
        format.format(amount)
    }}
    Text(
        text = formatted,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}}

/** Formats epoch-millis as a localized date (and optional time). */
@Composable
fun DateText(
    epochMillis: Long,
    modifier: {FQ} = {FQ},
    showTime: Boolean = false,
    zoneId: ZoneId = ZoneId.systemDefault(),
) {{
    val formatted = remember(epochMillis, showTime, zoneId) {{
        val formatter = if (showTime) {{
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(zoneId)
        }} else {{
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(zoneId)
        }}
        formatter.format(Instant.ofEpochMilli(epochMillis))
    }}
    Text(
        text = formatted,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}}
""",
    encoding="utf-8",
    newline="\n",
)

print("ok", FQ)
