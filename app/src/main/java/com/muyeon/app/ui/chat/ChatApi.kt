package com.muyeon.app.ui.chat

import com.muyeon.app.BuildConfig
import com.muyeon.app.ui.quote.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * 채팅 REST — iOS `ChatService.swift` 1:1. nginx `/api` 경유.
 *  ui/quote 의 QuoteApi 와 동일 패턴(OkHttp + org.json + Result).
 */
class ChatApi(private val token: String?) {

    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    /** GET /chat/rooms */
    suspend fun getRooms(): Result<List<ChatRoomSummary>> =
        call("/chat/rooms").map { JSONArray(it.ifBlank { "[]" }).map(ChatRoomSummary::from) }

    /** GET /chat/rooms/:id */
    suspend fun getRoomDetail(roomId: Int): Result<ChatRoomDetail> =
        call("/chat/rooms/$roomId").map { ChatRoomDetail.from(JSONObject(it)) }

    /** GET /chat/rooms/:id/messages?page=&limit= */
    suspend fun getMessages(roomId: Int, page: Int = 1, limit: Int = 50): Result<ChatMessagesResponse> =
        call("/chat/rooms/$roomId/messages?page=$page&limit=$limit").map { ChatMessagesResponse.from(JSONObject(it)) }

    /** DELETE /chat/rooms/:id/leave — 내 참여기록 삭제(방 나가기). */
    suspend fun leaveRoom(roomId: Int): Result<Unit> = call("/chat/rooms/$roomId/leave", "DELETE").map { }

    /** PATCH /chat/rooms/:id/mute { muted } */
    suspend fun setRoomMute(roomId: Int, muted: Boolean): Result<Unit> =
        call("/chat/rooms/$roomId/mute", "PATCH", JSONObject().put("muted", muted)).map { }

    /** POST /chat/rooms/:id/messages/:mid/reactions { emoji } — 토글. */
    suspend fun toggleReaction(roomId: Int, messageId: Int, emoji: String): Result<Unit> =
        call("/chat/rooms/$roomId/messages/$messageId/reactions", "POST", JSONObject().put("emoji", emoji)).map { }

    /** GET /chat/rooms/:id/quick-replies — 상대(강사)의 빠른 답변 칩. */
    suspend fun getQuickReplies(roomId: Int): Result<List<ChatQuickReply>> =
        call("/chat/rooms/$roomId/quick-replies").map { JSONArray(it.ifBlank { "[]" }).map(ChatQuickReply::from) }

    /** POST /quotes/lesson-complete { memberId } — 강사: 레슨 완료 확인(회원 리뷰 가능해짐). */
    suspend fun confirmLesson(memberId: Int): Result<Unit> =
        call("/quotes/lesson-complete", "POST", JSONObject().put("memberId", memberId)).map { }

    /** POST /uploads/image (field "file") → { url } */
    suspend fun uploadImage(bytes: ByteArray, mime: String = "image/jpeg"): Result<String> =
        upload("/uploads/image", bytes, "image.jpg", mime)

    /** POST /uploads/video (field "file") → { url } */
    suspend fun uploadVideo(bytes: ByteArray, ext: String = "mp4", mime: String = "video/mp4"): Result<String> =
        upload("/uploads/video", bytes, "video.$ext", mime)

    private suspend fun upload(path: String, bytes: ByteArray, filename: String, mime: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", filename, bytes.toRequestBody(mime.toMediaType()))
                    .build()
                val req = Request.Builder().url(apiBase + path).post(body)
                    .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                    .build()
                client.newCall(req).execute().use { res ->
                    val text = res.body?.string().orEmpty()
                    if (!res.isSuccessful) throw IllegalStateException("업로드 실패(${res.code})")
                    JSONObject(text).optString("url")
                }
            }
        }

    private suspend fun call(path: String, method: String = "GET", body: JSONObject? = null): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload: RequestBody? = when {
                    body != null -> body.toString().toRequestBody(JSON)
                    method != "GET" && method != "DELETE" -> "".toRequestBody(JSON)
                    else -> null
                }
                val req = Request.Builder().url(apiBase + path).method(method, payload)
                    .addHeader("Content-Type", "application/json")
                    .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                    .build()
                client.newCall(req).execute().use { res ->
                    val text = res.body?.string().orEmpty()
                    if (!res.isSuccessful) throw IllegalStateException(serverMessage(text) ?: "요청에 실패했어요.")
                    text
                }
            }
        }

    private fun serverMessage(text: String): String? = runCatching {
        val o = JSONObject(text)
        o.optJSONArray("message")
            ?.let { arr -> (0 until arr.length()).joinToString("\n") { arr.optString(it) } }?.ifEmpty { null }
            ?: o.optString("message").ifEmpty { null }
    }.getOrNull()

    private companion object { val JSON = "application/json".toMediaType() }
}
