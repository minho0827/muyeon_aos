package com.muyeon.app

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.muyeon.app.ui.chat.ChatActivity
import com.muyeon.app.ui.jobposting.JobPostingActivity
import com.muyeon.app.ui.notification.NotificationActivity
import com.muyeon.app.ui.notification.NotificationSettingsActivity
import com.muyeon.app.ui.quote.QuoteDashboardActivity
import com.muyeon.app.ui.quote.QuoteHubActivity
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 2026-10-27 목록 화면 틀(ui/common/MuyeonListKit) — 실기기(운영 데이터, 로그인된 기기).
 *  화면마다 왼쪽 굵은 제목이 뜨는지 보고 /sdcard/ll_*.png 로 찍는다(바꾸기 전/후 비교용).
 */
@RunWith(AndroidJUnit4::class)
class ListLayoutDeviceTest {
    private val instr = InstrumentationRegistry.getInstrumentation()
    private val ctx = instr.targetContext
    private val device = UiDevice.getInstance(instr)
    private val timeout = 15_000L

    private fun open(i: Intent, title: String, name: String) {
        ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        assertNotNull("$title 제목", device.wait(Until.findObject(By.text(title)), timeout))
        Thread.sleep(2500)
        device.executeShellCommand("screencap -p /sdcard/ll_$name.png")
    }

    @Test fun notifications() = open(NotificationActivity.intent(ctx), "알림", "noti")
    @Test fun notificationSettings() = open(Intent(ctx, NotificationSettingsActivity::class.java), "알림 설정", "noti_settings")
    @Test fun chatList() = open(ChatActivity.listIntent(ctx), "채팅", "chat")
    @Test fun quoteHub() = open(QuoteHubActivity.intent(ctx, isPro = false), "레슨 요청 내역", "quote_hub")
    @Test fun myPostings() = open(JobPostingActivity.listIntent(ctx), "내 공고 관리", "postings")
    @Test fun dashboard() = open(QuoteDashboardActivity.intent(ctx, ""), "내 레슨 요청", "dashboard")
}
