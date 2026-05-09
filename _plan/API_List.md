# 🔌 ARIA System — API Endpoints List

> **วันที่:** 10/03/2026
> **Base URL:** `http://{server}:8000/api`

---

## สรุปจำนวน Endpoints

| Group | Endpoints | Auth Required |
|---|---|---|
| Auth | 4 | บางส่วน |
| Users | 3 | Yes |
| Admin | 6 | Yes (Admin only) |
| Streaming | 2 | Yes |
| TTS | 5 | Yes |
| Notifications | 4 | Yes |
| WebSocket | 1 | Yes |
| **รวม** | **25** | |

---

## 1. Auth Endpoints (`/api/auth`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/auth/register` | No | สมัครสมาชิก (email + password) |
| POST | `/auth/login` | No | เข้าสู่ระบบ → JWT tokens |
| POST | `/auth/refresh` | No | Refresh access token |
| POST | `/auth/logout` | Yes | ออกจากระบบ (invalidate token) |

### Request/Response Details

#### POST `/auth/register`
```json
// Request
{
  "email": "user@example.com",
  "password": "P@ssw0rd",
  "display_name": "ชื่อผู้ใช้"
}

// Response 201
{
  "access_token": "eyJ...",
  "refresh_token": "eyJ...",
  "user": {
    "id": "uuid",
    "email": "user@example.com",
    "display_name": "ชื่อผู้ใช้",
    "role": "user"
  }
}
```

#### POST `/auth/login`
```json
// Request
{
  "email": "user@example.com",
  "password": "P@ssw0rd"
}

// Response 200
{
  "access_token": "eyJ...",
  "refresh_token": "eyJ...",
  "user": { ... }
}
```

#### POST `/auth/refresh`
```json
// Request
{ "refresh_token": "eyJ..." }

// Response 200
{
  "access_token": "eyJ...",
  "refresh_token": "eyJ..."
}
```

---

## 2. User Endpoints (`/api/users`)

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/users/me` | Yes | ดูข้อมูลตัวเอง |
| PUT | `/users/me` | Yes | แก้ไขข้อมูลตัวเอง |
| PUT | `/users/me/fcm-token` | Yes | อัปเดต FCM token |

#### PUT `/users/me/fcm-token`
```json
// Request
{ "fcm_token": "firebase_token_string" }

// Response 200
{ "message": "FCM token updated" }
```

---

## 3. Admin Endpoints (`/api/admin`)

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/admin/users` | Admin | ดูรายชื่อ User ทั้งหมด (online/offline) |
| GET | `/admin/users/{user_id}` | Admin | ดูรายละเอียด User |
| PUT | `/admin/users/{user_id}/status` | Admin | Block/Suspend/Activate User |
| GET | `/admin/users/{user_id}/streams` | Admin | ดูประวัติ stream sessions ของ User |
| GET | `/admin/dashboard` | Admin | สรุปสถิติ (online users, total streams, etc.) |
| DELETE | `/admin/messages/clear` | Admin | Clear ประวัติ TTS messages ทั้งหมด |

#### GET `/admin/users`
```json
// Response 200
{
  "users": [
    {
      "id": "uuid",
      "email": "user@example.com",
      "display_name": "ชื่อ",
      "role": "user",
      "status": "active",
      "is_online": true,
      "last_active_at": "2026-03-10T12:00:00Z"
    }
  ],
  "total": 3,
  "online_count": 2
}
```

#### PUT `/admin/users/{user_id}/status`
```json
// Request
{ "status": "blocked" }  // "active" | "blocked" | "suspended"

// Response 200
{ "message": "User status updated", "status": "blocked" }
```

---

## 4. Streaming Endpoints (`/api/streaming`)

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/streaming/sessions` | Admin | ดูรายการ stream sessions (history + active) |
| GET | `/streaming/recordings/{session_id}` | Admin | ดาวน์โหลดไฟล์บันทึก |

> **Note:** Stream control (start/stop) ทำผ่าน WebSocket ไม่ใช่ REST API

---

## 5. TTS Endpoints (`/api/tts`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/tts/send` | Admin | ส่ง TTS message ไปยัง User |
| POST | `/tts/broadcast` | Admin | ส่ง TTS message broadcast ถึงทุกคน |
| POST | `/tts/preview` | Admin | Preview เสียง TTS (ไม่ส่งให้ User) |
| GET | `/tts/messages` | Yes | ดูรายการ TTS messages (Admin ดูทั้งหมด, User ดูของตัวเอง) |
| GET | `/tts/audio/{message_id}` | Yes | ดาวน์โหลดไฟล์เสียง TTS |

#### POST `/tts/send`
```json
// Request
{
  "text": "สวัสดีค่ะ วันนี้อากาศดีนะ",
  "user_id": "uuid"
}

// Response 201
{
  "message_id": "uuid",
  "text": "สวัสดีค่ะ วันนี้อากาศดีนะ",
  "audio_path": "/storage/audio/2026-03-10/abc.wav",
  "audio_duration_ms": 3200,
  "status": "delivered",
  "created_at": "2026-03-10T12:00:00Z"
}
```

#### POST `/tts/preview`
```json
// Request
{ "text": "ทดสอบเสียง" }

// Response 200 → audio/wav binary stream
```

---

## 6. Notification Endpoints (`/api/notifications`)

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/notifications` | Yes | ดูรายการ notification ของตัวเอง |
| PUT | `/notifications/{id}/read` | Yes | Mark as read |
| PUT | `/notifications/read-all` | Yes | Mark all as read |
| GET | `/notifications/unread-count` | Yes | จำนวน notification ที่ยังไม่อ่าน |

#### GET `/notifications`
```json
// Query params: ?page=1&limit=20&unread_only=true
// Response 200
{
  "notifications": [
    {
      "id": "uuid",
      "title": "ข้อความเสียงใหม่",
      "body": "ARIA Admin ส่งข้อความเสียงถึงคุณ",
      "type": "tts_message",
      "is_read": false,
      "metadata": { "message_id": "uuid" },
      "created_at": "2026-03-10T12:00:00Z"
    }
  ],
  "total": 5,
  "unread_count": 3
}
```

---

## 7. WebSocket Endpoint

| Path | Auth | Description |
|---|---|---|
| `ws://{server}:8000/ws?token={jwt}` | Yes (via query param) | Main WebSocket connection |

### WebSocket Message Protocol

ทุก message เป็น JSON format:

```json
{
  "type": "MESSAGE_TYPE",
  "data": { ... },
  "timestamp": "2026-03-10T12:00:00Z"
}
```

### Client → Server Messages

| Type | Sender | Data | Description |
|---|---|---|---|
| `PING` | Any | `{}` | Heartbeat |
| `START_MIC_STREAM` | Admin | `{user_id}` | สั่งเปิดไมค์ User |
| `STOP_MIC_STREAM` | Admin | `{user_id}` | สั่งปิดไมค์ User |
| `START_CAM_STREAM` | Admin | `{user_id, camera: "front"|"back"}` | สั่งเปิดกล้อง User |
| `STOP_CAM_STREAM` | Admin | `{user_id}` | สั่งปิดกล้อง User |
| `SWITCH_CAMERA` | Admin | `{user_id, camera: "front"|"back"}` | สลับกล้อง |
| `AUDIO_CHUNK` | User | Binary data | ส่ง audio chunk |
| `VIDEO_FRAME` | User | Binary data (JPEG) | ส่ง video frame |
| `ACK_TTS_PLAYED` | User | `{message_id}` | แจ้งว่าเล่น TTS แล้ว |

### Server → Client Messages

| Type | Receiver | Data | Description |
|---|---|---|---|
| `PONG` | Any | `{}` | Heartbeat response |
| `OPEN_MIC` | User | `{}` | คำสั่งให้เปิดไมค์ |
| `CLOSE_MIC` | User | `{}` | คำสั่งให้ปิดไมค์ |
| `OPEN_CAMERA` | User | `{camera: "front"|"back"}` | คำสั่งให้เปิดกล้อง |
| `CLOSE_CAMERA` | User | `{}` | คำสั่งให้ปิดกล้อง |
| `SWITCH_CAMERA` | User | `{camera: "front"|"back"}` | คำสั่งให้สลับกล้อง |
| `AUDIO_CHUNK` | Admin | Binary data | ส่ง audio chunk ถึง Admin |
| `VIDEO_FRAME` | Admin | Binary data (JPEG) | ส่ง video frame ถึง Admin |
| `NEW_TTS_MESSAGE` | User | `{message_id, text}` | มีข้อความ TTS ใหม่ |
| `USER_ONLINE` | Admin | `{user_id}` | User เชื่อมต่อแล้ว |
| `USER_OFFLINE` | Admin | `{user_id}` | User หลุดการเชื่อมต่อ |
| `STREAM_ERROR` | Admin | `{user_id, error}` | Error ระหว่าง stream |
| `PERMISSION_REVOKED` | Admin | `{user_id, permission}` | User ถอน permission |

---

## Error Response Format

```json
{
  "detail": "Error message here",
  "error_code": "AUTH_INVALID_CREDENTIALS"
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|---|---|---|
| `AUTH_INVALID_CREDENTIALS` | 401 | Email/Password ไม่ถูกต้อง |
| `AUTH_TOKEN_EXPIRED` | 401 | JWT หมดอายุ |
| `AUTH_TOKEN_INVALID` | 401 | JWT ไม่ถูกต้อง |
| `AUTH_FORBIDDEN` | 403 | ไม่มีสิทธิ์เข้าถึง |
| `USER_NOT_FOUND` | 404 | ไม่พบ User |
| `USER_BLOCKED` | 403 | User ถูก Block |
| `USER_OFFLINE` | 400 | User ไม่ online (ไม่สามารถ stream) |
| `TTS_GENERATION_FAILED` | 500 | Moe-TTS generate เสียงไม่สำเร็จ |
| `STREAM_ALREADY_ACTIVE` | 409 | กำลัง stream User นี้อยู่แล้ว |

---

*API Endpoints List — ARIA System v1.0*
