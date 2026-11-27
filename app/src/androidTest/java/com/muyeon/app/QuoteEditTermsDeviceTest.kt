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
import com.muyeon.app.ui.quote.owner.QuoteResponseDraft
import com.muyeon.app.ui.quote.owner.QuoteResponseEditSheet
import com.muyeon.app.ui.review.quick.QuickReviewActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ★ 2026-11-27 보낸 제안 고치기 — 회당 수업 길이·장소(가짜 값) 실기기 점검. 서버를 부르지 않는다.
 *  고치기 시트만 그려 미리 채움 · 바꾸기 · 저장 콜백(PATCH 본문) · 닫힘을 본다. 캡처 /sdcard/qe_*.png.
 */
@RunWith(AndroidJUnit4::class)
class QuoteEditTermsDeviceTest {
    private val instr = InstrumentationRegistry.getInstrumentation()
    private val ctx = instr.targetContext
    private val device = UiDevice.getInstance(instr)
    private val timeout = 10_000L
    private var host: ComponentActivity? = null

    private var open by mutableStateOf(true)
    private val saved = mutableListOf<QuoteResponseDraft>()
    private var dismissed = 0

    private val sent = QuoteResponseDraft(50_000, "PER_SESSION", null, "안녕하세요, 발레 레슨 제안드려요.", durationMin = 90, place = "강남 스튜디오")
    private val saveLabel get() = ctx.getString(R.string.quote_owner_edit_save)

    /**
     * 고른 칩인가 — Compose 는 버튼 역할의 selected 를 checked 로 내보낸다.
     *  글자는 칩 상자 아래 자식일 때도, 상자에 합쳐져 있을 때도 있어 둘 다 본다.
     */
    private fun chipOn(label: String): Boolean {
        val end = System.currentTimeMillis() + timeout
        while (System.currentTimeMillis() < end) {
            if (device.findObjects(By.checked(true)).any { it.text == label || it.hasObject(By.text(label)) }) return true
            Thread.sleep(200)
        }
        return false
    }

    private fun show(draft: QuoteResponseDraft, askPlace: Boolean, durationRequired: Boolean) {
        open = true
        val a = instr.startActivitySync(
            QuickReviewActivity.intent(ctx, teacherId = 1, teacherName = "무용연", lessonLine = "발레 레슨")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        ) as ComponentActivity
        host = a
        instr.runOnMainSync {
            a.setContent {
                Box(Modifier.fillMaxSize().background(Color.White))
                if (open) {
                    QuoteResponseEditSheet(
                        categoryLabel = "발레", sent = draft, askPlace = askPlace, durationRequired = durationRequired,
                        onDismiss = { dismissed++; open = false },
                        onSave = { d -> saved += d; open = false; true },
                    )
                }
            }
        }
        instr.waitForIdleSync()
        assertNotNull("저장 버튼", device.wait(Until.findObject(By.text(saveLabel)), timeout))
    }

    private fun snap(name: String) {
        Thread.sleep(600)
        device.executeShellCommand("screencap -p /sdcard/qe_$name.png")
    }

    @After fun close() { host?.let { a -> instr.runOnMainSync { a.finish() } } }

    @Test
    fun prefilled_changeTo60_savesDurationOnly_andCloses() {
        show(sent, askPlace = true, durationRequired = true)
        assertTrue("90분 미리 고름", chipOn("90분"))
        assertNotNull("장소 미리 채움", device.findObject(By.clazz("android.widget.EditText").text("강남 스튜디오")))
        snap("1_prefilled")
        device.findObject(By.text("60분")).click()
        assertTrue("60분으로 바뀜", chipOn("60분"))
        device.findObject(By.text(saveLabel)).click()
        assertTrue("저장 후 닫힘", device.wait(Until.gone(By.text(saveLabel)), timeout))
        assertEquals(1, saved.size)
        val d = saved.single()
        assertEquals(60, d.durationMin)
        assertEquals("강남 스튜디오", d.place)
        val patch = d.patchFrom(sent)!!
        assertEquals(60, patch.getInt("durationMin"))
        assertFalse("장소는 안 바뀜", patch.has("place"))
        assertFalse(patch.has("message"))
        assertEquals(0, dismissed)
    }

    @Test
    fun placeEdited_isInPatch() {
        show(sent, askPlace = true, durationRequired = true)
        val field = device.wait(Until.findObject(By.clazz("android.widget.EditText").text("강남 스튜디오")), timeout)
        field.click()
        field.text = "압구정 연습실"
        instr.waitForIdleSync()
        device.pressBack()   // 키보드 내리기
        assertNotNull(device.wait(Until.findObject(By.text("압구정 연습실")), timeout))
        device.findObject(By.text(saveLabel)).click()
        assertTrue(device.wait(Until.gone(By.text(saveLabel)), timeout))
        assertEquals("압구정 연습실", saved.single().patchFrom(sent)!!.getString("place"))
    }

    @Test
    fun memberOwnedPlace_hidesPlaceField() {
        show(sent, askPlace = false, durationRequired = true)
        assertTrue(chipOn("90분"))
        assertFalse("장소 칸 없음", device.hasObject(By.text("강남 스튜디오")))
    }

    @Test
    fun oldQuoteWithoutDuration_saveDisabledUntilPicked() {
        show(sent.copy(durationMin = null), askPlace = false, durationRequired = true)
        device.findObject(By.text(saveLabel)).click()
        Thread.sleep(800)
        assertTrue("고르기 전엔 저장 안 됨", saved.isEmpty())
        device.findObject(By.text("120분")).click()
        assertTrue(chipOn("120분"))
        device.findObject(By.text(saveLabel)).click()
        assertTrue(device.wait(Until.gone(By.text(saveLabel)), timeout))
        assertEquals(120, saved.single().durationMin)
    }

    @Test
    fun backCloses_withoutSaving() {
        show(sent, askPlace = true, durationRequired = true)
        device.pressBack()
        assertTrue(device.wait(Until.gone(By.text(saveLabel)), timeout))
        assertEquals(1, dismissed)
        assertTrue(saved.isEmpty())
    }
}
