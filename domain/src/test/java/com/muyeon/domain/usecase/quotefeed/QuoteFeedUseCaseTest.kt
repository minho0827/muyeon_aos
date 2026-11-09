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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteFeedUseCaseTest {
    private fun item(id: Long, cat: String) = QuoteFeedItem(id, cat, null, emptyList(), emptyList(), emptyList(), null, false, false, false)

    private class Repo(val list: List<QuoteFeedItem>, val homePageMissing: Boolean = false) : QuoteFeedRepository {
        var homeCalls = 0
        var availableCalls = 0
        val homePageCalls = mutableListOf<Triple<String?, String?, Int>>()
        override suspend fun home(): ApiResult<List<QuoteFeedItem>> { homeCalls++; return ApiResult.Success(list) }
        override suspend fun available(categoryId: String?, page: Int): ApiResult<QuoteFeedPage> {
            availableCalls++
            return ApiResult.Success(QuoteFeedPage(list, hasMore = true, next = (page + 1).toString()))
        }
        override suspend fun homePage(categoryId: String?, cursor: String?, limit: Int): ApiResult<QuoteFeedPage> {
            homePageCalls += Triple(categoryId, cursor, limit)
            if (homePageMissing) return ApiResult.Error(RuntimeException("404"), statusCode = 404)
            return ApiResult.Success(QuoteFeedPage(list.filter { categoryId == null || it.categoryId == categoryId }, hasMore = true, next = "c2"))
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
        assertEquals(emptyList<QuoteFeedItem>(), uc.section(null).getOrNull())
        assertFalse(uc.page(null, null).getOrNull()!!.hasMore)
        assertFalse(uc.canBrowseAll)
        assertEquals(0, repo.homeCalls + repo.availableCalls + repo.homePageCalls.size)
    }

    @Test fun `강사·학원 활동 — 홈 전체는 home, 전체보기는 available 페이지 번호`() = runTest {
        val repo = Repo(listOf(item(1, "ballet")))
        val uc = QuoteFeedUseCase(repo, tokens("t"), type("ACADEMY"))
        assertTrue(uc.canBrowseAll)
        uc.section(null)
        assertEquals(1, repo.homeCalls)
        val p = uc.page(null, null).getOrNull()!!
        assertEquals("1", p.next)
        uc.page(null, p.next)
        assertEquals(2, repo.availableCalls)
        assertTrue(repo.homePageCalls.isEmpty())
    }

    @Test fun `일반회원 — home·page 커서로 끝까지, 홈 칸은 10건`() = runTest {
        val repo = Repo(listOf(item(1, "ballet"), item(2, "korean")))
        val uc = QuoteFeedUseCase(repo, tokens("t"), type("GENERAL"))
        assertEquals(listOf(2L), uc.section("korean").getOrNull()!!.map { it.id })
        val p = uc.page("korean", null).getOrNull()!!
        assertTrue(p.hasMore)
        uc.page("korean", p.next)
        assertEquals(listOf(Triple("korean", null, 10), Triple("korean", null, 20), Triple("korean", "c2", 20)), repo.homePageCalls)
        assertEquals(0, repo.availableCalls + repo.homeCalls)
    }

    @Test fun `일반회원 — 서버가 옛 버전(404)이면 home 10건을 장르로 거르고 끝`() = runTest {
        val repo = Repo(listOf(item(1, "ballet"), item(2, "korean")), homePageMissing = true)
        val uc = QuoteFeedUseCase(repo, tokens("t"), type("GENERAL"))
        val p = uc.page("korean", null).getOrNull()!!
        assertEquals(listOf(2L), p.items.map { it.id })
        assertFalse(p.hasMore)
        assertNull(p.next)
        assertEquals(listOf(1L, 2L), uc.section(null).getOrNull()!!.map { it.id })
        assertEquals(2, repo.homeCalls)
    }
}
