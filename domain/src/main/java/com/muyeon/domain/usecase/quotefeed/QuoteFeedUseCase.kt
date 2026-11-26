package com.muyeon.domain.usecase.quotefeed

import com.muyeon.domain.common.ActiveTypeStore
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.model.member.LessonProviders
import com.muyeon.domain.model.quotefeed.QuoteFeedItem
import com.muyeon.domain.model.quotefeed.QuoteFeedPage
import com.muyeon.domain.repository.QuoteFeedRepository

/**
 * ★ 2026-11-08 '레슨을 찾는 회원들' — 어디서 읽을지 한 곳에서 정한다.
 *   - 레슨 제공 활동 유형(LessonProviders): [ProQuoteFeedSource](/quotes/home · /quotes/available — 예전 그대로).
 *   - 그 밖 회원: [MemberQuoteFeedSource](★ 2026-11-09 /quotes/home/page 로 열린 요청 전체, 서버 옛 버전이면 /quotes/home 10건).
 *   - 비회원: ★ 2026-11-26 [GuestQuoteFeedSource](/public/quotes/home/page — 읽기 전용, 답장은 상세에서 로그인 안내).
 */
class QuoteFeedUseCase(
    private val tokens: TokenStore,
    private val activeType: ActiveTypeStore,
    private val pro: QuoteFeedSource,
    private val member: QuoteFeedSource,
    private val guest: QuoteFeedSource,
) {
    constructor(repo: QuoteFeedRepository, tokens: TokenStore, activeType: ActiveTypeStore) :
        this(tokens, activeType, ProQuoteFeedSource(repo), MemberQuoteFeedSource(repo), GuestQuoteFeedSource(repo))

    val isLoggedIn: Boolean get() = tokens.isLoggedIn

    /** 레슨 제공 유형(강사·학원, 서버가 켜면 무용수도) 활동 중 — '제안 보냄' 표시 · 강사용 목록. */
    val canBrowseAll: Boolean get() = tokens.isLoggedIn && LessonProviders.canProvide(activeType.current())

    private val source: QuoteFeedSource get() = when {
        !tokens.isLoggedIn -> guest // ★ 2026-11-26 비회원도 읽기 전용으로 본다
        canBrowseAll -> pro
        else -> member
    }

    /** 홈 칸(최대 10건). @param categoryId null = 전체 */
    suspend fun section(categoryId: String?): ApiResult<List<QuoteFeedItem>> = source.section(categoryId)

    /** 전체보기 한 장. @param categoryId null = 전체 · @param next null = 첫 장 */
    suspend fun page(categoryId: String?, next: String?): ApiResult<QuoteFeedPage> = source.page(categoryId, next)
}
