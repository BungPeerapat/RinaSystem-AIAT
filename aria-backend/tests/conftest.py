"""
Test fixtures for ARIA Backend.
Uses SQLite in-memory for fast, isolated tests.
"""
import os
import uuid
from datetime import datetime, timezone

import pytest
import pytest_asyncio
from httpx import ASGITransport, AsyncClient
from sqlalchemy import event, text
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.ext.compiler import compiles


# ── Make JSONB render as TEXT in SQLite ────────────────────────────────────────
@compiles(JSONB, "sqlite")
def _compile_jsonb_sqlite(type_, compiler, **kw):
    return "TEXT"

# ── Set env vars BEFORE importing app modules ─────────────────────────────────
os.environ.setdefault("DATABASE_URL", "sqlite+aiosqlite:///:memory:")
os.environ.setdefault("SECRET_KEY", "test-secret-key")
os.environ.setdefault("JWT_SECRET_KEY", "test-jwt-secret-key-for-testing")
os.environ.setdefault("DEBUG", "false")

from app.core.auth_utils import create_access_token, hash_password  # noqa: E402
from app.database import get_db  # noqa: E402
from app.main import app  # noqa: E402
from app.models.base import Base  # noqa: E402
from app.models.role import MasterRole  # noqa: E402, F401 — ensure table registered
from app.models.user import User  # noqa: E402, F401
from app.models.session import Session  # noqa: E402, F401
from app.models.stream_session import StreamSession  # noqa: E402, F401
from app.models.tts_message import TTSMessage  # noqa: E402, F401

# Tables to create in SQLite (skip 'notifications' which uses JSONB)
_TESTABLE_TABLES = [
    MasterRole.__table__,
    User.__table__,
    Session.__table__,
    StreamSession.__table__,
    TTSMessage.__table__,
]

# ── Test Engine (SQLite async in-memory) ──────────────────────────────────────

TEST_DATABASE_URL = "sqlite+aiosqlite:///:memory:"

test_engine = create_async_engine(TEST_DATABASE_URL, echo=False)
TestSessionLocal = async_sessionmaker(test_engine, class_=AsyncSession, expire_on_commit=False)


@event.listens_for(test_engine.sync_engine, "connect")
def _set_sqlite_pragma(dbapi_conn, connection_record):
    """Enable foreign keys in SQLite."""
    cursor = dbapi_conn.cursor()
    cursor.execute("PRAGMA foreign_keys=ON")
    cursor.close()


# ── Session-scoped: create/drop tables once ──────────────────────────────────

@pytest_asyncio.fixture(scope="session")
async def setup_db():
    """Create only testable tables (skip notifications with JSONB)."""
    async with test_engine.begin() as conn:
        for table in _TESTABLE_TABLES:
            await conn.run_sync(table.create, checkfirst=True)
    yield
    async with test_engine.begin() as conn:
        for table in reversed(_TESTABLE_TABLES):
            await conn.run_sync(table.drop, checkfirst=True)
    await test_engine.dispose()


# ── Per-test DB session (rolled back after each test) ────────────────────────

@pytest_asyncio.fixture
async def db_session(setup_db):
    """Provide a clean DB session for each test, cleaned up after."""
    async with TestSessionLocal() as session:
        yield session
        # Clean up all data after test
        for table in reversed(_TESTABLE_TABLES):
            await session.execute(text(f"DELETE FROM {table.name}"))
        await session.commit()


# ── Override FastAPI get_db dependency ────────────────────────────────────────

@pytest_asyncio.fixture
async def client(db_session: AsyncSession):
    """Async HTTP client with overridden DB dependency."""

    async def _override_get_db():
        yield db_session

    app.dependency_overrides[get_db] = _override_get_db
    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac
    app.dependency_overrides.clear()


# ── Seed roles ───────────────────────────────────────────────────────────────

@pytest_asyncio.fixture
async def seed_roles(db_session: AsyncSession):
    """Insert default master_roles (admin + user)."""
    from app.models.role import MasterRole

    admin_role = MasterRole(id=uuid.uuid4(), name="admin", description="Administrator")
    user_role = MasterRole(id=uuid.uuid4(), name="user", description="Regular user")
    db_session.add_all([admin_role, user_role])
    await db_session.commit()
    return {"admin": admin_role, "user": user_role}


# ── Test user fixtures ───────────────────────────────────────────────────────

@pytest_asyncio.fixture
async def test_user(db_session: AsyncSession, seed_roles):
    """Create a regular test user and return (user, access_token)."""
    from app.models.session import Session
    from app.models.user import User

    user_id = uuid.uuid4()
    user = User(
        id=user_id,
        email="testuser@aria.local",
        password_hash=hash_password("password123"),
        display_name="Test User",
        role_id=seed_roles["user"].id,
        is_online=False,
        is_test_user=False,
    )
    db_session.add(user)
    await db_session.flush()

    access_token = create_access_token({"sub": str(user_id)})
    session = Session(
        id=uuid.uuid4(),
        user_id=user_id,
        access_token=access_token,
        refresh_token="dummy-refresh",
    )
    db_session.add(session)
    await db_session.commit()

    return user, access_token


@pytest_asyncio.fixture
async def admin_user(db_session: AsyncSession, seed_roles):
    """Create an admin user and return (user, access_token)."""
    from app.models.session import Session
    from app.models.user import User

    user_id = uuid.uuid4()
    user = User(
        id=user_id,
        email="admin@aria.local",
        password_hash=hash_password("adminpass123"),
        display_name="Admin",
        role_id=seed_roles["admin"].id,
        is_online=False,
        is_test_user=False,
    )
    db_session.add(user)
    await db_session.flush()

    access_token = create_access_token({"sub": str(user_id)})
    session = Session(
        id=uuid.uuid4(),
        user_id=user_id,
        access_token=access_token,
        refresh_token="dummy-admin-refresh",
    )
    db_session.add(session)
    await db_session.commit()

    return user, access_token
