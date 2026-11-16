package com.muyeon.app.ui.chat.dealbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.chat.ChatPendingProposal
import com.muyeon.app.ui.chat.ChatRoomViewModel
import com.muyeon.app.ui.chat.LessonProposalBubble
import com.muyeon.app.ui.chat.LockCountdownLine
import com.muyeon.app.ui.common.MuyeonColors

/**
 * ★ 2026-11-14 채팅방 상단(헤더 아래 ~ 메시지 위) — 예전 LessonContextArea(10겹 if/else)를 대신한다(iOS ChatDealBarArea 1:1).
 *  쌓는 순서만 정한다: 대화 종료 예정 줄 → 고정 바 → (재입장으로 숨은) 대기 중 약속 카드.
 *  각 조각은 따로 파일(ChatDealBar · PinnedProposalCard · LockCountdownLine)이다.
 */
@Composable
fun ChatDealBarArea(
    vm: ChatRoomViewModel,
    onAction: (DealAction) -> Unit,
    onOpenCycles: () -> Unit,
    onProposalChanged: () -> Unit,
    onOpenProposalPayment: (Int) -> Unit,
    onSubAction: (SubAction, SubCardItem) -> Unit = { _, _ -> },
) {
    val cards = ChatTopCards.resolve(vm.topInput())
    Column(Modifier.fillMaxWidth()) {
        vm.lockDaysLeft()?.let { LockCountdownLine(it) }
        // ★ 2026-11-16 방 맥락 → 카드 표(ChatTopCards). 레슨 바와 대타 카드가 함께 걸리면 둘 다 보인다.
        cards.forEach { card ->
            when (card) {
                is ChatTopCard.Lesson -> ChatDealBar(card.model, onAction, onOpenCycles)
                is ChatTopCard.Sub -> {
                    SubContextCard(card.model, onSubAction)
                    SubNoticeOnce(vm.roomId)
                }
            }
        }
        vm.pendingProposal?.let { PinnedProposalCard(it, vm, onProposalChanged, onOpenProposalPayment) }
    }
}

/** 대타 카드 아래 안내 — 방마다 처음 연 그 화면에서만 보인다(다시 열면 안 보임). */
@Composable
private fun SubNoticeOnce(roomId: Int) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val show = androidx.compose.runtime.remember(roomId) { SubChatNotice.consume(ctx, roomId) }
    if (show) SubChatNoticeLine()
}

/** 재입장(나가기)으로 대화에서 숨은 대기 중 약속 제안 — 바 아래 고정(바의 [약속 확인하기]가 가리킬 카드가 대화에 없을 때). */
@Composable
private fun PinnedProposalCard(
    pp: ChatPendingProposal,
    vm: ChatRoomViewModel,
    onChanged: () -> Unit,
    onOpenPayment: (Int) -> Unit,
) {
    Box(Modifier.fillMaxWidth().background(MuyeonColors.groupedBg).padding(horizontal = 12.dp, vertical = 8.dp)) {
        LessonProposalBubble(
            contentJson = pp.json,
            isProposer = pp.isProposer,
            currentUserId = vm.currentUserId,
            token = vm.tokenForCards,
            onChanged = onChanged,
            onOpenPayment = onOpenPayment,
        )
    }
}
