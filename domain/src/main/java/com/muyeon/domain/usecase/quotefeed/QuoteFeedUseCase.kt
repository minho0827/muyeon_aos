package com.muyeon.domain.usecase.quotefeed

import com.muyeon.domain.common.ActiveTypeStore
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.model.quotefeed.QuoteFeedItem
import com.muyeon.domain.model.quotefeed.QuoteFeedPage
import com.muyeon.domain.repository.QuoteFeedRepository

/**
 * ★ 2026-11-08 '레슨을 찾는 회원들' — 어디서 읽을지 한 곳에서 정한다.
 *   - 강사·학원 활동 유형: [ProQuoteFeedSource](/quotes/home · /quotes/available — 예전 그대로).
 *   - 그 밖 회원: [MemberQuoteFeedSource](★ 2026-11-09 /quotes/home/page 로 열린 요청 전체, 서버 옛 버전이면 /quotes/home 10건).
 *   - 비회원: 서버 공개 목록이 없어 빈 목록(섹션 숨김).
 */
class QuoteFeedUseCase(
    private val tokens: TokenStore,
    private val activeType: ActiveTypeStore,
    private val pro: QuoteFeedSource,
    private val member: QuoteFeedSource,
) {
    constructor(repo: QuoteFeedRepository, tokens: TokenStore, activeType: ActiveTypeStore) :
        this(tokens, activeType, ProQuoteFeedSource(repo), MemberQuoteFeedSource(repo))

    val isLoggedIn: Boolean get() = tokens.isLoggedIn

    /** 강사·학원 활동 중 — '제안 보냄' 표시 · 강사용 목록. */
    val canBrowseAll: Boolean get() = tokens.isLoggedIn && activeType.current() in PRO_TYPES

    private val source: QuoteFeedSource get() = if (canBrowseAll) pro else member

    /** 홈 칸(최대 10건). @param categoryId null = 전체 */
    suspend fun section(categoryId: String?): ApiResult<List<QuoteFeedItem>> =
        if (!tokens.isLoggedIn) ApiResult.Success(emptyList()) else source.section(categoryId)

    /** 전체보기 한 장. @param categoryId null = 전체 · @param next null = 첫 장 */
    suspend fun page(categoryId: String?, next: String?): ApiResult<QuoteFeedPage> =
        if (!tokens.isLoggedIn) ApiResult.Success(QuoteFeedPage(emptyList(), hasMore = false)) else source.page(categoryId, next)

    private companion object {
        val PRO_TYPES = setOf("TEACHER", "ACADEMY")
    }
}
