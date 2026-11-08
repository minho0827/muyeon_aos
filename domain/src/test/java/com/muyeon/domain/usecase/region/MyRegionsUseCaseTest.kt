package com.muyeon.domain.usecase.region

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.region.ActiveRegion
import com.muyeon.domain.model.region.Coordinates
import com.muyeon.domain.model.region.RegionEditResult
import com.muyeon.domain.model.region.RegionEntry
import com.muyeon.domain.model.region.SavedRegions
import com.muyeon.domain.repository.RegionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

/** (b4) 웹 RegionContext addRegion·removeRegion·setRepresentative 와 같은 규칙. */
class MyRegionsUseCaseTest {
    private class FakeRepo(var s: SavedRegions = SavedRegions()) : RegionRepository {
        override val active: StateFlow<ActiveRegion?> = MutableStateFlow(null)
        override fun syncFromWeb(json: String?) = Unit
        override fun webJson(): String? = null
        override suspend fun nearest(c: Coordinates): ApiResult<ActiveRegion?> = ApiResult.Success(null)
        override fun saveDetected(region: ActiveRegion) = Unit
        override fun saved() = s
        override fun saveAll(saved: SavedRegions) { s = saved }
        override suspend fun allRegions(): ApiResult<List<RegionEntry>> = ApiResult.Success(emptyList())
    }

    private fun r(code: String) = ActiveRegion(ActiveRegion.SIGUNGU, code, "지역$code")

    @Test fun `첫 지역은 대표, 같은 지역은 그대로, 5개면 MAX`() {
        val repo = FakeRepo(); val uc = MyRegionsUseCase(repo)
        assertEquals(RegionEditResult.OK, uc.add(r("11680")))
        assertEquals("11680", repo.s.repCode)
        assertEquals(RegionEditResult.ALREADY, uc.add(r("11680")))
        listOf("1", "2", "3", "4").forEach { uc.add(r(it)) }
        assertEquals(5, repo.s.regions.size)
        assertEquals(RegionEditResult.MAX, uc.add(r("9")))
        assertEquals("11680", repo.s.repCode)
    }

    @Test fun `대표는 지울 수 없고, 대표를 바꾸면 지울 수 있다`() {
        val repo = FakeRepo(SavedRegions(listOf(r("a"), r("b")), "a")); val uc = MyRegionsUseCase(repo)
        assertEquals(RegionEditResult.REPRESENTATIVE, uc.remove("a"))
        uc.setRepresentative("b")
        assertEquals(RegionEditResult.OK, uc.remove("a"))
        assertEquals(listOf("b"), repo.s.regions.map { it.code })
    }

    @Test fun `시도 묶음 — 수도권 먼저, 그 밖은 서버 순서, 시군구 가나다`() {
        val g = AddRegionOptionsUseCase.group(listOf(
            RegionEntry("26350", "부산", "해운대구", "부산 해운대구"),
            RegionEntry("11740", "서울", "강동구", "서울 강동구"),
            RegionEntry("11680", "서울", "강남구", "서울 강남구"),
            RegionEntry("41135", "경기", "성남시 분당구", "경기 성남시 분당구"),
        ))
        assertEquals(listOf("서울", "경기", "부산"), g.map { it.first })
        assertEquals(listOf("강남구", "강동구"), g[0].second.map { it.sigungu })
        assertEquals("11", AddRegionOptionsUseCase.sidoAll(g[0].second, "서울 전체").code)
    }
}
