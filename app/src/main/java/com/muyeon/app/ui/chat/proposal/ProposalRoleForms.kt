package com.muyeon.app.ui.chat.proposal

import androidx.compose.runtime.Composable

/**
 * ★ 2026-11-14 역할별 약속잡기 본문 — 강사용·회원용을 따로 둔다(한 화면 안에서 if 로 칸을 끼우지 않는다).
 *  공통 칸(날짜·시간, 길이, 장소, 메모)은 같은 부품을 쓰고, 강사는 캘린더 칸·회원은 예약금 카드가 붙는다.
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
    ProposalIntro("강사가 수락하면 레슨이 확정되고 내 일정에도 등록돼요.")
    ProposalDateTimeField(s.startAt) { s.startAt = it }
    ProposalDurationChips(s.durationMin) { s.durationMin = it }
    ProposalPlaceField(s.place) { s.place = it }
    ProposalMemoField(s.memo) { s.memo = it }
    ProposalDepositCard(s.totalPrice, s.depositAmount, s.refundPolicy.bulletText, s.policyAgreed) { s.policyAgreed = it }
    ProposalError(s.errorText)
}
