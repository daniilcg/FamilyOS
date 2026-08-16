@echo off
cd /d "%~dp0"
if exist FamilyOS-KeyIssuer.exe (
  start "" FamilyOS-KeyIssuer.exe
  exit /b 0
)
where python >nul 2>&1
if errorlevel 1 (
  echo Build the EXE first: double-click build_exe.bat
  pause
  exit /b 1
)
python issuer.py
