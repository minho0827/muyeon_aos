package com.muyeon.app

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.muyeon.app.ui.castingstage.CastingStageActivity
import com.muyeon.app.webview.ActiveRole
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * 2026-10-26 공연 공고 네이티브 등록(공연팀) — 실기기.
 *  ⚠️ 운영 서버라 [공고 등록]은 누르지 않는다(진짜 공고가 올라가고 관심 무용수에게 알림이 간다). 4단계 확인까지만 본다.
 *  ＋ 버튼은 활성유형으로 정해지므로 테스트 동안만 로컬 활성유형을 바꾸고 끝나면 되돌린다.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class CastingCreateDeviceTest {
    private val instr = InstrumentationRegistry.getInstrumentation()
    private val ctx = instr.targetContext
    private val device = UiDevice.getInstance(instr)
    private val timeout = 15_000L
    private var original = "GENERAL"

    @Before fun saveRole() { original = ActiveRole.current(ctx) }
    @After fun restoreRole() { ActiveRole.store(ctx, original) }

    private fun snap(name: String) {
        device.executeShellCommand("uiautomator dump /sdcard/cc_$name.xml")
        device.executeShellCommand("screencap -p /sdcard/cc_$name.png")
    }

    private fun open(role: String) {
        ActiveRole.store(ctx, role)
        ctx.startActivity(Intent(ctx, CastingStageActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertNotNull("화면 제목", device.wait(Until.findObject(By.text("캐스팅·무대")), timeout))
    }

    private fun tapText(t: String) {
        val o = device.wait(Until.findObject(By.text(t)), timeout)
        assertNotNull("'$t' 가 안 보인다", o)
        o.click(); device.waitForIdle()
    }

    private fun scrollTo(t: String) {
        repeat(6) {
            if (device.findObject(By.text(t)) != null) return
            device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4, device.displayWidth / 2, device.displayHeight / 3, 20)
            device.waitForIdle()
        }
    }

    @Test
    fun a_nonTeamHasNoCreateButton() {
        open("DANCER")
        device.wait(Until.findObject(By.text("지금 열린 캐스팅")), timeout)
        assertNull("공연팀이 아니면 ＋가 없어야 한다", device.findObject(By.desc("공연 공고 등록")))
    }

    @Test
    fun b_teamWalksFourStepsWithoutSubmitting() {
        open("TEAM")
        val plus = device.wait(Until.findObject(By.desc("공연 공고 등록")), timeout)
        assertNotNull("공연팀이면 ＋가 보여야 한다", plus)
        plus.click()
        assertNotNull("등록 화면", device.wait(Until.findObject(By.text("1 / 4")), timeout))
        snap("1_empty")

        // 필수(공연명·장르) 전에는 [다음]이 안 넘어간다.
        tapText("다음")
        assertNotNull("필수 전에는 1단계에 머문다", device.findObject(By.text("1 / 4")))

        val title = device.wait(Until.findObject(By.clazz("android.widget.EditText")), timeout)
        title.click(); title.text = "실기기 테스트 공연"
        device.pressBack() // 키보드 닫기
        assertNotNull("뒤로는 키보드만 닫고 화면은 남는다", device.wait(Until.findObject(By.text("1 / 4")), 3_000))
        tapText("발레")

        // 공연 날짜 — 달력에서 [완료]
        scrollTo("리허설 기간") // 공연 날짜 줄이 아래 버튼에 가리지 않을 만큼 올린다
        // 스크롤이 멈춘 뒤 누른다 — 움직이는 중에 누르면 탭이 먹지 않는다(10-26 한 번 실패).
        device.waitForIdle(); Thread.sleep(500)
        var picker = null as androidx.test.uiautomator.UiObject2?
        repeat(3) {
            if (picker != null) return@repeat
            // 바로 찾으면 화면 구조를 못 읽는 순간이 있다(같은 순간 uiautomator dump 도 실패) → 기다리며 찾는다.
            val rows = device.wait(Until.findObjects(By.text("날짜 선택")), 5_000).orEmpty()
            if (rows.isEmpty()) snap("fail_rows")
            assertTrue("날짜 줄", rows.isNotEmpty())
            rows.first().click()
            picker = device.wait(Until.findObject(By.text("완료")), 4_000)
        }
        if (picker == null) snap("fail_datepicker")
        assertNotNull("달력 창", picker)
        picker!!.click(); device.waitForIdle()
        // 지원 마감 미정
        scrollTo("미정")
        device.findObject(By.desc("미정"))?.click() ?: tapText("미정")
        device.waitForIdle()
        snap("1_filled")
        tapText("다음")
        assertNotNull("2단계", device.wait(Until.findObject(By.text("2 / 4")), timeout))

        // 모집 역할 필수
        tapText("다음")
        assertNotNull("역할 전에는 2단계에 머문다", device.findObject(By.text("2 / 4")))
        tapText("군무")
        tapText("솔리스트")
        snap("2_filled")
        tapText("다음")
        assertNotNull("3단계", device.wait(Until.findObject(By.text("3 / 4")), timeout))
        device.findObject(By.desc("리허설비 지급"))?.click()
        scrollTo("영상 제출 필수")
        device.findObject(By.desc("영상 제출 필수"))?.click()
        snap("3_filled")
        tapText("다음")

        assertNotNull("4단계", device.wait(Until.findObject(By.text("4 / 4")), timeout))
        assertNotNull("요약: 공연명", device.findObject(By.text("실기기 테스트 공연")))
        assertNotNull("요약: 역할", device.findObject(By.text("군무, 솔리스트")))
        assertNotNull("등록 버튼", device.findObject(By.text("공고 등록")))
        snap("4_review")
        // 아래 요약도 확인(지원 마감 미정·필수 제출물)
        scrollTo("필수 제출물")
        assertNotNull("요약: 마감 미정", device.findObject(By.text("미정")))
        assertNotNull("요약: 필수 제출물", device.findObject(By.text("영상")))
        snap("4_review_bottom")

        // 뒤로 — 한 단계씩 돌아가고, 1단계에서는 나가기 확인
        device.pressBack(); assertNotNull(device.wait(Until.findObject(By.text("3 / 4")), timeout))
        device.pressBack(); assertNotNull(device.wait(Until.findObject(By.text("2 / 4")), timeout))
        assertNotNull("돌아와도 고른 역할이 남아 있다", device.findObject(By.text("군무")))
        device.pressBack(); assertNotNull(device.wait(Until.findObject(By.text("1 / 4")), timeout))
        assertNotNull("돌아와도 공연명이 남아 있다", device.findObject(By.text("실기기 테스트 공연")))
        device.pressBack()
        assertNotNull("나가기 확인", device.wait(Until.findObject(By.text("작성 중인 내용이 사라져요.")), timeout))
        snap("exit_confirm")
        tapText("나가기")
        assertNotNull("캐스팅·무대로 돌아온다", device.wait(Until.findObject(By.text("지금 열린 캐스팅")), timeout))
    }
}
