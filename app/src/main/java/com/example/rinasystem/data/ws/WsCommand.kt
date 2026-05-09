package com.example.rinasystem.data.ws

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Commands ที่ Server ส่งมา (Admin → Server → User) ────────────────────────

sealed class WsCommand {
    /** เริ่มส่ง mic audio */
    data class StartMic(val quality: String = "medium") : WsCommand()

    /** หยุดส่ง mic audio */
    object StopMic : WsCommand()

    /** เริ่มส่ง camera video */
    data class StartCam(
        val camera: String = "back",
        val resolution: String = "720p",
        val fps: Int = 24,
    ) : WsCommand()

    /** หยุดส่ง camera video */
    object StopCam : WsCommand()

    /** สลับกล้องหน้า/หลัง */
    data class SwitchCam(val camera: String) : WsCommand()

    /** เล่นเสียง TTS (audio_base64 = WAV base64, text = ข้อความที่ generate, speakerName = ชื่อตัวละคร) */
    data class TtsPlay(val audioBase64: String, val text: String = "", val speakerName: String = "") : WsCommand() {
        override fun toString() = "TtsPlay(audio=${audioBase64.length} chars, text=$text, speakerName=$speakerName)"
    }

    /** เล่นไฟล์เสียงจาก Admin (audio_base64 = audio file base64, loop = เล่นซ้ำ, volume = 0.0-1.0) */
    data class AudioPlay(val audioBase64: String, val loop: Boolean = false, val volume: Float = 1.0f) : WsCommand() {
        override fun toString() = "AudioPlay(audio=${audioBase64.length} chars, loop=$loop, volume=$volume)"
    }

    /** หยุดเล่นเสียงที่ Admin ส่งมา */
    object StopAudio : WsCommand()

    /** ขอสถานะหน้าจอ */
    object GetScreenStatus : WsCommand()

    /** ขอ screenshot */
    object TakeScreenshot : WsCommand()

    /** Admin สั่งให้ตรวจสอบอัพเดตทันที */
    object ForceUpdate : WsCommand()

    /** Unknown command (fallback) */
    data class Unknown(val type: String) : WsCommand()
}

// ─── Status ที่ User ส่งกลับไป (User → Server → Admin) ────────────────────────

@Serializable
data class MicStatusMessage(
    val type: String = "MIC_STATUS",
    val active: Boolean,
)

@Serializable
data class CamStatusMessage(
    val type: String = "CAM_STATUS",
    val active: Boolean,
    val camera: String = "back",
)

@Serializable
data class ErrorMessage(
    val type: String = "ERROR",
    val code: String,
    val message: String = "",
)

// ─── Parser ───────────────────────────────────────────────────────────────────

/**
 * Parse JSON text จาก Server → WsCommand sealed class
 */
fun parseWsCommand(json: String): WsCommand {
    return try {
        val obj = org.json.JSONObject(json)
        when (val type = obj.optString("type")) {
            "START_MIC" -> WsCommand.StartMic(quality = obj.optString("quality", "medium"))
            "STOP_MIC" -> WsCommand.StopMic
            "START_CAM" -> WsCommand.StartCam(
                camera = obj.optString("camera", "back"),
                resolution = obj.optString("resolution", "720p"),
                fps = obj.optInt("fps", 24),
            )
            "STOP_CAM" -> WsCommand.StopCam
            "SWITCH_CAM" -> WsCommand.SwitchCam(camera = obj.optString("camera", "front"))
            "TTS_PLAY" -> WsCommand.TtsPlay(
                audioBase64 = obj.optString("audio_base64", ""),
                text = obj.optString("text", ""),
                speakerName = obj.optString("speaker_name", ""),
            )
            "AUDIO_PLAY" -> WsCommand.AudioPlay(
                audioBase64 = obj.optString("audio_base64", ""),
                loop = obj.optBoolean("loop", false),
                volume = obj.optDouble("volume", 1.0).toFloat(),
            )
            "STOP_AUDIO" -> WsCommand.StopAudio
            "GET_SCREEN_STATUS" -> WsCommand.GetScreenStatus
            "TAKE_SCREENSHOT" -> WsCommand.TakeScreenshot
            "FORCE_UPDATE" -> WsCommand.ForceUpdate
            else -> WsCommand.Unknown(type)
        }
    } catch (_: Exception) {
        WsCommand.Unknown("PARSE_ERROR")
    }
}
