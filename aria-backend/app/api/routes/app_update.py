import json
import logging
from pathlib import Path

from fastapi import APIRouter, Depends, Form, UploadFile
from fastapi.responses import FileResponse

from app.config import settings
from app.core.dependencies import require_admin
from app.core.exceptions import AriaException
from app.models.user import User

router = APIRouter(prefix="/app", tags=["App Update"])
logger = logging.getLogger("aria")

RELEASES_DIR = Path(settings.APP_RELEASES_DIR)
VERSION_FILE = RELEASES_DIR / "version.json"


def _ensure_releases_dir() -> None:
    RELEASES_DIR.mkdir(parents=True, exist_ok=True)


def _read_version_info() -> dict:
    if not VERSION_FILE.exists():
        return {"version_code": 0}
    try:
        return json.loads(VERSION_FILE.read_text(encoding="utf-8"))
    except Exception:
        return {"version_code": 0}


@router.get("/version")
async def get_latest_version():
    """เช็ค latest version ของแอป (public, ไม่ต้อง auth)"""
    return _read_version_info()


@router.post("/upload")
async def upload_apk(
    apk: UploadFile,
    version_code: int = Form(...),
    version_name: str = Form(...),
    release_notes: str = Form(""),
    force_update: bool = Form(False),
    _admin: User = Depends(require_admin),
):
    """Admin อัพโหลด APK ใหม่"""
    if not apk.filename or not apk.filename.endswith(".apk"):
        raise AriaException(400, "ไฟล์ต้องเป็น .apk", "INVALID_FILE_TYPE")

    _ensure_releases_dir()

    # ตั้งชื่อไฟล์: aria-v{version_name}.apk
    safe_name = f"aria-v{version_name}.apk"
    file_path = RELEASES_DIR / safe_name

    # บันทึกไฟล์ APK
    content = await apk.read()
    file_path.write_bytes(content)

    # เขียน version.json
    version_info = {
        "version_code": version_code,
        "version_name": version_name,
        "download_url": f"/api/app/download/{safe_name}",
        "release_notes": release_notes,
        "force_update": force_update,
        "file_size": len(content),
    }
    VERSION_FILE.write_text(
        json.dumps(version_info, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    logger.info(f"APK uploaded: {safe_name} (v{version_code})")

    return {"message": f"อัพโหลด {safe_name} สำเร็จ", **version_info}


@router.get("/download/{filename}")
async def download_apk(filename: str):
    """ดาวน์โหลด APK file (public, ไม่ต้อง auth)"""
    # ป้องกัน path traversal
    safe_name = Path(filename).name
    if ".." in safe_name or "/" in safe_name or "\\" in safe_name:
        raise AriaException(400, "Invalid filename", "INVALID_FILENAME")

    file_path = RELEASES_DIR / safe_name

    if not file_path.exists() or not file_path.is_file():
        raise AriaException(404, "ไม่พบไฟล์ APK", "FILE_NOT_FOUND")

    return FileResponse(
        path=str(file_path),
        filename=safe_name,
        media_type="application/vnd.android.package-archive",
    )
