package com.muyeon.data.repository

import com.muyeon.data.common.safeApiCall
import com.muyeon.data.common.safeUnitCall
import com.muyeon.domain.common.ApiResult
import com.muyeon.domain.common.CursorPage
import com.muyeon.domain.common.CursorPaging
import com.muyeon.domain.model.dancer.CastingCallOption
import com.muyeon.domain.model.dancer.DancerCard
import com.muyeon.domain.model.dancer.DancerFilter
import com.muyeon.domain.model.dancer.DancerProfile
import com.muyeon.domain.repository.DancerRepository
import com.muyeon.network.api.DancerApi
import com.squareup.moshi.Moshi
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class DancerRepositoryImpl(private val api: DancerApi, private val moshi: Moshi) : DancerRepository {

    override suspend fun page(filter: DancerFilter, cursor: String?): ApiResult<CursorPage<DancerCard>> =
        safeApiCall(moshi) {
            api.page(filter.major, filter.gender, filter.hasVideo.takeIf { it }, filter.hasAward.takeIf { it }, cursor, CursorPaging.DEFAULT_LIMIT)
        }.map { body ->
            val o = JSONObject(body.string().ifBlank { "{}" })
            val items = o.optJSONArray("items").objects().map { d ->
                DancerCard(
                    id = d.optLong("id"), name = d.str("name"), image = d.str("image"),
                    major = d.str("major") ?: d.strings("genres").firstOrNull() ?: d.strings("fields").firstOrNull(),
                    gender = d.str("gender"), height = d.str("heightRange") ?: d.str("height")?.let { unit(it, "cm") },
                    region = d.str("region"), career = d.str("career"),
                    hasVideo = d.optBoolean("hasVideo"), hasAward = d.optBoolean("hasAward"),
                )
            }.filter { it.id > 0 }
            val next = o.str("nextCursor")?.takeIf { o.optBoolean("hasMore", true) }
            CursorPage(items, next)
        }

    override suspend fun detail(id: Long): ApiResult<DancerProfile> = safeApiCall(moshi) { api.detail(id) }.map { body ->
        val d = JSONObject(body.string().ifBlank { "{}" })
        val basic = d.optJSONObject("basic")
        DancerProfile(
            id = id,
            name = d.str("nickname") ?: d.str("name") ?: "",
            images = (listOfNotNull(d.str("image")) + d.strings("images")).distinct(),
            major = d.str("major") ?: d.strings("genres").firstOrNull() ?: d.strings("fields").firstOrNull(),
            gender = d.str("gender"),
            height = d.str("height")?.let { unit(it, "cm") },
            weight = d.str("weight")?.let { unit(it, "kg") },
            region = d.str("region") ?: d.str("activeRegion"),
            oneLiner = d.str("oneLiner"),
            intro = d.str("intro"),
            birth = d.str("birth") ?: basic?.str("birth"),
            education = d.str("education") ?: d.lines("educations", "school", "major", "period"),
            career = d.str("career"),
            companyCareer = d.str("companyCareer"),
            awards = d.str("awards") ?: d.lines("awards", "year", "title"),
            performances = d.str("performances") ?: d.lines("performances", "year", "title", "role"),
            videoUrl = d.str("videoUrl"),
            availableDays = d.strings("availableDays"),
            availableTimeSlots = d.strings("availableTimeSlots"),
            hasVideo = d.optBoolean("hasVideo"),
            self = d.optBoolean("self"),
        )
    }

    override suspend fun offer(id: Long, callId: Long?): ApiResult<Unit> = safeUnitCall(moshi) {
        api.offer(id, JSONObject().apply { callId?.let { put("callId", it) } }.toString().toRequestBody(JSON))
    }

    override suspend fun directRoom(userId: Long): ApiResult<Long> = safeApiCall(moshi) {
        api.directRoom(JSONObject().put("targetUserId", userId).put("targetType", "DANCER").toString().toRequestBody(JSON))
    }.map { body ->
        val o = JSONObject(body.string().ifBlank { "{}" })
        (o.optJSONObject("data")?.optLong("roomId") ?: 0L).takeIf { it > 0 } ?: o.optLong("roomId")
    }

    override suspend fun myOpenCastingCalls(): ApiResult<List<CastingCallOption>> = safeApiCall(moshi) { api.myPostings() }.map { body ->
        val raw = body.string().trim()
        val arr = if (raw.startsWith("[")) JSONArray(raw) else JSONObject(raw.ifBlank { "{}" }).optJSONArray("items")
        arr.objects().filter { it.str("kind") == "CASTING" && !it.optBoolean("closed") && it.str("status") != "CLOSED" }
            .map { CastingCallOption(it.optLong("id"), it.str("title").orEmpty(), it.str("genre"), it.str("region")) }
            .filter { it.id > 0 }
    }

    private companion object {
        val JSON = "application/json".toMediaType()

        fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

        /** 글자·숫자 칸 → 다듬은 글자(비면 null). */
        fun JSONObject.str(key: String): String? {
            if (!has(key) || isNull(key)) return null
            return when (val v = opt(key)) {
                is String -> v.trim().takeIf { it.isNotEmpty() }
                is Number -> v.toString().removeSuffix(".0")
                else -> null
            }
        }

        fun JSONObject.strings(key: String): List<String> {
            val a = optJSONArray(key) ?: return emptyList()
            return (0 until a.length()).mapNotNull { a.opt(it)?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() && s != "null" } }
        }

        /** [{year,title,…}] 배열 → "2024 · 제목" 줄들(웹 performanceText·awardText·educationText). */
        fun JSONObject.lines(key: String, vararg parts: String): String? {
            val a = optJSONArray(key) ?: return null
            return a.objects().map { o -> parts.mapNotNull { o.str(it) }.joinToString(" · ") }.filter { it.isNotBlank() }
                .joinToString("\n").takeIf { it.isNotBlank() }
        }

        fun unit(v: String, u: String) = if (v.lowercase().contains(u)) v else "$v$u"
    }
}
