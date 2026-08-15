from pathlib import Path

PKG = "androidx.compose.ui.modifier"
CLS = "Modifier"  # WRONG placeholder replaced below
CLS = "M" + "odifier"
FQCN = PKG + "." + CLS
print("FQCN", FQCN, [hex(ord(c)) for c in CLS])

scaffold = f'''package com.familyos.core.ui.components

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
    modifier: {FQCN} = {FQCN},
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
            Box(modifier = {FQCN}.padding(padding)) {{
                content(padding)
            }}
        }},
    )
}}
'''
Path(r"d:/Projects/Develop/FamilyOS/core_ui/src/main/java/com/familyos/core/ui/components/FamilyOsScaffold.kt").write_text(scaffold, encoding="utf-8", newline="\n")

# Fix all files: replace wrong lowercase class imports and FQCNs
wrong = PKG + ".modifier"
right = FQCN
root = Path(r"d:/Projects/Develop/FamilyOS/core_ui/src/main/java")
for p in root.rglob("*.kt"):
    t = p.read_text(encoding="utf-8")
    # Only replace the CLASS suffix after package, not the package itself
    nt = t.replace("androidx.compose.ui.modifier.modifier", right)
    # parameter types written as just Modifier should be fine if imported
    import_line = "import " + right
    lines = []
    for line in nt.splitlines():
        if line.strip().startswith("import androidx.compose.ui.modifier"):
            lines.append(import_line)
        else:
            lines.append(line)
    nt = "\n".join(lines) + "\n"
    if "Modifier: Modifier" in nt or "= Modifier" in nt or "Modifier.Modifier" in nt:
        # ensure import exists
        if import_line not in nt and ("Modifier: Modifier" in nt or "= Modifier" in nt):
            # insert after package
            parts = nt.split("\n", 2)
            # find first import block
            nt = nt.replace("package com.familyos.core.ui.components\n", "package com.familyos.core.ui.components\n\n" + import_line + "\n", 1)
            nt = nt.replace("package com.familyos.core.ui.theme\n", "package com.familyos.core.ui.theme\n\n" + import_line + "\n", 1)
    p.write_text(nt, encoding="utf-8", newline="\n")
    for line in nt.splitlines():
        if "compose.ui.modifier" in line:
            print(p.name, repr(line.split(".")[-1]))
