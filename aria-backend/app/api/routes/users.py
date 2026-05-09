from fastapi import APIRouter, Depends, Query
from sqlalchemy import or_, select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.dependencies import get_current_user
from app.database import get_db
from app.models.character_config import CharacterConfig
from app.models.tts_message import TTSMessage
from app.models.user import User
from app.schemas.auth import MessageResponse, UserResponse
from app.schemas.character import CharacterListResponse, CharacterResponse
from app.schemas.user import UpdateFCMTokenRequest, UpdateUserRequest

router = APIRouter(prefix="/users", tags=["Users"])


@router.get("/me", response_model=UserResponse)
async def get_me(user: User = Depends(get_current_user)):
    return UserResponse.model_validate(user)


@router.put("/me", response_model=UserResponse)
async def update_me(
    body: UpdateUserRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    if body.display_name is not None:
        user.display_name = body.display_name

    await db.commit()
    await db.refresh(user)

    return UserResponse.model_validate(user)


@router.put("/me/fcm-token", response_model=MessageResponse)
async def update_fcm_token(
    body: UpdateFCMTokenRequest,
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    user.fcm_token = body.fcm_token
    await db.commit()

    return MessageResponse(message="FCM token updated")


@router.get("/characters", response_model=CharacterListResponse)
async def get_characters(
    _user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """ดึง Character list ที่ Admin ตั้งค่าไว้ (User ใช้สำหรับ Trigger)"""
    result = await db.execute(
        select(CharacterConfig).order_by(CharacterConfig.sort_order, CharacterConfig.name)
    )
    chars = result.scalars().all()
    return CharacterListResponse(
        characters=[CharacterResponse.model_validate(c) for c in chars],
        total=len(chars),
    )


@router.get("/me/messages")
async def get_my_messages(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
    limit: int = Query(50, ge=1, le=200),
    offset: int = Query(0, ge=0),
):
    """ดึงประวัติ TTS messages ที่ส่งถึง User นี้ (รวม broadcast)"""
    stmt = (
        select(TTSMessage)
        .where(
            or_(
                TTSMessage.receiver_id == user.id,
                TTSMessage.receiver_id.is_(None),  # broadcast
            )
        )
        .order_by(TTSMessage.created_at.desc())
        .offset(offset)
        .limit(limit)
    )
    result = await db.execute(stmt)
    messages = result.scalars().all()

    return {
        "messages": [
            {
                "id": str(msg.id),
                "content": msg.content,
                "status": msg.status.value,
                "is_broadcast": msg.receiver_id is None,
                "created_at": msg.created_at.isoformat(),
            }
            for msg in messages
        ]
    }
