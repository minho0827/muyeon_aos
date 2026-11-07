package com.muyeon.network.api

import com.muyeon.network.dto.common.CursorPageDto
import com.muyeon.network.dto.community.CommentBody
import com.muyeon.network.dto.community.CommentUpdateBody
import com.muyeon.network.dto.community.CommunityCommentDto
import com.muyeon.network.dto.community.CommunityPostDto
import com.muyeon.network.dto.community.LikeResultDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** 출처: community.controller.ts. */
interface CommunityApi {
    @GET(ApiEndpoints.Community.POSTS_PAGE)
    suspend fun postsPage(
        @Query("category") category: String?,
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int,
    ): Response<CursorPageDto<CommunityPostDto>>

    /** comments=0 — 댓글은 따로 커서로 받는다(웹 callGetPostSummary). */
    @GET(ApiEndpoints.Community.POST)
    suspend fun post(@Path("id") id: Long, @Query("comments") comments: Int = 0): Response<CommunityPostDto>

    @DELETE(ApiEndpoints.Community.POST)
    suspend fun deletePost(@Path("id") id: Long): Response<ResponseBody>

    @POST(ApiEndpoints.Community.POST_LIKE)
    suspend fun likePost(@Path("id") id: Long): Response<LikeResultDto>

    @GET(ApiEndpoints.Community.COMMENTS_PAGE)
    suspend fun comments(@Path("id") postId: Long, @Query("cursor") cursor: String?, @Query("limit") limit: Int): Response<CursorPageDto<CommunityCommentDto>>

    @GET(ApiEndpoints.Community.REPLIES_PAGE)
    suspend fun replies(
        @Path("id") postId: Long,
        @Path("parentId") parentId: Long,
        @Query("cursor") cursor: String?,
        @Query("limit") limit: Int,
    ): Response<CursorPageDto<CommunityCommentDto>>

    @POST(ApiEndpoints.Community.COMMENTS)
    suspend fun addComment(@Path("id") postId: Long, @Body body: CommentBody): Response<ResponseBody>

    @PATCH(ApiEndpoints.Community.COMMENT)
    suspend fun updateComment(@Path("id") postId: Long, @Path("commentId") commentId: Long, @Body body: CommentUpdateBody): Response<ResponseBody>

    @DELETE(ApiEndpoints.Community.COMMENT)
    suspend fun deleteComment(@Path("id") postId: Long, @Path("commentId") commentId: Long): Response<ResponseBody>

    @POST(ApiEndpoints.Community.COMMENT_LIKE)
    suspend fun likeComment(@Path("id") postId: Long, @Path("commentId") commentId: Long): Response<LikeResultDto>
}
