package com.muyeon.network.dto.community

import com.muyeon.domain.model.community.CommunityAuthor
import com.muyeon.domain.model.community.CommunityComment
import com.muyeon.domain.model.community.CommunityPost
import com.muyeon.domain.model.community.CommunityPostSummary
import com.muyeon.domain.model.community.LikeResult
import com.squareup.moshi.JsonClass

/**
 * 커뮤니티 글 — community.service.ts listPage/findOne(authorView 가 붙는다). ⚠️ 서버 null 칸은 전부 `?`.
 *   목록은 body 를 싣지 않는다. 사진은 images[0](웹 normImg: image || images[0]).
 */
@JsonClass(generateAdapter = true)
data class CommunityPostDto(
    val id: Long? = null,
    val category: String? = null,
    val title: String? = null,
    val body: String? = null,
    val images: List<String>? = null,
    val image: String? = null,
    val imageUrl: String? = null,
    val authorId: Long? = null,
    val authorName: String? = null,
    val authorImage: String? = null,
    val authorIsTeacher: Boolean? = null,
    val isAnonymous: Boolean? = null,
    val viewCount: Int? = null,
    val likeCount: Int? = null,
    val commentCount: Int? = null,
    val likedByMe: Boolean? = null,
    val scrappedByMe: Boolean? = null,
    val createdAt: String? = null,
) {
    private fun author() = CommunityAuthor(authorId, authorName?.takeIf { it.isNotBlank() }, authorImage?.takeIf { it.isNotBlank() }, authorIsTeacher == true)

    fun toSummary(): CommunityPostSummary? {
        val pid = id ?: return null
        return CommunityPostSummary(
            id = pid,
            category = category,
            title = title.orEmpty(),
            image = (image ?: images?.firstOrNull { it.isNotBlank() } ?: imageUrl)?.takeIf { it.isNotBlank() },
            author = author(),
            likeCount = likeCount ?: 0,
            commentCount = commentCount ?: 0,
            createdAt = createdAt,
        )
    }

    fun toPost(myUserId: Long?): CommunityPost? {
        val s = toSummary() ?: return null
        return CommunityPost(
            summary = s,
            body = body.orEmpty(),
            images = images.orEmpty().filter { it.isNotBlank() },
            viewCount = viewCount ?: 0,
            likedByMe = likedByMe == true,
            scrappedByMe = scrappedByMe == true,
            isAnonymous = isAnonymous == true,
            isMine = myUserId != null && authorId == myUserId,
        )
    }
}

@JsonClass(generateAdapter = true)
data class CommunityCommentDto(
    val id: Long? = null,
    val parentId: Long? = null,
    val authorId: Long? = null,
    val authorName: String? = null,
    val authorImage: String? = null,
    val authorIsTeacher: Boolean? = null,
    val isAnonymous: Boolean? = null,
    val body: String? = null,
    val likeCount: Int? = null,
    val likedByMe: Boolean? = null,
    val deleted: Boolean? = null,
    val createdAt: String? = null,
    val replyCount: Int? = null,
) {
    fun toDomain(myUserId: Long?): CommunityComment? {
        val cid = id ?: return null
        return CommunityComment(
            id = cid,
            parentId = parentId,
            author = CommunityAuthor(authorId, authorName?.takeIf { it.isNotBlank() }, authorImage?.takeIf { it.isNotBlank() }, authorIsTeacher == true),
            body = body.orEmpty(),
            likeCount = likeCount ?: 0,
            likedByMe = likedByMe == true,
            deleted = deleted == true,
            createdAt = createdAt,
            replyCount = replyCount ?: 0,
            isAnonymous = isAnonymous == true,
            isMine = myUserId != null && authorId == myUserId,
        )
    }
}

@JsonClass(generateAdapter = true)
data class LikeResultDto(val liked: Boolean? = null, val likeCount: Int? = null) {
    fun toDomain() = LikeResult(liked == true, likeCount ?: 0)
}

@JsonClass(generateAdapter = true)
data class CommentBody(val body: String, val parentId: Long? = null)

@JsonClass(generateAdapter = true)
data class CommentUpdateBody(val body: String)
