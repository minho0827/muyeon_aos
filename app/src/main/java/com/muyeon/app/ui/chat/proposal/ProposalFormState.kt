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
    /** ★ 2026-11-27 견적에 적힌 수업 조건 — 강사는 미리 채움(수정 가능), 회원은 있으면 고정. */
    terms: LessonTerms? = null,
) {
    /** 회원 화면에서 바꿀 수 없는 길이·장소(강사님 견적). 강사는 언제나 null(수정 가능). */
    val fixedDurationMin: Int? = if (isTeacher) null else terms?.memberFixedDuration
    val fixedPlace: String? = if (isTeacher) null else terms?.memberFixedPlace

    var startAt by mutableLongStateOf(initialStartAt ?: ProposalTime.defaultStart())
    var durationMin by mutableIntStateOf(fixedDurationMin ?: initialDurationMin ?: terms?.durationMin?.takeIf { it > 0 } ?: 60)
    var place by mutableStateOf(fixedPlace ?: initialPlace.ifBlank { terms?.place.orEmpty() })
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
            // 고정값은 서버도 견적 값으로 덮어쓴다 — 같은 값을 보낸다.
            roomId, startAt, fixedDurationMin ?: durationMin, (fixedPlace ?: place).ifBlank { null }, memo,
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
        const val MEMO_MAX = 300   // ★ 2026-11-27 서버·웹·iOS 와 같게(300자 초과는 서버 400)
        val DURATIONS = listOf(30, 60, 90, 120)
    }
}
