"""Tests for /api/auth/ endpoints."""
import pytest


class TestRegister:
    @pytest.mark.asyncio
    async def test_register_success(self, client, seed_roles):
        resp = await client.post("/api/auth/register", json={
            "email": "new@test.com",
            "password": "password123",
            "display_name": "New User",
        })
        assert resp.status_code == 201
        data = resp.json()
        assert "access_token" in data
        assert "refresh_token" in data
        assert data["user"]["email"] == "new@test.com"
        assert data["user"]["display_name"] == "New User"
        assert data["user"]["role"] == "user"

    @pytest.mark.asyncio
    async def test_register_duplicate_email(self, client, seed_roles):
        await client.post("/api/auth/register", json={
            "email": "dup@test.com",
            "password": "password123",
            "display_name": "User 1",
        })
        resp = await client.post("/api/auth/register", json={
            "email": "dup@test.com",
            "password": "password456",
            "display_name": "User 2",
        })
        assert resp.status_code == 409
        assert resp.json()["error_code"] == "AUTH_EMAIL_EXISTS"

    @pytest.mark.asyncio
    async def test_register_short_password(self, client, seed_roles):
        resp = await client.post("/api/auth/register", json={
            "email": "short@test.com",
            "password": "12345",  # < 6 chars
            "display_name": "Short",
        })
        assert resp.status_code == 422  # Pydantic validation


class TestLogin:
    @pytest.mark.asyncio
    async def test_login_success(self, client, test_user):
        resp = await client.post("/api/auth/login", json={
            "email": "testuser@aria.local",
            "password": "password123",
        })
        assert resp.status_code == 200
        data = resp.json()
        assert "access_token" in data
        assert data["user"]["email"] == "testuser@aria.local"

    @pytest.mark.asyncio
    async def test_login_wrong_password(self, client, test_user):
        resp = await client.post("/api/auth/login", json={
            "email": "testuser@aria.local",
            "password": "wrongpassword",
        })
        assert resp.status_code == 401
        assert resp.json()["error_code"] == "AUTH_INVALID_CREDENTIALS"

    @pytest.mark.asyncio
    async def test_login_nonexistent_email(self, client, seed_roles):
        resp = await client.post("/api/auth/login", json={
            "email": "nobody@test.com",
            "password": "password123",
        })
        assert resp.status_code == 401

    @pytest.mark.asyncio
    async def test_login_blocked_user(self, client, test_user, db_session):
        user, _ = test_user
        from app.models.user import UserStatus
        user.status = UserStatus.blocked
        await db_session.commit()

        resp = await client.post("/api/auth/login", json={
            "email": "testuser@aria.local",
            "password": "password123",
        })
        assert resp.status_code == 403
        assert resp.json()["error_code"] == "USER_BLOCKED"


class TestRefresh:
    @pytest.mark.asyncio
    async def test_refresh_success(self, client, test_user, db_session):
        from app.core.auth_utils import create_refresh_token
        from app.models.session import Session
        from sqlalchemy import select

        user, access_token = test_user

        # Get the actual refresh token from the session
        result = await db_session.execute(
            select(Session).where(Session.user_id == user.id)
        )
        session = result.scalar_one()
        # Update with a real refresh token
        refresh_token = create_refresh_token({"sub": str(user.id)})
        session.refresh_token = refresh_token
        await db_session.commit()

        resp = await client.post("/api/auth/refresh", json={
            "refresh_token": refresh_token,
        })
        assert resp.status_code == 200
        data = resp.json()
        assert "access_token" in data
        assert "refresh_token" in data

    @pytest.mark.asyncio
    async def test_refresh_invalid_token(self, client, seed_roles):
        resp = await client.post("/api/auth/refresh", json={
            "refresh_token": "invalid-token",
        })
        assert resp.status_code == 401


class TestLogout:
    @pytest.mark.asyncio
    async def test_logout_success(self, client, test_user):
        _, access_token = test_user
        resp = await client.post(
            "/api/auth/logout",
            headers={"Authorization": f"Bearer {access_token}"},
        )
        assert resp.status_code == 200
        assert resp.json()["message"] == "Logged out successfully"
