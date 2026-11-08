package com.muyeon.data.repository

import android.content.Context
import com.muyeon.data.common.safeApiCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.region.ActiveRegion
import com.muyeon.domain.model.region.Coordinates
import com.muyeon.domain.repository.RegionRepository
import com.muyeon.network.api.RegionApi
import com.muyeon.network.dto.region.RegionDto
import com.muyeon.network.dto.region.WebRegionDto
import com.muyeon.network.dto.region.WebRegionsDto
import com.squareup.moshi.Moshi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 대표 지역 — 웹 muyeonRegions 와 같은 모양 JSON 을 기기에 둔다(웹·앱이 같은 값). */
class RegionRepositoryImpl(context: Context, private val api: RegionApi, private val moshi: Moshi) : RegionRepository {
    private val prefs = context.applicationContext.getSharedPreferences("muyeon.region", Context.MODE_PRIVATE)
    private val adapter = moshi.adapter(WebRegionsDto::class.java)
    private val _active = MutableStateFlow(parse(prefs.getString(KEY, null)))
    override val active: StateFlow<ActiveRegion?> = _active.asStateFlow()

    private fun parse(json: String?): ActiveRegion? {
        val v = json?.let { runCatching { adapter.fromJson(it) }.getOrNull() } ?: return null
        val rep = v.regions.orEmpty().firstOrNull { it.code == v.repCode } ?: return null
        val code = rep.code ?: return null
        return ActiveRegion(rep.kind ?: ActiveRegion.SIDO, code, rep.name.orEmpty())
    }

    override fun syncFromWeb(json: String?) {
        val r = parse(json) ?: return
        prefs.edit().putString(KEY, json).apply()
        if (r != _active.value) _active.value = r
    }

    override fun webJson(): String? = prefs.getString(KEY, null)

    override suspend fun nearest(c: Coordinates): ApiResult<ActiveRegion?> =
        safeApiCall(moshi) { api.nearest(c.lat, c.lng) }.map { body ->
            val raw = body.string()
            if (raw.isBlank() || raw == "null") null else runCatching { moshi.adapter(RegionDto::class.java).fromJson(raw)?.toActive() }.getOrNull()
        }

    override fun saveDetected(region: ActiveRegion) {
        val json = adapter.toJson(WebRegionsDto(listOf(WebRegionDto(region.kind, region.code, region.name)), region.code))
        prefs.edit().putString(KEY, json).apply()
        _active.value = region
    }

    // ★ 2026-11-08 (b4) 관심 지역 관리(앱 화면) — 목록 전체.
    override fun saved(): com.muyeon.domain.model.region.SavedRegions {
        val v = prefs.getString(KEY, null)?.let { runCatching { adapter.fromJson(it) }.getOrNull() }
            ?: return com.muyeon.domain.model.region.SavedRegions()
        val list = v.regions.orEmpty().mapNotNull { r -> r.code?.takeIf { it.isNotBlank() }?.let { ActiveRegion(r.kind ?: ActiveRegion.SIDO, it, r.name.orEmpty()) } }
        return com.muyeon.domain.model.region.SavedRegions(list, v.repCode)
    }

    override fun saveAll(saved: com.muyeon.domain.model.region.SavedRegions) {
        val json = adapter.toJson(WebRegionsDto(saved.regions.map { WebRegionDto(it.kind, it.code, it.name) }, saved.repCode))
        prefs.edit().putString(KEY, json).apply()
        val rep = saved.representative
        if (rep != _active.value) _active.value = rep
    }

    override suspend fun allRegions(): ApiResult<List<com.muyeon.domain.model.region.RegionEntry>> =
        safeApiCall(moshi) { api.regions() }.map { l -> l.mapNotNull { it.toDomain() } }

    private companion object { const val KEY = "muyeonRegions" }
}
