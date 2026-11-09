package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.quotefeed.QuoteFeedItem
import com.muyeon.domain.model.quotefeed.QuoteFeedPage

/** ★ 2026-11-08 '레슨을 찾는 회원들' — quotes.controller.ts home · home/page · available(로그인 필요). */
interface QuoteFeedRepository {
    /** 최근 OPEN 요청 10건(모든 유형). */
    suspend fun home(): ApiResult<List<QuoteFeedItem>>
    /** 응답 가능한 요청(강사·학원 활동 유형만 — 그 밖은 서버 403). [categoryId] null = 전체. */
    suspend fun available(categoryId: String?, page: Int): ApiResult<QuoteFeedPage>
    /**
     * ★ 2026-11-09 열린 요청 전체(로그인한 누구나) — GET /quotes/home/page, 최신순 커서.
     *   서버에 아직 없으면 404 — 부르는 쪽이 [home] 으로 돌아간다.
     */
    suspend fun homePage(categoryId: String?, cursor: String?, limit: Int): ApiResult<QuoteFeedPage>
}
