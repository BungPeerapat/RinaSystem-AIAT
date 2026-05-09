import logging

from fastapi import APIRouter, Depends, Request
from slowapi import Limiter
from slowapi.util import get_remote_address
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

limiter = Limiter(key_func=get_remote_address)

from app.core.auth_utils import (
    create_access_token,
    create_refresh_token,
    decode_token,
    hash_password,
    verify_password,
)
from app.core.dependencies import get_current_user
from app.core.exceptions import AriaException
from app.database import get_db
from app.models.role import MasterRole
from app.models.session import Session
from app.models.user import User, UserStatus
from app.schemas.auth import (
    LoginRequest,
    MessageResponse,
    RefreshRequest,
    RegisterRequest,
    TestAccountResponse,
    TestLoginRequest,
    TokenResponse,
    UserResponse,
)

router = APIRouter(prefix="/auth", tags=["Auth"])
logger = logging.getLogger("aria")


@router.post("/register", response_model=TokenResponse, status_code=201)
@limiter.limit("10/minute")
async def register(request: Request, body: RegisterRequest, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(User).where(User.email == body.email))
    if result.scalar_one_or_none() is not None:
        raise AriaException(409, "Email already registered", "AUTH_EMAIL_EXISTS")

    # Lookup default "user" role
    role_result = await db.execute(select(MasterRole).where(MasterRole.name == "user"))
    default_role = role_result.scalar_one_or_none()
    if default_role is None:
        raise AriaException(500, "Default role not configured", "ROLE_NOT_FOUND")

    user = User(
        email=body.email,
        password_hash=hash_password(body.password),
        display_name=body.display_name,
        role_id=default_role.id,
    )
    db.add(user)
    await db.flush()

    access_token = create_access_token({"sub": str(user.id)})
    refresh_token = create_refresh_token({"sub": str(user.id)})

    session = Session(
        user_id=user.id,
        access_token=access_token,
        refresh_token=refresh_token,
    )
    db.add(session)
    await db.commit()
    await db.refresh(user)

    logger.info("User registered: %s", user.email)

    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        user=UserResponse.model_validate(user),
    )


@router.post("/login", response_model=TokenResponse)
@limiter.limit("10/minute")
async def login(request: Request, body: LoginRequest, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(User).where(User.email == body.email))
    user = result.scalar_one_or_none()

    if user is None or not verify_password(body.password, user.password_hash):
        raise AriaException(401, "Invalid email or password", "AUTH_INVALID_CREDENTIALS")

    if user.status == UserStatus.blocked:
        raise AriaException(403, "User is blocked", "USER_BLOCKED")

    if user.status == UserStatus.suspended:
        raise AriaException(403, "User is suspended", "USER_BLOCKED")

    access_token = create_access_token({"sub": str(user.id)})
    refresh_token = create_refresh_token({"sub": str(user.id)})

    session = Session(
        user_id=user.id,
        access_token=access_token,
        refresh_token=refresh_token,
    )
    db.add(session)
    await db.commit()

    logger.info("User logged in: %s", user.email)

    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        user=UserResponse.model_validate(user),
    )


@router.post("/refresh", response_model=TokenResponse)
@limiter.limit("10/minute")
async def refresh(request: Request, body: RefreshRequest, db: AsyncSession = Depends(get_db)):
    payload = decode_token(body.refresh_token)
    if payload is None or payload.get("type") != "refresh":
        raise AriaException(401, "Invalid refresh token", "AUTH_TOKEN_INVALID")

    result = await db.execute(
        select(Session).where(Session.refresh_token == body.refresh_token)
    )
    session = result.scalar_one_or_none()

    if session is None:
        raise AriaException(401, "Refresh token not found or revoked", "AUTH_TOKEN_INVALID")

    import uuid as _uuid
    user_id = _uuid.UUID(payload.get("sub"))
    result = await db.execute(select(User).where(User.id == user_id))
    user = result.scalar_one_or_none()

    if user is None:
        raise AriaException(401, "User not found", "USER_NOT_FOUND")

    new_access = create_access_token({"sub": str(user.id)})
    new_refresh = create_refresh_token({"sub": str(user.id)})

    session.access_token = new_access
    session.refresh_token = new_refresh
    await db.commit()

    return TokenResponse(
        access_token=new_access,
        refresh_token=new_refresh,
        user=UserResponse.model_validate(user),
    )


@router.post("/logout", response_model=MessageResponse)
async def logout(
    user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    result = await db.execute(
        select(Session).where(Session.user_id == user.id)
    )
    sessions = result.scalars().all()
    for s in sessions:
        await db.delete(s)
    await db.commit()

    logger.info("User logged out: %s", user.email)

    return MessageResponse(message="Logged out successfully")


@router.get("/test-accounts", response_model=list[TestAccountResponse])
async def get_test_accounts(db: AsyncSession = Depends(get_db)):
    result = await db.execute(
        select(User).where(User.is_test_user == True, User.status == UserStatus.active)  # noqa: E712
    )
    users = result.scalars().all()
    return [TestAccountResponse.model_validate(u) for u in users]


@router.post("/test-login", response_model=TokenResponse)
@limiter.limit("10/minute")
async def test_login(request: Request, body: TestLoginRequest, db: AsyncSession = Depends(get_db)):
    result = await db.execute(select(User).where(User.id == body.user_id))
    user = result.scalar_one_or_none()

    if user is None:
        raise AriaException(404, "User not found", "USER_NOT_FOUND")

    if not user.is_test_user:
        raise AriaException(403, "This account is not a test account", "NOT_TEST_ACCOUNT")

    if user.status == UserStatus.blocked:
        raise AriaException(403, "User is blocked", "USER_BLOCKED")

    access_token = create_access_token({"sub": str(user.id)})
    refresh_token = create_refresh_token({"sub": str(user.id)})

    session = Session(
        user_id=user.id,
        access_token=access_token,
        refresh_token=refresh_token,
    )
    db.add(session)
    await db.commit()

    logger.info("Test login: %s", user.email)

    return TokenResponse(
        access_token=access_token,
        refresh_token=refresh_token,
        user=UserResponse.model_validate(user),
    )
