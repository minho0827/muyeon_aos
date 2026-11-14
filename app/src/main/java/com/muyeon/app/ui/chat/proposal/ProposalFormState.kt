package com.muyeon.app.ui.chat.proposal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.muyeon.app.ui.chat.LessonProposalApi
import com.muyeon.app.ui.chat.ProposalConflict
import com.muyeon.app.ui.lesson.LessonRefundPolicy
import com.muyeon.app.ui.lesson.UserCalendar

/**
 * ★ 2026-11-14 약속잡기 입력값·보내기 — 화면은 그리지 않는다(부품들이 이 값을 읽고 바꾼다).
 *  보내는 내용은 예전과 같다: POST /lesson-proposals, 409 면 겹침 경고 후 force 재시도.
 */
class ProposalFormState(
    private val api: LessonProposalApi,
    private val roomId: Int,
    val isTeacher: Boolean,
    val totalPrice: Int,
    val depositAmount: Int,
    initialStartAt: Long?,
    initialDurationMin: Int?,
    initialPlace: String,
    initialMemo: String,
    initialCalendarId: Int?,
    initialPolicy: LessonRefundPolicy,
) {
    var startAt by mutableLongStateOf(initialStartAt ?: ProposalTime.defaultStart())
    var durationMin by mutableIntStateOf(initialDurationMin ?: 60)
    var place by mutableStateOf(initialPlace)
    var memo by mutableStateOf(initialMemo)
    var calendars by mutableStateOf<List<UserCalendar>>(emptyList())
    var calendarId by mutableStateOf(initialCalendarId)
    var policyAgreed by mutableStateOf(false)
    var refundPolicy by mutableStateOf(initialPolicy)
    var sending by mutableStateOf(false)
        private set
    var errorText by mutableStateOf<String?>(null)
        private set
    var conflictText by mutableStateOf<String?>(null)

    val selectedCalendar: UserCalendar get() = calendars.firstOrNull { it.id == calendarId } ?: UserCalendar.DEFAULT

    /** 회원은 예약금 규정을 확인해야 보낼 수 있다(iOS 와 같다). */
    val canSend: Boolean get() = !sending && (isTeacher || policyAgreed)

    /** 성공이면 true. 겹침이면 [conflictText] 를 채우고 false. */
    suspend fun submit(force: Boolean): Boolean {
        if (sending) return false
        sending = true
        errorText = null
        val r = api.create(
            roomId, startAt, durationMin, place.ifBlank { null }, memo,
            if (isTeacher) calendarId else null, force,
        )
        sending = false
        r.exceptionOrNull()?.let { e ->
            if (e is ProposalConflict) conflictText = e.message else errorText = e.message
        }
        return r.isSuccess
    }

    companion object {
        const val PLACE_MAX = 100
        const val MEMO_MAX = 500
        val DURATIONS = listOf(30, 60, 90, 120)
    }
}
