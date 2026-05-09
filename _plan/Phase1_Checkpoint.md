# 📋 Phase 1 Checkpoint — Backend Core Setup
> **สถานะ:** ⏳ NOT STARTED (รอ Phase 0 เสร็จก่อน)

---

## 📋 Checkpoint Info

| Field | Value |
|---|---|
| **Phase** | Phase 1 — Backend Core Setup |
| **เริ่มวันที่** | - |
| **อัปเดตล่าสุด** | - |
| **สถานะโดยรวม** | ⏳ NOT STARTED |

---

## 🎯 เป้าหมายของ Phase นี้

ตั้งค่า Backend Server ด้วย FastAPI + PostgreSQL ให้รันได้
พร้อม Docker Compose, DB Migration, และ Project Structure ที่ถูกต้อง

---

## 📌 Task List

### 🗂️ 1. Project Structure
| # | Task | สถานะ | หมายเหตุ |
|---|---|---|---|
| 1.1 | สร้าง FastAPI project structure | ⏳ | |
| 1.2 | ตั้งค่า virtual environment / requirements.txt | ⏳ | |
| 1.3 | สร้าง `.env` file + `.env.example` | ⏳ | |
| 1.4 | ตั้งค่า `config.py` (Settings class) | ⏳ | |
| 1.5 | สร้าง `main.py` entry point | ⏳ | |

### 🐘 2. PostgreSQL + SQLAlchemy
| # | Task | สถานะ | หมายเหตุ |
|---|---|---|---|
| 2.1 | ติดตั้ง SQLAlchemy + asyncpg | ⏳ | |
| 2.2 | สร้าง Database connection (`database.py`) | ⏳ | |
| 2.3 | สร้าง Base Model class | ⏳ | |
| 2.4 | สร้าง Models: `users`, `sessions`, `messages`, `stream_sessions` | ⏳ | |

### 🔄 3. Alembic Migrations
| # | Task | สถานะ | หมายเหตุ |
|---|---|---|---|
| 3.1 | ติดตั้งและ init Alembic | ⏳ | |
| 3.2 | สร้าง initial migration (all tables) | ⏳ | |
| 3.3 | รัน migration → ยืนยัน tables ใน DB | ⏳ | |

### 🐳 4. Docker Setup
| # | Task | สถานะ | หมายเหตุ |
|---|---|---|---|
| 4.1 | สร้าง `Dockerfile` สำหรับ FastAPI | ⏳ | |
| 4.2 | สร้าง `docker-compose.yml` (api + postgres + redis) | ⏳ | |
| 4.3 | ตั้งค่า volumes สำหรับ postgres data | ⏳ | |
| 4.4 | ตั้งค่า volumes สำหรับ audio storage | ⏳ | |
| 4.5 | รัน `docker-compose up` → ผ่าน health check | ⏳ | |

### 🌐 5. Basic API Setup
| # | Task | สถานะ | หมายเหตุ |
|---|---|---|---|
| 5.1 | ตั้งค่า CORS middleware | ⏳ | |
| 5.2 | สร้าง `GET /health` endpoint | ⏳ | |
| 5.3 | ตั้งค่า API Router structure | ⏳ | |
| 5.4 | ตั้งค่า Exception Handlers | ⏳ | |
| 5.5 | ตั้งค่า Logging | ⏳ | |

### 🔴 6. Redis Setup
| # | Task | สถานะ | หมายเหตุ |
|---|---|---|---|
| 6.1 | ติดตั้ง redis + aioredis | ⏳ | |
| 6.2 | สร้าง Redis connection | ⏳ | |
| 6.3 | ทดสอบ Redis pub/sub | ⏳ | |

---

## 📁 Output Files ที่ต้องได้

```
aria-backend/
├── app/
│   ├── main.py
│   ├── config.py
│   ├── database.py
│   ├── models/
│   │   ├── __init__.py
│   │   ├── user.py
│   │   ├── message.py
│   │   └── stream_session.py
│   ├── api/
│   │   └── routes/
│   └── core/
├── migrations/
│   └── versions/
├── .env.example
├── requirements.txt
├── Dockerfile
└── docker-compose.yml
```

- [ ] Docker compose รันได้ไม่มี error
- [ ] `GET /health` ตอบ `{ status: "ok" }`
- [ ] Database tables สร้างสำเร็จ (ตรวจใน pgAdmin หรือ psql)

---

## ➡️ เงื่อนไขก่อนไป Phase 2

- [ ] `docker-compose up` รันได้สำเร็จ
- [ ] Tables ทั้งหมดถูกสร้างใน PostgreSQL
- [ ] `/health` endpoint ตอบกลับได้
- [ ] Redis connection ทำงานได้

---

## 🔁 Claude Code Resume Prompt

```
ฉันกำลังทำโปรเจ็ค ARIA System — Backend (FastAPI + PostgreSQL)
อยู่ที่ Phase 1: Backend Core Setup

ทำถึง Task: [บอก task ที่ค้างอยู่]
สิ่งที่เสร็จแล้ว: [สรุปสั้นๆ]
ปัญหาที่เจอ: [ถ้ามี]

โปรดช่วยทำ Task ถัดไปให้ฉัน
```

---

*Phase 1 Checkpoint — ARIA Project*
