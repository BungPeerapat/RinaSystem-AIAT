"""
WebSocket Connection Manager — จัดการ WS connections สำหรับ streaming
- User connections: รอรับ commands จาก Admin
- Admin connections: ส่ง commands, รับ audio/video binary stream
- Stream relay: ส่ง binary frames จาก User → Admin
"""
import asyncio
import logging
import uuid
from dataclasses import dataclass, field
from datetime import datetime, timezone
from typing import Optional

from fastapi import WebSocket

logger = logging.getLogger("aria.ws")


@dataclass
class StreamState:
    """สถานะ stream ของ User คนหนึ่ง"""
    user_id: str
    admin_id: Optional[str] = None
    mic_active: bool = False
    cam_active: bool = False
    audio_quality: str = "medium"  # low/medium/high
    video_quality: str = "720p"    # 480p/720p/1080p
    video_fps: int = 24
    camera_side: str = "back"      # front/back
    session_id: Optional[str] = None
    started_at: Optional[datetime] = None


class ConnectionManager:
    """
    Singleton WebSocket connection manager.
    - user_connections:  user_id → WebSocket
    - admin_connections: admin_id → WebSocket
    - stream_states:     user_id → StreamState
    """

    def __init__(self) -> None:
        self.user_connections: dict[str, WebSocket] = {}
        self.admin_connections: dict[str, WebSocket] = {}
        self.stream_states: dict[str, StreamState] = {}

    # ─── Connect / Disconnect ────────────────────────────────────────────────

    async def connect_user(self, user_id: str, ws: WebSocket) -> None:
        await ws.accept()
        self.user_connections[user_id] = ws
        logger.info(f"User {user_id} connected via WS")
        # แจ้ง Admin ทุกคนว่า User เชื่อมต่อแล้ว
        await self._broadcast_to_all_admins({
            "type": "USER_CONNECTED",
            "user_id": user_id,
            "online_users": self.online_user_ids(),
        })

    async def connect_admin(self, admin_id: str, ws: WebSocket) -> None:
        await ws.accept()
        self.admin_connections[admin_id] = ws
        logger.info(f"Admin {admin_id} connected via WS")

    async def disconnect_user(self, user_id: str) -> None:
        self.user_connections.pop(user_id, None)
        logger.info(f"User {user_id} disconnected from WS")
        # แจ้ง Admin ทุกคนว่า User ออกไปแล้ว
        await self._broadcast_to_all_admins({
            "type": "USER_DISCONNECTED",
            "user_id": user_id,
            "online_users": self.online_user_ids(),
        })

    async def disconnect_admin(self, admin_id: str) -> None:
        self.admin_connections.pop(admin_id, None)
        logger.info(f"Admin {admin_id} disconnected from WS")

    async def _broadcast_to_all_admins(self, data: dict) -> None:
        """ส่ง event ไปยัง Admin ทุกคนที่เชื่อมต่ออยู่"""
        dead: list[str] = []
        for admin_id, ws in self.admin_connections.items():
            try:
                await ws.send_json(data)
            except Exception:
                dead.append(admin_id)
        for admin_id in dead:
            self.admin_connections.pop(admin_id, None)

    # ─── Send helpers ────────────────────────────────────────────────────────

    async def send_json_to_user(self, user_id: str, data: dict) -> bool:
        """ส่ง JSON command ไปยัง User"""
        ws = self.user_connections.get(user_id)
        if ws is None:
            return False
        try:
            await ws.send_json(data)
            return True
        except Exception as e:
            logger.warning(f"Failed to send JSON to user {user_id}: {e}")
            return False

    async def send_json_to_admin(self, admin_id: str, data: dict) -> bool:
        """ส่ง JSON status/event ไปยัง Admin"""
        ws = self.admin_connections.get(admin_id)
        if ws is None:
            return False
        try:
            await ws.send_json(data)
            return True
        except Exception as e:
            logger.warning(f"Failed to send JSON to admin {admin_id}: {e}")
            return False

    async def relay_binary_to_admin(self, user_id: str, data: bytes) -> bool:
        """Relay binary frame (audio/video) จาก User ไปยัง Admin ที่กำลัง stream
        Format: [original 4-byte tag] + [user_id 36 bytes UTF-8] + [payload]
        """
        state = self.stream_states.get(user_id)
        if state is None or state.admin_id is None:
            return False
        ws = self.admin_connections.get(state.admin_id)
        if ws is None:
            return False
        try:
            # Inject user_id (36 bytes UUID string) after the 4-byte tag
            if len(data) >= 4:
                tag = data[:4]
                payload = data[4:]
                uid_bytes = user_id.encode("utf-8")[:36].ljust(36, b"\x00")
                frame = tag + uid_bytes + payload
                await ws.send_bytes(frame)
            else:
                await ws.send_bytes(data)
            return True
        except Exception as e:
            logger.warning(f"Failed to relay binary to admin {state.admin_id}: {e}")
            return False

    async def send_bytes_to_user(self, user_id: str, data: bytes) -> bool:
        """ส่ง binary (เช่น TTS audio) ไปยัง User"""
        ws = self.user_connections.get(user_id)
        if ws is None:
            return False
        try:
            await ws.send_bytes(data)
            return True
        except Exception as e:
            logger.warning(f"Failed to send bytes to user {user_id}: {e}")
            return False

    # ─── Stream State Management ─────────────────────────────────────────────

    def get_or_create_state(self, user_id: str) -> StreamState:
        if user_id not in self.stream_states:
            self.stream_states[user_id] = StreamState(user_id=user_id)
        return self.stream_states[user_id]

    def start_stream(
        self,
        user_id: str,
        admin_id: str,
        stream_type: str,
        **kwargs,
    ) -> StreamState:
        state = self.get_or_create_state(user_id)
        state.admin_id = admin_id
        if state.session_id is None:
            state.session_id = str(uuid.uuid4())
            state.started_at = datetime.now(timezone.utc)

        if stream_type == "mic":
            state.mic_active = True
            state.audio_quality = kwargs.get("quality", "medium")
        elif stream_type in ("cam", "camera"):
            state.cam_active = True
            state.video_quality = kwargs.get("resolution", "720p")
            state.video_fps = int(kwargs.get("fps", 24))
            state.camera_side = kwargs.get("camera", "back")

        logger.info(f"Stream started: user={user_id} type={stream_type} admin={admin_id}")
        return state

    def stop_stream(self, user_id: str, stream_type: str) -> StreamState:
        state = self.get_or_create_state(user_id)
        if stream_type == "mic":
            state.mic_active = False
        elif stream_type in ("cam", "camera"):
            state.cam_active = False

        if not state.mic_active and not state.cam_active:
            state.admin_id = None
            state.session_id = None
            state.started_at = None

        logger.info(f"Stream stopped: user={user_id} type={stream_type}")
        return state

    def get_active_streams(self) -> list[dict]:
        """ดู active streams ทั้งหมด (สำหรับ Admin REST API)"""
        result = []
        for user_id, state in self.stream_states.items():
            if state.mic_active or state.cam_active:
                result.append({
                    "user_id": user_id,
                    "admin_id": state.admin_id,
                    "session_id": state.session_id,
                    "mic_active": state.mic_active,
                    "cam_active": state.cam_active,
                    "audio_quality": state.audio_quality,
                    "video_quality": state.video_quality,
                    "camera_side": state.camera_side,
                    "started_at": state.started_at.isoformat() if state.started_at else None,
                })
        return result

    # ─── Utility ─────────────────────────────────────────────────────────────

    def is_user_online(self, user_id: str) -> bool:
        return user_id in self.user_connections

    def online_user_ids(self) -> list[str]:
        return list(self.user_connections.keys())


# Singleton instance — import ใช้ร่วมกันทุก module
ws_manager = ConnectionManager()
