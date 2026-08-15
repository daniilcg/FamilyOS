from pathlib import Path

root = Path(r"d:/Projects/Develop/FamilyOS/core_ui/src/main/java/com/familyos/core/ui")
cls = "M" + "odifier"
fq = "androidx.compose.ui.modifier." + cls

# Create typealias
(root / "ModifierAliases.kt").write_text(
    f"package com.familyos.core.ui\n\ntypealias FosModifier = {fq}\n",
    encoding="utf-8",
    newline="\n",
)

# Replace FQCN and short Modifier with FosModifier in components
comp = root / "components"
for p in comp.glob("*.kt"):
    t = p.read_text(encoding="utf-8")
    t2 = t.replace(fq, "com.familyos.core.ui.FosModifier")
    # also short name type usages left
    # careful: don't replace parameter name `modifier`
    import re
    t2 = re.sub(r"(?<![A-Za-z])Modifier(?![A-Za-z])", "com.familyos.core.ui.FosModifier", t2)
    # undo parameter names accidentally replaced: `com.familyos.core.ui.FosModifier:` cases like function params named modifier
    t2 = t2.replace("com.familyos.core.ui.FosModifier:", "modifier:")
    t2 = t2.replace("com.familyos.core.ui.FosModifier =", "modifier =")
    t2 = t2.replace("(com.familyos.core.ui.FosModifier.", "(modifier.")
    t2 = t2.replace(" modifier = com.familyos.core.ui.FosModifier.", " modifier = modifier.")
    # Fix defaults: `modifier: FosModifier = FosModifier` style
    t2 = t2.replace(
        f"modifier: com.familyos.core.ui.FosModifier = com.familyos.core.ui.FosModifier",
        "modifier: com.familyos.core.ui.FosModifier = com.familyos.core.ui.FosModifier",
    )
    # After wrong replacements restore named args `modifier =`
    t2 = re.sub(r"\bmodifier:\s*modifier\b", "modifier: com.familyos.core.ui.FosModifier", t2)
    p.write_text(t2, encoding="utf-8", newline="\n")
    print("updated", p.name)

print("alias", fq, [hex(ord(c)) for c in cls])
print((root / "ModifierAliases.kt").read_text())
