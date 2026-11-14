package com.muyeon.app

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.muyeon.app.ui.review.quick.QuickReviewActivity
import com.muyeon.app.ui.review.quick.QuickReviewOptions
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 2026-11-14 빠른 후기(당근식) 실기기 화면 점검 — 화면만 열어 단계를 넘겨 보고 캡처한다.
 *  [후기 보내기]는 누르지 않는다(운영 데이터를 바꾸지 않는다). 캡처는 /sdcard/qr_*.png.
 */
@RunWith(AndroidJUnit4::class)
class QuickReviewDeviceTest {
    private val instr = InstrumentationRegistry.getInstrumentation()
    private val ctx = instr.targetContext
    private val device = UiDevice.getInstance(instr)
    private val timeout = 10_000L

    private fun snap(name: String) {
        Thread.sleep(600)
        device.executeShellCommand("screencap -p /sdcard/qr_$name.png")
    }

    private fun open() {
        ctx.startActivity(
            QuickReviewActivity.intent(ctx, teacherId = 1, teacherName = "무용연", lessonLine = "발레 레슨 11/14(금)")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        )
        assertNotNull("1단계가 뜨지 않았다", device.wait(Until.findObject(By.text(java.util.regex.Pattern.compile("(?s).*레슨이 어떠셨나요.*"))), timeout))
    }

    @Test
    fun goodPath_ratingReasonComment() {
        open()
        snap("1_rating")
        assertNotNull(device.findObject(By.text("다음")))

        device.findObject(By.text("최고예요!")).click()
        assertNotNull("사유 질문이 펼쳐지지 않았다", device.wait(Until.findObject(By.text("어떤 점이 최고였나요?")), timeout))
        snap("2_best_reasons")

        val firstTag = QuickReviewOptions.goodTags.first().second
        device.findObject(By.text(firstTag)).click()
        snap("3_tag_checked")

        device.findObject(By.text("다음")).click()
        assertNotNull("2단계(글·사진)가 뜨지 않았다", device.wait(Until.findObject(By.text("따뜻한 레슨 경험을 알려주세요!")), timeout))
        assertNotNull(device.findObject(By.text("자세히 쓰기")))
        assertNotNull(device.findObject(By.text("후기 보내기")))
        snap("4_comment")
    }

    @Test
    fun badPath_privateReasons() {
        open()
        device.findObject(By.text("별로예요")).click()
        assertNotNull(device.wait(Until.findObject(By.text("어떤 점이 아쉬웠나요?")), timeout))
        QuickReviewOptions.badTags.forEach { (_, label) -> assertNotNull("아쉬운 점 '$label' 없음", device.findObject(By.text(label))) }
        snap("5_bad_reasons")
        // 고르기 전에는 [다음]이 눌리지 않아야 한다 — 눌러도 2단계로 안 간다.
        device.findObject(By.text("다음")).click()
        assertFalse("사유 없이 다음으로 넘어갔다", device.wait(Until.hasObject(By.text("따뜻한 레슨 경험을 알려주세요!")), 1500))
        assertTrue(device.hasObject(By.text("어떤 점이 아쉬웠나요?")))
    }
}
