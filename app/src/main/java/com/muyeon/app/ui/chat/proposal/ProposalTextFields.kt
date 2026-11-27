package com.muyeon.app.ui.chat.proposal

import androidx.compose.runtime.Composable
import com.muyeon.app.ui.common.FormField
import com.muyeon.app.ui.common.FormTextField

/** ★ 2026-11-14 장소 — 무용연 회색 입력 상자(선택). */
@Composable
internal fun ProposalPlaceField(place: String, label: String = "장소", onChange: (String) -> Unit) {
    // ★ 2026-11-27 견적 응답의 '수업 장소'(장소 주인이 강사일 때)도 이 칸을 쓴다.
    FormField(label, hint = "(선택)") {
        FormTextField(place, onChange, "예: 강남 무용연 연습실", ProposalFormState.PLACE_MAX, tag = "proposal.place")
    }
}

/** ★ 2026-11-14 메모 — 무용연 회색 입력 상자, 여러 줄(선택). */
@Composable
internal fun ProposalMemoField(memo: String, onChange: (String) -> Unit) {
    FormField("메모", hint = "(선택)") {
        FormTextField(memo, onChange, "예: 기초 스트레칭 준비해 오세요", ProposalFormState.MEMO_MAX, tag = "proposal.memo", minLines = 3)
    }
}
