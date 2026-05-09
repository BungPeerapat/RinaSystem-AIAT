import uuid as _uuid

from fastapi import Depends
from fastapi.security import OAuth2PasswordBearer
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.auth_utils import decode_token
from app.core.exceptions import AriaException
from app.database import get_db
from app.models.session import Session
from app.models.user import User, UserStatus

oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/auth/login")


async def get_current_user(
    token: str = Depends(oauth2_scheme),
    db: AsyncSession = Depends(get_db),
) -> User:
    payload = decode_token(token)
    if payload is None or payload.get("type") != "access":
        raise AriaException(401, "Invalid or expired token", "AUTH_TOKEN_INVALID")

    user_id_str = payload.get("sub")
    if user_id_str is None:
        raise AriaException(401, "Invalid token payload", "AUTH_TOKEN_INVALID")

    try:
        user_id = _uuid.UUID(user_id_str)
    except (ValueError, AttributeError):
        raise AriaException(401, "Invalid token payload", "AUTH_TOKEN_INVALID")

    session_result = await db.execute(
        select(Session).where(Session.user_id == user_id, Session.access_token == token)
    )
    if session_result.scalar_one_or_none() is None:
        raise AriaException(401, "Token has been revoked", "AUTH_TOKEN_INVALID")

    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()

    if user is None:
        raise AriaException(401, "User not found", "USER_NOT_FOUND")

    if user.status == UserStatus.blocked:
        raise AriaException(403, "User is blocked", "USER_BLOCKED")

    if user.status == UserStatus.suspended:
        raise AriaException(403, "User is suspended", "USER_BLOCKED")

    return user


async def require_admin(
    user: User = Depends(get_current_user),
) -> User:
    if user.role != "admin":
        raise AriaException(403, "Admin access required", "AUTH_FORBIDDEN")
    return user
