package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.application.MyApplicationTab
import com.muyeon.domain.model.application.MyApplicationsPage
import com.muyeon.domain.model.application.ReceivedApplicationsPage

/** 지원 내역 — 내가 지원한 공고 · 내 공고에 받은 지원. */
interface ApplicationRepository {
    suspend fun minePage(tab: MyApplicationTab, period: String?, cursor: String?): ApiResult<MyApplicationsPage>
    suspend fun withdraw(kind: String, id: Long): ApiResult<Unit>
    suspend fun archiveMine(kind: String, id: Long): ApiResult<Unit>
    suspend fun receivedPage(kind: String?, postingId: Long?, archived: Boolean, cursor: String?): ApiResult<ReceivedApplicationsPage>
    suspend fun archiveReceived(kind: String, id: Long): ApiResult<Unit>
    suspend fun restoreReceived(kind: String, id: Long): ApiResult<Unit>
    suspend fun purgeReceived(kind: String, id: Long): ApiResult<Unit>
}
