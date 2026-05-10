@echo off
chcp 65001 >nul
title ARIA - Upload APK to Server

echo ========================================
echo   ARIA Upload APK to Server
echo ========================================
echo.

rem --- Check curl ---
where curl.exe >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo   [FAIL] curl.exe not found.
    pause
    exit /b 1
)

rem --- Read version from gradle.properties ---
for /f "eol=# tokens=1,2 delims==" %%a in (gradle.properties) do (
    if "%%a"=="ARIA_VERSION_CODE" set "VER_CODE=%%b"
    if "%%a"=="ARIA_VERSION_NAME" set "VER_NAME=%%b"
)

echo   Version: %VER_NAME% (code %VER_CODE%)
echo.

rem --- Select APK ---
echo   [1] Debug   (aria-v%VER_NAME%-debug.apk)
echo   [2] Release (aria-v%VER_NAME%.apk)
echo.
set /p APK_CHOICE="  Select (1/2): "

if "%APK_CHOICE%"=="2" (
    set "APK_FILE=aria-v%VER_NAME%.apk"
) else (
    set "APK_FILE=aria-v%VER_NAME%-debug.apk"
)

if not exist "%APK_FILE%" (
    echo.
    echo   [FAIL] APK not found: %APK_FILE%
    echo   Please run build-apk.bat first.
    pause
    exit /b 1
)

for %%F in ("%APK_FILE%") do set "APK_SIZE=%%~zF"
echo.
echo   APK file : %APK_FILE%
echo   Size     : %APK_SIZE% bytes
echo.

rem --- Server URL ---
set "DEFAULT_SERVER=http://systemdeveloper2.ddns.net:8000"
set /p SERVER_URL="  Server URL [%DEFAULT_SERVER%]: "
if "%SERVER_URL%"=="" set "SERVER_URL=%DEFAULT_SERVER%"

rem --- Admin credentials (retry loop) ---
:LOGIN_RETRY
set "TOKEN="
echo.
set /p ADMIN_EMAIL="  Admin Email (leave empty to exit): "
if "%ADMIN_EMAIL%"=="" (
    echo   Cancelled.
    pause
    exit /b 0
)
set /p ADMIN_PASS="  Admin Password: "

echo.
echo   Logging in to %SERVER_URL%...

rem --- Login via curl, save response to temp file ---
set "TEMP_RESP=%TEMP%\aria_login_resp.json"
curl.exe -s --max-time 15 -X POST "%SERVER_URL%/api/auth/login" -H "Content-Type: application/json" -d "{\"email\":\"%ADMIN_EMAIL%\",\"password\":\"%ADMIN_PASS%\"}" -o "%TEMP_RESP%"

rem --- Extract token via PowerShell from temp file ---
for /f "usebackq delims=" %%t in (`powershell -NoProfile -Command "try { (Get-Content '%TEMP_RESP%' -Raw | ConvertFrom-Json).access_token } catch { '' }"`) do set "TOKEN=%%t"
del "%TEMP_RESP%" 2>nul

if "%TOKEN%"=="" (
    echo   [FAIL] Email or Password incorrect. Please try again.
    goto LOGIN_RETRY
)

echo   [OK] Login successful
echo.

rem --- Release notes ---
set "RELEASE_NOTES="
set /p RELEASE_NOTES="  Release notes (optional): "

set "FORCE=false"
set /p FORCE_INPUT="  Force update? (y/N): "
if /i "%FORCE_INPUT%"=="y" set "FORCE=true"

echo.
echo ========================================
echo   Uploading %APK_FILE% to server...
echo   This may take a while for large files.
echo ========================================
echo.

curl.exe -s --max-time 300 -X POST "%SERVER_URL%/api/app/upload" ^
    -H "Authorization: Bearer %TOKEN%" ^
    -F "apk=@%APK_FILE%" ^
    -F "version_code=%VER_CODE%" ^
    -F "version_name=%VER_NAME%" ^
    -F "release_notes=%RELEASE_NOTES%" ^
    -F "force_update=%FORCE%"

if %ERRORLEVEL% neq 0 (
    echo.
    echo   [FAIL] Upload failed.
    pause
    exit /b 1
)

echo.
echo.
echo ========================================
echo   [OK] Upload complete!
echo   Users will see update: v%VER_NAME%
echo ========================================
echo.
pause
