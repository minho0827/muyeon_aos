package com.muyeon.domain.repository

import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.model.community.CommunityComment
import com.muyeon.domain.model.community.CommunityPost
import com.muyeon.domain.model.community.CommunityPostSummary
import com.muyeon.domain.model.community.LikeResult

interface CommunityRepository {
    /** 최신순 목록. @param category null = 전체 */
    suspend fun postsPage(category: String?, cursor: String?): ApiResult<CursorPage<CommunityPostSummary>>
    suspend fun post(id: Long): ApiResult<CommunityPost>
    suspend fun deletePost(id: Long): ApiResult<Unit>
    suspend fun likePost(id: Long): ApiResult<LikeResult>

    /** @param parentId null = 최상위 댓글(좋아요 많은 순), 있으면 그 댓글의 답글(작성 순). */
    suspend fun comments(postId: Long, parentId: Long?, cursor: String?): ApiResult<CursorPage<CommunityComment>>
    suspend fun addComment(postId: Long, body: String, parentId: Long?): ApiResult<Unit>
    suspend fun updateComment(postId: Long, commentId: Long, body: String): ApiResult<Unit>
    suspend fun deleteComment(postId: Long, commentId: Long): ApiResult<Unit>
    suspend fun likeComment(postId: Long, commentId: Long): ApiResult<LikeResult>
}
