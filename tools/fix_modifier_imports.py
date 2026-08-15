from pathlib import Path

IMPORT = "import androidx.compose.ui.modifier." + "M" + "odifier"
print("IMPORT", IMPORT, [hex(ord(c)) for c in IMPORT.split(".")[-1]])

root = Path(r"d:/Projects/Develop/FamilyOS/core_ui/src/main/java")
for p in root.rglob("*.kt"):
    raw = p.read_bytes()
    if raw.startswith(b"\xef\xbb\xbf"):
        raw = raw[3:]
    text = raw.decode("utf-8")
    lines = []
    for line in text.splitlines():
        if "androidx.compose.ui.Modifier" in line and line.strip().startswith("import"):
            lines.append(IMPORT)
        else:
            lines.append(line)
    out = "\n".join(lines) + "\n"
    p.write_bytes(out.encode("utf-8"))
    for line in out.splitlines():
        if "compose.ui.Modifier" in line:
            end = line.split(".")[-1]
            print(p.name, end, hex(ord(end[0])))
