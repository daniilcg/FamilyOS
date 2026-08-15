from pathlib import Path

UI = Path(r"d:/Projects/Develop/FamilyOS/core_ui/src/main/java/com/familyos/core/ui")

# Remove broken alias files
for p in [
    UI / "ModifierAliases.kt",
    UI / "theme" / "ModifierAliases.kt",
]:
    if p.exists():
        p.unlink()

components = {
"FamilyOsScaffold.kt": '''package com.familyos.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Standard FamilyOS scaffold with optional top bar, FAB, and snackbar host.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyOsScaffold(
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = androidx.compose.ui.Modifier.Modifier.fillMaxSize(),
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        content = { padding ->
            Box(modifier = androidx.compose.ui.Modifier.Modifier.padding(padding)) {
                content(padding)
            }
        },
    )
}
'''.replace(
    "androidx.compose.ui.modifier.modifier",
    "androidx.compose.ui.modifier." + "M" + "odifier",
),

"StateViews.kt": '''package com.familyos.core.ui.components

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

private val M = androidx.compose.ui.modifier.''' + ("M" + "odifier") + '''

@Composable
fun EmptyState(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = M.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Outlined.Inbox, null, modifier = M.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = M.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(modifier = M.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = M.height(20.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
fun LoadingState(message: String = "Loading…") {
    Column(M.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(modifier = M.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun ErrorState(message: String, retryLabel: String = "Retry", onRetry: (() -> Unit)? = null) {
    Column(M.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.WarningAmber, null, modifier = M.size(56.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(modifier = M.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        if (onRetry != null) {
            Spacer(modifier = M.height(20.dp))
            Button(onClick = onRetry) { Text(retryLabel) }
        }
    }
}
''',
}

# Fix StateViews - I embedded broken string concat in the dict above. Write properly:
Mref = "androidx.compose.ui.modifier." + "M" + "odifier"
components["StateViews.kt"] = f'''package com.familyos.core.ui.components

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

@Composable
fun EmptyState(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {{
    Column(
        modifier = {Mref}.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {{
        Icon(Icons.Outlined.Inbox, null, modifier = {Mref}.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = {Mref}.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(modifier = {Mref}.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {{
            Spacer(modifier = {Mref}.height(20.dp))
            Button(onClick = onAction) {{ Text(actionLabel) }}
        }}
    }}
}}

@Composable
fun LoadingState(message: String = "Loading…") {{
    Column(
        modifier = {Mref}.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {{
        CircularProgressIndicator()
        Spacer(modifier = {Mref}.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }}
}}

@Composable
fun ErrorState(message: String, retryLabel: String = "Retry", onRetry: (() -> Unit)? = null) {{
    Column(
        modifier = {Mref}.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {{
        Icon(Icons.Outlined.WarningAmber, null, modifier = {Mref}.size(56.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(modifier = {Mref}.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        if (onRetry != null) {{
            Spacer(modifier = {Mref}.height(20.dp))
            Button(onClick = onRetry) {{ Text(retryLabel) }}
        }}
    }}
}}
'''

components["FormatText.kt"] = f'''package com.familyos.core.ui.components

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

@Composable
fun CurrencyText(amount: Double, currencyCode: String, locale: Locale = Locale.getDefault()) {{
    val formatted = remember(amount, currencyCode, locale) {{
        val format = NumberFormat.getCurrencyInstance(locale)
        runCatching {{ format.currency = Currency.getInstance(currencyCode) }}
        format.format(amount)
    }}
    Text(formatted, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
}}

@Composable
fun DateText(epochMillis: Long, showTime: Boolean = false, zoneId: ZoneId = ZoneId.systemDefault()) {{
    val formatted = remember(epochMillis, showTime, zoneId) {{
        val formatter = if (showTime) DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(zoneId)
        else DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(zoneId)
        formatter.format(Instant.ofEpochMilli(epochMillis))
    }}
    Text(formatted, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}}
'''

components["CommonUi.kt"] = f'''package com.familyos.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

@Composable
fun FamilyLoading() {{
    Box(modifier = {Mref}.fillMaxSize(), contentAlignment = Alignment.Center) {{
        CircularProgressIndicator()
    }}
}}

@Composable
fun FamilyEmptyState(message: String) {{
    Box(modifier = {Mref}.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {{
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }}
}}
'''

# Fix FamilyOsScaffold with proper Mref
components["FamilyOsScaffold.kt"] = f'''package com.familyos.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyOsScaffold(
    topBar: @Composable () -> Unit = {{}},
    bottomBar: @Composable () -> Unit = {{}},
    floatingActionButton: @Composable () -> Unit = {{}},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    snackbarHostState: SnackbarHostState = remember {{ SnackbarHostState() }},
    content: @Composable (PaddingValues) -> Unit,
) {{
    Scaffold(
        modifier = {Mref}.fillMaxSize(),
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        snackbarHost = {{ SnackbarHost(snackbarHostState) }},
        containerColor = MaterialTheme.colorScheme.background,
        content = {{ padding ->
            Box(modifier = {Mref}.padding(padding)) {{
                content(padding)
            }}
        }},
    )
}}
'''

for name, content in components.items():
    (UI / "components" / name).write_text(content, encoding="utf-8", newline="\n")
    print("wrote", name)

# Strip Modifier type params from remaining files by regenerating simpler versions without Modifier params
for p in (UI / "components").glob("*.kt"):
    if p.name in components:
        continue
    t = p.read_text(encoding="utf-8")
    # Remove imports of Modifier/FosModifier
    lines = [ln for ln in t.splitlines() if "FosModifier" not in ln and "ui.modifier" not in ln]
    t2 = "\n".join(lines) + "\n"
    # Replace `: FosModifier = FosModifier` and similar
    import re
    t2 = re.sub(r",?\s*modifier:\s*\S+\s*=\s*\S+", "", t2)
    t2 = t2.replace("FosModifier.", Mref + ".")
    t2 = t2.replace("Modifier = modifier", f"modifier = {Mref}")
    # Fix broken signatures leftover
    p.write_text(t2, encoding="utf-8", newline="\n")
    print("cleaned", p.name)

print("Mref bytes", [hex(ord(c)) for c in Mref.split(".")[-1]])
