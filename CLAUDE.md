# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**ARIA (Adaptive Remote Intelligence Assistant)** — ระบบควบคุมและสื่อสารกับ Android Device จากระยะไกล ประกอบด้วย:

- **Android App** (Kotlin + Jetpack Compose) — สำหรับ User (ถูกควบคุม) และ Admin (ผู้ควบคุม)
- **Backend Server** (FastAPI + PostgreSQL + Redis) — รันบน Windows PC (`aria-backend/`)
- **TTS Engine** (Moe-TTS / VITS) — Gradio app บน `http://127.0.0.1:7860`, ถูกเรียกโดย Backend

**Features หลัก:** Admin ฟังไมค์/ดูกล้อง User แบบ real-time, ส่ง TTS voice message, ส่งไฟล์เสียง, User management

**Scale:** 1-5 users, Admin คนเดียว, ใช้ส่วนตัว/กลุ่มเพื่อน

## Build & Development Commands

### Android App

```bash
# Build debug APK (interactive — bumps version, copies to root)
build-apk.bat

# Or build directly with Gradle
./gradlew assembleDebug
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run a single unit test class
./gradlew testDebugUnitTest --tests "com.example.rinasystem.ExampleUnitTest"

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Clean build
./gradlew clean
```

Version is managed in `gradle.properties` (`ARIA_VERSION_CODE` / `ARIA_VERSION_NAME`). `build-apk.bat` auto-increments `VERSION_CODE` and copies the APK to the repo root as `aria-v<name>[-debug].apk`.

### Backend

```bash
cd aria-backend

# Run (Windows — activates venv, starts uvicorn on :8000)
run-backend.bat

# Or manually
source venv/Scripts/activate   # bash
uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# Run tests (uses aiosqlite in-memory DB — no PostgreSQL required)
pytest

# Run a single test file
pytest tests/test_auth_routes.py

# Database migrations
alembic upgrade head
alembic revision --autogenerate -m "description"
```

Backend requires a `.env` file in `aria-backend/` with `DATABASE_URL` and `SECRET_KEY` (see `app/config.py`).

## Architecture

### Android App (`app/`)

- **Single-module Gradle project**, version catalog at `gradle/libs.versions.toml`
- **Kotlin 2.0** + Compose BOM 2024.04.01 + Material3, targeting **JVM 11**, **minSdk/targetSdk 34**
- **Hilt** for DI (`di/AppModule.kt`)
- Entry point: `app/src/main/java/com/example/rinasystem/`

```
data/
  api/           — Retrofit interface (AriaApi.kt), interceptors (auth token, dynamic base URL)
  local/         — TokenManager (DataStore)
  model/         — API response data classes
  repository/    — AuthRepository, AdminRepository, UserRepository, UpdateRepository
  ws/            — AriaWebSocket.kt (OkHttp WS + JWT auto-refresh), WsCommand.kt
service/
  StreamingService.kt  — LifecycleService Foreground Service; handles mic/camera streaming + TTS playback
  AudioStreamer.kt     — AudioRecord → WebSocket binary frames
  VideoStreamer.kt     — CameraX → WebSocket binary frames
  TtsPlayer.kt         — receives WAV bytes from WS, queues and plays via AudioTrack
  AriaLogBuffer.kt     — circular in-memory log buffer
ui/
  screens/       — login/, register/, splash/, dashboard/, admin/, messages/, profile/, settings/, update/
  viewmodel/     — AuthViewModel, StreamViewModel, AdminViewModel, MessageViewModel, UserViewModel, SettingsViewModel, UpdateViewModel
  navigation/    — Compose NavHost
  components/    — shared Composables
  theme/         — Dark-only Sci-fi/HUD theme
```

**Key Android patterns:**
- `StreamingService` runs as Foreground Service with `IMPORTANCE_MIN` notification (hidden from User)
- `AriaWebSocket` handles JWT auto-refresh when 403 is received mid-connection
- `DynamicBaseUrlInterceptor` — server URL is configurable from Settings screen (stored in DataStore)
- Binary frame format over WebSocket: `[4-byte tag][36-byte user_id UTF-8][payload]`

### Backend (`aria-backend/`)

```
app/
  main.py        — FastAPI app, lifespan (resets online status on startup), CORS, rate limiting
  config.py      — pydantic-settings (reads .env)
  database.py    — async SQLAlchemy engine + session
  models/        — SQLAlchemy ORM: User, Session, StreamSession, TtsMessage, Notification, Role
  schemas/       — Pydantic request/response schemas
  api/routes/    — auth.py, admin.py, users.py, ws.py, health.py, app_update.py
  core/
    ws_manager.py   — ConnectionManager singleton: user/admin WS connections, stream state relay
    tts_service.py  — calls Moe-TTS Gradio API at 127.0.0.1:7860
    auth_utils.py   — JWT create/verify, bcrypt hashing
    dependencies.py — FastAPI deps: get_db, get_current_user, require_admin
    exceptions.py   — AriaException + handlers
migrations/      — Alembic migration files
tests/           — pytest-asyncio, uses aiosqlite (no real DB needed)
```

**WebSocket routes:** `ws.py` exposes `/ws/user/{id}` and `/ws/admin/{id}`. The `ws_manager` singleton relays binary audio/video frames from User → Admin and routes JSON commands Admin → User.

**TTS flow:** Admin sends REST `POST /api/tts` → Backend calls Moe-TTS Gradio (`/api/v1/speech/get_voice/{text}`) → gets WAV bytes → sends via `ws_manager.send_bytes_to_user()` → User's `TtsPlayer` auto-plays.

## Key Configuration

- **AGP 8.7.2**, Kotlin 2.0.0
- ProGuard/R8 minification **disabled** for release builds
- `android.nonTransitiveRClass=true`
- Backend `.env` needs: `DATABASE_URL` (PostgreSQL async URL `postgresql+asyncpg://...`), `SECRET_KEY`, optional `JWT_SECRET_KEY`

## Key Design Decisions

- **Dark Mode only** + ภาษาไทย + Sci-fi/HUD theme
- **No Docker** — รัน FastAPI + PostgreSQL + Redis ตรงบน Windows
- **Email/Password auth only** — ไม่มี Social Login, 2FA, หรือ session timeout
- **Admin ฟังไมค์/เปิดกล้อง User ได้โดยไม่ต้องขออนุญาต** — Foreground Service ซ่อน 100%
- **ข้าม FCM** — ใช้ WebSocket + Foreground Service แทน (WS เชื่อมตลอด)
- **TTS auto-play + queue** — User ไม่ต้องกด play
- **WakeLock + Battery Optimization Exemption** — ป้องกัน WS disconnect เมื่อปิดจอ/Doze

## Planning Documents

ทุกเอกสารการออกแบบอยู่ใน `_plan/`:

- `MASTER_PROGRESS.md` — Progress tracker ทุก Phase (Phase 8 Deployment กำลังทำ)
- `Phase0_Checkpoint.md` — Requirements (Q1-Q61 answers)
- `SRS_Document.md` — Software Requirements Specification
- `Tech_Stack_Decision.md` — เหตุผลเลือก Tech Stack
- `System_Architecture.md` — Component/Data flow diagrams
- `DB_ERD.md` — Database schema (PostgreSQL)
- `API_List.md` — 25 REST endpoints + WebSocket protocol
