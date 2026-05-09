import base64
import os
import uuid
from pathlib import Path

from fastapi import APIRouter, Depends
from sqlalchemy import delete, func, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.dependencies import require_admin
from app.core.exceptions import AriaException
from app.core.ws_manager import ws_manager
from app.database import get_db
from app.models.character_config import CharacterConfig
from app.models.role import MasterRole
from app.models.stream_session import StreamSession
from app.models.tts_message import TTSMessage
from app.models.user import User
from app.schemas.admin import (
    DashboardResponse,
    StreamSessionResponse,
    UpdateRoleRequest,
    UpdateStatusRequest,
    UserListResponse,
)
from app.schemas.auth import MessageResponse, UserResponse
from app.schemas.character import CharacterCreateRequest, CharacterListResponse, CharacterResponse

router = APIRouter(prefix="/admin", tags=["Admin"])


@router.get("/users", response_model=UserListResponse)
async def list_users(
    _admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(select(User))
    users = result.scalars().all()

    online_count = sum(1 for u in users if u.is_online)

    return UserListResponse(
        users=[UserResponse.model_validate(u) for u in users],
        total=len(users),
        online_count=online_count,
    )


@router.get("/users/{user_id}", response_model=UserResponse)
async def get_user(
    user_id: uuid.UUID,
    _admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()

    if user is None:
        raise AriaException(404, "User not found", "USER_NOT_FOUND")

    return UserResponse.model_validate(user)


@router.put("/users/{user_id}/status", response_model=MessageResponse)
async def update_user_status(
    user_id: uuid.UUID,
    body: UpdateStatusRequest,
    _admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()

    if user is None:
        raise AriaException(404, "User not found", "USER_NOT_FOUND")

    user.status = body.status
    await db.commit()

    return MessageResponse(message=f"User status updated to {body.status.value}")


@router.put("/users/{user_id}/role", response_model=UserResponse)
async def update_user_role(
    user_id: uuid.UUID,
    body: UpdateRoleRequest,
    _admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()

    if user is None:
        raise AriaException(404, "User not found", "USER_NOT_FOUND")

    role_result = await db.execute(
        select(MasterRole).where(MasterRole.name == body.role)
    )
    role = role_result.scalar_one_or_none()

    if role is None:
        raise AriaException(400, f"Role '{body.role}' not found", "ROLE_NOT_FOUND")

    user.role_id = role.id
    await db.commit()
    await db.refresh(user)

    return UserResponse.model_validate(user)


@router.get("/users/{user_id}/streams", response_model=list[StreamSessionResponse])
async def get_user_streams(
    user_id: uuid.UUID,
    _admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(StreamSession)
        .where(StreamSession.user_id == user_id)
        .order_by(StreamSession.started_at.desc())
    )
    sessions = result.scalars().all()

    return [StreamSessionResponse.model_validate(s) for s in sessions]


@router.get("/dashboard", response_model=DashboardResponse)
async def dashboard(
    _admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    total_users = await db.scalar(select(func.count()).select_from(User))
    online_count = await db.scalar(
        select(func.count()).select_from(User).where(User.is_online == True)  # noqa: E712
    )
    total_streams = await db.scalar(select(func.count()).select_from(StreamSession))
    total_messages = await db.scalar(select(func.count()).select_from(TTSMessage))

    return DashboardResponse(
        total_users=total_users or 0,
        online_count=online_count or 0,
        total_streams=total_streams or 0,
        total_messages=total_messages or 0,
    )


@router.get("/streams/active")
async def get_active_streams(
    _admin: User = Depends(require_admin),
):
    """ดู real-time streams ที่กำลัง active อยู่ (จาก WebSocket manager)"""
    return {"streams": ws_manager.get_active_streams()}


@router.get("/streams/online-users")
async def get_online_users(
    _admin: User = Depends(require_admin),
):
    """ดู User IDs ที่เชื่อม WebSocket อยู่"""
    return {"user_ids": ws_manager.online_user_ids()}


@router.delete("/messages/clear", response_model=MessageResponse)
async def clear_messages(
    _admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    await db.execute(delete(TTSMessage))
    await db.commit()

    return MessageResponse(message="All TTS messages cleared")


# ─── TTS Models & Speakers ────────────────────────────────────────────────────

@router.get("/tts/models")
async def list_tts_models(
    _admin: User = Depends(require_admin),
):
    """ดึงรายชื่อ TTS models จาก Moe-TTS API"""
    from app.core.tts_service import get_tts_models
    models = await get_tts_models()
    return {"models": models}


@router.get("/tts/speakers")
async def list_tts_speakers(
    model_id: int = 0,
    _admin: User = Depends(require_admin),
):
    """ดึงรายชื่อ speakers ของ model จาก Moe-TTS API"""
    from app.core.tts_service import get_tts_speakers
    speakers = await get_tts_speakers(model_id)
    return {"speakers": speakers, "model_id": model_id}


# ─── Audio Presets ────────────────────────────────────────────────────────────

AUDIO_PRESET_DIR = Path(__file__).resolve().parent.parent.parent.parent / "_Audio_Preset"
ALLOWED_EXTENSIONS = {".mp3", ".wav", ".mp4", ".ogg", ".m4a", ".flac"}


# ─── Character Config ──────────────────────────────────────────────────────────

@router.get("/characters", response_model=CharacterListResponse)
async def list_characters(
    _admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(select(CharacterConfig).order_by(CharacterConfig.sort_order, CharacterConfig.name))
    chars = result.scalars().all()
    return CharacterListResponse(
        characters=[CharacterResponse.model_validate(c) for c in chars],
        total=len(chars),
    )


@router.post("/characters", response_model=CharacterResponse, status_code=201)
async def create_character(
    body: CharacterCreateRequest,
    _admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    char = CharacterConfig(
        name=body.name,
        model_id=body.model_id,
        speaker_id=body.speaker_id,
        emoji=body.emoji,
        sort_order=body.sort_order,
    )
    db.add(char)
    await db.commit()
    await db.refresh(char)
    return CharacterResponse.model_validate(char)


@router.put("/characters/{character_id}", response_model=CharacterResponse)
async def update_character(
    character_id: uuid.UUID,
    body: CharacterCreateRequest,
    _admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(select(CharacterConfig).where(CharacterConfig.id == character_id))
    char = result.scalar_one_or_none()
    if char is None:
        raise AriaException(404, "Character not found", "CHARACTER_NOT_FOUND")
    char.name = body.name
    char.model_id = body.model_id
    char.speaker_id = body.speaker_id
    char.emoji = body.emoji
    char.sort_order = body.sort_order
    await db.commit()
    await db.refresh(char)
    return CharacterResponse.model_validate(char)


@router.delete("/characters/{character_id}", response_model=MessageResponse)
async def delete_character(
    character_id: uuid.UUID,
    _admin: User = Depends(require_admin),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(select(CharacterConfig).where(CharacterConfig.id == character_id))
    char = result.scalar_one_or_none()
    if char is None:
        raise AriaException(404, "Character not found", "CHARACTER_NOT_FOUND")
    await db.delete(char)
    await db.commit()
    return MessageResponse(message="Character deleted")


@router.get("/audio-presets")
async def list_audio_presets(
    _admin: User = Depends(require_admin),
):
    """ดึงรายชื่อไฟล์เสียงจาก _Audio_Preset folder"""
    if not AUDIO_PRESET_DIR.exists():
        return {"presets": []}

    presets = []
    for f in sorted(AUDIO_PRESET_DIR.iterdir()):
        if f.is_file() and f.suffix.lower() in ALLOWED_EXTENSIONS:
            presets.append({
                "name": f.name,
                "size": f.stat().st_size,
                "extension": f.suffix.lower(),
            })
    return {"presets": presets}


@router.get("/audio-presets/{filename}")
async def get_audio_preset(
    filename: str,
    _admin: User = Depends(require_admin),
):
    """ดึงไฟล์เสียง preset เป็น base64"""
    # ป้องกัน path traversal
    safe_name = Path(filename).name
    file_path = AUDIO_PRESET_DIR / safe_name

    if not file_path.exists() or not file_path.is_file():
        raise AriaException(404, "Preset not found", "PRESET_NOT_FOUND")

    if file_path.suffix.lower() not in ALLOWED_EXTENSIONS:
        raise AriaException(400, "Invalid file type", "INVALID_FILE_TYPE")

    audio_bytes = file_path.read_bytes()
    audio_b64 = base64.b64encode(audio_bytes).decode("utf-8")

    return {
        "name": safe_name,
        "audio_base64": audio_b64,
        "size": len(audio_bytes),
    }
