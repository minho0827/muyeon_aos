package com.muyeon.app

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.muyeon.app.ui.chat.LessonProposalApi
import com.muyeon.app.ui.chat.LessonProposalBubble
import com.muyeon.app.ui.chat.proposal.LessonTerms
import com.muyeon.app.ui.chat.proposal.MemberProposalForm
import com.muyeon.app.ui.chat.proposal.ProposalCounterSheet
import com.muyeon.app.ui.chat.proposal.ProposalFormState
import com.muyeon.app.ui.chat.proposal.TeacherProposalForm
import com.muyeon.app.ui.lesson.LessonRefundPolicyRepo
import com.muyeon.app.ui.review.quick.QuickReviewActivity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ★ 2026-11-27 약속잡기 개편(견적의 수업 길이·장소 고정 · 다른 시간 제안) 실기기 점검 — 서버를 부르지 않는다(가짜 값).
 *  아무 앱 화면 하나 위에 회원·강사 약속잡기 본문, 다른 시간 제안 시트, 제안 카드만 그려 본다. 캡처 /sdcard/pt_*.png.
 */
@RunWith(AndroidJUnit4::class)
class ProposalTermsDeviceTest {
    private val instr = InstrumentationRegistry.getInstrumentation()
    private val ctx = instr.targetContext
    private val device = UiDevice.getInstance(instr)
    private val timeout = 10_000L
    private var host: ComponentActivity? = null

    private val teacherTerms = LessonTerms(durationMin = 90, place = "강남 스튜디오", placeOwner = LessonTerms.TEACHER)

    @OptIn(ExperimentalComposeUiApi::class)
    private fun show(content: @Composable () -> Unit) {
        val a = instr.startActivitySync(
            QuickReviewActivity.intent(ctx, teacherId = 1, teacherName = "무용연", lessonLine = "발레 레슨")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        ) as ComponentActivity
        host = a
        instr.runOnMainSync {
            a.setContent {
                Column(
                    Modifier.fillMaxSize().background(Color.White).statusBarsPadding()
                        .semantics { testTagsAsResourceId = true }
                        .verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                ) { content() }
            }
        }
        instr.waitForIdleSync()
    }

    private fun form(isTeacher: Boolean, terms: LessonTerms?) = ProposalFormState(
        LessonProposalApi(null), roomId = 1, isTeacher = isTeacher, totalPrice = 60_000, depositAmount = 10_000,
        initialStartAt = null, initialDurationMin = null, initialPlace = "", initialMemo = "", initialCalendarId = null,
        initialPolicy = LessonRefundPolicyRepo.current(), terms = terms,
    )

    private fun snap(name: String) {
        Thread.sleep(600)
        device.executeShellCommand("screencap -p /sdcard/pt_$name.png")
    }

    /** 고른 칩 — Compose 는 버튼 역할의 selected 를 checked 로 내보낸다(칩 상자 = 부모). */
    private fun chipOn(label: String) = By.checked(true).hasDescendant(By.text(label))

    // 시트(ModalBottomSheet)는 다른 창이라 testTag 가 resource-id 로 안 나온다 — 시트 안은 글자·입력칸 종류로 찾는다.
    private val sendBtn get() = By.text(java.util.regex.Pattern.compile("제안 보내기|보내는 중…"))
    private val editField = By.clazz("android.widget.EditText")

    private fun waitText(t: String) = assertNotNull("'$t' 없음", device.wait(Until.findObject(By.text(t)), timeout))

    @After fun close() { host?.let { a -> instr.runOnMainSync { a.finish() } } }

    // ── 회원 약속잡기 ──

    @Test
    fun memberForm_quoteTerms_areFixedRows() {
        val s = form(isTeacher = false, terms = teacherTerms)
        show { MemberProposalForm(s) }
        assertNotNull("길이 고정 줄", device.wait(Until.findObject(By.res("proposal.fixed.duration")), timeout))
        assertNotNull("장소 고정 줄", device.findObject(By.res("proposal.fixed.place")))
        waitText("90분"); waitText("강남 스튜디오")
        assertEquals("출처 표시 두 줄", 2, device.findObjects(By.text("강사님 견적")).size)
        assertFalse("길이 칩이 보이면 안 됨", device.hasObject(By.text("30분")))
        assertFalse("장소 입력칸이 보이면 안 됨", device.hasObject(By.res("proposal.place")))
        assertEquals(90, s.durationMin)
        assertEquals("강남 스튜디오", s.place)
        snap("1_member_fixed")
    }

    @Test
    fun memberForm_oldQuote_staysEditable() {
        val s = form(isTeacher = false, terms = null)
        show { MemberProposalForm(s) }
        waitText("30분")
        assertTrue("장소 입력칸", device.hasObject(By.res("proposal.place")))
        assertFalse(device.hasObject(By.text("강사님 견적")))
        device.findObject(By.text("120분")).click()
        assertTrue(device.wait(Until.hasObject(chipOn("120분")), timeout))
        assertEquals(120, s.durationMin)
    }

    @Test
    fun memberForm_memberOwnedPlace_onlyDurationFixed() {
        val s = form(isTeacher = false, terms = LessonTerms(durationMin = 60, place = null, placeOwner = LessonTerms.MEMBER))
        show { MemberProposalForm(s) }
        assertNotNull(device.wait(Until.findObject(By.res("proposal.fixed.duration")), timeout))
        assertTrue("회원이 장소 입력", device.hasObject(By.res("proposal.place")))
        assertFalse(device.hasObject(By.res("proposal.fixed.place")))
    }

    // ── 강사 약속잡기 ──

    @Test
    fun teacherForm_prefilled_andEditable() {
        val s = form(isTeacher = true, terms = teacherTerms)
        show { TeacherProposalForm(s) }
        assertNotNull("90분 칩 선택", device.wait(Until.findObject(chipOn("90분")), timeout))
        assertFalse("강사는 고정 줄 없음", device.hasObject(By.res("proposal.fixed.duration")))
        val place = device.findObject(By.res("proposal.place"))
        assertNotNull("장소 입력칸", place)
        assertEquals("강남 스튜디오", place.text)
        snap("2_teacher_prefill")
        device.findObject(By.text("30분")).click()
        assertTrue(device.wait(Until.hasObject(chipOn("30분")), timeout))
        assertEquals(30, s.durationMin)
        val field = device.findObject(By.res("proposal.place"))
        field.click()
        field.text = "압구정 연습실"
        instr.waitForIdleSync()
        device.pressBack()   // 키보드 내리기
        assertEquals("압구정 연습실", device.findObject(By.res("proposal.place"))?.text)
        assertEquals("압구정 연습실", s.place)
    }

    // ── 다른 시간 제안 시트 ──

    private var counterOpen by mutableStateOf(true)
    private val events = mutableListOf<String>()

    private fun showCounter(send: suspend (Long, String) -> Result<Unit>) {
        counterOpen = true
        show {
            if (counterOpen) {
                ProposalCounterSheet(
                    initialStartAt = System.currentTimeMillis() + 86_400_000L,
                    durationMin = 60, place = "강남 스튜디오", send = send,
                    onSent = { events += "sent"; counterOpen = false },
                    onDismiss = { events += "dismiss"; counterOpen = false },
                )
            }
        }
        waitText("날짜·시간만 바꿔 다시 제안해요. 수업 길이·장소는 그대로예요.")
    }

    @Test
    fun counterSheet_doubleTap_sendsOnce_thenCloses() {
        var calls = 0
        val gate = CompletableDeferred<Result<Unit>>()
        val memos = mutableListOf<String>()
        showCounter { _, memo -> calls++; memos += memo; gate.await() }
        assertNotNull("길이 그대로(읽기 전용)", device.findObject(By.text("60분")))
        assertNotNull("장소 그대로(읽기 전용)", device.findObject(By.text("강남 스튜디오")))
        assertEquals("입력칸은 메모 하나", 1, device.findObjects(editField).size)
        val memo = device.findObject(editField)
        memo.click()
        memo.text = "저녁 7시 이후 괜찮아요"
        instr.waitForIdleSync()
        device.pressBack()   // 키보드 내리기(시트는 남는다)
        assertEquals("저녁 7시 이후 괜찮아요", device.findObject(editField)?.text)
        snap("3_counter")
        val send = device.findObject(By.text("제안 보내기"))
        send.click(); send.click()
        assertNotNull("보내는 중", device.wait(Until.findObject(By.text("보내는 중…")), timeout))
        device.findObject(By.text("보내는 중…"))?.click()
        runBlocking { gate.complete(Result.success(Unit)) }
        assertTrue("성공 후 닫힘", device.wait(Until.gone(sendBtn), timeout))
        assertEquals(1, calls)
        assertEquals(listOf("저녁 7시 이후 괜찮아요"), memos)
        assertEquals(listOf("sent"), events)
    }

    @Test
    fun counterSheet_cancelCloses_withoutSending() {
        var calls = 0
        showCounter { _, _ -> calls++; Result.success(Unit) }
        device.findObject(By.text("취소")).click()
        assertTrue(device.wait(Until.gone(sendBtn), timeout))
        assertEquals(0, calls)
        assertEquals(listOf("dismiss"), events)
    }

    @Test
    fun counterSheet_serverError_staysOpen_withMessage() {
        showCounter { _, _ -> Result.failure(IllegalStateException("이미 끝난 제안이에요.")) }
        device.findObject(By.text("제안 보내기")).click()
        waitText("이미 끝난 제안이에요.")
        assertTrue("시트 유지 · 다시 보낼 수 있음", device.wait(Until.hasObject(By.text("제안 보내기")), timeout))
        assertTrue(events.isEmpty())
        snap("4_counter_error")
    }

    // ── 제안 카드 ──

    private fun card(status: String) = JSONObject()
        .put("proposalId", 7).put("startAt", "2099-01-05T10:00:00Z").put("durationMin", 60)
        .put("place", "강남 스튜디오").put("status", status).put("proposerId", 1).put("memberId", 2).toString()

    @Test
    fun receivedCard_showsCounter_andOpensSheet() {
        show { LessonProposalBubble(card("PROPOSED"), isProposer = false, currentUserId = 2, token = null, onChanged = {}, onOpenPayment = {}) }
        waitText("수락"); waitText("거절")
        val btn = device.findObject(By.res("proposal.counter"))
        assertNotNull("[다른 시간 제안]", btn)
        snap("5_card")
        btn.click()
        waitText("날짜·시간만 바꿔 다시 제안해요. 수업 길이·장소는 그대로예요.")
        device.findObject(By.text("취소")).click()
        assertTrue(device.wait(Until.gone(sendBtn), timeout))
    }

    @Test
    fun proposerCard_hasNoCounter() {
        show { LessonProposalBubble(card("PROPOSED"), isProposer = true, currentUserId = 1, token = null, onChanged = {}, onOpenPayment = {}) }
        waitText("제안 취소")
        assertFalse(device.hasObject(By.res("proposal.counter")))
    }

    @Test
    fun acceptedCard_hasNoCounter() {
        show { LessonProposalBubble(card("ACCEPTED"), isProposer = false, currentUserId = 2, token = null, onChanged = {}, onOpenPayment = {}) }
        waitText("수락됨")
        assertFalse(device.hasObject(By.res("proposal.counter")))
    }
}
