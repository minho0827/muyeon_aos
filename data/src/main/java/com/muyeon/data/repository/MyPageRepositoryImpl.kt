package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.domain.model.my.MyMembership
import com.muyeon.domain.model.my.PendingPostings
import com.muyeon.domain.repository.MyPageRepository
import com.muyeon.network.api.MyApi
import com.squareup.moshi.Moshi

/** 부가 정보라 실패는 조용히 기본값(웹 MyHome 도 catch 후 무시한다). */
class MyPageRepositoryImpl(private val api: MyApi, private val moshi: Moshi) : MyPageRepository {
    override suspend fun myName(): String? = safeApiCall(moshi) { api.me() }.getOrNull()?.name?.takeIf { it.isNotBlank() }
    override suspend fun notiUnread(): Int = safeApiCall(moshi) { api.notiUnread() }.getOrNull()?.string()?.trim()?.toDoubleOrNull()?.toInt() ?: 0
    override suspend fun quoteUnread(): Int = safeApiCall(moshi) { api.quoteUnread() }.getOrNull()?.count ?: 0
    override suspend fun membership(): MyMembership? = safeApiCall(moshi) { api.membership() }.getOrNull()?.toDomain()
    override suspend fun pendingPostings(): PendingPostings? = safeApiCall(moshi) { api.pendingPostings() }.getOrNull()?.toDomain()
    override suspend fun castingMaterialAllowed(): Boolean = safeApiCall(moshi) { api.castingAccess() }.getOrNull()?.allowed == true
    override suspend fun supportChatEligible(): Boolean = safeApiCall(moshi) { api.supportChat() }.getOrNull()?.eligible == true
}
