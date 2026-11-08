package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.region.ActiveRegion
import com.muyeon.domain.model.region.Coordinates
import kotlinx.coroutines.flow.StateFlow

/**
 * 대표 지역 — 웹과 같은 값을 쓴다. 지역 고르기 화면은 웹(/myRegions)이라, 웹 창이 닫힐 때 웹 저장값
 *   (localStorage muyeonRegions)을 [syncFromWeb] 으로 받아 둔다. 처음이면 가까운 지역을 정한다.
 */
interface RegionRepository {
    val active: StateFlow<ActiveRegion?>
    /** 웹 muyeonRegions JSON(`{ regions:[{kind,code,name}], repCode }`). 비었거나 깨졌으면 무시. */
    fun syncFromWeb(json: String?)
    /** 웹이 아직 지역이 없을 때 쓰도록 같은 모양 JSON(웹 창을 열 때 넣는다). 없으면 null. */
    fun webJson(): String?
    suspend fun nearest(c: Coordinates): ApiResult<ActiveRegion?>
    fun saveDetected(region: ActiveRegion)

    // ★ 2026-11-08 (b4) 관심 지역 관리가 앱 화면이 됐다(웹 /myRegions·/addRegion) — 목록 전체를 읽고 쓴다.
    /** 저장된 관심 지역 목록(없으면 빈 목록). */
    fun saved(): com.muyeon.domain.model.region.SavedRegions
    /** 목록 저장 — 같은 JSON 을 웹 창에도 넣는다(WebViewActivity). 대표가 바뀌면 [active] 도 바뀐다. */
    fun saveAll(saved: com.muyeon.domain.model.region.SavedRegions)
    /** `GET /regions` — 시군구 전체. */
    suspend fun allRegions(): ApiResult<List<com.muyeon.domain.model.region.RegionEntry>>
}
