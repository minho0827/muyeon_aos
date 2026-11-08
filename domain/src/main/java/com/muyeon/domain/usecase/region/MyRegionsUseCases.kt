package com.muyeon.domain.usecase.region

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.region.ActiveRegion
import com.muyeon.domain.model.region.RegionEditResult
import com.muyeon.domain.model.region.RegionEntry
import com.muyeon.domain.model.region.SavedRegions
import com.muyeon.domain.repository.RegionRepository

/**
 * ★ 2026-11-08 (b4) 관심 지역 관리 — 웹 RegionContext 의 addRegion·removeRegion·setRepresentative 와 같은 규칙.
 *   추가: 이미 있으면 그대로(ALREADY), 5개면 MAX, 처음이면 대표로. 삭제: 대표는 안 된다(REPRESENTATIVE).
 */
class MyRegionsUseCase(private val repo: RegionRepository) {
    fun saved(): SavedRegions = repo.saved()

    fun add(item: ActiveRegion): RegionEditResult {
        val s = repo.saved()
        if (s.regions.any { it.code == item.code }) return RegionEditResult.ALREADY
        if (s.regions.size >= SavedRegions.MAX) return RegionEditResult.MAX
        repo.saveAll(SavedRegions(s.regions + item, s.repCode ?: item.code))
        return RegionEditResult.OK
    }

    fun remove(code: String): RegionEditResult {
        val s = repo.saved()
        if (code == s.repCode) return RegionEditResult.REPRESENTATIVE
        repo.saveAll(s.copy(regions = s.regions.filterNot { it.code == code }))
        return RegionEditResult.OK
    }

    fun setRepresentative(code: String) {
        repo.saveAll(repo.saved().copy(repCode = code))
    }
}

/**
 * 지역 추가 화면의 시·도 묶음 — 웹 AddRegion: 수도권(서울·경기·인천) 먼저, 그 밖은 서버 순서(행정코드 순), 시군구는 가나다순.
 */
class AddRegionOptionsUseCase(private val repo: RegionRepository) {
    suspend operator fun invoke(): ApiResult<List<Pair<String, List<RegionEntry>>>> = repo.allRegions().map(::group)

    companion object {
        val METRO = listOf("서울", "경기", "인천")

        fun group(all: List<RegionEntry>): List<Pair<String, List<RegionEntry>>> {
            val m = LinkedHashMap<String, MutableList<RegionEntry>>()
            all.forEach { m.getOrPut(it.sido) { mutableListOf() } += it }
            val keys = METRO.filter { it in m } + m.keys.filterNot { it in METRO }
            return keys.map { k -> k to m.getValue(k).sortedBy { it.sigungu } }
        }

        /** 'OO 전체'(시·도 단위) 한 칸 — 코드는 그 시·도 첫 시군구 앞 2자리(웹 sidoPrefix). */
        fun sidoAll(items: List<RegionEntry>, name: String): ActiveRegion =
            ActiveRegion(ActiveRegion.SIDO, items.firstOrNull()?.code?.take(2).orEmpty(), name)
    }
}
