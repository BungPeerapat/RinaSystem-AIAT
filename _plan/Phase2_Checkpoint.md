# 📋 Phase 2 Checkpoint — Authentication System
> **สถานะ:** ⏳ NOT STARTED (รอ Phase 1 เสร็จก่อน)

---

## 📋 Checkpoint Info

| Field | Value |
|---|---|
| **Phase** | Phase 2 — Authentication System (Backend + Android) |
| **เริ่มวันที่** | - |
| **อัปเดตล่าสุด** | - |
| **สถานะโดยรวม** | ⏳ NOT STARTED |

---

## 🎯 เป้าหมายของ Phase นี้

สร้างระบบ Auth ที่สมบูรณ์ทั้งฝั่ง Backend (FastAPI) และ Android App
ครอบคลุม Register, Login, JWT, Refresh Token, และ Role-based Access

---

## 📌 Task List

### 🔐 1. Backend — Auth API
| # | Task | สถานะ | หมายเหตุ |
|---|---|---|---|
| 1.1 | ติดตั้ง bcrypt + python-jose (JWT) | ⏳ | |
| 1.2 | สร้าง `auth_utils.py` (hash password, verify, create JWT) | ⏳ | |
| 1.3 | `POST /api/auth/register` | ⏳ | |
| 1.4 | `POST /api/auth/login` → return access + refresh token | ⏳ | |
| 1.5 | `POST /api/auth/refresh` → exchange refresh token | ⏳ | |
| 1.6 | `POST /api/auth/logout` → revoke refresh token | ⏳ | |
| 1.7 | `PUT /api/auth/fcm-token` → update FCM token | ⏳ | |
| 1.8 | JWT Middleware — ตรวจสอบทุก protected route | ⏳ | |
| 1.9 | Role Guard — admin-only routes | ⏳ | |
| 1.10 | Rate limiting บน auth endpoints (10 req/min) | ⏳ | |

### 🧪 2. Backend — Auth Testing
| # | Task | สถานะ | หมายเหตุ |
|---|---|---|---|
| 2.1 | ทดสอบ Register ด้วย Postman/Insomnia | ⏳ | |
| 2.2 | ทดสอบ Login → ได้ JWT กลับมา | ⏳ | |
| 2.3 | ทดสอบ Refresh Token | ⏳ | |
| 2.4 | ทดสอบ Logout → Token ใช้ไม่ได้แล้ว | ⏳ | |
| 2.5 | ทดสอบ Protected Route โดยไม่มี Token → 401 | ⏳ | |
| 2.6 | ทดสอบ Admin Route ด้วย User Token → 403 | ⏳ | |

### 📱 3. Android — Project Setup
| # | Task | สถานะ | หมายเหตุ |
|---|---|---|---|
| 3.1 | สร้าง Android Project (Kotlin + Jetpack Compose) | ⏳ | |
| 3.2 | ตั้งค่า Hilt (Dependency Injection) | ⏳ | |
| 3.3 | ตั้งค่า Retrofit2 + OkHttp3 | ⏳ | |
| 3.4 | ตั้งค่า EncryptedSharedPreferences สำหรับเก็บ JWT | ⏳ | |
| 3.5 | ตั้งค่า Room Database (local cache) | ⏳ | |
| 3.6 | สร้าง ARIA Design System (Colors, Typography, Components) | ⏳ | |

### 📱 4. Android — Auth Screens
| # | Task | สถานะ | หมายเหตุ |
|---|---|---|---|
| 4.1 | SplashScreen + animated ARIA logo | ⏳ | |
| 4.2 | LoginScreen (Email/Password + sci-fi UI) | ⏳ | |
| 4.3 | RegisterScreen | ⏳ | |
| 4.4 | AuthViewModel (Login/Register logic) | ⏳ | |
| 4.5 | AuthRepository (API calls) | ⏳ | |
| 4.6 | Token auto-refresh interceptor (OkHttp) | ⏳ | |
| 4.7 | Auto-redirect: ถ้ามี Token → ข้าม Login | ⏳ | |
| 4.8 | Biometric Login (fingerprint) | ⏳ | |

### 🧪 5. Android — Auth Testing
| # | Task | สถานะ | หมายเหตุ |
|---|---|---|---|
| 5.1 | Register User ผ่านแอป → ตรวจ DB | ⏳ | |
| 5.2 | Login → เก็บ Token → เข้า Dashboard | ⏳ | |
| 5.3 | ปิดแอปแล้วเปิดใหม่ → ไม่ต้อง Login อีก | ⏳ | |
| 5.4 | Token หมดอายุ → Refresh อัตโนมัติ | ⏳ | |

---

## 📁 Output Files ที่ต้องได้

**Backend:**
- [ ] `app/api/routes/auth.py`
- [ ] `app/core/auth_utils.py`
- [ ] `app/core/dependencies.py` (JWT middleware)

**Android:**
- [ ] `SplashScreen.kt`
- [ ] `LoginScreen.kt`
- [ ] `RegisterScreen.kt`
- [ ] `AuthViewModel.kt`
- [ ] `AuthRepository.kt`
- [ ] `AriaTheme.kt` (Design System)

---

## ➡️ เงื่อนไขก่อนไป Phase 3

- [ ] Register / Login ผ่านแอปได้จริง
- [ ] JWT เก็บใน EncryptedSharedPreferences
- [ ] Token refresh ทำงานอัตโนมัติ
- [ ] Role (user/admin) แยก Navigation ได้

---

## 🔁 Claude Code Resume Prompt

```
ฉันกำลังทำโปรเจ็ค ARIA System — Auth System
อยู่ที่ Phase 2: Authentication System

ทำถึง Task: [บอก task ที่ค้างอยู่]
สิ่งที่เสร็จแล้ว: Backend Auth API ทำงานได้แล้ว / Android setup เสร็จแล้ว
ปัญหาที่เจอ: [ถ้ามี]

Tech Stack: FastAPI + PostgreSQL + Kotlin + Jetpack Compose + Hilt + Retrofit2
โปรดช่วยทำ Task ถัดไปให้ฉัน
```

---

*Phase 2 Checkpoint — ARIA Project*
