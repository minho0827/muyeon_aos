package com.muyeon.domain.usecase.quotefeed

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.quotefeed.QuoteFeedItem
import com.muyeon.domain.model.quotefeed.QuoteFeedPage
import com.muyeon.domain.repository.QuoteFeedRepository

/**
 * ★ 2026-11-09 '레슨을 찾는 회원들' 어디서 읽나 — 강사·학원용 / 그 밖 회원용 두 갈래.
 *   화면은 갈래를 모른다. [QuoteFeedUseCase] 가 활동 유형으로 하나를 고른다.
 */
interface QuoteFeedSource {
    /** 홈 칸(최대 [SECTION_SIZE]건). [categoryId] null = 전체. */
    suspend fun section(categoryId: String?): ApiResult<List<QuoteFeedItem>>

    /** 전체보기 한 장. [next] null = 첫 장. */
    suspend fun page(categoryId: String?, next: String?): ApiResult<QuoteFeedPage>

    companion object {
        const val SECTION_SIZE = 10
    }
}

/** 강사·학원 — 예전 그대로. 홈 '전체' = /quotes/home, 장르·전체보기 = /quotes/available 페이지 번호. */
class ProQuoteFeedSource(private val repo: QuoteFeedRepository) : QuoteFeedSource {
    override suspend fun section(categoryId: String?): ApiResult<List<QuoteFeedItem>> =
        if (categoryId == null) repo.home()
        else repo.available(categoryId, 0).map { it.items.take(QuoteFeedSource.SECTION_SIZE) }

    override suspend fun page(categoryId: String?, next: String?): ApiResult<QuoteFeedPage> =
        repo.available(categoryId, next?.toIntOrNull() ?: 0)
}

/**
 * 그 밖 회원 — /quotes/home/page 커서로 열린 요청을 끝까지.
 *   서버가 아직 옛 버전(404)이면 /quotes/home 10건을 장르로 거른다(예전 동작).
 */
class MemberQuoteFeedSource(private val repo: QuoteFeedRepository) : QuoteFeedSource {
    override suspend fun section(categoryId: String?): ApiResult<List<QuoteFeedItem>> =
        withFallback(categoryId, repo.homePage(categoryId, null, QuoteFeedSource.SECTION_SIZE)).map { it.items }

    override suspend fun page(categoryId: String?, next: String?): ApiResult<QuoteFeedPage> {
        val r = repo.homePage(categoryId, next, PAGE_SIZE)
        return if (next == null) withFallback(categoryId, r) else r
    }

    private suspend fun withFallback(categoryId: String?, r: ApiResult<QuoteFeedPage>): ApiResult<QuoteFeedPage> {
        if (r !is ApiResult.Error || r.statusCode != 404) return r
        return repo.home().map { list -> QuoteFeedPage(list.filter { categoryId == null || it.categoryId == categoryId }, hasMore = false) }
    }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
