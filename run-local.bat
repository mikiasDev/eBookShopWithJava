@echo off
REM Batch wrapper to run the PowerShell launcher. Run this from project root.
set SCRIPT_DIR=%~dp0
nPowerShell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%run-local.ps1" %*
