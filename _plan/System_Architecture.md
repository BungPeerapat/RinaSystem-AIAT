# 🏗️ ARIA System — System Architecture

> **วันที่:** 10/03/2026

---

## 1. High-Level Architecture

```mermaid
graph TB
    subgraph "Android Devices"
        UA1["👤 User App #1"]
        UA2["👤 User App #2"]
        AA["👑 Admin App"]
    end

    subgraph "Windows Server (Single Machine)"
        subgraph "Application Layer"
            API["FastAPI Server<br/>:8000"]
            WS["WebSocket Manager"]
        end

        subgraph "Data Layer"
            PG[(PostgreSQL<br/>:5432)]
            RD[(Redis<br/>:6379)]
        end

        subgraph "Service Layer"
            TTS["Moe-TTS Engine<br/>(VITS)"]
            FS["📁 File Storage<br/>/storage/audio/<br/>/storage/video/"]
        end
    end

    FCM["☁️ Firebase Cloud<br/>Messaging"]

    UA1 <-->|"REST + WebSocket"| API
    UA2 <-->|"REST + WebSocket"| API
    AA <-->|"REST + WebSocket"| API

    API <--> WS
    API <--> PG
    API <--> RD
    API --> TTS
    TTS --> FS
    API --> FS
    API --> FCM
    FCM -.->|"Push"| UA1
    FCM -.->|"Push"| UA2
```

---

## 2. Component Diagram

```mermaid
graph LR
    subgraph "Android App (Shared Codebase)"
        UI["UI Layer<br/>Jetpack Compose"]
        VM["ViewModel Layer<br/>Hilt"]
        REPO["Repository Layer"]
        NET["Network Layer<br/>Retrofit + OkHttp"]
        WSC["WebSocket Client<br/>OkHttp WS"]
        LOC["Local Storage<br/>Room + EncryptedPrefs"]
        CAM["Camera Service<br/>CameraX"]
        MIC["Mic Service<br/>AudioRecord"]
        AUD["Audio Player<br/>MediaPlayer/ExoPlayer"]
        FG["Foreground Service<br/>Background Tasks"]
    end

    UI --> VM --> REPO
    REPO --> NET
    REPO --> WSC
    REPO --> LOC
    REPO --> CAM
    REPO --> MIC
    REPO --> AUD
    FG --> MIC
    FG --> CAM
    FG --> WSC
```

```mermaid
graph LR
    subgraph "Backend (FastAPI)"
        RTR["Routers<br/>auth, users, admin,<br/>streaming, tts, notify"]
        SVC["Services<br/>Business Logic"]
        WSM["WebSocket Manager<br/>Connection Pool"]
        MDL["Models<br/>SQLAlchemy"]
        SCH["Schemas<br/>Pydantic"]
        MID["Middleware<br/>Auth, CORS, Logging"]
    end

    RTR --> SVC --> MDL
    RTR --> SCH
    MID --> RTR
    SVC --> WSM
```

---

## 3. Data Flow Diagrams

### 3.1 Audio Streaming Flow

```mermaid
sequenceDiagram
    participant A as Admin App
    participant S as Backend Server
    participant R as Redis
    participant U as User App
    participant FS as File Storage

    A->>S: WS: START_MIC_STREAM {user_id}
    S->>R: Publish stream command
    R->>S: Notify WS Manager
    S->>U: WS: OPEN_MIC
    U->>U: Start AudioRecord (background)
    loop Real-time streaming
        U->>S: WS: AUDIO_CHUNK {binary data}
        S->>FS: Append to audio file
        S->>A: WS: AUDIO_CHUNK {binary data}
        A->>A: Play audio chunk
    end
    A->>S: WS: STOP_MIC_STREAM
    S->>U: WS: CLOSE_MIC
    U->>U: Stop AudioRecord
    S->>FS: Finalize audio file
```

### 3.2 Video Streaming Flow

```mermaid
sequenceDiagram
    participant A as Admin App
    participant S as Backend Server
    participant U as User App
    participant FS as File Storage

    A->>S: WS: START_CAM_STREAM {user_id, camera: "back"}
    S->>U: WS: OPEN_CAMERA {camera: "back"}
    U->>U: Start CameraX (background)
    loop Real-time streaming
        U->>S: WS: VIDEO_FRAME {JPEG binary}
        S->>FS: Save frame / record
        S->>A: WS: VIDEO_FRAME {JPEG binary}
        A->>A: Display frame
    end
    Note over A: Admin toggles camera
    A->>S: WS: SWITCH_CAMERA {camera: "front"}
    S->>U: WS: SWITCH_CAMERA {camera: "front"}
    A->>S: WS: STOP_CAM_STREAM
    S->>U: WS: CLOSE_CAMERA
```

### 3.3 TTS Message Flow

```mermaid
sequenceDiagram
    participant A as Admin App
    participant S as Backend Server
    participant TTS as Moe-TTS Engine
    participant FS as File Storage
    participant DB as PostgreSQL
    participant U as User App
    participant FCM as Firebase

    A->>S: POST /api/tts/send {text, user_id}
    S->>TTS: Generate speech (text)
    TTS->>FS: Save audio.wav
    TTS-->>S: audio_path
    S->>DB: INSERT message (text, audio_path, user_id)
    S->>U: WS: NEW_TTS_MESSAGE {message_id}
    S->>FCM: Send push notification
    FCM-->>U: Push: "มีข้อความเสียงใหม่"
    U->>S: GET /api/tts/audio/{message_id}
    S-->>U: Audio file (stream)
    U->>U: Auto-play audio + typewriter text
```

### 3.4 Authentication Flow

```mermaid
sequenceDiagram
    participant U as User/Admin App
    participant S as Backend Server
    participant DB as PostgreSQL

    U->>S: POST /api/auth/register {email, password}
    S->>S: Hash password (bcrypt)
    S->>DB: INSERT user
    S-->>U: {access_token, refresh_token}

    Note over U,S: Normal API calls
    U->>S: GET /api/... (Authorization: Bearer {access_token})
    S->>S: Verify JWT
    S-->>U: Response

    Note over U,S: Token expired
    U->>S: POST /api/auth/refresh {refresh_token}
    S->>S: Verify refresh token
    S-->>U: {new_access_token, new_refresh_token}
```

---

## 4. Backend Project Structure

```
aria-backend/
├── main.py                    # FastAPI app entry point
├── config.py                  # Settings & environment
├── database.py                # DB connection & session
│
├── routers/
│   ├── auth.py                # /api/auth/*
│   ├── users.py               # /api/users/*
│   ├── admin.py               # /api/admin/*
│   ├── streaming.py           # /api/streaming/*
│   ├── tts.py                 # /api/tts/*
│   └── notifications.py       # /api/notifications/*
│
├── services/
│   ├── auth_service.py        # Login, register, JWT
│   ├── user_service.py        # User CRUD
│   ├── streaming_service.py   # Audio/Video stream mgmt
│   ├── tts_service.py         # Moe-TTS integration
│   ├── notification_service.py # FCM + in-app
│   └── websocket_manager.py   # WS connection pool
│
├── models/
│   ├── user.py                # User model
│   ├── message.py             # TTS message model
│   ├── stream_session.py      # Stream session model
│   └── notification.py        # Notification model
│
├── schemas/
│   ├── auth.py                # Auth request/response
│   ├── user.py                # User schemas
│   ├── message.py             # TTS message schemas
│   └── notification.py        # Notification schemas
│
├── middleware/
│   ├── auth.py                # JWT verification
│   └── cors.py                # CORS configuration
│
├── storage/
│   ├── audio/                 # Recorded audio files
│   └── video/                 # Recorded video files
│
├── alembic/                   # DB migrations
│   ├── versions/
│   └── env.py
│
├── alembic.ini
└── requirements.txt
```

---

## 5. Android Project Structure

```
aria-android/
├── app/src/main/java/com/aria/system/
│   ├── ARIAApplication.kt         # Hilt Application
│   ├── MainActivity.kt            # Single Activity
│   │
│   ├── di/                         # Dependency Injection
│   │   ├── AppModule.kt
│   │   └── NetworkModule.kt
│   │
│   ├── data/
│   │   ├── remote/
│   │   │   ├── api/               # Retrofit interfaces
│   │   │   │   ├── AuthApi.kt
│   │   │   │   ├── UserApi.kt
│   │   │   │   ├── TtsApi.kt
│   │   │   │   └── NotificationApi.kt
│   │   │   ├── websocket/
│   │   │   │   └── AriaWebSocket.kt
│   │   │   └── interceptor/
│   │   │       └── AuthInterceptor.kt
│   │   ├── local/
│   │   │   ├── db/                # Room Database
│   │   │   └── prefs/             # EncryptedSharedPrefs
│   │   └── repository/
│   │       ├── AuthRepository.kt
│   │       ├── StreamRepository.kt
│   │       ├── TtsRepository.kt
│   │       └── NotificationRepository.kt
│   │
│   ├── domain/
│   │   └── model/                  # Domain models
│   │
│   ├── service/
│   │   ├── StreamingService.kt     # Foreground Service (mic+cam)
│   │   └── FCMService.kt          # Firebase messaging
│   │
│   ├── ui/
│   │   ├── theme/                  # Dark theme, colors, typography
│   │   ├── components/             # Reusable composables
│   │   │   ├── GlowCard.kt
│   │   │   ├── AriaButton.kt
│   │   │   └── WaveformVisualizer.kt
│   │   ├── navigation/
│   │   │   └── AriaNavGraph.kt
│   │   ├── onboarding/             # Onboarding screens
│   │   ├── auth/                   # Login/Register
│   │   ├── dashboard/              # Main dashboard
│   │   ├── admin/                  # Admin panel
│   │   │   ├── UserListScreen.kt
│   │   │   ├── StreamViewScreen.kt
│   │   │   └── TtsComposerScreen.kt
│   │   └── user/                   # User screens
│   │       ├── HomeScreen.kt
│   │       └── NotificationScreen.kt
│   │
│   └── util/                       # Utilities
│
├── app/src/main/res/               # Resources
└── build.gradle.kts
```

---

## 6. Network & Port Configuration

| Service | Port | Protocol |
|---|---|---|
| FastAPI (HTTP + WS) | 8000 | HTTP/1.1 + WebSocket |
| PostgreSQL | 5432 | TCP |
| Redis | 6379 | TCP |
| Moe-TTS (internal) | — | Function call (same process) |

**External Access:** Ngrok หรือ Cloudflare Tunnel → Forward port 8000

---

*System Architecture — ARIA System v1.0*
