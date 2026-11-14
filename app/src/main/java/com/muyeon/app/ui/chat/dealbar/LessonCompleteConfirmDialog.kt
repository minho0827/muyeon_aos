package com.muyeon.app.ui.chat.dealbar

import androidx.compose.runtime.Composable
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.ui.common.MuyeonDialogKind

/**
 * ★ 2026-11-14 강사 [레슨 완료] 확인창(당근 '거래하셨나요?' → 무용연은 '레슨 완료', iOS LessonCompleteConfirmDialog 1:1).
 *  확인하면 PATCH /lessons/schedules/:id/complete — 회원에게 '레슨이 완료됐어요. 후기를 남겨주세요!' 가 간다.
 *  [beforeStart] = 레슨 시작 시각 전이면 한 줄 경고를 덧붙인다(레슨 상세의 조기 완료 경고와 같은 뜻).
 */
@Composable
fun LessonCompleteConfirmDialog(
    opponentName: String,
    lessonLine: String,
    beforeStart: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val warn = if (beforeStart) "\n아직 레슨 시작 전이에요. 레슨을 마친 뒤 눌러 주세요." else ""
    MuyeonDialog(
        title = "${opponentName}님과 레슨을 마치셨나요?",
        message = "'$lessonLine' 레슨을 완료로 바꾸면 회원님께 후기 요청이 가요.$warn",
        actions = listOf(
            MuyeonDialogAction("네, 레슨을 마쳤어요", MuyeonDialogKind.PRIMARY, onConfirm),
            MuyeonDialogAction("취소", MuyeonDialogKind.CANCEL),
        ),
        onDismiss = onDismiss,
    )
}
