# ARIA Version Management

## Version File

Version ของแอปถูกจัดการจากไฟล์เดียว:

```
version.properties
```

```properties
VERSION_CODE=1
VERSION_NAME=1.0.0
```

| Field | Description | Example |
|---|---|---|
| `VERSION_CODE` | เลข integer ที่เพิ่มขึ้นทุกครั้งที่ release (Android ใช้เทียบว่า update ได้หรือไม่) | `1`, `2`, `3` |
| `VERSION_NAME` | เลข version ที่แสดงให้ user เห็น (Semantic Versioning) | `1.0.0`, `1.1.0`, `2.0.0` |

## !! ต้องเปลี่ยน Version ทุกครั้งก่อน Release !!

**ทุกครั้ง** ที่จะ build APK สำหรับ release หรืออัพโหลดผ่าน Auto Update ต้อง:

1. เปิดไฟล์ `version.properties`
2. เพิ่ม `VERSION_CODE` ขึ้น 1 (เช่น `1` -> `2`)
3. เปลี่ยน `VERSION_NAME` ตามความเหมาะสม (เช่น `1.0.0` -> `1.1.0`)
4. Build APK (`./gradlew assembleRelease`)

### Versioning Rules

- **VERSION_CODE ต้องเพิ่มขึ้นเสมอ** — ห้ามลด ห้ามซ้ำ (Android จะไม่ยอมให้ install ถ้า code ต่ำกว่าที่ติดตั้งอยู่)
- **VERSION_NAME** ใช้ Semantic Versioning: `MAJOR.MINOR.PATCH`
  - `MAJOR` — เปลี่ยนเมื่อมี breaking change หรือ redesign ครั้งใหญ่
  - `MINOR` — เปลี่ยนเมื่อเพิ่ม feature ใหม่
  - `PATCH` — เปลี่ยนเมื่อ fix bug

### Example

```properties
# Release ครั้งแรก
VERSION_CODE=1
VERSION_NAME=1.0.0

# แก้ bug เล็กน้อย
VERSION_CODE=2
VERSION_NAME=1.0.1

# เพิ่ม feature TTS timeout toggle
VERSION_CODE=3
VERSION_NAME=1.1.0

# Redesign UI ใหม่ทั้งหมด
VERSION_CODE=4
VERSION_NAME=2.0.0
```

## Version แสดงที่ไหน

- **Settings Screen** — ด้านล่างสุดของหน้าตั้งค่าเซิร์ฟเวอร์ แสดง `ARIA v1.0.0 (build 1)`
- **Auto Update** — Backend เทียบ `VERSION_CODE` ของแอปกับ version ล่าสุดบน Server เพื่อแจ้ง update

## How It Works

```
version.properties          (แก้ไขที่นี่)
       |
       v
build.gradle.kts            (อ่าน version.properties ตอน build)
       |
       v
BuildConfig.VERSION_CODE    (ใช้ในโค้ด Kotlin)
BuildConfig.VERSION_NAME
       |
       v
Settings Screen UI          (แสดงให้ user เห็น)
Auto Update check           (เทียบกับ Server)
```
