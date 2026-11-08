package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.data.common.safeUnitCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.alert.AlertPrefs
import com.muyeon.domain.repository.AlertPrefsRepository
import com.muyeon.network.api.AlertPrefsApi
import com.muyeon.network.dto.alert.AlertPrefsBody
import com.squareup.moshi.Moshi

/** ★ 2026-11-08 (b4) 관심 조건 알림 — 웹 callGetAlertPrefs·callUpdateAlertPrefs. */
class AlertPrefsRepositoryImpl(private val api: AlertPrefsApi, private val moshi: Moshi) : AlertPrefsRepository {
    override suspend fun get(): ApiResult<AlertPrefs> = safeApiCall(moshi) { api.get() }.map { it.toDomain() }
    override suspend fun save(prefs: AlertPrefs): ApiResult<Unit> = safeUnitCall(moshi) { api.save(AlertPrefsBody.of(prefs)) }
}
