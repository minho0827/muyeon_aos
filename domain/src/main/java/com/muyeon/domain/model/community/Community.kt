package com.muyeon.domain.model.community

/**
 * 커뮤니티 — muyeon-backend community.service.ts(listPage·findOne·commentsPage).
 *   익명 글은 폐지됐지만 옛 글은 authorName "익명" · authorId 없음으로 온다.
 */
data class CommunityAuthor(
    val id: Long?,
    val name: String?,
    val image: String?,
    /** 강사면 아바타 누름 → 강사 프로필(웹 openTeacherProfile). */
    val isTeacher: Boolean,
)

/** 목록 한 줄. */
data class CommunityPostSummary(
    val id: Long,
    val category: String?,
    val title: String,
    val image: String?,
    val author: CommunityAuthor,
    val likeCount: Int,
    val commentCount: Int,
    val createdAt: String?,
)

/** 글 상세(댓글 제외 — 댓글은 따로 커서로 받는다). */
data class CommunityPost(
    val summary: CommunityPostSummary,
    val body: String,
    val images: List<String>,
    val viewCount: Int,
    val likedByMe: Boolean,
    val scrappedByMe: Boolean,
    val isAnonymous: Boolean,
    /** 내 글(작성자 id = 내 id). */
    val isMine: Boolean,
)

/** 댓글·답글. [deleted] 면 본문은 서버 문구("삭제된 댓글입니다.")이고 작성자는 비어 있다. */
data class CommunityComment(
    val id: Long,
    val parentId: Long?,
    val author: CommunityAuthor,
    val body: String,
    val likeCount: Int,
    val likedByMe: Boolean,
    val deleted: Boolean,
    val createdAt: String?,
    /** 최상위 댓글만 — 보이는 답글 수. */
    val replyCount: Int,
    val isAnonymous: Boolean,
    val isMine: Boolean,
)

/** 좋아요 토글 결과(서버 { liked, likeCount }). */
data class LikeResult(val liked: Boolean, val likeCount: Int)

/** 카테고리 코드 — 웹 constants/communityCategories.js 와 같은 순서(이름은 앱 strings.xml). */
object CommunityCategory {
    val ALL_CODES = listOf("QUESTION", "CHAT", "ACADEMY_REVIEW", "SHOW_REVIEW", "MEETING", "MARKET")
}
