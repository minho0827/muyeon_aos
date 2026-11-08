package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.alert.AlertPrefs

/** ★ 2026-11-08 (b4) 관심 조건 알림 — `GET/PATCH /me/alert-prefs`. 로그인한 회원만. */
interface AlertPrefsRepository {
    suspend fun get(): ApiResult<AlertPrefs>
    suspend fun save(prefs: AlertPrefs): ApiResult<Unit>
}
