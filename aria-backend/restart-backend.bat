@echo off
title ARIA - Restart Backend
chcp 65001 >nul 2>&1
echo ========================================
echo   ARIA System - Restart Backend
echo ========================================
echo.

:: Set UTF-8 encoding for Python (prevent crash on Thai text)
set PYTHONIOENCODING=utf-8

:: Kill ALL python processes on port 8000
echo [ARIA] Killing all processes on port 8000...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8000 ^| findstr LISTENING 2^>nul') do (
    echo [ARIA]   Killing PID %%a
    taskkill /F /PID %%a >nul 2>&1
)

:: Kill all uvicorn and python that might be holding the port
taskkill /F /IM uvicorn.exe >nul 2>&1

:: Aggressive: kill python processes running uvicorn
for /f "tokens=2" %%a in ('wmic process where "CommandLine like '%%uvicorn%%'" get ProcessId 2^>nul ^| findstr /r "[0-9]"') do (
    echo [ARIA]   Killing uvicorn python PID %%a
    taskkill /F /PID %%a >nul 2>&1
)

:: Wait and verify port is free
echo [ARIA] Waiting for port 8000 to be free...
timeout /t 2 /nobreak >nul
netstat -ano | findstr :8000 | findstr LISTENING >nul 2>&1
if %errorlevel%==0 (
    echo [ARIA]   Port 8000 still in use, retrying...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8000 ^| findstr LISTENING 2^>nul') do (
        taskkill /F /PID %%a >nul 2>&1
    )
    timeout /t 3 /nobreak >nul
    netstat -ano | findstr :8000 | findstr LISTENING >nul 2>&1
    if %errorlevel%==0 (
        echo [ARIA] WARNING: Port 8000 still occupied. Try "Run as Administrator" or restart PC.
        echo [ARIA] Press any key to try starting anyway...
        pause >nul
    )
)

echo [ARIA] Port 8000 is free!
echo.

:: Start fresh
cd /d "%~dp0"
call venv\Scripts\activate.bat
echo [ARIA] venv activated!
echo [ARIA] Starting FastAPI server...
echo [ARIA] Swagger UI: http://localhost:8000/docs
echo [ARIA] Health:     http://localhost:8000/api/health
echo.
set PYTHONIOENCODING=utf-8
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000
