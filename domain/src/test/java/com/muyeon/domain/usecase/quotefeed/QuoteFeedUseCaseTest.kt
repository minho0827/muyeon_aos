package com.muyeon.domain.usecase.quotefeed

import com.muyeon.domain.common.ActiveTypeStore
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.model.quotefeed.QuoteFeedItem
import com.muyeon.domain.model.quotefeed.QuoteFeedPage
import com.muyeon.domain.repository.QuoteFeedRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteFeedUseCaseTest {
    private fun item(id: Long, cat: String) = QuoteFeedItem(id, cat, null, emptyList(), emptyList(), emptyList(), null, false, false, false)

    private class Repo(val list: List<QuoteFeedItem>) : QuoteFeedRepository {
        var homeCalls = 0
        var availableCalls = 0
        override suspend fun home(): ApiResult<List<QuoteFeedItem>> { homeCalls++; return ApiResult.Success(list) }
        override suspend fun available(categoryId: String?, page: Int): ApiResult<QuoteFeedPage> {
            availableCalls++
            return ApiResult.Success(QuoteFeedPage(list, page, hasMore = true))
        }
    }

    private fun tokens(token: String?) = object : TokenStore {
        override fun accessToken() = token
        override fun myUserId(): Long? = null
    }

    private fun type(t: String?) = object : ActiveTypeStore {
        override fun current() = t
        override fun save(type: String) = Unit
    }

    @Test fun `비회원 — 서버를 부르지 않고 빈 목록(섹션 숨김)`() = runTest {
        val repo = Repo(listOf(item(1, "ballet")))
        val uc = QuoteFeedUseCase(repo, tokens(null), type("TEACHER"))
        assertEquals(emptyList<QuoteFeedItem>(), uc.home().getOrNull())
        assertFalse(uc.canBrowseAll)
        assertEquals(0, repo.homeCalls)
    }

    @Test fun `강사·학원 활동 — 전체보기는 available 페이지`() = runTest {
        val repo = Repo(listOf(item(1, "ballet")))
        val uc = QuoteFeedUseCase(repo, tokens("t"), type("ACADEMY"))
        assertTrue(uc.canBrowseAll)
        assertTrue(uc.page(null, 0).getOrNull()!!.hasMore)
        assertEquals(1, repo.availableCalls)
    }

    @Test fun `일반회원 — 홈 10건을 장르로만 거르고 다음 페이지 없음`() = runTest {
        val repo = Repo(listOf(item(1, "ballet"), item(2, "korean")))
        val uc = QuoteFeedUseCase(repo, tokens("t"), type("GENERAL"))
        val p = uc.page("korean", 0).getOrNull()!!
        assertEquals(listOf(2L), p.items.map { it.id })
        assertFalse(p.hasMore)
        assertEquals(0, repo.availableCalls)
    }
}
