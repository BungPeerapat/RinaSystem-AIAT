@echo off
chcp 65001 >nul
title ARIA - Build APK

echo ========================================
echo   ARIA Build APK
echo ========================================
echo.

rem --- Read current version from gradle.properties ---
for /f "eol=# tokens=1,2 delims==" %%a in (gradle.properties) do (
    if "%%a"=="ARIA_VERSION_CODE" set "VER_CODE=%%b"
    if "%%a"=="ARIA_VERSION_NAME" set "VER_NAME=%%b"
)

echo   Current version: %VER_NAME% [code %VER_CODE%]
echo.

rem --- Ask for new version ---
set /p NEW_NAME="  New version name [%VER_NAME%]: "
if "%NEW_NAME%"=="" set "NEW_NAME=%VER_NAME%"

rem --- Auto increment VERSION_CODE ---
set /a NEW_CODE=%VER_CODE%+1

echo.
echo   %VER_NAME% [code %VER_CODE%] --^> %NEW_NAME% [code %NEW_CODE%]
echo.
set /p CONFIRM="  Confirm? [Y/n]: "
if /i "%CONFIRM%"=="n" (
    echo   Cancelled.
    pause
    exit /b 0
)

rem --- Update gradle.properties (replace version lines) ---
powershell -NoProfile -Command ^
    "(Get-Content 'gradle.properties') -replace '^ARIA_VERSION_CODE=.*', 'ARIA_VERSION_CODE=%NEW_CODE%' -replace '^ARIA_VERSION_NAME=.*', 'ARIA_VERSION_NAME=%NEW_NAME%' | Set-Content 'gradle.properties'"

set "VER_CODE=%NEW_CODE%"
set "VER_NAME=%NEW_NAME%"

echo   [OK] gradle.properties updated
echo.

rem --- Select build type ---
echo   [1] Debug
echo   [2] Release
echo.
set /p BUILD_TYPE="  Select (1/2): "

if "%BUILD_TYPE%"=="2" (
    set "TASK=assembleRelease"
    set "APK_DIR=app\build\outputs\apk\release"
    set "APK_NAME=app-release-unsigned.apk"
) else (
    set "TASK=assembleDebug"
    set "APK_DIR=app\build\outputs\apk\debug"
    set "APK_NAME=app-debug.apk"
)

rem --- Stop Gradle daemon to pick up new version ---
call "%~dp0gradlew.bat" --stop >nul 2>nul

echo.
echo   Building %TASK%...
echo ========================================

call "%~dp0gradlew.bat" %TASK%

if %ERRORLEVEL% neq 0 (
    echo.
    echo   [FAIL] Build failed!
    pause
    exit /b 1
)

echo.
echo ========================================

if "%BUILD_TYPE%"=="2" (
    set "APK_DST=aria-v%VER_NAME%.apk"
) else (
    set "APK_DST=aria-v%VER_NAME%-debug.apk"
)

if exist "%APK_DIR%\%APK_NAME%" (
    copy /y "%APK_DIR%\%APK_NAME%" "%APK_DST%" >nul
    echo   [OK] Build successful!
    echo.
    echo   APK : %APK_DST%
    echo   Ver : %VER_NAME% [code %VER_CODE%]
) else (
    echo   [WARN] Build OK but APK not found.
    echo   Expected: %APK_DIR%\%APK_NAME%
)

echo ========================================
echo.
set /p DO_UPLOAD="  Upload to server? [y/N]: "
if /i "%DO_UPLOAD%"=="y" (
    call upload-apk.bat
) else (
    pause
)
