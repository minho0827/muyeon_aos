package com.muyeon.network.dto.support

import com.muyeon.domain.model.support.Faq
import com.muyeon.domain.model.support.Notice
import com.muyeon.domain.model.support.SupportInquiry
import com.squareup.moshi.JsonClass

/** content notice.entity.ts */
@JsonClass(generateAdapter = true)
data class NoticeDto(
    val id: Long? = null, val title: String? = null, val content: String? = null,
    val isPinned: Boolean? = null, val createdAt: String? = null,
) {
    fun toDomain() = id?.let { Notice(it, title.orEmpty(), content.orEmpty(), isPinned == true, createdAt) }
}

/** content faq.entity.ts */
@JsonClass(generateAdapter = true)
data class FaqDto(val id: Long? = null, val category: String? = null, val question: String? = null, val answer: String? = null) {
    fun toDomain() = id?.let { Faq(it, category.orEmpty(), question.orEmpty(), answer.orEmpty()) }
}

/** support-inquiries.service.ts toUserView */
@JsonClass(generateAdapter = true)
data class SupportInquiryDto(
    val id: Long? = null, val type: String? = null, val content: String? = null, val status: String? = null,
    val answer: String? = null, val answeredAt: String? = null, val createdAt: String? = null,
) {
    fun toDomain() = id?.let {
        SupportInquiry(it, content.orEmpty(), status ?: SupportInquiry.RECEIVED, answer?.takeIf { a -> a.isNotBlank() }, answeredAt, createdAt)
    }
}

/** POST /support-inquiries — CreateSupportInquiryDto. */
@JsonClass(generateAdapter = true)
data class CreateSupportInquiryBody(val type: String, val content: String)
