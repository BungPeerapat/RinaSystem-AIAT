# 📋 Phase 0 Checkpoint — Requirements & SRS
> **สถานะ:** ✅ DONE

---

## 📋 Checkpoint Info

| Field | Value |
|---|---|
| **Phase** | Phase 0 — Requirements Elicitation & SRS |
| **เริ่มวันที่** | 10/03/2026 |
| **อัปเดตล่าสุด** | 10/03/2026 |
| **สถานะโดยรวม** | ✅ DONE |

---

## 🎯 เป้าหมายของ Phase นี้

รวบรวม Requirement จากทุก Phase คำถาม (0–6) และสร้าง SRS Document
สมบูรณ์ก่อนเริ่มเขียน Code แม้แต่บรรทัดเดียว

---

## 📌 Task List — BA Elicitation Questions

### 🔵 Phase คำถาม 1 : Business Context
| # | คำถาม | ตอบแล้ว | คำตอบสรุป |
|---|---|---|---|
| Q1 | แอปใช้ภายในองค์กร หรือเผยแพร่สาธารณะ? | ✅ | ใช้ส่วนตัว / ภายในกลุ่ม (APK โดยตรง ไม่ขึ้น Play Store) |
| Q2 | กลุ่มผู้ใช้งานหลักคือใคร? | ✅ | ทีมงาน / กลุ่มเพื่อน |
| Q3 | จำนวน User พร้อมกันสูงสุด? | ✅ | 1-5 คน |
| Q4 | แอปแก้ Pain Point อะไร? | ✅ | ควบคุม/สื่อสารกับ Device + Monitoring/Surveillance + Entertainment/Vtuber |
| Q5 | มี Competitor หรือระบบเดิมไหม? | ✅ | ไม่มี สร้างใหม่ทั้งหมด |
| Q6 | Timeline และ Deadline? | ✅ | ไม่มี Deadline ทำตามสะดวก |
| Q7 | งบประมาณ Infrastructure? | ✅ | ฟรี / ใช้เครื่องตัวเอง |

**สถานะ Phase นี้:** ✅

---

### 🔵 Phase คำถาม 2 : User Roles & Permissions
| # | คำถาม | ตอบแล้ว | คำตอบสรุป |
|---|---|---|---|
| Q8 | มี Role อะไรบ้าง? | ✅ | Admin + User (2 roles) |
| Q9 | Admin มีกี่คน? สิทธิ์ต่างกันไหม? | ✅ | Admin คนเดียว (ตัวเอง) |
| Q10 | User สมัครเองหรือต้อง Admin อนุมัติ? | ✅ | User สมัครเองได้เลย |
| Q11 | Admin account สร้างอย่างไร? | ✅ | Hardcode หรือ seed ในระบบ (Admin คนเดียว) |
| Q12 | Block/Suspend User ได้ไหม? | ✅ | ได้ |
| Q13 | Login หลาย Device พร้อมกันได้ไหม? | ✅ | ได้ หลาย Device |
| Q14 | Admin 2 คนฟัง User เดียวกันพร้อมกัน? | ✅ | ไม่ได้ — Admin มีคนเดียว |

**สถานะ Phase นี้:** ✅

---

### 🔵 Phase คำถาม 3A : Authentication
| # | คำถาม | ตอบแล้ว | คำตอบสรุป |
|---|---|---|---|
| Q15 | ต้องการ Social Login ไหม? | ✅ | ไม่ — Email/Password เท่านั้น |
| Q16 | Forgot Password — Email หรือ OTP? | ✅ | ไม่ต้องการ |
| Q17 | ต้องการ 2FA ไหม? | ✅ | ไม่ต้องการ |
| Q18 | Session timeout เท่าไหร่? | ✅ | ไม่มี timeout — Login ครั้งเดียวใช้ได้ตลอด |
| Q19 | Biometric Login ต้องการไหม? | ✅ | ทำทีหลัง (nice to have) |

**สถานะ Phase นี้:** ✅

---

### 🔵 Phase คำถาม 3B : Microphone Streaming
| # | คำถาม | ตอบแล้ว | คำตอบสรุป |
|---|---|---|---|
| Q20 | User ต้อง Accept ทุกครั้ง หรือ Grant ครั้งเดียว? | ✅ | Admin ฟังได้เลย ไม่ต้องขออนุญาต User |
| Q21 | Audio — Real-time stream หรือ Record แล้วส่ง? | ✅ | Real-time stream |
| Q22 | บันทึกเสียงไว้ใน Server ไหม? | ✅ | ได้ — บันทึกไว้ใน Server |
| Q23 | Background listening ทำได้ไหม? | ✅ | ได้ — ฟังได้แม้ User ปิดแอปหรือล็อคหน้าจอ |
| Q24 | แสดง indicator ว่ากำลังถูกฟังไหม? | ✅ | ไม่แสดง — เงียบๆ |

**สถานะ Phase นี้:** ✅

---

### 🔵 Phase คำถาม 3C : Camera Streaming
| # | คำถาม | ตอบแล้ว | คำตอบสรุป |
|---|---|---|---|
| Q25 | กล้องหน้า/หลัง หรือทั้งคู่? | ✅ | ทั้งคู่ — สลับกล้องหน้า/หลังได้ |
| Q26 | Real-time video หรือแค่ Snapshot? | ✅ | Real-time video |
| Q27 | ความละเอียด Video? | ✅ | ต่ำ (480p) เพื่อประหยัด bandwidth |
| Q28 | บันทึก Video ใน Server ไหม? | ✅ | ได้ — บันทึกไว้ |
| Q29 | User เห็นว่าถูกเปิดกล้องไหม? | ✅ | ไม่เห็น — เงียบๆ |

**สถานะ Phase นี้:** ✅

---

### 🔵 Phase คำถาม 3D : TTS Voice System
| # | คำถาม | ตอบแล้ว | คำตอบสรุป |
|---|---|---|---|
| Q30 | TTS Engine อยู่ Server เดียวกับ Backend ไหม? | ✅ | ได้ — Server เดียวกัน |
| Q31 | OS ของ TTS Server คืออะไร? | ✅ | Windows |
| Q32 | User เลือก Voice Profile ได้ไหม? | ✅ | ไม่ได้ — ใช้เสียงตาม Moe-TTS ที่ตั้งไว้ |
| Q33 | User ต้องกด Play เอง หรือ Auto-play? | ✅ | Auto-play |
| Q34 | หลาย Message — Queue หรือรอ User กด? | ✅ | Queue — เล่นต่อกันอัตโนมัติ |
| Q35 | เก็บประวัติ TTS Messages กี่วัน? | ✅ | ไม่จำกัด + Admin สั่ง Clear ได้ |

**สถานะ Phase นี้:** ✅

---

### 🔵 Phase คำถาม 3E : Notification System
| # | คำถาม | ตอบแล้ว | คำตอบสรุป |
|---|---|---|---|
| Q36 | In-App เท่านั้น หรือ Push Notification (FCM) ด้วย? | ✅ | ทั้งคู่ — In-App + FCM Push |
| Q37 | ส่ง Notification ตอนปิดแอปได้ไหม? | ✅ | ได้ — ผ่าน FCM |
| Q38 | Notification Type มีอะไรบ้าง? | ✅ | TTS message, Stream request, System alert, Admin broadcast |
| Q39 | ต้องการ Notification History ไหม? | ✅ | ได้ — เก็บประวัติ |

**สถานะ Phase นี้:** ✅

---

### 🔵 Phase คำถาม 4 : Technical & Infrastructure
| # | คำถาม | ตอบแล้ว | คำตอบสรุป |
|---|---|---|---|
| Q40 | Server อยู่ที่ไหน? | ✅ | เครื่องตัวเอง (PC/Laptop) |
| Q41 | OS ของ Server คืออะไร? | ✅ | Windows |
| Q42 | Spec: RAM/CPU/Storage? | ✅ | ตามเครื่องส่วนตัว |
| Q43 | มี Domain? ต้องการ SSL ไหม? | ✅ | ยังไม่มี — ใช้ IP/Ngrok ก่อน |
| Q44 | ใช้ Docker ไหม? | ✅ | ไม่ใช้ — รันตรงบน Windows |
| Q45 | ต้องการ CI/CD ไหม? | ✅ | ไม่ต้องการ |
| Q46 | DB อยู่ Server เดียวกับ Backend ไหม? | ✅ | ได้ — เครื่องเดียวกัน |
| Q47 | ต้องการ DB Backup อัตโนมัติไหม? | ✅ | ไม่ต้องการ |
| Q48 | ต้องการ Monitoring/Alerting ไหม? | ✅ | ไม่ต้องการ |

**สถานะ Phase นี้:** ✅

---

### 🔵 Phase คำถาม 5 : UX/UI Requirements
| # | คำถาม | ตอบแล้ว | คำตอบสรุป |
|---|---|---|---|
| Q49 | Dark Mode อย่างเดียว หรือ Light Mode ด้วย? | ✅ | Dark Mode อย่างเดียว |
| Q50 | ภาษา: ไทย / อังกฤษ / ทั้งคู่? | ✅ | ไทย |
| Q51 | มี Branding/Logo ไหม? | ✅ | ใช้ ARIA branding / sci-fi theme |
| Q52 | ต้องการ Onboarding Screen ไหม? | ✅ | ต้องการ |
| Q53 | Android เวอร์ชันต่ำสุด? | ✅ | Android 14 (API 34) |
| Q54 | รองรับ Tablet ไหม? | ✅ | รองรับ |
| Q55 | มี Reference UI ที่ชอบไหม? | ✅ | Sci-fi / HUD / Neon theme |

**สถานะ Phase นี้:** ✅

---

### 🔵 Phase คำถาม 6 : Risk & Edge Cases
| # | คำถาม | ตอบแล้ว | คำตอบสรุป |
|---|---|---|---|
| Q56 | Network หลุดระหว่าง Stream — ทำอะไร? | ✅ | Auto-reconnect |
| Q57 | TTS Server ล่ม — Fallback คืออะไร? | ✅ | แสดง Error ให้ Admin (ไม่มี fallback TTS) |
| Q58 | ต้องการ Offline Mode ไหม? | ✅ | ไม่ต้องการ |
| Q59 | ต้องการ E2E Encryption ไหม? | ✅ | ไม่ต้องการ |
| Q60 | User ถอน Permission กลาง Session — ทำอะไร? | ✅ | หยุด stream + แจ้ง Admin |
| Q61 | มี Compliance ที่เกี่ยวข้องไหม? (PDPA?) | ✅ | ไม่มี — ใช้ส่วนตัว |

**สถานะ Phase นี้:** ✅

---

## 📄 Output ที่ต้องได้จาก Phase 0

- [x] คำตอบ Q1–Q61 ครบถ้วน
- [x] `SRS_Document.md` — Software Requirements Specification ฉบับสมบูรณ์
- [x] `Tech_Stack_Decision.md` — เหตุผลที่เลือก Tech Stack แต่ละอย่าง
- [x] `System_Architecture.md` — Diagram ระบบ (Mermaid)
- [x] `DB_ERD.md` — Entity Relationship Diagram
- [x] `API_List.md` — รายการ API Endpoints ทั้งหมด

---

## ➡️ เงื่อนไขก่อนไป Phase 1

- [x] ตอบครบทุกคำถาม Q1–Q61
- [x] SRS Document สร้างเสร็จ
- [ ] User/Owner อนุมัติ SRS แล้ว
- [x] Tech Stack ตัดสินใจสุดท้ายแล้ว

---

*Phase 0 Checkpoint — ARIA Project*
