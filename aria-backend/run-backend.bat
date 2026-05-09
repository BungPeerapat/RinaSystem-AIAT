@echo off
title ARIA - Backend Server
chcp 65001 >nul 2>&1
echo ========================================
echo   ARIA System - Backend Server
echo ========================================
echo.
set PYTHONIOENCODING=utf-8
cd /d "%~dp0"
call venv\Scripts\activate.bat
echo [ARIA] venv activated!
echo [ARIA] Starting FastAPI server...
echo [ARIA] Swagger UI: http://localhost:8000/docs
echo [ARIA] Health:     http://localhost:8000/api/health
echo.
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
