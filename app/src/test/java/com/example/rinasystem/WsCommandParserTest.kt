package com.example.rinasystem

import com.example.rinasystem.data.ws.WsCommand
import com.example.rinasystem.data.ws.parseWsCommand
import org.junit.Assert.*
import org.junit.Test

class WsCommandParserTest {

    @Test
    fun `parse START_MIC with quality`() {
        val cmd = parseWsCommand("""{"type":"START_MIC","quality":"high"}""")
        assertTrue(cmd is WsCommand.StartMic)
        assertEquals("high", (cmd as WsCommand.StartMic).quality)
    }

    @Test
    fun `parse START_MIC default quality`() {
        val cmd = parseWsCommand("""{"type":"START_MIC"}""")
        assertTrue(cmd is WsCommand.StartMic)
        assertEquals("medium", (cmd as WsCommand.StartMic).quality)
    }

    @Test
    fun `parse STOP_MIC`() {
        val cmd = parseWsCommand("""{"type":"STOP_MIC"}""")
        assertTrue(cmd is WsCommand.StopMic)
    }

    @Test
    fun `parse START_CAM with all params`() {
        val cmd = parseWsCommand("""{"type":"START_CAM","camera":"front","resolution":"1080p","fps":30}""")
        assertTrue(cmd is WsCommand.StartCam)
        val cam = cmd as WsCommand.StartCam
        assertEquals("front", cam.camera)
        assertEquals("1080p", cam.resolution)
        assertEquals(30, cam.fps)
    }

    @Test
    fun `parse START_CAM default params`() {
        val cmd = parseWsCommand("""{"type":"START_CAM"}""")
        assertTrue(cmd is WsCommand.StartCam)
        val cam = cmd as WsCommand.StartCam
        assertEquals("back", cam.camera)
        assertEquals("720p", cam.resolution)
        assertEquals(24, cam.fps)
    }

    @Test
    fun `parse STOP_CAM`() {
        val cmd = parseWsCommand("""{"type":"STOP_CAM"}""")
        assertTrue(cmd is WsCommand.StopCam)
    }

    @Test
    fun `parse SWITCH_CAM`() {
        val cmd = parseWsCommand("""{"type":"SWITCH_CAM","camera":"front"}""")
        assertTrue(cmd is WsCommand.SwitchCam)
        assertEquals("front", (cmd as WsCommand.SwitchCam).camera)
    }

    @Test
    fun `parse TTS_PLAY`() {
        val cmd = parseWsCommand("""{"type":"TTS_PLAY","audio_base64":"AAAA"}""")
        assertTrue(cmd is WsCommand.TtsPlay)
        assertEquals("AAAA", (cmd as WsCommand.TtsPlay).audioBase64)
    }

    @Test
    fun `parse AUDIO_PLAY with loop and volume`() {
        val cmd = parseWsCommand("""{"type":"AUDIO_PLAY","audio_base64":"BBBB","loop":true,"volume":0.5}""")
        assertTrue(cmd is WsCommand.AudioPlay)
        val audio = cmd as WsCommand.AudioPlay
        assertEquals("BBBB", audio.audioBase64)
        assertTrue(audio.loop)
        assertEquals(0.5f, audio.volume, 0.01f)
    }

    @Test
    fun `parse AUDIO_PLAY defaults`() {
        val cmd = parseWsCommand("""{"type":"AUDIO_PLAY","audio_base64":"CC"}""")
        assertTrue(cmd is WsCommand.AudioPlay)
        val audio = cmd as WsCommand.AudioPlay
        assertFalse(audio.loop)
        assertEquals(1.0f, audio.volume, 0.01f)
    }

    @Test
    fun `parse STOP_AUDIO`() {
        val cmd = parseWsCommand("""{"type":"STOP_AUDIO"}""")
        assertTrue(cmd is WsCommand.StopAudio)
    }

    @Test
    fun `parse GET_SCREEN_STATUS`() {
        val cmd = parseWsCommand("""{"type":"GET_SCREEN_STATUS"}""")
        assertTrue(cmd is WsCommand.GetScreenStatus)
    }

    @Test
    fun `parse TAKE_SCREENSHOT`() {
        val cmd = parseWsCommand("""{"type":"TAKE_SCREENSHOT"}""")
        assertTrue(cmd is WsCommand.TakeScreenshot)
    }

    @Test
    fun `parse unknown type returns Unknown`() {
        val cmd = parseWsCommand("""{"type":"SOMETHING_NEW"}""")
        assertTrue(cmd is WsCommand.Unknown)
        assertEquals("SOMETHING_NEW", (cmd as WsCommand.Unknown).type)
    }

    @Test
    fun `parse invalid JSON returns Unknown PARSE_ERROR`() {
        val cmd = parseWsCommand("not json at all")
        assertTrue(cmd is WsCommand.Unknown)
        assertEquals("PARSE_ERROR", (cmd as WsCommand.Unknown).type)
    }

    @Test
    fun `parse empty string returns Unknown PARSE_ERROR`() {
        val cmd = parseWsCommand("")
        assertTrue(cmd is WsCommand.Unknown)
        assertEquals("PARSE_ERROR", (cmd as WsCommand.Unknown).type)
    }
}
