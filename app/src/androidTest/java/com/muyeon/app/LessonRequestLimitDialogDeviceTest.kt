package com.muyeon.app

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.muyeon.app.ui.quote.LessonRequestLimitDialog
import com.muyeon.app.ui.review.quick.QuickReviewActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ★ 2026-11-27 레슨 요청 한도 팝업 실기기 점검 — 서버를 부르지 않는다.
 *  일반 회원(showPostings=false)은 [멤버십 가입하기][닫기]만, 원장·공연팀은 공고 버튼 둘까지 네 개.
 *  콜백은 QuoteWizardActivity 처럼 누르면 팝업 상태를 지운다(open=false).
 */
@RunWith(AndroidJUnit4::class)
class LessonRequestLimitDialogDeviceTest {
    private val instr = InstrumentationRegistry.getInstrumentation()
    private val ctx = instr.targetContext
    private val device = UiDevice.getInstance(instr)
    private val timeout = 10_000L
    private var host: ComponentActivity? = null

    private val taps = mutableListOf<String>()
    private var open by mutableStateOf(true)

    private val membership get() = ctx.getString(R.string.lrq_action_membership)
    private val job get() = ctx.getString(R.string.lrq_action_job)
    private val sub get() = ctx.getString(R.string.lrq_action_sub)
    private val close get() = ctx.getString(R.string.lrq_action_close)

    private fun show(showPostings: Boolean) {
        val a = instr.startActivitySync(
            QuickReviewActivity.intent(ctx, teacherId = 1, teacherName = "무용연", lessonLine = "발레 레슨")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        ) as ComponentActivity
        host = a
        instr.runOnMainSync {
            a.setContent {
                Box(Modifier.fillMaxSize().background(Color.White))
                if (open) {
                    LessonRequestLimitDialog(
                        message = null,
                        onJob = { taps += "job"; open = false },
                        onSub = { taps += "sub"; open = false },
                        onMembership = { taps += "membership"; open = false },
                        onDismiss = { taps += "dismiss"; open = false },
                        showPostings = showPostings,
                    )
                }
            }
        }
        instr.waitForIdleSync()
        assertNotNull("팝업이 안 뜸", device.wait(Until.findObject(By.text(ctx.getString(R.string.lrq_limit_title))), timeout))
    }

    @After fun finish() { host?.let { a -> instr.runOnMainSync { a.finish() } } }

    @Test
    fun generalShowsOnlyMembershipAndClose() {
        show(showPostings = false)
        assertNotNull(device.wait(Until.findObject(By.text(membership)), timeout))
        assertNotNull(device.wait(Until.findObject(By.text(close)), timeout))
        assertFalse("일반 회원인데 채용 공고 버튼", device.hasObject(By.text(job)))
        assertFalse("일반 회원인데 대타공고 버튼", device.hasObject(By.text(sub)))
    }

    @Test
    fun postingsShowAllFour() {
        show(showPostings = true)
        for (t in listOf(membership, job, sub, close)) {
            assertNotNull("$t 없음", device.wait(Until.findObject(By.text(t)), timeout))
        }
    }

    @Test
    fun closeCallsDismissOnce() {
        show(showPostings = false)
        device.wait(Until.findObject(By.text(close)), timeout).click()
        assertTrue("닫기로 안 닫힘", device.wait(Until.gone(By.text(membership)), timeout))
        instr.waitForIdleSync()
        assertEquals(listOf("dismiss"), taps)
    }

    /** 같은 자리를 연달아 두 번(기다림 없이) — 팝업이 막아 주면 멤버십 한 번만. */
    @Test
    fun rapidDoubleTapMembershipFiresOnce() {
        show(showPostings = false)
        val c = device.wait(Until.findObject(By.text(membership)), timeout).visibleCenter
        device.click(c.x, c.y)
        device.click(c.x, c.y)
        assertTrue(device.wait(Until.gone(By.text(membership)), timeout))
        instr.waitForIdleSync()
        Thread.sleep(500)
        assertEquals(listOf("membership"), taps)
    }
}
