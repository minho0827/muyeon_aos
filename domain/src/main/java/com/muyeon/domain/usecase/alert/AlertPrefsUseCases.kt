package com.muyeon.domain.usecase.alert

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.alert.AlertForm
import com.muyeon.domain.model.alert.AlertPrefs
import com.muyeon.domain.model.region.RegionEntry
import com.muyeon.domain.repository.AlertPrefsRepository
import com.muyeon.domain.repository.MemberRepository
import com.muyeon.domain.repository.RegionRepository

/**
 * ★ 2026-11-08 (b4) 관심 조건 알림 — 웹 AlertSettings: 저장된 조건을 읽고(실패해도 빈 조건으로 연다 — 웹 catch 무시), 보유 유형으로 항목을 고른다.
 */
class AlertPrefsUseCase(
    private val repo: AlertPrefsRepository,
    private val member: MemberRepository,
    private val regions: RegionRepository,
) {
    suspend fun load(): AlertForm {
        val prefs = repo.get().getOrNull() ?: AlertPrefs()
        val held = member.myRoles().getOrNull()?.held.orEmpty().map { it.uppercase() }
        return AlertForm(prefs, held)
    }

    /** 지역 코드 → 이름(시군구 5자리면 그 이름, 2자리면 'OO 전체'). 목록을 못 읽으면 빈 목록. */
    suspend fun regionList(): List<RegionEntry> = regions.allRegions().getOrNull().orEmpty()

    /** 종류를 하나도 안 골랐으면 보내지 않는다(서버가 예전 값을 둔다 — 웹 types: undefined). */
    suspend fun save(p: AlertPrefs): ApiResult<Unit> = repo.save(p)
}
