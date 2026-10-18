@echo off
rem Punto de entrada del Maven Wrapper en Windows. La logica esta en herramientas\maven\mvnw.ps1.
rem ExecutionPolicy Bypass aplica solo a este proceso: no cambia la politica del equipo.
chcp 65001 >nul
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0herramientas\maven\mvnw.ps1" %*
exit /b %ERRORLEVEL%
