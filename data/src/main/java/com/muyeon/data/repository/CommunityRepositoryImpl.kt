package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.data.common.safeUnitCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.common.CursorPaging
import com.muyeon.domain.common.ErrorKind
import com.muyeon.domain.common.TokenStore
import com.muyeon.domain.common.UserFacingError
import com.muyeon.domain.model.community.CommunityComment
import com.muyeon.domain.model.community.CommunityPost
import com.muyeon.domain.model.community.CommunityPostSummary
import com.muyeon.domain.model.community.LikeResult
import com.muyeon.domain.repository.CommunityRepository
import com.muyeon.network.api.CommunityApi
import com.muyeon.network.dto.community.CommentBody
import com.muyeon.network.dto.community.CommentUpdateBody
import com.squareup.moshi.Moshi

class CommunityRepositoryImpl(
    private val api: CommunityApi,
    private val moshi: Moshi,
    private val tokens: TokenStore,
) : CommunityRepository {
    override suspend fun postsPage(category: String?, cursor: String?): ApiResult<CursorPage<CommunityPostSummary>> =
        safeApiCall(moshi) { api.postsPage(category, cursor, CursorPaging.DEFAULT_LIMIT) }
            .map { CursorPage(it.items.orEmpty().mapNotNull { d -> d.toSummary() }, it.next) }

    override suspend fun post(id: Long): ApiResult<CommunityPost> =
        when (val r = safeApiCall(moshi) { api.post(id) }) {
            is ApiResult.Error -> r
            is ApiResult.Success -> r.data.toPost(tokens.myUserId())?.let { ApiResult.Success(it) }
                ?: ApiResult.Error(IllegalStateException("no id"), UserFacingError.server, kind = ErrorKind.SERVER)
        }

    override suspend fun deletePost(id: Long): ApiResult<Unit> = safeUnitCall(moshi) { api.deletePost(id) }

    override suspend fun likePost(id: Long): ApiResult<LikeResult> = safeApiCall(moshi) { api.likePost(id) }.map { it.toDomain() }

    override suspend fun comments(postId: Long, parentId: Long?, cursor: String?): ApiResult<CursorPage<CommunityComment>> {
        val me = tokens.myUserId()
        return safeApiCall(moshi) {
            if (parentId == null) api.comments(postId, cursor, CursorPaging.DEFAULT_LIMIT)
            else api.replies(postId, parentId, cursor, CursorPaging.DEFAULT_LIMIT)
        }.map { CursorPage(it.items.orEmpty().mapNotNull { d -> d.toDomain(me) }, it.next) }
    }

    override suspend fun addComment(postId: Long, body: String, parentId: Long?): ApiResult<Unit> =
        safeUnitCall(moshi) { api.addComment(postId, CommentBody(body, parentId)) }

    override suspend fun updateComment(postId: Long, commentId: Long, body: String): ApiResult<Unit> =
        safeUnitCall(moshi) { api.updateComment(postId, commentId, CommentUpdateBody(body)) }

    override suspend fun deleteComment(postId: Long, commentId: Long): ApiResult<Unit> =
        safeUnitCall(moshi) { api.deleteComment(postId, commentId) }

    override suspend fun likeComment(postId: Long, commentId: Long): ApiResult<LikeResult> =
        safeApiCall(moshi) { api.likeComment(postId, commentId) }.map { it.toDomain() }
}
