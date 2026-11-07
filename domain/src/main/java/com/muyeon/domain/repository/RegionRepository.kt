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
}
