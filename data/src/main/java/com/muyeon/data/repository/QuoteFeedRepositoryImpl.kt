package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.quotefeed.QuoteFeedItem
import com.muyeon.domain.model.quotefeed.QuoteFeedPage
import com.muyeon.domain.repository.QuoteFeedRepository
import com.muyeon.network.api.QuoteFeedApi
import com.squareup.moshi.Moshi

/** ★ 2026-11-08 '레슨을 찾는 회원들' — 웹 callGetHomeQuotes · callGetAvailableQuotes 와 같은 호출. */
class QuoteFeedRepositoryImpl(private val api: QuoteFeedApi, private val moshi: Moshi) : QuoteFeedRepository {
    override suspend fun home(): ApiResult<List<QuoteFeedItem>> =
        safeApiCall(moshi) { api.home() }.map { list -> list.mapNotNull { it.toDomain() } }

    override suspend fun available(categoryId: String?, page: Int): ApiResult<QuoteFeedPage> =
        safeApiCall(moshi) { api.available(categoryId, page) }.map { it.toDomain(page) }

    override suspend fun homePage(categoryId: String?, cursor: String?, limit: Int): ApiResult<QuoteFeedPage> =
        safeApiCall(moshi) { api.homePage(categoryId, cursor, limit) }.map { it.toDomain() }
}
