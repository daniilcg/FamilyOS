@echo off
setlocal
cd /d "%~dp0"

where python >nul 2>&1
if errorlevel 1 (
  echo Python is required to build FamilyOS-KeyIssuer.exe
  echo Install Python 3 from https://www.python.org/downloads/ and tick "Add python.exe to PATH".
  pause
  exit /b 1
)

python -m pip install --upgrade pip pyinstaller
python -m PyInstaller --noconfirm --onefile --windowed --name FamilyOS-KeyIssuer --distpath . --workpath build --specpath build issuer.py

if exist FamilyOS-KeyIssuer.exe (
  echo.
  echo Built: %cd%\FamilyOS-KeyIssuer.exe
) else (
  echo Build failed.
  exit /b 1
)
pause
