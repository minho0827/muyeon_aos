package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.data.common.safeUnitCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.support.Faq
import com.muyeon.domain.model.support.Notice
import com.muyeon.domain.model.support.SupportComposeType
import com.muyeon.domain.model.support.SupportInquiry
import com.muyeon.domain.repository.SupportRepository
import com.muyeon.network.api.SupportApi
import com.muyeon.network.dto.support.CreateSupportInquiryBody
import com.squareup.moshi.Moshi

class SupportRepositoryImpl(private val api: SupportApi, private val moshi: Moshi) : SupportRepository {
    override suspend fun notices(): ApiResult<List<Notice>> = safeApiCall(moshi) { api.notices() }.map { l -> l.mapNotNull { it.toDomain() } }

    override suspend fun notice(id: Long): ApiResult<Notice> = safeApiCall(moshi) { api.notice(id) }.map { d ->
        d.toDomain() ?: Notice(id, "", "", false, null)
    }

    override suspend fun faqs(): ApiResult<List<Faq>> = safeApiCall(moshi) { api.faqs(null) }.map { l -> l.mapNotNull { it.toDomain() } }

    override suspend fun faqCategories(): ApiResult<List<String>> =
        safeApiCall(moshi) { api.faqCategories() }.map { l -> l.filter { it.isNotBlank() } }

    override suspend fun myInquiries(): ApiResult<List<SupportInquiry>> =
        safeApiCall(moshi) { api.myInquiries() }.map { l -> l.mapNotNull { it.toDomain() } }

    override suspend fun inquiry(id: Long): ApiResult<SupportInquiry> = safeApiCall(moshi) { api.inquiry(id) }.map { d ->
        d.toDomain() ?: SupportInquiry(id, "", SupportInquiry.RECEIVED, null, null, null)
    }

    override suspend fun deleteInquiry(id: Long): ApiResult<Unit> = safeUnitCall(moshi) { api.deleteInquiry(id) }

    override suspend fun send(type: SupportComposeType, content: String): ApiResult<Unit> =
        safeUnitCall(moshi) { api.createInquiry(CreateSupportInquiryBody(type.serverType, content)) }
}
