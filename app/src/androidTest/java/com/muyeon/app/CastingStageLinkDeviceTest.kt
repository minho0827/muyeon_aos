package com.muyeon.app

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.muyeon.app.ui.castingstage.CastingStageActivity
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 2026-10-27 링크(/casting/12 등)로 '캐스팅·무대'를 열 때 — 그 화면부터 열리고 뒤로 가면 첫 화면(실기기, 운영 데이터).
 *  웹 nativeLink.js → openCastingStage payload → AppBridgeInterface 가 linkRoute 로 바꾼 경로를 그대로 넣는다.
 */
@RunWith(AndroidJUnit4::class)
class CastingStageLinkDeviceTest {
    private val instr = InstrumentationRegistry.getInstrumentation()
    private val ctx = instr.targetContext
    private val device = UiDevice.getInstance(instr)
    private val timeout = 15_000L

    private fun open(castingId: String? = null, screen: String? = null, issueNo: String? = null, category: String? = null) {
        val route = CastingStageActivity.linkRoute(castingId, screen, issueNo, category)
        ctx.startActivity(Intent(ctx, CastingStageActivity::class.java).putExtra("route", route)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }

    private fun backToHome(name: String) {
        device.executeShellCommand("screencap -p /sdcard/csl_$name.png")
        device.pressBack()
        assertNotNull("뒤로 가면 캐스팅·무대 첫 화면", device.wait(Until.findObject(By.text("지금 열린 캐스팅")), timeout))
    }

    @Test
    fun castingDetail() {
        open(castingId = "13")
        assertNotNull("공고 상세부터", device.wait(Until.findObject(By.textContains("틱톡 댄스컨텐츠")), timeout))
        backToHome("detail")
    }

    @Test
    fun castingList() {
        open(screen = "castingList")
        assertNotNull("공고 전체 목록부터", device.wait(Until.findObject(By.text("캐스팅 공고")), timeout))
        backToHome("list")
    }

    @Test
    fun issues() {
        open(screen = "issues")
        assertNotNull("지난 무대 소식부터", device.wait(Until.findObject(By.text("지난 무대 소식")), timeout))
        backToHome("issues")
    }

    @Test
    fun issueCategory() {
        open(issueNo = "1", category = "DOMESTIC")
        assertNotNull("그 호 그 칸 목록부터", device.wait(Until.findObject(By.text("국내소식")), timeout))
        backToHome("news")
    }
}
