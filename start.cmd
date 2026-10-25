@echo off
REM ASCII-only launcher: cmd + UTF-8 cmd files are unreliable on Chinese Windows.
REM Real logic lives in start.ps1 (UTF-8 with BOM, Chinese output).
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1"
if errorlevel 1 pause
