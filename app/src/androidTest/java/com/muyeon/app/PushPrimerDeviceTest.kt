package com.muyeon.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.muyeon.app.ui.onboarding.OnboardingActivity
import com.muyeon.app.utils.PushPrimer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import java.util.regex.Pattern

/**
 * 2026-10-25 알림 허용을 첫 실행에서 빼고 '필요한 순간'에 묻는 변경 — 실기기(Android 13+) 확인.
 *  새로 설치한 상태에서 순서대로 돈다: 첫 실행 안내 → 알림 안내 시트 → 시스템 허용 → 다시 묻지 않음.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class PushPrimerDeviceTest {
    private val instr = InstrumentationRegistry.getInstrumentation()
    private val ctx = instr.targetContext
    private val device = UiDevice.getInstance(instr)
    private val timeout = 10_000L

    /** 실패 원인 확인용 — 화면 구성·캡처를 기기에 남긴다(앱이 지워져도 남는 /sdcard). */
    private fun snap(name: String) {
        device.executeShellCommand("uiautomator dump /sdcard/pp_$name.xml")
        device.executeShellCommand("screencap -p /sdcard/pp_$name.png")
    }

    @Test
    fun a_firstLaunchGuideHasNoNotificationRow() {
        assertTrue("새로 설치한 상태여야 한다(아직 알림을 묻지 않음)", PushPrimer.shouldAsk(ctx))
        val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)!!
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        ctx.startActivity(launch)
        val shown = device.wait(Until.findObject(By.text("앱 접근권한 안내")), timeout)
        snap("a_guide")
        assertNotNull("첫 실행 권한 안내가 뜨지 않았다", shown)
        assertNotNull(device.findObject(By.textContains("카메라")))
        assertNull("첫 실행 안내에 알림 줄이 남아 있다", device.findObject(By.textStartsWith("알림")))
    }

    @Test
    fun b_consentSheetSingleButtonThenSystemAllow() {
        OnboardingActivity.startNotificationConsent(ctx)
        val sheet = device.wait(Until.findObject(By.text("견적 소식을 바로 알려 드릴게요")), timeout)
        snap("b_sheet")
        assertNotNull("알림 안내 시트가 뜨지 않았다", sheet)
        // UiAutomator 의 textContains·textStartsWith 는 줄바꿈을 넘지 못한다(정규식 DOTALL 없음) → 두 줄 설명은 (?s) 로 찾는다.
        assertNotNull("설명 문구가 없다", device.findObject(By.text(Pattern.compile("(?s).*놓치지 않도록 알림을 보내요.*"))))
        assertNull("나중에 받기 버튼이 남아 있다", device.findObject(By.text("나중에 받기")))
        assertNull(device.findObject(By.text("알림 허용하기")))
        assertNull("가짜 켜기 스위치가 남아 있다", device.findObject(By.checkable(true)))

        device.findObject(By.text("계속하기")).click()
        val allow = device.wait(Until.findObject(By.text("허용")), timeout)
        snap("b_system")
        assertNotNull("계속하기 뒤에 시스템 허용 창이 뜨지 않았다", allow)
        allow.click()
        // 권한 반영은 허용 창이 닫힌 뒤에 된다 — 시트가 닫히길 기다린 다음 확인한다.
        assertTrue("허용 뒤 시트가 닫히지 않았다", device.wait(Until.gone(By.text("견적 소식을 바로 알려 드릴게요")), timeout))
        assertTrue(
            "허용 뒤에도 권한이 없다",
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        )
    }

    @Test
    fun c_notAskedAgainAfterDecision() {
        assertFalse("한 번 물은 뒤에도 다시 물으려 한다", PushPrimer.shouldAsk(ctx))
    }
}
