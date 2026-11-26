package com.muyeon.app

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.muyeon.app.ui.chat.ChatRoomHeaderSubtitle
import com.muyeon.app.ui.chat.ChatRoomMenuSheet
import com.muyeon.app.ui.review.quick.QuickReviewActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ★ 2026-11-26 채팅방 ⋯ 시트 + 머리글 응답 시간(가짜 값) 실기기 점검 — 서버를 부르지 않는다.
 *  아무 앱 화면 하나 위에 [ChatRoomHeaderSubtitle] 과 [ChatRoomMenuSheet] 만 그려 문구·누름·닫힘을 본다. 캡처 /sdcard/cm_*.png.
 */
@RunWith(AndroidJUnit4::class)
class ChatRoomMenuDeviceTest {
    private val instr = InstrumentationRegistry.getInstrumentation()
    private val ctx = instr.targetContext
    private val device = UiDevice.getInstance(instr)
    private val timeout = 10_000L
    private var host: ComponentActivity? = null

    private val taps = mutableListOf<String>()
    private var open by mutableStateOf(true)
    private var muted by mutableStateOf(false)

    private fun show(canBlock: Boolean = true) {
        val a = instr.startActivitySync(
            QuickReviewActivity.intent(ctx, teacherId = 1, teacherName = "무용연", lessonLine = "발레 레슨")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        ) as ComponentActivity
        host = a
        instr.runOnMainSync {
            a.setContent {
                Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding()) {
                    ChatRoomHeaderSubtitle(typing = false, presence = "접속 중", responseLabel = "보통 10분 이내 응답")
                }
                if (open) {
                    ChatRoomMenuSheet(
                        canBlock = canBlock, muted = muted, muteBusy = false,
                        onBlock = { taps += "block"; open = false },
                        onReport = { taps += "report"; open = false },
                        onToggleMute = { taps += "mute"; muted = !muted; open = false },
                        onLeave = { taps += "leave"; open = false },
                        onDismiss = { taps += "dismiss"; open = false },
                    )
                }
            }
        }
        instr.waitForIdleSync()
    }

    private fun snap(name: String) {
        Thread.sleep(600)
        device.executeShellCommand("screencap -p /sdcard/cm_$name.png")
    }

    private fun reopen() {
        instr.runOnMainSync { open = true }
        instr.waitForIdleSync()
        assertNotNull("시트가 다시 안 열림", device.wait(Until.findObject(By.text("닫기")), timeout))
    }

    @After fun close() { host?.let { a -> instr.runOnMainSync { a.finish() } } }

    @Test
    fun subtitleAndRows() {
        show()
        assertNotNull("응답 시간 줄", device.wait(Until.findObject(By.text("접속 중 · 보통 10분 이내 응답")), timeout))
        for (t in listOf("차단하기", "신고하기", "알림끄기", "채팅방 나가기", "닫기")) {
            assertNotNull("$t 없음", device.wait(Until.findObject(By.text(t)), timeout))
        }
        snap("1_sheet")
        device.findObject(By.text("닫기")).click()
        assertTrue("닫기로 안 닫힘", device.wait(Until.gone(By.text("채팅방 나가기")), timeout))
        assertEquals(listOf("dismiss"), taps)
    }

    @Test
    fun eachRowFiresOnce() {
        show()
        device.wait(Until.findObject(By.text("차단하기")), timeout).click()
        assertTrue(device.wait(Until.gone(By.text("채팅방 나가기")), timeout))
        reopen(); device.findObject(By.text("신고하기")).click()
        assertTrue(device.wait(Until.gone(By.text("채팅방 나가기")), timeout))
        reopen(); device.findObject(By.text("채팅방 나가기")).click()
        assertTrue(device.wait(Until.gone(By.text("닫기")), timeout))
        assertEquals(listOf("block", "report", "leave"), taps)
    }

    @Test
    fun muteTogglesLabel() {
        show()
        device.wait(Until.findObject(By.text("알림끄기")), timeout).click()
        assertTrue(device.wait(Until.gone(By.text("채팅방 나가기")), timeout))
        reopen()
        assertNotNull("끈 뒤엔 알림켜기", device.wait(Until.findObject(By.text("알림켜기")), timeout))
        assertFalse(device.hasObject(By.text("알림끄기")))
        snap("2_muted")
    }

    @Test
    fun noBlockWhenNoOpponent() {
        show(canBlock = false)
        assertNotNull(device.wait(Until.findObject(By.text("신고하기")), timeout))
        assertFalse("상대가 없는데 차단하기", device.hasObject(By.text("차단하기")))
    }

    @Test
    fun swipeDownDismisses() {
        show()
        assertNotNull(device.wait(Until.findObject(By.text("닫기")), timeout))
        val h = device.displayHeight; val w = device.displayWidth
        device.swipe(w / 2, (h * 0.55).toInt(), w / 2, h - 10, 12)
        assertTrue("쓸어내려 안 닫힘", device.wait(Until.gone(By.text("채팅방 나가기")), timeout))
        assertEquals(listOf("dismiss"), taps)
    }
}
