"""Tests for /api/admin/ endpoints."""
import pytest


class TestListUsers:
    @pytest.mark.asyncio
    async def test_list_users_as_admin(self, client, admin_user, test_user):
        _, admin_token = admin_user
        resp = await client.get(
            "/api/admin/users",
            headers={"Authorization": f"Bearer {admin_token}"},
        )
        assert resp.status_code == 200
        data = resp.json()
        assert "users" in data
        assert data["total"] >= 2  # admin + test user
        assert "online_count" in data

    @pytest.mark.asyncio
    async def test_list_users_as_non_admin(self, client, test_user):
        _, user_token = test_user
        resp = await client.get(
            "/api/admin/users",
            headers={"Authorization": f"Bearer {user_token}"},
        )
        assert resp.status_code == 403
        assert resp.json()["error_code"] == "AUTH_FORBIDDEN"

    @pytest.mark.asyncio
    async def test_list_users_unauthenticated(self, client, seed_roles):
        resp = await client.get("/api/admin/users")
        assert resp.status_code in (401, 403)


class TestDashboard:
    @pytest.mark.asyncio
    async def test_dashboard_as_admin(self, client, admin_user):
        _, admin_token = admin_user
        resp = await client.get(
            "/api/admin/dashboard",
            headers={"Authorization": f"Bearer {admin_token}"},
        )
        assert resp.status_code == 200
        data = resp.json()
        assert "total_users" in data
        assert "online_count" in data
        assert "total_streams" in data
        assert "total_messages" in data


class TestUpdateUserStatus:
    @pytest.mark.asyncio
    async def test_suspend_user(self, client, admin_user, test_user):
        _, admin_token = admin_user
        user, _ = test_user
        resp = await client.put(
            f"/api/admin/users/{user.id}/status",
            json={"status": "suspended"},
            headers={"Authorization": f"Bearer {admin_token}"},
        )
        assert resp.status_code == 200
        assert "suspended" in resp.json()["message"]

    @pytest.mark.asyncio
    async def test_block_user(self, client, admin_user, test_user):
        _, admin_token = admin_user
        user, _ = test_user
        resp = await client.put(
            f"/api/admin/users/{user.id}/status",
            json={"status": "blocked"},
            headers={"Authorization": f"Bearer {admin_token}"},
        )
        assert resp.status_code == 200


class TestUpdateUserRole:
    @pytest.mark.asyncio
    async def test_change_role_to_admin(self, client, admin_user, test_user):
        _, admin_token = admin_user
        user, _ = test_user
        resp = await client.put(
            f"/api/admin/users/{user.id}/role",
            json={"role": "admin"},
            headers={"Authorization": f"Bearer {admin_token}"},
        )
        assert resp.status_code == 200
        assert resp.json()["role"] == "admin"

    @pytest.mark.asyncio
    async def test_change_role_invalid(self, client, admin_user, test_user):
        _, admin_token = admin_user
        user, _ = test_user
        resp = await client.put(
            f"/api/admin/users/{user.id}/role",
            json={"role": "nonexistent"},
            headers={"Authorization": f"Bearer {admin_token}"},
        )
        assert resp.status_code == 400
        assert resp.json()["error_code"] == "ROLE_NOT_FOUND"


class TestAudioPresets:
    @pytest.mark.asyncio
    async def test_list_presets_empty(self, client, admin_user):
        _, admin_token = admin_user
        resp = await client.get(
            "/api/admin/audio-presets",
            headers={"Authorization": f"Bearer {admin_token}"},
        )
        assert resp.status_code == 200
        data = resp.json()
        assert "presets" in data
        assert isinstance(data["presets"], list)

    @pytest.mark.asyncio
    async def test_get_preset_not_found(self, client, admin_user):
        _, admin_token = admin_user
        resp = await client.get(
            "/api/admin/audio-presets/nonexistent.mp3",
            headers={"Authorization": f"Bearer {admin_token}"},
        )
        assert resp.status_code == 404
