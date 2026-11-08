package com.muyeon.domain.model.support

/** 공지사항(운영자 등록 · 노출분만). [pinned] = 상단 고정('중요'). */
data class Notice(val id: Long, val title: String, val content: String, val pinned: Boolean, val createdAt: String?)

/** 자주 묻는 질문. */
data class Faq(val id: Long, val category: String, val question: String, val answer: String)

/** 내 1:1 문의 — [status] RECEIVED·REVIEWING(답변 대기) · RESOLVED(답변 완료). 답변은 완료일 때만 온다. */
data class SupportInquiry(
    val id: Long,
    val content: String,
    val status: String,
    val answer: String?,
    val answeredAt: String?,
    val createdAt: String?,
) {
    val resolved: Boolean get() = status == RESOLVED

    /** 목록 제목 — 첫 줄 36자(웹 SupportInquiries titleOf). 비면 null(화면이 '1:1 문의'). */
    val titleLine: String?
        get() = content.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
            ?.let { if (it.length > 36) it.take(36) + "…" else it }

    companion object {
        const val RECEIVED = "RECEIVED"
        const val RESOLVED = "RESOLVED"
    }
}

/** 보내는 글 종류 — 서버 SupportInquiryType. */
enum class SupportComposeType(val serverType: String) { INQUIRY("INQUIRY"), FEEDBACK("FEEDBACK") }
