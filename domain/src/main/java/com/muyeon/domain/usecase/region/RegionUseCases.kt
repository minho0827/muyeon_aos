package com.muyeon.domain.usecase.region

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.region.ActiveRegion
import com.muyeon.domain.model.region.Coordinates
import com.muyeon.domain.repository.RegionRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveActiveRegionUseCase(private val repo: RegionRepository) {
    operator fun invoke(): StateFlow<ActiveRegion?> = repo.active
}

/**
 * 지역이 아직 없으면 좌표로 가장 가까운 지역을 대표로(웹 RegionProvider 첫 실행과 같다).
 *   수도권은 시군구 그대로, 그 밖은 시·도로 줄인다. 이미 있으면 아무것도 하지 않는다.
 */
class DetectRegionUseCase(private val repo: RegionRepository) {
    suspend operator fun invoke(coords: suspend () -> Coordinates?) {
        if (repo.active.value != null) return
        val c = coords() ?: return
        val r = repo.nearest(c)
        if (r is ApiResult.Success) r.data?.let(repo::saveDetected)
    }
}
