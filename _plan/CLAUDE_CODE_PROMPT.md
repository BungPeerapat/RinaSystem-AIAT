# 🤖 ARIA System — Claude Code Master Prompt
> วาง Prompt นี้ใน Claude Code ทุกครั้งที่เริ่ม Session ใหม่

---

## ⚡ Quick Resume Prompt (ใช้ทุกครั้งที่กลับมาทำ)

```
คุณคือ Senior BA + Senior Software Architect สำหรับโปรเจ็ค ARIA System

=== PROJECT CONTEXT ===
ชื่อโปรเจ็ค: ARIA (Adaptive Remote Intelligence Assistant)
ระบบ: Android App (Kotlin + Jetpack Compose) + Backend (FastAPI + PostgreSQL)
ฟีเจอร์หลัก: Auth, Admin ฟังไมล์ User, เปิดกล้อง User, ส่ง TTS Voice (Rina-Vtuber), Push Notification

=== CHECKPOINT SYSTEM ===
ระบบ Checkpoint อยู่ที่: [ระบุ path ของไฟล์ checkpoint ของคุณ]
Phase ปัจจุบัน: Phase [N] — [ชื่อ Phase]
Task ล่าสุด: [Task ที่กำลังทำหรือค้างอยู่]

=== สิ่งที่เสร็จแล้ว ===
[สรุปสั้นๆ 3-5 บรรทัด]

=== สิ่งที่ต้องทำต่อ ===
[Task ถัดไปที่ต้องทำ]

=== กฎที่ต้องทำตาม ===
1. หลังทำแต่ละ Task เสร็จ → อัปเดต Checkpoint file ให้ฉันด้วย
2. ถ้าเจอ Decision ที่ต้องตัดสินใจ → บอกฉันก่อน อย่าเดาเอง
3. ถ้าเจอ Error → แสดง error message + วิธีแก้
4. ทุก code ที่เขียน → เขียน comment ภาษาไทย/อังกฤษให้ชัดเจน
5. หลังทำ Phase เสร็จ → สร้าง summary ว่าทำอะไรไปบ้าง

โปรดเริ่มต่อจากจุดที่ค้างอยู่
```

---

## 📋 วิธีใช้ Checkpoint System

### ขั้นตอนทุกครั้งที่เริ่มทำงาน:

```
1. เปิด MASTER_PROGRESS.md
   → ดูว่าอยู่ Phase ไหน

2. เปิด Checkpoints/Phase{N}_Checkpoint.md
   → ดู Task List ว่าค้างที่ไหน

3. Copy "Quick Resume Prompt" ด้านบน
   → กรอก Phase, Task, และ Context

4. วาง Prompt ใน Claude Code
   → เริ่มทำงานต่อ

5. เมื่อ Task เสร็จ:
   → อัปเดต ✅ ใน Phase{N}_Checkpoint.md
   → อัปเดต Progress bar ใน MASTER_PROGRESS.md
```

---

## 🔄 สัญลักษณ์สถานะ

| สัญลักษณ์ | ความหมาย |
|---|---|
| ⏳ | รอทำ (Not Started) |
| 🔄 | กำลังทำ (In Progress) |
| ✅ | เสร็จแล้ว (Done) |
| ❌ | ติดปัญหา (Blocked) |
| ⏭️ | ข้าม (Skipped / Out of Scope) |

---

## 📁 ไฟล์ทั้งหมดในระบบ

```
ARIA-Project/
│
├── 📄 MASTER_PROGRESS.md          ← ดูก่อนทุกครั้ง
├── 📄 CHECKPOINT_TEMPLATE.md      ← Template สำหรับ copy สร้าง Phase ใหม่
├── 📄 CLAUDE_CODE_PROMPT.md       ← ไฟล์นี้ (Quick Resume Prompt)
├── 📄 SRS_Document.md             ← สร้างหลัง Phase 0 เสร็จ
│
└── 📁 Checkpoints/
    ├── Phase0_Checkpoint.md       ← Requirements & SRS
    ├── Phase1_Checkpoint.md       ← Backend Core Setup
    ├── Phase2_Checkpoint.md       ← Authentication System
    ├── Phase3_Checkpoint.md       ← Android App Foundation
    ├── Phase4_to_Phase8_Checkpoints.md
    │   ├── Phase 4               ← Real-time Streaming
    │   ├── Phase 5               ← TTS Voice System
    │   ├── Phase 6               ← Admin Control Panel
    │   ├── Phase 7               ← Testing & QA
    │   └── Phase 8               ← Deployment & Launch
```

---

## 🆘 Troubleshooting Prompts

### ถ้าเจอ Error:
```
ฉันเจอ Error นี้ใน Phase [N] Task [X]:

[วาง error message ที่นี่]

Context: [อธิบายว่ากำลังทำอะไรอยู่]
Tech Stack: FastAPI + PostgreSQL + Kotlin + Jetpack Compose
โปรดช่วยแก้ไขและอธิบายสาเหตุ
```

### ถ้าต้องการ Review Code:
```
โปรด Review code ต่อไปนี้ในบริบทของโปรเจ็ค ARIA System
Phase [N] — Task [X]

[วาง code ที่นี่]

ตรวจสอบ: Security, Performance, Best Practices, และ ARIA Design Pattern
```

### ถ้าต้องการ Debug Streaming:
```
ระบบ WebSocket ของโปรเจ็ค ARIA มีปัญหา:
[อธิบายปัญหา]

Architecture: Android WebSocket Client ↔ FastAPI WebSocket Server ↔ Redis Pub/Sub
Phase 4 — [ระบุ Task]
```

---

*ARIA System — Claude Code Master Prompt v1.0*
