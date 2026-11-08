package com.muyeon.domain.usecase.quotefeed

import com.muyeon.domain.common.ActiveTypeStore
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.model.quotefeed.QuoteFeedItem
import com.muyeon.domain.model.quotefeed.QuoteFeedPage
import com.muyeon.domain.repository.QuoteFeedRepository

/**
 * ★ 2026-11-08 '레슨을 찾는 회원들' — 어디서 읽을지 한 곳에서 정한다.
 *   - 홈 섹션: GET /quotes/home(웹 HomeQuoteRequests 와 같은 10건). 비회원은 서버 공개 목록이 없어 빈 목록(섹션 숨김).
 *   - 전체보기: 강사·학원 활동 유형이면 GET /quotes/available(웹 /availableQuotes 목록 — 서버 페이지·장르 필터),
 *     그 밖(일반회원 등)은 /quotes/home 10건을 장르로만 거른다(서버가 다른 유형에 목록을 열지 않는다).
 */
class QuoteFeedUseCase(
    private val repo: QuoteFeedRepository,
    private val tokens: TokenStore,
    private val activeType: ActiveTypeStore,
) {
    val isLoggedIn: Boolean get() = tokens.isLoggedIn

    /** 강사·학원 활동 중 — 전체 요청 목록(페이지)을 볼 수 있다. */
    val canBrowseAll: Boolean get() = tokens.isLoggedIn && activeType.current() in PRO_TYPES

    suspend fun home(): ApiResult<List<QuoteFeedItem>> =
        if (!tokens.isLoggedIn) ApiResult.Success(emptyList()) else repo.home()

    /** @param categoryId null = 전체 */
    suspend fun page(categoryId: String?, page: Int): ApiResult<QuoteFeedPage> {
        if (canBrowseAll) return repo.available(categoryId, page)
        return home().map { list -> QuoteFeedPage(list.filter { categoryId == null || it.categoryId == categoryId }, 0, hasMore = false) }
    }

    private companion object {
        val PRO_TYPES = setOf("TEACHER", "ACADEMY")
    }
}
