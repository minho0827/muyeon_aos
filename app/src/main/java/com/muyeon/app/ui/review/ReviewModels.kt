package com.muyeon.app.ui.review

import com.muyeon.app.BuildConfig
import com.muyeon.app.ui.quote.boolOrNull
import com.muyeon.app.ui.quote.doubleOrNull
import com.muyeon.app.ui.quote.intOrNull
import com.muyeon.app.ui.quote.map
import com.muyeon.app.ui.quote.stringList
import com.muyeon.app.ui.quote.stringOrNull
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
import java.net.URLEncoder

/**
 * 리뷰 옵션·모델·API — iOS `ReviewModels.swift` 1:1.
 *  값 계약 정본: muyeon-backend/src/modules/reviews/review-options.ts
 *  3축 별점(강의력/소통/시설) + 레슨형태·목적·수강기간·태그·재수강·사진.
 */
object ReviewOptions {

    /** 3축 별점. */
    val ratingAxes = listOf("teaching" to "강의력", "communication" to "소통·친절", "facility" to "시설·환경")

    /** 별점 라벨 — 웹 MSG_LIST 동일. */
    val starLabels = listOf("아쉬워요", "별로에요", "보통이에요", "만족해요", "최고예요")

    val lessonTypes = listOf("PRIVATE" to "개인 레슨", "GROUP" to "그룹 레슨", "ACADEMY" to "학원 정규반")

    val purposes = listOf(
        "HOBBY" to "취미·재미", "POSTURE" to "체형교정·자세", "DIET" to "다이어트·체력",
        "EXAM" to "입시·전공 준비", "COMPETITION" to "대회·공연 준비", "REHAB" to "재활·통증 완화",
    )

    val durations = listOf("LT1M" to "1개월 미만", "M1_3" to "1~3개월", "M3_6" to "3~6개월", "GT6M" to "6개월 이상")

    /** 이런 점이 좋았어요. */
    val tags = listOf(
        "KIND" to "친절해요", "IMPROVE" to "실력이 늘어요", "BASIC" to "기초부터 꼼꼼해요",
        "COMFY" to "분위기가 편해요", "PUNCTUAL" to "시간 약속을 잘 지켜요",
        "CUSTOM" to "눈높이 맞춤 지도", "PASSION" to "열정적이에요", "CLEAN" to "공간이 깔끔해요",
    )

    fun lessonTypeLabel(v: String?) = lessonTypes.firstOrNull { it.first == v }?.second ?: ""
    fun purposeLabel(v: String?) = purposes.firstOrNull { it.first == v }?.second ?: ""
    fun durationLabel(v: String?) = durations.firstOrNull { it.first == v }?.second ?: ""
    fun tagLabel(v: String) = tags.firstOrNull { it.first == v }?.second ?: v
    fun axisLabel(v: String) = ratingAxes.firstOrNull { it.first == v }?.second ?: v
}

/** GET /teachers/:id/reviews 응답. */
data class ReviewList(
    val avg: Double?,
    val count: Int?,
    val sort: String?,
    val distribution: Map<String, Int>,      // { "5": n … "1": n }
    val lessonTypeCounts: Map<String, Int>,
    val purposeCounts: Map<String, Int>,
    val axisAverages: Map<String, Double>,   // { teaching, communication, facility }
    val tagCounts: Map<String, Int>,
    val items: List<Item>,
    val canWrite: Boolean?,
) {
    data class Item(
        val id: Int,
        val rating: Int?,
        val ratings: Map<String, Int>,
        val revisit: Int?,
        val content: String?,
        val reviewerName: String?,
        val mine: Boolean?,
        val lessonType: String?,
        val purpose: String?,
        val durationBucket: String?,
        val tags: List<String>?,
        val images: List<String>?,
        val helpfulCount: Int?,
        val helpfulByMe: Boolean?,
        val createdAt: String?,
    ) {
        companion object {
            fun from(o: JSONObject) = Item(
                id = o.optInt("id"), rating = o.intOrNull("rating"),
                ratings = o.optJSONObject("ratings").toIntMap(),
                revisit = o.intOrNull("revisit"), content = o.stringOrNull("content"),
                reviewerName = o.stringOrNull("reviewerName"), mine = o.boolOrNull("mine"),
                lessonType = o.stringOrNull("lessonType"), purpose = o.stringOrNull("purpose"),
                durationBucket = o.stringOrNull("durationBucket"),
                tags = o.stringList("tags"), images = o.stringList("images"),
                helpfulCount = o.intOrNull("helpfulCount"), helpfulByMe = o.boolOrNull("helpfulByMe"),
                createdAt = o.stringOrNull("createdAt"),
            )
        }
    }

    companion object {
        fun from(o: JSONObject) = ReviewList(
            avg = o.doubleOrNull("avg"), count = o.intOrNull("count"), sort = o.stringOrNull("sort"),
            distribution = o.optJSONObject("distribution").toIntMap(),
            lessonTypeCounts = o.optJSONObject("lessonTypeCounts").toIntMap(),
            purposeCounts = o.optJSONObject("purposeCounts").toIntMap(),
            axisAverages = o.optJSONObject("axisAverages").toDoubleMap(),
            tagCounts = o.optJSONObject("tagCounts").toIntMap(),
            items = o.optJSONArray("items")?.map { Item.from(it) } ?: emptyList(),
            canWrite = o.boolOrNull("canWrite"),
        )
    }
}

private fun JSONObject?.toIntMap(): Map<String, Int> {
    if (this == null) return emptyMap()
    return keys().asSequence().associateWith { optInt(it) }
}

private fun JSONObject?.toDoubleMap(): Map<String, Double> {
    if (this == null) return emptyMap()
    return keys().asSequence().associateWith { optDouble(it, 0.0) }
}

/** 작성 페이로드 — 서버 create dto 와 키 일치. */
data class ReviewSubmit(
    val teacherId: Int,
    val ratings: Map<String, Int>,   // { teaching, communication, facility }
    val revisit: Boolean?,
    val content: String?,
    val lessonType: String?,
    val purpose: String?,
    val durationBucket: String?,
    val tags: List<String>,
    val images: List<String>,
) {
    fun toJson(): JSONObject = JSONObject()
        .put("teacherId", teacherId)
        .put("ratings", JSONObject().apply { ratings.forEach { (k, v) -> put(k, v) } })
        .putOpt("revisit", revisit)
        .putOpt("content", content)
        .putOpt("lessonType", lessonType)
        .putOpt("purpose", purpose)
        .putOpt("durationBucket", durationBucket)
        .put("tags", JSONArray(tags))
        .put("images", JSONArray(images))
}

class ReviewApi(private val token: String?) {

    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    /** 강사 후기 목록 — 정렬(sort)·필터(lessonType/purpose/photoOnly). */
    suspend fun list(
        teacherId: Int,
        sort: String = "best",
        lessonType: String? = null,
        purpose: String? = null,
        photoOnly: Boolean = false,
    ): Result<ReviewList> {
        var q = "sort=$sort"
        if (!lessonType.isNullOrEmpty()) q += "&lessonType=$lessonType"
        if (!purpose.isNullOrEmpty()) q += "&purpose=$purpose"
        if (photoOnly) q += "&photoOnly=1"
        return call("/teachers/$teacherId/reviews?$q").map { ReviewList.from(JSONObject(it)) }
    }

    /** 리뷰 작성/수정. */
    suspend fun submit(payload: ReviewSubmit): Result<Unit> = call("/reviews", "POST", payload.toJson()).map { }

    /** 도움돼요 토글 → { liked, count }. */
    suspend fun toggleHelpful(reviewId: Int): Result<Pair<Boolean, Int>> =
        call("/reviews/$reviewId/helpful", "POST").map {
            val o = JSONObject(it.ifBlank { "{}" })
            o.optBoolean("liked", false) to o.optInt("count", 0)
        }

    suspend fun uploadImage(bytes: ByteArray): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", "image.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
                .build()
            val req = Request.Builder().url("$apiBase/uploads/image").post(body)
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
                    if (!res.isSuccessful) {
                        val msg = runCatching { JSONObject(text).optString("message") }.getOrNull()
                        throw IllegalStateException(msg?.ifEmpty { null } ?: "리뷰 등록에 실패했어요.")
                    }
                    text
                }
            }
        }

    private companion object { val JSON = "application/json".toMediaType() }
}
