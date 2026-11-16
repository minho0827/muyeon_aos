package com.muyeon.app.ui.chat.dealbar

import com.muyeon.app.ui.chat.ChatSubContext

/**
 * ★ 2026-11-16 채팅방 상단 카드 고르기 — "방 맥락 → 카드" 표(화면 안 if 를 늘리지 않는다).
 *  한 방에 레슨과 대타가 함께 걸리면 둘 다 보인다. 새 맥락(예: 채용)은 [table] 에 한 줄만 더한다.
 */
sealed interface ChatTopCard {
    data class Lesson(val model: DealBarModel) : ChatTopCard
    data class Sub(val model: SubCardModel) : ChatTopCard
}

/** 상단 카드가 읽는 방 값(VM 이 채운다). */
data class ChatTopInput(
    val deal: DealBarInput,
    val subContexts: List<ChatSubContext>,
    val locked: Boolean,
)

object ChatTopCards {

    /** 맥락별 카드 만들기 — 순서가 곧 위→아래 순서. 해당 없으면 null. */
    val table: List<(ChatTopInput) -> ChatTopCard?> = listOf(
        { i -> ChatDealBarResolver.resolve(i.deal)?.let { ChatTopCard.Lesson(it) } },
        { i -> SubContextResolver.resolve(i.subContexts, i.locked)?.let { ChatTopCard.Sub(it) } },
    )

    fun resolve(input: ChatTopInput): List<ChatTopCard> = table.mapNotNull { it(input) }
}
