"""
WebSocket Routes — Real-time streaming สำหรับ ARIA System

Endpoints:
  WS /ws/user/{user_id}?token=<access_token>   ← User device เชื่อม รอรับ commands
  WS /ws/admin/{admin_id}?token=<access_token>  ← Admin เชื่อม ส่ง/รับ stream

Command Protocol (Admin → Server → User):
  { "type": "START_MIC", "quality": "medium" }
  { "type": "STOP_MIC" }
  { "type": "START_CAM", "camera": "back", "resolution": "720p", "fps": 24 }
  { "type": "STOP_CAM" }
  { "type": "SWITCH_CAM", "camera": "front" }
  { "type": "TTS_TEXT", "text": "สวัสดี", "model_id": 0 }

Binary Frames (User → Server → Admin):
  First 4 bytes = frame type tag: b"AUDI" or b"VIDE"
  Remaining bytes = raw PCM16 (audio) or JPEG bytes (video)
"""
import asyncio
import base64
import json
import logging
from dataclasses import dataclass
from datetime import datetime, timezone

from fastapi import APIRouter, Query, WebSocket, WebSocketDisconnect
from sqlalchemy import select

from app.core.auth_utils import decode_token
from app.core.tts_service import generate_tts
from app.core.ws_manager import ws_manager
from app.database import async_session
from app.models.tts_message import MessageStatus, TTSMessage
from app.models.user import User

logger = logging.getLogger("aria.ws.routes")

router = APIRouter(prefix="/ws", tags=["WebSocket"])

# Frame type tags (4 bytes)
AUDIO_TAG = b"AUDI"
VIDEO_TAG = b"VIDE"


# ─── TTS Auto-Queue (per-admin) ───────────────────────────────────────────────

@dataclass
class _TtsRequest:
    user_id: str
    text: str
    model_id: int
    speaker_id: int
    speed: float
    speaker_name: str
    admin_id: str


# Per-admin TTS queues and background worker tasks
_tts_queues: dict[str, asyncio.Queue] = {}
_tts_workers: dict[str, asyncio.Task] = {}


async def _tts_worker(admin_id: str) -> None:
    """Background worker ที่ process TTS queue แบบ sequential สำหรับแต่ละ admin"""
    q = _tts_queues.get(admin_id)
    if q is None:
        return

    processed = 0
    cancelled = False
    try:
        while True:
            try:
                # รอ item ใหม่ไม่เกิน 500ms — ถ้าไม่มีอะไรมาถือว่า queue หมดแล้ว
                req: _TtsRequest = await asyncio.wait_for(q.get(), timeout=0.5)
            except asyncio.TimeoutError:
                break
            except asyncio.CancelledError:
                cancelled = True
                raise

            processed += 1
            admin_ws = ws_manager.admin_connections.get(admin_id)
            remaining = q.qsize()
            total = processed + remaining

            if admin_ws:
                await admin_ws.send_json({
                    "type": "TTS_QUEUE_PROGRESS",
                    "index": processed - 1,
                    "total": total,
                    "text": req.text,
                })

            try:
                audio_bytes = await generate_tts(req.text, req.model_id, req.speaker_id, req.speed)
                audio_b64 = base64.b64encode(audio_bytes).decode("utf-8")
                tts_payload = {
                    "type": "TTS_PLAY",
                    "audio_base64": audio_b64,
                    "text": req.text,
                    "speaker_name": req.speaker_name,
                }
                if req.user_id == "ALL":
                    for uid in ws_manager.online_user_ids():
                        await ws_manager.send_json_to_user(uid, tts_payload)
                else:
                    await ws_manager.send_json_to_user(req.user_id, tts_payload)

                await _save_tts_message(
                    admin_id,
                    None if req.user_id == "ALL" else req.user_id,
                    req.text,
                    MessageStatus.delivered,
                )
            except asyncio.CancelledError:
                cancelled = True
                raise
            except RuntimeError as e:
                admin_ws = ws_manager.admin_connections.get(admin_id)
                if admin_ws:
                    await admin_ws.send_json({
                        "type": "TTS_QUEUE_ITEM_FAILED",
                        "index": processed - 1,
                        "text": req.text,
                        "error": str(e),
                    })
            except Exception as e:
                logger.error(f"TTS worker error for admin {admin_id}: {e}")
    except asyncio.CancelledError:
        cancelled = True
    finally:
        _tts_workers.pop(admin_id, None)
        admin_ws = ws_manager.admin_connections.get(admin_id)
        if admin_ws:
            if cancelled:
                await admin_ws.send_json({"type": "TTS_QUEUE_CANCELLED", "processed": processed})
            elif processed > 0:
                await admin_ws.send_json({"type": "TTS_QUEUE_DONE", "total": processed})


async def _save_tts_message(
    sender_id: str, receiver_id: str | None, text: str, status: MessageStatus,
) -> None:
    """บันทึก TTS message ลง DB (receiver_id=None = broadcast)"""
    try:
        import uuid as _uuid
        async with async_session() as db:
            msg = TTSMessage(
                sender_id=_uuid.UUID(sender_id),
                receiver_id=_uuid.UUID(receiver_id) if receiver_id else None,
                content=text,
                audio_path="",  # เก็บ inline ผ่าน WS ไม่ได้ save file
                status=status,
            )
            db.add(msg)
            await db.commit()
    except Exception as e:
        logger.warning(f"Failed to save TTS message: {e}")


async def _get_user_display_name(user_id: str) -> str:
    """ดึง display_name ของ User จาก DB"""
    try:
        import uuid as _uuid
        uid = _uuid.UUID(user_id)
        async with async_session() as db:
            result = await db.execute(select(User).where(User.id == uid))
            user = result.scalar_one_or_none()
            return user.display_name if user else user_id[:8]
    except Exception:
        return user_id[:8]


async def _set_user_online(user_id: str, online: bool) -> None:
    """อัพเดต is_online + last_active_at ใน DB"""
    try:
        import uuid as _uuid
        uid = _uuid.UUID(user_id)
        async with async_session() as db:
            result = await db.execute(select(User).where(User.id == uid))
            user = result.scalar_one_or_none()
            if user:
                user.is_online = online
                if online:
                    user.last_active_at = datetime.now(timezone.utc)
                await db.commit()
    except Exception as e:
        logger.warning(f"Failed to update is_online for {user_id}: {e}")


def _validate_ws_token(token: str) -> dict | None:
    """Validate JWT token จาก WS query param"""
    if not token:
        return None
    payload = decode_token(token)
    if payload is None or payload.get("type") != "access":
        return None
    return payload


async def _validate_ws_admin_token(token: str, admin_id: str) -> bool:
    """Validate JWT token AND ตรวจสอบว่า user มี role เป็น admin จริง"""
    import uuid as _uuid
    payload = _validate_ws_token(token)
    if payload is None or payload.get("sub") != admin_id:
        return False
    try:
        uid = _uuid.UUID(admin_id)
        async with async_session() as db:
            result = await db.execute(select(User).where(User.id == uid))
            user = result.scalar_one_or_none()
            return user is not None and user.role == "admin"
    except Exception:
        return False


# ─── User WebSocket ───────────────────────────────────────────────────────────

@router.websocket("/user/{user_id}")
async def ws_user_endpoint(
    websocket: WebSocket,
    user_id: str,
    token: str = Query(...),
):
    """
    User device เชื่อม WebSocket เพื่อรอรับ commands จาก Admin
    - รับ JSON commands → ส่งต่อไปให้ Android app (via message relay)
    - ส่ง binary audio/video frames กลับมา → relay ไปยัง Admin
    """
    # Validate token
    payload = _validate_ws_token(token)
    if payload is None or payload.get("sub") != user_id:
        await websocket.close(code=4001)
        logger.warning(f"WS User auth failed for user_id={user_id}")
        return

    await ws_manager.connect_user(user_id, websocket)
    await _set_user_online(user_id, True)
    try:
        while True:
            # User ส่งข้อมูลกลับมา (status JSON หรือ binary stream)
            message = await websocket.receive()

            if message["type"] == "websocket.disconnect":
                break

            if "bytes" in message and message["bytes"]:
                # Binary frame: audio หรือ video → relay ไปยัง Admin
                data: bytes = message["bytes"]
                await ws_manager.relay_binary_to_admin(user_id, data)

            elif "text" in message and message["text"]:
                # Status update JSON จาก User
                try:
                    status = json.loads(message["text"])
                    msg_type = status.get("type", "")
                    logger.debug(f"User {user_id} status: {msg_type}")

                    status["user_id"] = user_id

                    if msg_type == "TRIGGER_CHARACTER":
                        # User กระตุ้น Character → broadcast ไปยัง Admin ทุกคน
                        user_name = await _get_user_display_name(user_id)
                        payload = {
                            "type": "CHARACTER_TRIGGERED",
                            "user_id": user_id,
                            "user_name": user_name,
                            "character_name": status.get("character_name", ""),
                            "model_id": status.get("model_id", 0),
                            "speaker_id": status.get("speaker_id", 0),
                            "emoji": status.get("emoji", "🎭"),
                        }
                        await ws_manager._broadcast_to_all_admins(payload)
                        logger.info(f"Character trigger: user={user_name} char={status.get('character_name')}")
                    else:
                        # Relay status ไปยัง Admin — inject user_id เพื่อแยก multi-user
                        state = ws_manager.stream_states.get(user_id)
                        if state and state.admin_id:
                            await ws_manager.send_json_to_admin(state.admin_id, status)
                except json.JSONDecodeError:
                    pass

    except WebSocketDisconnect:
        logger.info(f"User {user_id} WS disconnected")
    finally:
        await ws_manager.disconnect_user(user_id)
        await _set_user_online(user_id, False)


# ─── Admin WebSocket ──────────────────────────────────────────────────────────

@router.websocket("/admin/{admin_id}")
async def ws_admin_endpoint(
    websocket: WebSocket,
    admin_id: str,
    token: str = Query(...),
):
    """
    Admin เชื่อม WebSocket เพื่อ:
    - ส่ง streaming commands ไปยัง User
    - รับ audio/video binary stream จาก User
    - ส่ง TTS text → generate audio → ส่งไปยัง User
    """
    # Validate token + must be admin role
    if not await _validate_ws_admin_token(token, admin_id):
        await websocket.close(code=4001)
        logger.warning(f"WS Admin auth failed for admin_id={admin_id}")
        return

    await ws_manager.connect_admin(admin_id, websocket)
    try:
        while True:
            message = await websocket.receive()

            if message["type"] == "websocket.disconnect":
                break

            if "text" in message and message["text"]:
                try:
                    cmd = json.loads(message["text"])
                    await _handle_admin_command(admin_id, cmd)
                except json.JSONDecodeError:
                    await websocket.send_json({"type": "ERROR", "code": "INVALID_JSON"})

    except WebSocketDisconnect:
        logger.info(f"Admin {admin_id} WS disconnected")
    finally:
        await ws_manager.disconnect_admin(admin_id)
        worker = _tts_workers.pop(admin_id, None)
        if worker and not worker.done():
            worker.cancel()
        _tts_queues.pop(admin_id, None)


# ─── Admin Command Handler ────────────────────────────────────────────────────

async def _handle_admin_command(admin_id: str, cmd: dict) -> None:
    """จัดการ commands จาก Admin และส่งต่อไปยัง User ที่เกี่ยวข้อง"""
    cmd_type = cmd.get("type", "")
    user_id = cmd.get("user_id")  # Admin ต้องระบุ user_id ที่จะ stream
    admin_ws = ws_manager.admin_connections.get(admin_id)

    async def send_error(code: str, msg: str = "") -> None:
        if admin_ws:
            await admin_ws.send_json({"type": "ERROR", "code": code, "message": msg})

    # Commands ที่ไม่ต้องการ user_id
    if cmd_type in ("GET_ONLINE_USERS", "GET_ACTIVE_STREAMS", "FORCE_UPDATE"):
        pass
    elif not user_id:
        await send_error("MISSING_USER_ID", "user_id is required in command")
        return

    if cmd_type == "START_MIC":
        quality = cmd.get("quality", "medium")
        ws_manager.start_stream(user_id, admin_id, "mic", quality=quality)
        success = await ws_manager.send_json_to_user(user_id, {
            "type": "START_MIC",
            "quality": quality,
        })
        if not success:
            await send_error("USER_OFFLINE", f"User {user_id} is not connected")

    elif cmd_type == "STOP_MIC":
        ws_manager.stop_stream(user_id, "mic")
        await ws_manager.send_json_to_user(user_id, {"type": "STOP_MIC"})

    elif cmd_type == "START_CAM":
        camera = cmd.get("camera", "back")
        resolution = cmd.get("resolution", "720p")
        fps = int(cmd.get("fps", 24))
        ws_manager.start_stream(
            user_id, admin_id, "cam",
            camera=camera, resolution=resolution, fps=fps,
        )
        success = await ws_manager.send_json_to_user(user_id, {
            "type": "START_CAM",
            "camera": camera,
            "resolution": resolution,
            "fps": fps,
        })
        if not success:
            await send_error("USER_OFFLINE", f"User {user_id} is not connected")

    elif cmd_type == "STOP_CAM":
        ws_manager.stop_stream(user_id, "cam")
        await ws_manager.send_json_to_user(user_id, {"type": "STOP_CAM"})

    elif cmd_type == "SWITCH_CAM":
        camera = cmd.get("camera", "front")
        state = ws_manager.get_or_create_state(user_id)
        state.camera_side = camera
        await ws_manager.send_json_to_user(user_id, {
            "type": "SWITCH_CAM",
            "camera": camera,
        })

    elif cmd_type == "TTS_TEXT":
        text = cmd.get("text", "").strip()
        model_id = int(cmd.get("model_id", 0))
        speaker_id = int(cmd.get("speaker_id", 0))
        speed = float(cmd.get("speed", 1.0))
        speaker_name = cmd.get("speaker_name", "")

        if not text:
            await send_error("EMPTY_TEXT", "TTS text cannot be empty")
            return

        # Fallback: ถ้า Admin ไม่ส่ง speaker_name มา → lookup จาก TTS API
        if not speaker_name:
            try:
                from app.core.tts_service import get_tts_speakers
                speakers = await get_tts_speakers(model_id)
                for sp in speakers:
                    if isinstance(sp, dict) and str(sp.get("id")) == str(speaker_id):
                        speaker_name = sp.get("name", "")
                        break
                    elif isinstance(sp, str) and speakers.index(sp) == speaker_id:
                        speaker_name = sp
                        break
            except Exception as e:
                logger.warning(f"Speaker name lookup failed: {e}")

        logger.info(f"TTS_TEXT queued: model={model_id}, speaker={speaker_id}, text='{text[:30]}'")

        # เพิ่มเข้า per-admin queue
        if admin_id not in _tts_queues:
            _tts_queues[admin_id] = asyncio.Queue()
        await _tts_queues[admin_id].put(_TtsRequest(
            user_id=user_id,
            text=text,
            model_id=model_id,
            speaker_id=speaker_id,
            speed=speed,
            speaker_name=speaker_name,
            admin_id=admin_id,
        ))

        # Spawn worker ถ้ายังไม่มีหรือ worker เก่า done แล้ว
        worker = _tts_workers.get(admin_id)
        if worker is None or worker.done():
            task = asyncio.create_task(_tts_worker(admin_id))
            _tts_workers[admin_id] = task
            logger.info(f"TTS worker started for admin {admin_id}")

    elif cmd_type == "CANCEL_TTS_QUEUE":
        # ล้าง queue ที่รออยู่ และยกเลิก item ที่กำลัง generate
        q = _tts_queues.get(admin_id)
        cancelled_count = 0
        if q:
            while not q.empty():
                try:
                    q.get_nowait()
                    cancelled_count += 1
                except asyncio.QueueEmpty:
                    break

        worker = _tts_workers.get(admin_id)
        if worker and not worker.done():
            worker.cancel()
            logger.info(f"TTS queue cancelled for admin {admin_id} ({cancelled_count} pending items)")
        elif admin_ws:
            # Worker ไม่ได้รัน — ส่ง CANCELLED ทันที
            await admin_ws.send_json({"type": "TTS_QUEUE_CANCELLED", "processed": 0})

    elif cmd_type == "TTS_QUEUE":
        items = cmd.get("items", [])
        if not items:
            await send_error("EMPTY_QUEUE", "items list cannot be empty")
            return
        if admin_id not in _tts_queues:
            _tts_queues[admin_id] = asyncio.Queue()
        queued = 0
        for item in items:
            text = str(item.get("text", "")).strip()
            if not text:
                continue
            await _tts_queues[admin_id].put(_TtsRequest(
                user_id=user_id,
                text=text,
                model_id=int(item.get("model_id", 0)),
                speaker_id=int(item.get("speaker_id", 0)),
                speed=float(item.get("speed", 1.0)),
                speaker_name=item.get("speaker_name", ""),
                admin_id=admin_id,
            ))
            queued += 1
        worker = _tts_workers.get(admin_id)
        if worker is None or worker.done():
            _tts_workers[admin_id] = asyncio.create_task(_tts_worker(admin_id))
        if admin_ws:
            await admin_ws.send_json({"type": "TTS_QUEUE_START", "total": queued})

    elif cmd_type == "GET_ONLINE_USERS":
        # Admin ขอรายชื่อ User ที่ออนไลน์
        if admin_ws:
            await admin_ws.send_json({
                "type": "ONLINE_USERS",
                "user_ids": ws_manager.online_user_ids(),
            })

    elif cmd_type == "GET_ACTIVE_STREAMS":
        if admin_ws:
            await admin_ws.send_json({
                "type": "ACTIVE_STREAMS",
                "streams": ws_manager.get_active_streams(),
            })

    elif cmd_type == "GET_SCREEN_STATUS":
        # ต้อง set admin_id เพื่อให้ response relay กลับได้
        ws_manager.get_or_create_state(user_id).admin_id = admin_id
        success = await ws_manager.send_json_to_user(user_id, {
            "type": "GET_SCREEN_STATUS",
        })
        if not success:
            await send_error("USER_OFFLINE", f"User {user_id} is not connected")

    elif cmd_type == "PLAY_AUDIO":
        # Admin ส่งไฟล์เสียง (base64) ไปเล่นบน User device
        audio_b64 = cmd.get("audio_base64", "")
        if not audio_b64:
            await send_error("EMPTY_AUDIO", "audio_base64 is required")
            return

        loop = bool(cmd.get("loop", False))
        volume = float(cmd.get("volume", 1.0))
        volume = max(0.0, min(1.0, volume))  # clamp 0.0-1.0
        ws_manager.get_or_create_state(user_id).admin_id = admin_id
        success = await ws_manager.send_json_to_user(user_id, {
            "type": "AUDIO_PLAY",
            "audio_base64": audio_b64,
            "loop": loop,
            "volume": volume,
        })

        if admin_ws:
            if success:
                await admin_ws.send_json({
                    "type": "AUDIO_SENT",
                    "user_id": user_id,
                })
            else:
                await send_error("USER_OFFLINE", f"User {user_id} is not connected")

    elif cmd_type == "STOP_AUDIO":
        ws_manager.get_or_create_state(user_id).admin_id = admin_id
        success = await ws_manager.send_json_to_user(user_id, {
            "type": "STOP_AUDIO",
        })
        if not success:
            await send_error("USER_OFFLINE", f"User {user_id} is not connected")

    elif cmd_type == "TAKE_SCREENSHOT":
        ws_manager.get_or_create_state(user_id).admin_id = admin_id
        success = await ws_manager.send_json_to_user(user_id, {
            "type": "TAKE_SCREENSHOT",
        })
        if not success:
            await send_error("USER_OFFLINE", f"User {user_id} is not connected")

    elif cmd_type == "FORCE_UPDATE":
        # ส่งคำสั่งให้ User ตรวจสอบอัพเดตทันที
        target = cmd.get("user_id")
        if target:
            # ส่งไปยัง User เฉพาะคน
            success = await ws_manager.send_json_to_user(target, {"type": "FORCE_UPDATE"})
            if success:
                if admin_ws:
                    await admin_ws.send_json({"type": "FORCE_UPDATE_SENT", "user_id": target})
            else:
                await send_error("USER_OFFLINE", f"User {target} is not connected")
        else:
            # ส่งไปยัง User ทุกคนที่ online
            sent_count = 0
            for uid in list(ws_manager.user_connections.keys()):
                ok = await ws_manager.send_json_to_user(uid, {"type": "FORCE_UPDATE"})
                if ok:
                    sent_count += 1
            if admin_ws:
                await admin_ws.send_json({"type": "FORCE_UPDATE_SENT", "sent_count": sent_count})

    else:
        await send_error("UNKNOWN_COMMAND", f"Unknown command type: {cmd_type}")
        logger.warning(f"Admin {admin_id} sent unknown command: {cmd_type}")
