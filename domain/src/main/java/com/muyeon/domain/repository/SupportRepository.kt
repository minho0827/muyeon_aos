package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.support.Faq
import com.muyeon.domain.model.support.Notice
import com.muyeon.domain.model.support.SupportComposeType
import com.muyeon.domain.model.support.SupportInquiry

/** 고객센터 — 공지·FAQ(공개) · 1:1 문의·의견(로그인). */
interface SupportRepository {
    suspend fun notices(): ApiResult<List<Notice>>
    suspend fun notice(id: Long): ApiResult<Notice>
    suspend fun faqs(): ApiResult<List<Faq>>
    suspend fun faqCategories(): ApiResult<List<String>>
    suspend fun myInquiries(): ApiResult<List<SupportInquiry>>
    suspend fun inquiry(id: Long): ApiResult<SupportInquiry>
    suspend fun deleteInquiry(id: Long): ApiResult<Unit>
    suspend fun send(type: SupportComposeType, content: String): ApiResult<Unit>
}
