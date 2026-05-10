"""
TTS Service — เรียก Moe-TTS API เพื่อ generate เสียงพูด
Moe-TTS รันเป็น Gradio app, base URL อ่านจาก settings.TTS_BASE_URL
(default: http://127.0.0.1:7860, override ผ่าน .env: TTS_BASE_URL=...)
API endpoint: GET /api/v1/speech/get_voice/{text}?model_id=0&speaker_id=0&speed=1.0
"""
import logging

import httpx

from app.config import settings

logger = logging.getLogger("aria.tts")


async def generate_tts(
    text: str,
    model_id: int = 0,
    speaker_id: int = 0,
    speed: float = 1.0,
) -> bytes:
    """
    Generate TTS audio จาก text โดยเรียก Moe-TTS API

    Returns:
        WAV audio bytes

    Raises:
        RuntimeError: ถ้า TTS API ไม่พร้อมหรือ error
    """
    import urllib.parse
    encoded_text = urllib.parse.quote(text, safe="")
    url = f"{settings.TTS_BASE_URL}/api/v1/speech/get_voice/{encoded_text}"
    params = {
        "model_id": model_id,
        "speaker_id": speaker_id,
        "speed": speed,
        "language": "auto",
    }

    try:
        async with httpx.AsyncClient(timeout=settings.TTS_TIMEOUT_SECONDS) as client:
            response = await client.get(url, params=params)
            response.raise_for_status()
            audio_bytes = response.content
            logger.info(f"TTS generated: {len(audio_bytes)} bytes for '{text[:30]}...'")
            return audio_bytes
    except httpx.ConnectError:
        raise RuntimeError(f"Moe-TTS API is not running ({settings.TTS_BASE_URL})")
    except httpx.HTTPStatusError as e:
        raise RuntimeError(f"TTS API returned error: {e.response.status_code}")
    except Exception as e:
        raise RuntimeError(f"TTS generation failed: {e}")


async def get_tts_models() -> list[dict]:
    """ดึงรายชื่อ models ทั้งหมดจาก Moe-TTS API"""
    try:
        async with httpx.AsyncClient(timeout=5.0) as client:
            response = await client.get(f"{settings.TTS_BASE_URL}/api/v1/speech/get_voice_info")
            response.raise_for_status()
            data = response.json()
            models = data.get("models", {})
            result = []
            # Also fetch speaker counts per model
            for model_id_str, info in models.items():
                mid = int(model_id_str)
                name = info.get("title") or info.get("name") or f"Model {model_id_str}"
                # Try to get speaker count
                num_speakers = info.get("num_speakers", 0)
                if num_speakers == 0:
                    try:
                        sp_resp = await client.get(
                            f"{settings.TTS_BASE_URL}/api/v1/speech/get_speakers",
                            params={"model_id": mid},
                        )
                        if sp_resp.status_code == 200:
                            num_speakers = len(sp_resp.json())
                    except Exception:
                        pass
                result.append({
                    "id": mid,
                    "name": name,
                    "num_speakers": num_speakers,
                })
            result.sort(key=lambda m: m["id"])
            return result
    except Exception as e:
        logger.warning(f"Failed to get TTS models: {e}")
        return []


async def get_tts_speakers(model_id: int = 0) -> list[dict]:
    """ดึงรายชื่อ speakers ของ model จาก Moe-TTS API"""
    try:
        async with httpx.AsyncClient(timeout=5.0) as client:
            response = await client.get(
                f"{settings.TTS_BASE_URL}/api/v1/speech/get_speakers",
                params={"model_id": model_id},
            )
            response.raise_for_status()
            return response.json()  # [{"id": 0, "name": "Rina"}, ...]
    except Exception as e:
        logger.warning(f"Failed to get TTS speakers: {e}")
        return []


async def is_tts_available() -> bool:
    """ตรวจว่า Moe-TTS API พร้อมใช้งานหรือไม่"""
    try:
        async with httpx.AsyncClient(timeout=3.0) as client:
            response = await client.get(f"{settings.TTS_BASE_URL}/api/v1/speech/get_voice_info")
            return response.status_code == 200
    except Exception:
        return False
