"""Tests for app.core.ws_manager — ConnectionManager logic, no real WebSocket needed."""
import os

os.environ.setdefault("DATABASE_URL", "sqlite+aiosqlite:///:memory:")
os.environ.setdefault("SECRET_KEY", "test-secret-key")
os.environ.setdefault("JWT_SECRET_KEY", "test-jwt-secret-key-for-testing")

from app.core.ws_manager import ConnectionManager, StreamState


class TestStreamState:
    def test_default_state(self):
        state = StreamState(user_id="u1")
        assert state.mic_active is False
        assert state.cam_active is False
        assert state.admin_id is None
        assert state.audio_quality == "medium"
        assert state.camera_side == "back"


class TestConnectionManagerStreamState:
    def test_get_or_create_state_creates_new(self):
        mgr = ConnectionManager()
        state = mgr.get_or_create_state("user-1")
        assert state.user_id == "user-1"
        assert "user-1" in mgr.stream_states

    def test_get_or_create_state_returns_existing(self):
        mgr = ConnectionManager()
        s1 = mgr.get_or_create_state("user-1")
        s1.mic_active = True
        s2 = mgr.get_or_create_state("user-1")
        assert s2.mic_active is True
        assert s1 is s2

    def test_start_stream_mic(self):
        mgr = ConnectionManager()
        state = mgr.start_stream("u1", "admin1", "mic", quality="high")
        assert state.mic_active is True
        assert state.admin_id == "admin1"
        assert state.audio_quality == "high"
        assert state.session_id is not None
        assert state.started_at is not None

    def test_start_stream_cam(self):
        mgr = ConnectionManager()
        state = mgr.start_stream("u1", "admin1", "cam", resolution="1080p", fps=30, camera="front")
        assert state.cam_active is True
        assert state.video_quality == "1080p"
        assert state.video_fps == 30
        assert state.camera_side == "front"

    def test_stop_stream_mic(self):
        mgr = ConnectionManager()
        mgr.start_stream("u1", "admin1", "mic")
        state = mgr.stop_stream("u1", "mic")
        assert state.mic_active is False
        # No active streams → admin_id cleared
        assert state.admin_id is None
        assert state.session_id is None

    def test_stop_one_keeps_other(self):
        mgr = ConnectionManager()
        mgr.start_stream("u1", "admin1", "mic")
        mgr.start_stream("u1", "admin1", "cam")
        state = mgr.stop_stream("u1", "mic")
        assert state.mic_active is False
        assert state.cam_active is True
        # Still has active stream → admin_id kept
        assert state.admin_id == "admin1"
        assert state.session_id is not None

    def test_get_active_streams_empty(self):
        mgr = ConnectionManager()
        assert mgr.get_active_streams() == []

    def test_get_active_streams_returns_active(self):
        mgr = ConnectionManager()
        mgr.start_stream("u1", "admin1", "mic")
        mgr.start_stream("u2", "admin1", "cam")
        active = mgr.get_active_streams()
        assert len(active) == 2
        user_ids = {s["user_id"] for s in active}
        assert user_ids == {"u1", "u2"}


class TestConnectionManagerOnlineUsers:
    def test_online_user_ids_empty(self):
        mgr = ConnectionManager()
        assert mgr.online_user_ids() == []

    def test_is_user_online_false(self):
        mgr = ConnectionManager()
        assert mgr.is_user_online("nobody") is False

    def test_online_tracking_via_connections_dict(self):
        """Directly test the connections dict since we can't mock WebSocket accept()."""
        mgr = ConnectionManager()
        mgr.user_connections["user-1"] = "fake-ws"
        mgr.user_connections["user-2"] = "fake-ws"
        assert mgr.is_user_online("user-1") is True
        assert set(mgr.online_user_ids()) == {"user-1", "user-2"}


class TestRelayBinaryFormat:
    """Test the binary frame format: [tag 4B] + [user_id 36B] + [payload]."""

    def test_uid_bytes_padding(self):
        """Verify that user_id is padded to exactly 36 bytes."""
        short_id = "abc"
        uid_bytes = short_id.encode("utf-8")[:36].ljust(36, b"\x00")
        assert len(uid_bytes) == 36
        assert uid_bytes[:3] == b"abc"
        assert uid_bytes[3:] == b"\x00" * 33

    def test_uid_bytes_exact_36(self):
        """UUID string is exactly 36 chars."""
        uid = "550e8400-e29b-41d4-a716-446655440000"
        uid_bytes = uid.encode("utf-8")[:36].ljust(36, b"\x00")
        assert len(uid_bytes) == 36
        assert uid_bytes == uid.encode("utf-8")
