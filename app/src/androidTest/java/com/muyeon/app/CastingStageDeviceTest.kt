package com.muyeon.app

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.muyeon.app.ui.castingstage.CastingStageActivity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 * 2026-10-26 '캐스팅·무대' 네이티브 화면 — 실기기(운영 서버 데이터).
 *  무대 소식 호가 아직 없어도 캐스팅 섹션·장르 칩·맨 위로·뒤로가 돼야 한다. 호가 있으면 소식 칸 이동까지 본다.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class CastingStageDeviceTest {
    private val instr = InstrumentationRegistry.getInstrumentation()
    private val ctx = instr.targetContext
    private val device = UiDevice.getInstance(instr)
    private val timeout = 15_000L

    private fun snap(name: String) {
        device.executeShellCommand("uiautomator dump /sdcard/cs_$name.xml")
        device.executeShellCommand("screencap -p /sdcard/cs_$name.png")
    }

    private fun open() {
        ctx.startActivity(Intent(ctx, CastingStageActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertNotNull("화면 제목이 안 보인다", device.wait(Until.findObject(By.text("캐스팅·무대")), timeout))
    }

    @Test
    fun a_showsCastingSectionAndIcons() {
        open()
        assertNotNull("캐스팅 섹션 제목", device.wait(Until.findObject(By.text("지금 열린 캐스팅")), timeout))
        // 소식 호가 있을 때만 아이콘 칸·붙는 탭이 나온다.
        if (device.findObject(By.desc("stage.icon.DOMESTIC")) != null || device.findObject(By.desc("stage.icon.CONTEST")) != null) {
            assertNotNull("붙는 탭(캐스팅 선택)", device.findObject(By.desc("stage.tab.CASTING.selected")))
        }
        assertNotNull("장르 칩", device.findObject(By.text("발레")))
        snap("a_top")
    }

    @Test
    fun b_genreChipReloads() {
        open()
        val chip = device.wait(Until.findObject(By.text("발레")), timeout)
        assertNotNull(chip)
        chip.click()
        device.waitForIdle()
        // 결과가 있든 없든 섹션은 남아 있어야 한다(빈 문구 또는 카드).
        assertNotNull(device.wait(Until.findObject(By.text("지금 열린 캐스팅")), timeout))
        snap("b_genre")
    }

    @Test
    fun c_scrollShowsTopButtonAndBackCloses() {
        open()
        device.wait(Until.findObject(By.text("지금 열린 캐스팅")), timeout)
        val w = device.displayWidth
        val h = device.displayHeight
        repeat(4) { device.swipe(w / 2, h * 3 / 4, w / 2, h / 4, 20) }
        // 소식 호가 없으면 화면이 짧아 맨 위로 버튼이 나오지 않는 게 정상이다 → 호가 있을 때만 확인.
        val hasNews = device.findObject(By.desc("stage.tab.CASTING")) != null || device.findObject(By.descStartsWith("stage.tab.")) != null
        val top = device.wait(Until.findObject(By.desc("맨 위로")), if (hasNews) timeout else 3_000L)
        snap("c_bottom")
        if (hasNews) {
            assertNotNull("내려가면 맨 위로 버튼이 보여야 한다", top)
            top.click()
            assertTrue("맨 위로 누르면 버튼이 사라진다(맨 위)", device.wait(Until.gone(By.desc("맨 위로")), timeout))
        }
        device.findObject(By.desc("뒤로")).click()
        assertTrue("뒤로 누르면 닫혀야 한다", device.wait(Until.gone(By.text("캐스팅·무대")), timeout))
    }

    @Test
    fun d_newsIconJumpsWhenIssueExists() {
        open()
        val contest = device.wait(Until.findObject(By.desc("stage.icon.CONTEST")), 6_000L) ?: return // 호가 아직 없으면 건너뜀
        contest.click()
        assertNotNull("콩쿠르 탭이 선택돼야 한다", device.wait(Until.findObject(By.desc("stage.tab.CONTEST.selected")), timeout))
        snap("d_contest")
        device.findObject(By.desc("stage.tab.CASTING"))?.scroll(Direction.LEFT, 1f)
    }
}
