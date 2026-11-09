package com.muyeon.data.repository

import android.content.Context
import com.muyeon.data.common.safeApiCall
import com.muyeon.data.common.safeUnitCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.repository.UiFlagRepository
import com.muyeon.network.api.RegionApi
import com.squareup.moshi.Moshi

class UiFlagRepositoryImpl(
    context: Context,
    private val api: RegionApi,
    private val moshi: Moshi,
    private val tokens: TokenStore,
) : UiFlagRepository {
    private val prefs = context.applicationContext.getSharedPreferences("muyeon.uiFlags", Context.MODE_PRIVATE)
    @Volatile private var synced = false

    init {
        // ★ 2026-11-09 계정이 바뀌면 새 계정의 서버 플래그를 다시 합친다(본 기록 자체는 기기 것이라 남긴다).
        com.muyeon.domain.common.SessionEvents.register { _, _ -> synced = false }
    }

    override fun hasSeen(key: String): Boolean = prefs.getBoolean(key, false)

    override suspend fun markSeen(key: String) {
        prefs.edit().putBoolean(key, true).apply()
        if (tokens.isLoggedIn) safeUnitCall(moshi) { api.setUiFlags(mapOf(key to true)) }
    }

    override suspend fun syncFromServer() {
        if (synced) return
        val r = safeApiCall(moshi) { api.uiFlags() }
        if (r is ApiResult.Success) {
            synced = true
            prefs.edit().apply { r.data.flags.orEmpty().forEach { (k, v) -> if (v == true) putBoolean(k, true) } }.apply()
        }
    }
}
