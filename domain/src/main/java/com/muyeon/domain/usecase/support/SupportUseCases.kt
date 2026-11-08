package com.muyeon.domain.usecase.support

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.support.Faq
import com.muyeon.domain.model.support.Notice
import com.muyeon.domain.model.support.SupportComposeType
import com.muyeon.domain.model.support.SupportInquiry
import com.muyeon.domain.repository.SupportRepository

/** 공지 목록·상세(웹 Notice·NoticeDetail). */
class GetNoticesUseCase(private val repo: SupportRepository) {
    suspend operator fun invoke(): ApiResult<List<Notice>> = repo.notices()
    suspend fun detail(id: Long): ApiResult<Notice> = repo.notice(id)
}

/** FAQ 전체 + 분류 탭(웹 faq.js — 분류는 받아 온 목록 안에서 거른다). 분류 실패는 빈 탭으로(전체만). */
class GetFaqsUseCase(private val repo: SupportRepository) {
    suspend operator fun invoke(): ApiResult<Pair<List<String>, List<Faq>>> {
        val cats = repo.faqCategories().getOrNull().orEmpty()
        return repo.faqs().map { faqs -> (cats.ifEmpty { faqs.map { it.category }.distinct() }) to faqs }
    }
}

/** 내 1:1 문의 목록·상세·삭제. */
class MyInquiriesUseCase(private val repo: SupportRepository) {
    suspend fun list(): ApiResult<List<SupportInquiry>> = repo.myInquiries()
    suspend fun detail(id: Long): ApiResult<SupportInquiry> = repo.inquiry(id)
    suspend fun delete(id: Long): ApiResult<Unit> = repo.deleteInquiry(id)
}

/** 1:1 문의·의견 보내기 — 앞뒤 공백을 빼고, 비었으면 보내지 않는다(화면 필수값 확인 뒤). */
class SendSupportUseCase(private val repo: SupportRepository) {
    suspend operator fun invoke(type: SupportComposeType, content: String): ApiResult<Unit> {
        val text = content.trim()
        if (text.isEmpty()) return ApiResult.Error(IllegalArgumentException("empty"), "내용을 입력해 주세요.")
        return repo.send(type, text.take(MAX))
    }

    companion object { const val MAX = 1000 }
}
