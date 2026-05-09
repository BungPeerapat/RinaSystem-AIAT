@echo off
title ARIA - Virtual Environment
echo ========================================
echo   ARIA System - Activate Virtual Env
echo ========================================
echo.
cd /d "%~dp0"
call venv\Scripts\activate.bat
echo [ARIA] venv activated!
echo.
cmd /k
