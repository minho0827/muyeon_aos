package com.muyeon.app.ui.chat.proposal

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R

/**
 * ★ 2026-11-14 역할별 약속잡기 본문 — 강사용·회원용을 따로 둔다(한 화면 안에서 if 로 칸을 끼우지 않는다).
 *  공통 칸(날짜·시간, 길이, 장소, 메모)은 같은 부품을 쓰고, 강사는 캘린더 칸·회원은 예약금 카드가 붙는다.
 *  ★ 2026-11-27 견적에 적힌 길이·장소 — 강사는 미리 채워 고칠 수 있고, 회원은 읽기 전용 줄(ProposalFixedRow)로 본다.
 */
@Composable
fun TeacherProposalForm(s: ProposalFormState) {
    ProposalIntro("회원이 수락하면 레슨이 확정되고 캘린더에 등록돼요.")
    ProposalDateTimeField(s.startAt) { s.startAt = it }
    ProposalDurationChips(s.durationMin) { s.durationMin = it }
    ProposalCalendarField(s.selectedCalendar, s.calendars) { s.calendarId = it }
    ProposalPlaceField(s.place) { s.place = it }
    ProposalMemoField(s.memo) { s.memo = it }
    ProposalError(s.errorText)
}

@Composable
fun MemberProposalForm(s: ProposalFormState) {
    val fromQuote = stringResource(R.string.proposal_fixed_source_teacher_quote)
    ProposalIntro("강사가 수락하면 레슨이 확정되고 내 일정에도 등록돼요.")
    ProposalDateTimeField(s.startAt) { s.startAt = it }
    val fixedDuration = s.fixedDurationMin
    if (fixedDuration != null) {
        ProposalFixedRow(
            stringResource(R.string.proposal_fixed_duration_label),
            stringResource(R.string.proposal_fixed_duration_value, fixedDuration), fromQuote, "proposal.fixed.duration",
        )
    } else {
        ProposalDurationChips(s.durationMin) { s.durationMin = it }
    }
    val fixedPlace = s.fixedPlace
    if (fixedPlace != null) {
        ProposalFixedRow(stringResource(R.string.proposal_fixed_place_label), fixedPlace, fromQuote, "proposal.fixed.place")
    } else {
        ProposalPlaceField(s.place) { s.place = it }
    }
    ProposalMemoField(s.memo) { s.memo = it }
    ProposalDepositCard(s.totalPrice, s.depositAmount, s.refundPolicy.bulletText, s.policyAgreed) { s.policyAgreed = it }
    ProposalError(s.errorText)
}
