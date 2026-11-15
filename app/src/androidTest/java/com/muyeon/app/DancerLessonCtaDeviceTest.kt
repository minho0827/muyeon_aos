package com.muyeon.app

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.muyeon.app.ui.dancer.DancerLessonCta
import com.muyeon.app.ui.dancer.DancerLessonCtaBar
import com.muyeon.app.ui.review.quick.QuickReviewActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ★ 2026-11-16 무용수 상세 레슨 버튼(가짜 값) 실기기 점검 — 서버를 부르지 않는다.
 *  아무 앱 화면 하나를 열고 그 위에 [DancerLessonCtaBar] 만 그려, 버튼 문구와 누름을 확인한다. 캡처는 /sdcard/dl_*.png.
 */
@RunWith(AndroidJUnit4::class)
class DancerLessonCtaDeviceTest {
    private val instr = InstrumentationRegistry.getInstrumentation()
    private val ctx = instr.targetContext
    private val device = UiDevice.getInstance(instr)
    private val timeout = 10_000L
    private var host: ComponentActivity? = null

    private val chats = mutableListOf<Long>()
    private val received = mutableListOf<Long>()
    private var requests = 0

    private fun show(cta: DancerLessonCta) {
        val a = instr.startActivitySync(
            QuickReviewActivity.intent(ctx, teacherId = 1, teacherName = "무용연", lessonLine = "발레 레슨")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        ) as ComponentActivity
        host = a
        instr.runOnMainSync {
            a.setContent {
                // 실제 상세 화면처럼 시스템 바 안쪽에 그린다(안 그러면 아래 버튼이 내비게이션 바에 가려 눌리지 않는다).
                Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()) {
                    Spacer(Modifier.weight(1f))
                    DancerLessonCtaBar(cta, onChat = { chats += it }, onReceived = { received += it }, onRequest = { requests++ })
                }
            }
        }
        instr.waitForIdleSync()
    }

    private fun snap(name: String) {
        Thread.sleep(500)
        device.executeShellCommand("screencap -p /sdcard/dl_$name.png")
    }

    @After fun close() { host?.let { a -> instr.runOnMainSync { a.finish() } } }

    @Test
    fun firstInquiry_onlyLessonButton() {
        show(DancerLessonCta(chatRoomId = null, receivedQuoteId = null))
        val btn = device.wait(Until.findObject(By.text("레슨 문의")), timeout)
        assertNotNull("[레슨 문의]가 없다", btn)
        assertFalse("방이 없는데 [문의하기]가 보인다", device.hasObject(By.text("문의하기")))
        assertFalse(device.hasObject(By.text("받은 견적 보기")))
        snap("1_first")
        btn.click()
        instr.waitForIdleSync()
        Thread.sleep(300)
        assertEquals(1, requests)
    }

    @Test
    fun existingRoomAndAnswer_chatAndReceived() {
        show(DancerLessonCta(chatRoomId = 77L, receivedQuoteId = 12L))
        val rec = device.wait(Until.findObject(By.text("받은 견적 보기")), timeout)
        assertNotNull("[받은 견적 보기]가 없다", rec)
        val chat = device.findObject(By.text("문의하기"))
        assertNotNull("[문의하기]가 없다", chat)
        assertFalse(device.hasObject(By.text("레슨 문의")))
        snap("2_room_received")
        chat.click(); instr.waitForIdleSync(); Thread.sleep(300)
        device.findObject(By.text("받은 견적 보기")).click(); instr.waitForIdleSync(); Thread.sleep(300)
        assertEquals(listOf(77L), chats)
        assertEquals(listOf(12L), received)
        assertEquals(0, requests)
    }
}
