package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.data.common.safeUnitCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPaging
import com.muyeon.domain.model.application.MyApplicationTab
import com.muyeon.domain.model.application.MyApplicationsPage
import com.muyeon.domain.model.application.ReceivedApplicationsPage
import com.muyeon.domain.repository.ApplicationRepository
import com.muyeon.network.api.ApplicationApi
import com.squareup.moshi.Moshi

class ApplicationRepositoryImpl(private val api: ApplicationApi, private val moshi: Moshi) : ApplicationRepository {
    override suspend fun minePage(tab: MyApplicationTab, period: String?, cursor: String?): ApiResult<MyApplicationsPage> =
        safeApiCall(moshi) { api.minePage(tab.name, period, cursor, CursorPaging.DEFAULT_LIMIT) }.map { d ->
            val c = d.counts
            MyApplicationsPage(
                d.items.orEmpty().mapNotNull { it.toDomain() },
                if (c == null) emptyMap() else mapOf(
                    MyApplicationTab.ALL to (c.ALL ?: 0), MyApplicationTab.PROGRESS to (c.PROGRESS ?: 0),
                    MyApplicationTab.CONFIRMED to (c.CONFIRMED ?: 0), MyApplicationTab.REJECTED to (c.REJECTED ?: 0),
                ),
                d.next,
            )
        }

    override suspend fun withdraw(kind: String, id: Long): ApiResult<Unit> = safeUnitCall(moshi) { api.withdraw(kind, id) }
    override suspend fun archiveMine(kind: String, id: Long): ApiResult<Unit> = safeUnitCall(moshi) { api.archiveMine(kind, id) }

    override suspend fun receivedPage(kind: String?, postingId: Long?, archived: Boolean, cursor: String?): ApiResult<ReceivedApplicationsPage> =
        safeApiCall(moshi) { api.receivedPage(kind, postingId, archived, cursor, CursorPaging.DEFAULT_LIMIT) }.map { d ->
            ReceivedApplicationsPage(d.items.orEmpty().mapNotNull { it.toDomain() }, d.archivedCount ?: 0, d.next)
        }

    override suspend fun archiveReceived(kind: String, id: Long): ApiResult<Unit> = safeUnitCall(moshi) { api.archiveReceived(kind, id) }
    override suspend fun restoreReceived(kind: String, id: Long): ApiResult<Unit> = safeUnitCall(moshi) { api.restoreReceived(kind, id) }
    override suspend fun purgeReceived(kind: String, id: Long): ApiResult<Unit> = safeUnitCall(moshi) { api.purgeReceived(kind, id) }
}
