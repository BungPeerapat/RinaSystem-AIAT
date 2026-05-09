# 🛠️ ARIA System — Tech Stack Decision

> **วันที่:** 10/03/2026

---

## สรุป Tech Stack

| Layer | Technology | Version |
|---|---|---|
| **Android** | Kotlin + Jetpack Compose | Kotlin 2.0, Compose BOM 2024.04 |
| **Android DI** | Hilt (Dagger) | Latest |
| **Android Network** | Retrofit2 + OkHttp3 | Latest |
| **Android WebSocket** | OkHttp WebSocket | Built-in |
| **Android Storage** | EncryptedSharedPreferences + Room | Latest |
| **Android Camera** | CameraX | Latest |
| **Android Push** | Firebase Cloud Messaging (FCM) | Latest |
| **Backend** | FastAPI (Python) | 0.100+ |
| **Database** | PostgreSQL | 16+ |
| **Cache/PubSub** | Redis | 7+ |
| **ORM** | SQLAlchemy (async) + asyncpg | 2.0+ |
| **Migration** | Alembic | Latest |
| **Auth** | python-jose (JWT) + bcrypt | Latest |
| **TTS Engine** | Moe-TTS (VITS) | Local |
| **Push Server** | Firebase Admin SDK (Python) | Latest |

---

## เหตุผลในการเลือก

### Backend: FastAPI
- **ทำไม**: รองรับ async/await, WebSocket built-in, auto-generate API docs (Swagger)
- **ทำไมไม่ Django/Flask**: Django หนักเกินไปสำหรับ 1-5 users, Flask ไม่มี async built-in
- **ทำไมไม่ Node.js**: เจ้าของโปรเจ็คคุ้นเคย Python มากกว่า + Moe-TTS เป็น Python

### Database: PostgreSQL
- **ทำไม**: Robust, รองรับ JSON, full-text search, concurrent connections ดี
- **ทำไมไม่ SQLite**: ไม่รองรับ concurrent access ดีพอสำหรับ WebSocket + API
- **ทำไมไม่ MySQL**: PostgreSQL มี feature มากกว่า และทำงานร่วมกับ SQLAlchemy ได้ดีกว่า

### Cache: Redis
- **ทำไม**: ใช้เป็น Pub/Sub สำหรับ WebSocket event relay, session cache
- **จำเป็นไหม**: จำเป็น — เพราะต้องมี message broker สำหรับ real-time streaming events

### Android: Kotlin + Jetpack Compose
- **ทำไม**: Modern Android UI toolkit, declarative UI, ลด boilerplate
- **ทำไมไม่ Flutter/React Native**: Native performance สำหรับ camera/mic streaming, ไม่ต้อง cross-platform

### Android DI: Hilt
- **ทำไม**: Official Android DI, ลด boilerplate จาก Dagger, lifecycle-aware
- **ทำไมไม่ Koin**: Hilt มี compile-time validation ดีกว่า

### Android Network: Retrofit2 + OkHttp3
- **ทำไม**: Industry standard, OkHttp มี WebSocket + interceptor สำหรับ JWT auto-refresh
- **ทำไมไม่ Ktor Client**: Retrofit มี ecosystem ใหญ่กว่า

### TTS: Moe-TTS (VITS)
- **ทำไม**: มี model พร้อมใช้ (saved_model 0-18), เสียง anime-style ตรงตามความต้องการ
- **ทำไมไม่ Google TTS**: ต้องการเสียง custom (Rina-Vtuber), ไม่ต้องการจ่ายค่า API

### Deployment: ไม่ใช้ Docker
- **ทำไม**: รันบน Windows PC ส่วนตัว, คนใช้ 1-5 คน, ลดความซับซ้อนในการ setup
- **ทำอย่างไร**: ติดตั้ง PostgreSQL + Redis + Python บน Windows โดยตรง

---

## Dependency Summary

### Backend (requirements.txt)
```
fastapi
uvicorn[standard]
sqlalchemy[asyncio]
asyncpg
alembic
python-jose[cryptography]
bcrypt
redis
aioredis
firebase-admin
python-multipart
```

### Android (build.gradle.kts)
```
// Core
androidx.core:core-ktx
androidx.lifecycle:lifecycle-runtime-ktx
androidx.activity:activity-compose

// Compose
androidx.compose:compose-bom
androidx.compose.material3:material3

// DI
com.google.dagger:hilt-android
com.google.dagger:hilt-compiler

// Network
com.squareup.retrofit2:retrofit
com.squareup.retrofit2:converter-gson
com.squareup.okhttp3:okhttp
com.squareup.okhttp3:logging-interceptor

// Storage
androidx.security:security-crypto
androidx.room:room-runtime
androidx.room:room-ktx

// Camera
androidx.camera:camera-core
androidx.camera:camera-camera2
androidx.camera:camera-lifecycle
androidx.camera:camera-view

// Firebase
com.google.firebase:firebase-messaging

// Navigation
androidx.navigation:navigation-compose
androidx.hilt:hilt-navigation-compose
```

---

*Tech Stack Decision — ARIA System*
