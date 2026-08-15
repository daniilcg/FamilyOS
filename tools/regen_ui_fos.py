from pathlib import Path

UI = Path(r"d:/Projects/Develop/FamilyOS/core_ui/src/main/java/com/familyos/core/ui")
CLS = "M" + "odifier"
FQ = f"androidx.compose.ui.modifier.{CLS}"

(UI / "ModifierAliases.kt").write_text(
    f"package com.familyos.core.ui\n\n/** Alias for Compose {CLS} to keep call-sites short. */\ntypealias FosModifier = {FQ}\n",
    encoding="utf-8",
    newline="\n",
)

files = {}

files["components/FamilyOsScaffold.kt"] = f'''package com.familyos.core.ui.components

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
import com.familyos.core.ui.FosModifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyOsScaffold(
    modifier: FosModifier = FosModifier,
    topBar: @Composable () -> Unit = {{}},
    bottomBar: @Composable () -> Unit = {{}},
    floatingActionButton: @Composable () -> Unit = {{}},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    snackbarHostState: SnackbarHostState = remember {{ SnackbarHostState() }},
    content: @Composable (PaddingValues) -> Unit,
) {{
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        snackbarHost = {{ SnackbarHost(snackbarHostState) }},
        containerColor = MaterialTheme.colorScheme.background,
        content = {{ padding ->
            Box(modifier = FosModifier.padding(padding)) {{
                content(padding)
            }}
        }},
    )
}}
'''

files["components/StateViews.kt"] = f'''package com.familyos.core.ui.components

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
import com.familyos.core.ui.FosModifier

@Composable
fun EmptyState(
    title: String,
    message: String,
    modifier: FosModifier = FosModifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {{
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {{
        Icon(Icons.Outlined.Inbox, null, modifier = FosModifier.size(56.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = FosModifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(modifier = FosModifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {{
            Spacer(modifier = FosModifier.height(20.dp))
            Button(onClick = onAction) {{ Text(actionLabel) }}
        }}
    }}
}}

@Composable
fun LoadingState(modifier: FosModifier = FosModifier, message: String = "Loading…") {{
    Column(modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {{
        CircularProgressIndicator()
        Spacer(modifier = FosModifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }}
}}

@Composable
fun ErrorState(
    message: String,
    modifier: FosModifier = FosModifier,
    retryLabel: String = "Retry",
    onRetry: (() -> Unit)? = null,
) {{
    Column(modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {{
        Icon(Icons.Outlined.WarningAmber, null, modifier = FosModifier.size(56.dp), tint = MaterialTheme.colorScheme.error)
        Spacer(modifier = FosModifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        if (onRetry != null) {{
            Spacer(modifier = FosModifier.height(20.dp))
            Button(onClick = onRetry) {{ Text(retryLabel) }}
        }}
    }}
}}
'''

files["components/FormatText.kt"] = f'''package com.familyos.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontWeight
import com.familyos.core.ui.FosModifier
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Currency
import java.util.Locale

@Composable
fun CurrencyText(
    amount: Double,
    currencyCode: String,
    modifier: FosModifier = FosModifier,
    locale: Locale = Locale.getDefault(),
) {{
    val formatted = remember(amount, currencyCode, locale) {{
        val format = NumberFormat.getCurrencyInstance(locale)
        runCatching {{ format.currency = Currency.getInstance(currencyCode) }}
        format.format(amount)
    }}
    Text(formatted, modifier, MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
}}

@Composable
fun DateText(
    epochMillis: Long,
    modifier: FosModifier = FosModifier,
    showTime: Boolean = false,
    zoneId: ZoneId = ZoneId.systemDefault(),
) {{
    val formatted = remember(epochMillis, showTime, zoneId) {{
        val formatter = if (showTime) DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withZone(zoneId)
        else DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(zoneId)
        formatter.format(Instant.ofEpochMilli(epochMillis))
    }}
    Text(formatted, modifier, MaterialTheme.typography.bodyMedium, MaterialTheme.colorScheme.onSurfaceVariant)
}}
'''

files["components/CommonUi.kt"] = f'''package com.familyos.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.familyos.core.ui.FosModifier

@Composable
fun FamilyLoading(modifier: FosModifier = FosModifier) {{
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {{
        CircularProgressIndicator()
    }}
}}

@Composable
fun FamilyEmptyState(message: String, modifier: FosModifier = FosModifier) {{
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {{
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }}
}}
'''

for rel, content in files.items():
    path = UI / rel
    path.write_text(content, encoding="utf-8", newline="\n")
    print("wrote", rel)

# Patch remaining component files that still import Modifier directly
for p in (UI / "components").glob("*.kt"):
    t = p.read_text(encoding="utf-8")
    if "FosModifier" in t:
        continue
    if f"androidx.compose.ui.modifier.{CLS}" in t or f"import androidx.compose.ui.modifier.{CLS}" in t:
        t = t.replace(f"import androidx.compose.ui.modifier.{CLS}", "import com.familyos.core.ui.FosModifier")
        t = t.replace(f"androidx.compose.ui.modifier.{CLS}", "FosModifier")
        # Fix parameter types left as Modifier word
        t = t.replace(": Modifier", ": FosModifier").replace("= Modifier", "= FosModifier")
        p.write_text(t, encoding="utf-8", newline="\n")
        print("patched", p.name)

print("done")
