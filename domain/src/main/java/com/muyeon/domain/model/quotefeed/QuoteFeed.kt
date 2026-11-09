package com.muyeon.domain.model.quotefeed

/**
 * ★ 2026-11-08 '레슨을 찾는 회원들' 한 칸 — 회원이 올린 레슨 요청(견적 요청) 요약.
 *   웹 components/muyeon/HomeQuoteRequests.js 카드와 같은 재료만 담는다(요청자 이름·연락처·자유 답변은 서버가 주지 않거나 버린다).
 *   요일·시간대·레슨 형태는 서버 선택지 id 그대로(mon·evening·private …) — 이름은 화면 문구가 붙인다.
 */
data class QuoteFeedItem(
    val id: Long,
    /** ballet·korean·modern·practical·musical·barre·balletfit·hobby — 없으면 null. */
    val categoryId: String?,
    val region: String?,
    val days: List<String>,
    val times: List<String>,
    val formats: List<String>,
    val createdAt: String?,
    /** 내가 올린 요청 — 누르면 받은 견적(/myQuotes/:id). */
    val isMine: Boolean,
    /** 지금 견적을 보낼 수 있다(강사·학원 · 내 요청 아님 · 아직 안 보냄). 일반회원은 false. */
    val canRespond: Boolean,
    /** 비회원이 올린 요청(목록 화면 '비회원 요청' 배지 — 웹 AvailableQuotes). */
    val guestRequest: Boolean,
)

/**
 * 목록 한 페이지. [next] = 다음 장을 부를 열쇠(강사·학원은 페이지 번호, 그 밖은 서버 커서) — 없으면 끝.
 */
data class QuoteFeedPage(val items: List<QuoteFeedItem>, val hasMore: Boolean, val next: String? = null)

/** 서버 견적 장르 id(웹 constants/quoteCategories QUOTE_CATEGORIES 순서). */
object QuoteFeedCategories {
    val ALL: List<String> = listOf("ballet", "korean", "modern", "practical", "musical", "barre", "balletfit", "hobby")
}
