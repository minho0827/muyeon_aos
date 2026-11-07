package com.muyeon.domain.usecase.community

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.model.community.CommunityComment
import com.muyeon.domain.model.community.CommunityPost
import com.muyeon.domain.model.community.CommunityPostSummary
import com.muyeon.domain.model.community.LikeResult
import com.muyeon.domain.repository.CommunityRepository

class GetCommunityPostsUseCase(private val repo: CommunityRepository) {
    suspend operator fun invoke(category: String?, cursor: String?): ApiResult<CursorPage<CommunityPostSummary>> = repo.postsPage(category, cursor)
}

class GetCommunityPostUseCase(private val repo: CommunityRepository) {
    suspend operator fun invoke(id: Long): ApiResult<CommunityPost> = repo.post(id)
}

class GetCommunityCommentsUseCase(private val repo: CommunityRepository) {
    suspend operator fun invoke(postId: Long, parentId: Long?, cursor: String?): ApiResult<CursorPage<CommunityComment>> =
        repo.comments(postId, parentId, cursor)
}

/** 댓글·답글 쓰기·고치기·지우기 — 빈 글은 보내지 않는다(웹과 같다). */
class EditCommunityCommentUseCase(private val repo: CommunityRepository) {
    suspend fun add(postId: Long, body: String, parentId: Long?): ApiResult<Unit>? =
        body.trim().takeIf { it.isNotEmpty() }?.let { repo.addComment(postId, it, parentId) }

    suspend fun update(postId: Long, commentId: Long, body: String): ApiResult<Unit>? =
        body.trim().takeIf { it.isNotEmpty() }?.let { repo.updateComment(postId, commentId, it) }

    suspend fun delete(postId: Long, commentId: Long): ApiResult<Unit> = repo.deleteComment(postId, commentId)
}

class LikeCommunityUseCase(private val repo: CommunityRepository) {
    suspend fun post(id: Long): ApiResult<LikeResult> = repo.likePost(id)
    suspend fun comment(postId: Long, commentId: Long): ApiResult<LikeResult> = repo.likeComment(postId, commentId)
}

class DeleteCommunityPostUseCase(private val repo: CommunityRepository) {
    suspend operator fun invoke(id: Long): ApiResult<Unit> = repo.deletePost(id)
}
