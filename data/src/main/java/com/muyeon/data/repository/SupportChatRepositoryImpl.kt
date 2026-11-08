package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.data.common.safeUnitCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.model.support.SupportChatAccessInfo
import com.muyeon.domain.model.support.SupportChatMenu
import com.muyeon.domain.model.support.SupportChatMessage
import com.muyeon.domain.model.support.SupportChatNotice
import com.muyeon.domain.model.support.SupportChatRoomInfo
import com.muyeon.domain.model.support.SupportChatSummary
import com.muyeon.domain.model.support.SupportChatView
import com.muyeon.domain.repository.SupportChatRepository
import com.muyeon.network.api.SupportChatApi
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject

class SupportChatRepositoryImpl(private val api: SupportChatApi, private val moshi: Moshi) : SupportChatRepository {

    override suspend fun access(): ApiResult<SupportChatAccessInfo> = safeApiCall(moshi) { api.eligibility() }.map { b ->
        val o = b.obj(); SupportChatAccessInfo(o.optBoolean("eligible"), o.optBoolean("hasRooms"))
    }

    override suspend fun summary(): ApiResult<SupportChatSummary> = safeApiCall(moshi) { api.summary() }.map { b ->
        val o = b.obj(); SupportChatSummary(o.s("delayNoticeText")?.takeIf { o.optBoolean("delayNoticeOn") })
    }

    override suspend fun notices(): ApiResult<List<SupportChatNotice>> = safeApiCall(moshi) { api.notices() }.map { b ->
        b.arr().objects().map { n ->
            SupportChatNotice(n.optLong("id"), n.s("title").orEmpty(), n.s("body"), n.s("imageUrl")?.takeIf { it.startsWith("https://") },
                n.s("buttonUrl"), n.s("buttonLabel"), n.s("createdAt"), n.optBoolean("isRead"))
        }
    }

    override suspend fun readAllNotices(): ApiResult<Unit> = safeUnitCall(moshi) { api.readAllNotices(json(JSONObject())) }

    override suspend fun rooms(): ApiResult<List<SupportChatRoomInfo>> = safeApiCall(moshi) { api.rooms() }.map { b -> b.arr().objects().map(::room) }

    override suspend fun createRoom(): ApiResult<Long> = safeApiCall(moshi) { api.createRoom(json(JSONObject())) }.map { b ->
        b.obj().optJSONObject("room")?.optLong("id") ?: 0L
    }

    override suspend fun messages(roomId: Long, after: Long) = view { api.messages(roomId, after) }
    override suspend fun send(roomId: Long, content: String, after: Long) = view { api.send(roomId, json(JSONObject().put("content", content).put("after", after))) }
    override suspend fun choose(roomId: Long, nodeId: Long?, action: String?, after: Long) = view {
        api.choose(roomId, json(JSONObject().apply { nodeId?.let { put("nodeId", it) }; action?.let { put("action", it) }; put("after", after) }))
    }
    override suspend fun handoff(roomId: Long, after: Long) = view { api.handoff(roomId, json(JSONObject().put("after", after))) }
    override suspend fun close(roomId: Long, after: Long) = view { api.close(roomId, json(JSONObject().put("after", after))) }
    override suspend fun read(roomId: Long): ApiResult<Unit> = safeUnitCall(moshi) { api.read(roomId, json(JSONObject())) }

    private suspend fun view(call: suspend () -> retrofit2.Response<ResponseBody>): ApiResult<SupportChatView> = safeApiCall(moshi) { call() }.map { b ->
        val o = b.obj()
        val m = o.optJSONObject("menu")
        SupportChatView(
            room = o.optJSONObject("room")?.let(::room),
            menu = m?.let {
                SupportChatMenu(
                    it.optJSONArray("nodes").objects().map { n -> n.optLong("id") to n.s("label").orEmpty() },
                    it.optBoolean("canBack"), it.optBoolean("canHome"), it.optBoolean("canHandoff"),
                )
            },
            messages = o.optJSONArray("messages").objects().map { x ->
                SupportChatMessage(x.optLong("id"), x.s("senderType") ?: "BOT", x.s("content").orEmpty(), x.s("createdAt"))
            }.filter { it.id > 0 },
        )
    }

    private companion object {
        val JSON = "application/json".toMediaType()
        fun json(o: JSONObject): RequestBody = o.toString().toRequestBody(JSON)
        fun ResponseBody.obj() = JSONObject(string().ifBlank { "{}" })
        fun ResponseBody.arr(): JSONArray { val r = string().trim(); return if (r.startsWith("[")) JSONArray(r) else JSONArray() }
        fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
        fun JSONObject.s(k: String): String? = if (isNull(k)) null else optString(k).ifBlank { null }
        fun room(o: JSONObject) = SupportChatRoomInfo(
            o.optLong("id"), o.s("title"), o.s("status") ?: "WAITING", o.s("lastMessage"), o.s("lastMessageAt"), o.s("createdAt"), o.optInt("unread"),
        )
    }
}
