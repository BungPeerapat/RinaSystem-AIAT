# ARIA System — Master Progress Tracker

> ไฟล์นี้คือ "แผนที่" ของทั้งโปรเจ็ค อัปเดตทุกครั้งที่ Phase เสร็จ

---

## Overall Progress

```
Phase 0 [Requirements]  ██████████ 100%  ✅ DONE
Phase 1 [Backend Core]  ██████████ 100%  ✅ DONE
Phase 2 [Auth System]   ██████████ 100%  ✅ DONE
Phase 3 [Android App]   ██████████ 100%  ✅ DONE
Phase 4 [Streaming]     ██████████ 100%  ✅ DONE
Phase 5 [TTS System]    ██████████ 100%  ✅ DONE
Phase 6 [Admin Panel]   ██████████ 100%  ✅ DONE
Phase 7 [Testing]       ██████████ 100%  ✅ DONE
Phase 8 [Deployment]    ░░░░░░░░░░   0%  🔄 IN PROGRESS
```

**Last updated:** 13/03/2026
**ทำล่าสุดถึง:** Phase 7 — DONE (76 automated + 8 manual E2E ผ่านหมด)
**ถัดไป:** Phase 8 — Deployment & Launch

---

## Phase Map

| Phase | ชื่อ | Checkpoint File | สถานะ | วันเสร็จ |
|---|---|---|---|---|
| 0 | Requirements & SRS | `Phase0_Checkpoint.md` | ✅ DONE | 10/03/2026 |
| 1 | Backend Core Setup | `Phase1_Checkpoint.md` | ✅ DONE | 11/03/2026 |
| 2 | Authentication System | `Phase2_Checkpoint.md` | ✅ DONE | 11/03/2026 |
| 3 | Android App Foundation | `Phase3_Checkpoint.md` | ✅ DONE | 11/03/2026 |
| 4 | Real-time Streaming | `Phase4_to_Phase8_Checkpoints.md` | ✅ DONE | 12/03/2026 |
| 5 | TTS Voice System | `Phase4_to_Phase8_Checkpoints.md` | ✅ DONE | 12/03/2026 |
| 6 | Admin Control Panel | `Phase4_to_Phase8_Checkpoints.md` | ✅ DONE | 13/03/2026 |
| 7 | Testing & QA | `Phase4_to_Phase8_Checkpoints.md` | ✅ DONE | 13/03/2026 |
| 8 | Deployment & Launch | `Phase4_to_Phase8_Checkpoints.md` | 🔄 IN PROGRESS | - |

---

## Key Decisions Made

| Phase | วันที่ | Decision | ผลกระทบ |
|---|---|---|---|
| 0 | 10/03/2026 | ไม่ใช้ Docker — รันตรงบน Windows | ลดความซับซ้อนในการ setup |
| 0 | 10/03/2026 | Email/Password auth เท่านั้น | ไม่ต้อง integrate Social Login |
| 0 | 10/03/2026 | Dark Mode + ภาษาไทย only | ลด scope UI |
| 0 | 10/03/2026 | Android 14 (API 34) minimum | ใช้ API ใหม่ได้เต็มที่ |
| 0 | 10/03/2026 | TTS + Backend บน Windows เครื่องเดียวกัน | ไม่ต้อง network call ระหว่าง service |
| 4 | 12/03/2026 | Admin stream โดย User ไม่ต้องอนุญาต | ซ่อน 100% ตาม requirement |
| 4 | 12/03/2026 | ใช้ LifecycleService + IMPORTANCE_MIN notification | ซ่อน notification จาก User |
| 5 | 12/03/2026 | ข้าม FCM — ใช้ WS + Foreground Service แทน | WS เชื่อมตลอด ไม่จำเป็นต้อง push |
| 5 | 12/03/2026 | ข้าม REST message history — TTS real-time only | ลด scope ไม่ต้องเก็บประวัติ |
| 5+ | 12/03/2026 | Multi-user streaming แทน single-user dropdown | รองรับ stream หลาย User พร้อมกัน |
| 5+ | 12/03/2026 | WakeLock + Battery Optimization | ป้องกัน disconnect เมื่อปิดจอ/Doze |
| 5+ | 12/03/2026 | JWT auto-refresh ใน AriaWebSocket | ป้องกัน WS 403 expired token |

---

## Features สำเร็จ (Phase 4+5+6)

### Phase 4 — Streaming
- ✅ Admin ฟังไมค์ User real-time + Waveform visualizer
- ✅ Admin ดูกล้อง User real-time + PiP + LIVE badge
- ✅ สลับกล้องหน้า/หลัง
- ✅ ปรับ quality (audio: 8k/16k/44.1k, video: 480p/720p/1080p, fps: 15/24/30)
- ✅ WS auto-reconnect (exponential backoff)
- ✅ Background service ทำงานแม้ปิดแอป
- ✅ Admin เห็นชื่อ User (ไม่ใช่ UUID) ใน dropdown
- ✅ ตรวจสอบสถานะหน้าจอ (on/off, locked)

### Phase 5 — TTS
- ✅ Admin ส่ง TTS → เสียงดังที่ User (auto-play queue)
- ✅ เลือก TTS model (0-18)

### Phase 5+ — Enhancement (หลัง Phase 5)
- ✅ Multi-user streaming — stream หลาย User พร้อมกัน + per-user controls
- ✅ WakeLock + Battery Optimization Exemption — ป้องกัน disconnect เมื่อปิดจอ
- ✅ JWT auto-refresh — ป้องกัน WS 403 เมื่อ token หมดอายุ
- ✅ ส่งไฟล์เสียงไปเล่นบน User device (background, ไม่บันทึกไฟล์)
- ✅ Audio Preset — โหลดจาก `_Audio_Preset/` folder + dropdown + loop + stop
- ✅ Volume control — ปรับระดับเสียงที่ส่งไป (0-100%)

### Phase 6 — Admin Panel + User Management
- ✅ User management: Block/Suspend/เปิดใช้ (Backend + ViewModel + UI)
- ✅ Change user role: Admin/User (Backend + ViewModel + UI dropdown)
- ✅ UserDetailScreen: ข้อมูล User + role change + status actions + stream history
- ✅ AdminUserCard: click-to-detail + quick action buttons
- ✅ ProfileScreen: แก้ไข display name (inline edit)

---

## Project File Structure

```
ARIA-Project/
│
├── _plan/
│   ├── MASTER_PROGRESS.md              ← ไฟล์นี้
│   ├── Phase0_Checkpoint.md
│   ├── Phase1_Checkpoint.md
│   ├── Phase2_Checkpoint.md
│   ├── Phase3_Checkpoint.md
│   ├── Phase4_to_Phase8_Checkpoints.md
│   ├── SRS_Document.md
│   ├── Tech_Stack_Decision.md
│   ├── System_Architecture.md
│   ├── DB_ERD.md
│   └── API_List.md
│
├── aria-backend/               ← Backend (FastAPI + PostgreSQL + Redis)
└── app/                        ← Android App (Kotlin + Compose + Hilt)
```

---

## Quick Resume Guide

> **ทุกครั้งที่กลับมาทำงาน ให้อ่านส่วนนี้ก่อน**

1. เปิด `MASTER_PROGRESS.md` → ดูว่าอยู่ Phase ไหน
2. เปิด `Phase4_to_Phase8_Checkpoints.md` → หา Phase ปัจจุบัน
3. ดู Task List → หา task ที่สถานะ ⏳
4. เปิด Claude Code แล้ว paste prompt นี้:

```
ฉันกำลังทำโปรเจ็ค ARIA System
ปัจจุบันอยู่ที่: [Phase N — ชื่อ Phase]
Task ที่กำลังทำ: [Task Name]
Context: [สรุปสั้นๆ ว่าทำอะไรไปแล้ว]

โปรดช่วยฉันต่อจากจุดนี้
```

---

*ARIA System — Master Progress Tracker v2.0*
