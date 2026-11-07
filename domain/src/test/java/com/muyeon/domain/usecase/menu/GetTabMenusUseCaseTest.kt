package com.muyeon.domain.usecase.menu

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.menu.AppMenuItem
import com.muyeon.domain.model.menu.AppMenuSurface
import com.muyeon.domain.repository.AppMenuRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetTabMenusUseCaseTest {
    private class FakeRepo(var result: ApiResult<List<AppMenuItem>>) : AppMenuRepository {
        val cache = mutableMapOf<String, List<AppMenuItem>>()
        override suspend fun menus(surface: AppMenuSurface) = result
        override fun cached(surface: AppMenuSurface, activeType: String) = cache["${surface.value}:$activeType"]
        override fun saveCache(surface: AppMenuSurface, activeType: String, items: List<AppMenuItem>) {
            cache["${surface.value}:$activeType"] = items
        }
    }

    private val server = listOf(AppMenuItem("tab.home", "/home", "filled-home", "홈"))
    private val fail = ApiResult.Error(RuntimeException(), "x")

    @Test fun `서버 성공이면 그 목록을 쓰고 유형별로 저장한다`() = runTest {
        val repo = FakeRepo(ApiResult.Success(server))
        assertEquals(server, GetTabMenusUseCase(repo)("teacher"))
        assertEquals(server, repo.cache["TAB:TEACHER"])
    }

    @Test fun `실패면 그 유형의 캐시`() = runTest {
        val repo = FakeRepo(fail).apply { cache["TAB:GENERAL"] = server }
        assertEquals(server, GetTabMenusUseCase(repo)(null))
    }

    @Test fun `캐시도 없으면 기본 목록 — 일반과 전문 유형이 다르다`() = runTest {
        val repo = FakeRepo(fail)
        assertEquals(listOf("home", "lessons", "community", "performance", "my"), GetTabMenusUseCase(repo)("GENERAL").map { it.tabKey })
        assertEquals(listOf("home", "jobs", "casting", "subs", "my"), GetTabMenusUseCase(repo)("ACADEMY").map { it.tabKey })
    }

    @Test fun `서버가 빈 목록을 주면 쓰지 않는다`() = runTest {
        val repo = FakeRepo(ApiResult.Success(emptyList()))
        assertEquals(5, GetTabMenusUseCase(repo)("DANCER").size)
    }
}
