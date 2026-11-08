package com.muyeon.network.dto.quotefeed

import com.muyeon.domain.model.quotefeed.QuoteFeedItem
import com.muyeon.domain.model.quotefeed.QuoteFeedPage
import com.squareup.moshi.JsonClass

/** 문진 답 한 개(quote.entity answers) — 카드는 선택지 id 만 쓴다. */
@JsonClass(generateAdapter = true)
data class QuoteFeedAnswerDto(val questionId: String? = null, val optionIds: List<String>? = null)

/**
 * `GET /quotes/home` 한 칸(quotes.service listHome — 카드 칸만) 과 `GET /quotes/available` items 의 Quote 행(필요한 칸만 읽는다).
 *   available 행에는 isMine·canRespond 가 없다 — 서버가 내 요청·이미 응답한 요청을 빼고 주므로 canRespond=true 로 본다.
 */
@JsonClass(generateAdapter = true)
data class QuoteFeedItemDto(
    val id: Long? = null,
    val categoryId: String? = null,
    val region: String? = null,
    val answers: List<QuoteFeedAnswerDto>? = null,
    val createdAt: String? = null,
    val isMine: Boolean? = null,
    val canRespond: Boolean? = null,
    val guestRequest: Boolean? = null,
) {
    fun toDomain(fromAvailable: Boolean = false): QuoteFeedItem? {
        val id = id ?: return null
        fun opts(q: String) = answers.orEmpty().firstOrNull { it.questionId == q }?.optionIds.orEmpty()
        return QuoteFeedItem(
            id = id,
            categoryId = categoryId?.takeIf { it.isNotBlank() },
            region = region?.takeIf { it.isNotBlank() },
            days = opts("days").sortedBy { DAY_ORDER.indexOf(it).let { i -> if (i < 0) Int.MAX_VALUE else i } },
            times = opts("time"),
            formats = opts("format"),
            createdAt = createdAt,
            isMine = isMine == true,
            canRespond = if (fromAvailable) true else canRespond == true,
            guestRequest = guestRequest == true,
        )
    }

    private companion object {
        val DAY_ORDER = listOf("mon", "tue", "wed", "thu", "fri", "sat", "sun")
    }
}

/** `GET /quotes/available` → { items, page, hasMore, … }(quotes.service listAvailable). */
@JsonClass(generateAdapter = true)
data class QuoteFeedAvailableDto(
    val items: List<QuoteFeedItemDto>? = null,
    val page: Int? = null,
    val hasMore: Boolean? = null,
) {
    fun toDomain(requestedPage: Int) = QuoteFeedPage(
        items = items.orEmpty().mapNotNull { it.toDomain(fromAvailable = true) },
        page = page ?: requestedPage,
        hasMore = hasMore == true,
    )
}
