# Phase 4 Checkpoint — Real-time Streaming
> **สถานะ:** ✅ DONE

---

## Checkpoint Info
| Field | Value |
|---|---|
| **Phase** | Phase 4 — WebSocket + Audio/Video Streaming |
| **เริ่มวันที่** | 12/03/2026 |
| **เสร็จวันที่** | 12/03/2026 |
| **สถานะโดยรวม** | ✅ DONE |

## เป้าหมาย
สร้างระบบ WebSocket สำหรับ Real-time streaming ทั้ง Audio (ฟังไมค์) และ Video (กล้อง)

## Task List

### Backend — WebSocket Server
| # | Task | สถานะ |
|---|---|---|
| 1.1 | WebSocket Manager class (connection pool) | ✅ `ws_manager.py` |
| 1.2 | `ws://server/ws/user/{user_id}` endpoint | ✅ `ws.py` |
| 1.3 | `ws://server/ws/admin/{admin_id}` endpoint | ✅ `ws.py` |
| 1.4 | JWT auth บน WebSocket handshake | ✅ `_validate_ws_token()` |
| 1.5 | Command handlers (START_MIC, STOP_MIC, START_CAM, etc.) | ✅ |
| 1.6 | Audio chunk relay: User → Admin | ✅ AUDI tag binary |
| 1.7 | Video frame relay: User → Admin | ✅ VIDE tag binary |
| 1.8 | Redis Pub/Sub สำหรับ multi-instance scaling | ➖ ข้าม (ใช้ 1 instance) |
| 1.9 | Stream session model ใน DB | ✅ `stream_session.py` |

### Android — WebSocket Client
| # | Task | สถานะ |
|---|---|---|
| 2.1 | Foreground Service (ซ่อน notification) | ✅ `StreamingService.kt` |
| 2.2 | Auto-reconnect (exponential backoff 1s→30s) | ✅ `AriaWebSocket.kt` |
| 2.3 | Admin สั่ง stream โดย User ไม่ต้องอนุญาต | ✅ (ตาม requirement) |
| 2.4 | Audio capture (3 quality: 8k/16k/44.1k) | ✅ `AudioStreamer.kt` |
| 2.5 | Camera capture (CameraX + rotation fix) | ✅ `VideoStreamer.kt` |
| 2.6 | ซ่อน 100% — User ไม่เห็นอะไร | ✅ IMPORTANCE_MIN |
| 2.7 | Admin: รับ Audio → Waveform visualizer | ✅ `StreamingControlScreen.kt` |
| 2.8 | Admin: รับ Video → PiP + LIVE badge | ✅ `StreamingControlScreen.kt` |
| 2.9 | Admin: Front/Back camera toggle | ✅ SwitchCam command |
| 2.10 | Admin: ตรวจสอบสถานะหน้าจอ (on/off, locked) | ✅ `GetScreenStatus` |
| 2.11 | Admin: เลือก User จาก dropdown (แสดงชื่อ) | ✅ `userNameMap` |

## เงื่อนไขผ่าน Phase 4
- [x] Admin กด Start Mic → User ส่งเสียง → Admin ได้ยิน + เห็น waveform
- [x] Admin กด Start Cam → User ส่งภาพ → Admin เห็น video PiP
- [x] WebSocket reconnect อัตโนมัติเมื่อ network หลุด
- [x] Service ทำงาน background แม้ User ปิดแอป

---
*Phase 4 Checkpoint — ARIA Project*

---
---

# Phase 5 Checkpoint — TTS Voice System
> **สถานะ:** ✅ DONE

---

## Checkpoint Info
| Field | Value |
|---|---|
| **Phase** | Phase 5 — TTS Integration (Real-time via WebSocket) |
| **เริ่มวันที่** | 12/03/2026 |
| **เสร็จวันที่** | 12/03/2026 |
| **สถานะโดยรวม** | ✅ DONE |

## เป้าหมาย
เชื่อม Moe-TTS (VITS) เข้ากับ Backend — Admin ส่งข้อความ → generate เสียง → เล่นที่ User ทันที

## Task List

### Backend — TTS Service
| # | Task | สถานะ |
|---|---|---|
| 1.1 | `tts_service.py` (เรียก Moe-TTS API port 7860) | ✅ |
| 1.2 | TTS_TEXT command handler ใน WS | ✅ `ws.py` |
| 1.3 | Generate → base64 encode → ส่งกลับ User | ✅ |
| 1.4 | TTS_GENERATING / TTS_SENT status ส่งกลับ Admin | ✅ |
| 1.5 | TTSMessage DB model | ✅ `tts_message.py` |
| 1.6 | REST API สำหรับ message history | ➖ ข้าม (ใช้ real-time WS เท่านั้น) |
| 1.7 | FCM push notification | ➖ ข้าม (ไม่จำเป็น — WS เชื่อมตลอด) |

### Android — Admin (TTS Composer)
| # | Task | สถานะ |
|---|---|---|
| 2.1 | TTS text input + Model selector (0-18) | ✅ `TtsControlCard` |
| 2.2 | Send button + generating indicator | ✅ |
| 2.3 | Model selection dropdown | ✅ |

### Android — User (TTS Playback)
| # | Task | สถานะ |
|---|---|---|
| 3.1 | รับ TTS_PLAY via WebSocket | ✅ `WsCommand.TtsPlay` |
| 3.2 | Base64 decode → WAV → MediaPlayer | ✅ `TtsPlayer.kt` |
| 3.3 | Auto-play queue (เล่นทีละอัน) | ✅ Channel-based queue |

## เงื่อนไขผ่าน Phase 5
- [x] Admin พิมพ์ข้อความ → กด Send → User ได้ยินเสียง TTS
- [x] Queue ทำงาน — ส่งหลายข้อความ เล่นต่อกัน
- [x] Admin เห็น generating state

## หมายเหตุ
- ข้าม FCM เพราะ WS + Foreground Service เชื่อมต่อตลอดอยู่แล้ว
- ข้าม REST message history — TTS ส่งแบบ real-time ไม่จำเป็นต้องเก็บประวัติ
- ข้าม Speed/Pitch sliders — Moe-TTS API รองรับแค่ speed parameter

---
*Phase 5 Checkpoint — ARIA Project*

---
---

# Phase 6 Checkpoint — Admin Control Panel Complete
> **สถานะ:** ✅ DONE

---

## Checkpoint Info
| Field | Value |
|---|---|
| **Phase** | Phase 6 — Admin Panel + User Management + Enhancement Features |
| **เริ่มวันที่** | 12/03/2026 |
| **เสร็จวันที่** | 13/03/2026 |
| **สถานะโดยรวม** | ✅ DONE |

## Task List

### Admin Features — User Management
| # | Task | สถานะ |
|---|---|---|
| 1.1 | User management: Block/Suspend/เปิดใช้ | ✅ Backend + ViewModel + UI |
| 1.2 | Change user role (Admin/User) | ✅ Backend + ViewModel + UI dropdown |
| 1.3 | View user detail screen | ✅ `UserDetailScreen.kt` |
| 1.4 | Stream history log (per user) | ✅ แสดงใน UserDetailScreen |
| 1.5 | AdminUserCard click-to-detail + quick actions | ✅ `AdminDashboardScreen.kt` |

### User Features
| # | Task | สถานะ |
|---|---|---|
| 2.1 | Account settings | ✅ (ProfileScreen) |
| 2.2 | Profile edit display name | ✅ inline edit + save |

### Enhancement Features (Phase 5+)
| # | Task | สถานะ |
|---|---|---|
| 3.1 | Multi-user simultaneous streaming UI | ✅ `StreamingControlScreen.kt` rewrite |
| 3.2 | WakeLock + Battery Optimization Exemption | ✅ `StreamingService.kt` + `AndroidManifest.xml` |
| 3.3 | JWT auto-refresh ก่อน WS reconnect | ✅ `AriaWebSocket.kt` |
| 3.4 | ส่งไฟล์เสียงไปเล่นบน User (background) | ✅ `AudioPlaybackCard` + `TtsPlayer.kt` loop |
| 3.5 | Audio Preset — Backend endpoints | ✅ `admin.py` GET presets + GET preset detail |
| 3.6 | Audio Preset — Android data layer | ✅ `AriaApi` + `AdminModels` + `AdminRepository` + `StreamViewModel` |
| 3.7 | Audio Preset — UI (AudioPresetCard) | ✅ Dropdown + loop + send + stop + refresh |
| 3.8 | Volume control ส่งไปกับเสียง (end-to-end) | ✅ VolumeSlider → ViewModel → WS → Backend → User TtsPlayer |

### Polish (nice-to-have — ข้ามได้)
| # | Task | สถานะ |
|---|---|---|
| 4.1 | Success/Error Snackbar ใน AdminDashboardScreen | ➖ ข้าม |
| 4.2 | Pull-to-refresh บน Users tab | ➖ ข้าม |

## เงื่อนไขก่อนไป Phase 7
- [x] Admin จัดการ User ได้ (block/suspend/role)
- [x] User แก้ไข profile ได้
- [x] AudioPresetCard UI ทำงานได้
- [x] Volume control ส่งค่าไปกับเสียง
- [x] Build สำเร็จ (assembleDebug)

---
*Phase 6 Checkpoint — ARIA Project*

---
---

# Phase 7 Checkpoint — Testing & QA
> **สถานะ:** ✅ DONE

---

## Checkpoint Info
| Field | Value |
|---|---|
| **Phase** | Phase 7 — Testing & QA |
| **เริ่มวันที่** | 13/03/2026 |
| **เสร็จวันที่** | 13/03/2026 |
| **สถานะโดยรวม** | ✅ DONE |

---

## Part A: Backend Automated Tests (Python/pytest)

**ผลรัน:** ✅ **44/44 tests PASSED**

| # | Test File | Tests | ผล |
|---|---|---|---|
| A1 | `tests/test_auth_utils.py` | 10 | ✅ PASS |
| A2 | `tests/test_auth_routes.py` | 10 | ✅ PASS |
| A3 | `tests/test_admin_routes.py` | 10 | ✅ PASS |
| A4 | `tests/test_ws_manager.py` | 14 | ✅ PASS |

**Test Infrastructure:**
- `tests/conftest.py` — async SQLite in-memory DB, fixtures (seed_roles, test_user, admin_user)
- `pytest.ini` — asyncio_mode = auto
- JSONB → TEXT compiler for SQLite compatibility
- UUID string→object conversion fix (also fixes production bug)

---

## Part B: Android Automated Tests (Kotlin/JUnit)

**ผลรัน:** ✅ **32/32 tests PASSED**

| # | Test File | Tests | ผล |
|---|---|---|---|
| B1 | `WsCommandParserTest.kt` | 16 | ✅ PASS |
| B2 | `StreamViewModelTest.kt` | 8 | ✅ PASS |
| B3 | `TtsPlayerTest.kt` | 7 | ✅ PASS |
| B4 | `ExampleUnitTest.kt` | 1 | ✅ PASS |

**ทดสอบอะไร:**
- WsCommand parser: ทุก command type (START_MIC, STOP_MIC, START_CAM, STOP_CAM, SWITCH_CAM, TTS_PLAY, AUDIO_PLAY, STOP_AUDIO, GET_SCREEN_STATUS, TAKE_SCREENSHOT) + malformed JSON + unknown type
- StreamViewModel: `computeAmplitude()` — empty, silence, max/min amplitude, mid-level, odd bytes, clamping
- TtsPlayer: `guessExtensionFromHeader()` — WAV, MP3 (ID3/sync), M4A, unknown, empty, short

**Companion Object Extraction (for testability):**
- `StreamViewModel.computeAmplitude()` — extracted to companion object
- `TtsPlayer.guessExtensionFromHeader()` — extracted to companion object

---

## Part C: Bugs Found & Fixed During Testing

| # | Bug | Fix | ไฟล์ |
|---|---|---|---|
| 1 | UUID string ไม่แปลงเป็น UUID object (SQLite crash, PostgreSQL ซ่อนไว้) | เพิ่ม `_uuid.UUID()` conversion | `dependencies.py`, `auth.py` |
| 2 | `computeAmplitude()` single-byte input → division by zero → NaN | เปลี่ยน guard จาก `isEmpty()` เป็น `size < 2` | `StreamViewModel.kt` |

---

## Part D: Manual E2E Tests (ทดสอบบนเครื่องจริง)

| # | Test Case | ผล | หมายเหตุ |
|---|---|---|---|
| T1 | Register → Login → Dashboard | ✅ | ทดสอบจริงแล้ว |
| T2 | Admin ฟังเสียง User (audio stream) | ✅ | ทดสอบจริงแล้ว |
| T3 | Admin เปิดกล้อง User (video stream) | ✅ | ทดสอบจริงแล้ว |
| T4 | Admin ส่ง TTS → User ได้ยินเสียง | ✅ | ทดสอบจริงแล้ว |
| T5 | Network หลุด → Auto reconnect | ✅ | ทดสอบจริงแล้ว |
| T6 | Token expired → Auto refresh | ✅ | ทดสอบจริงแล้ว |
| T7 | สลับกล้องหน้า/หลัง ขณะ stream | ✅ | ทดสอบจริงแล้ว |
| T8 | ตรวจสอบสถานะหน้าจอ | ✅ | ทดสอบจริงแล้ว |

---

## รวม Automated Tests

| Platform | Tests | ผล |
|---|---|---|
| Backend (pytest) | 44 | ✅ ALL PASS |
| Android (JUnit) | 32 | ✅ ALL PASS |
| **รวม** | **76** | **✅ ALL PASS** |

---
*Phase 7 Checkpoint — ARIA Project*

---
---

# Phase 8 Checkpoint — Deployment & Launch
> **สถานะ:** ⏳ NOT STARTED

---

## Task List

| # | Task | สถานะ |
|---|---|---|
| 1 | ตั้งค่า SSL/HTTPS (Let's Encrypt) | ⏳ |
| 2 | ตั้งค่า Nginx reverse proxy | ⏳ |
| 3 | Auto-restart service | ⏳ |
| 4 | PostgreSQL backup | ⏳ |
| 5 | Build Android APK (release) | ⏳ |
| 6 | ทดสอบบน VPS จริง | ⏳ |

## Definition of Done
- [ ] APK ติดตั้งและใช้งานได้บน Android จริง
- [ ] Backend รันบน VPS โดยไม่ crash ใน 24 ชั่วโมง
- [ ] ทุก feature ทำงานได้ครบ

---
*Phase 8 Checkpoint — ARIA Project*
