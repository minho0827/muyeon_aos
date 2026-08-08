package com.muyeon.app.ui.lesson

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
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * 레슨 개설 위저드 옵션·모델·API — iOS `Wizard/LessonWizardModels.swift` 1:1.
 *  ⚠️ 옵션 값(요일 dayOfWeek, 반복 주수 상한)은 **서버 계약**이다. 임의로 바꾸면 슬롯 생성이 어긋난다.
 */
object LessonOptions {

    val genres = listOf("발레", "한국무용", "현대무용", "실용무용", "바레", "발레핏")
    val levels = listOf("입문", "초급", "중급", "고급", "전공")

    /** 요일 — value = 백엔드 dayOfWeek(0=일). 표시는 월~일 순. */
    val weekdays = listOf(1 to "월", 2 to "화", 3 to "수", 4 to "목", 5 to "금", 6 to "토", 0 to "일")

    /** 06:00~22:30, 30분 간격. */
    val startTimes: List<String> = buildList {
        for (h in 6..22) for (m in listOf(0, 30)) add(String.format(Locale.US, "%02d:%02d", h, m))
    }

    val durations = listOf(50, 60, 80, 90, 100, 120)
    val capacities = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20)
    val prices = listOf(0, 10000, 15000, 20000, 25000, 30000, 40000, 50000, 70000, 100000)

    /**
     * 예약 반복: 4/8/12주 + 매주 반복(=24주 롤링, 매일 크론이 앞으로 민다).
     *  ⚠️ 서버 부하 방지로 큰 weeksAhead 금지 — 24주 상한(슬롯 API @Max(24)와 통일).
     */
    const val WEEKS_RECURRING = 24
    val weeks = listOf(4, 8, 12, WEEKS_RECURRING)

    fun addMinutes(hhmm: String, mins: Int): String {
        val p = hhmm.split(":").mapNotNull { it.toIntOrNull() }
        if (p.size != 2) return hhmm
        val total = p[0] * 60 + p[1] + mins
        return String.format(Locale.US, "%02d:%02d", (total / 60) % 24, total % 60)
    }

    fun dayLabel(d: Int): String = listOf("일", "월", "화", "수", "목", "금", "토")[d.coerceIn(0, 6)]

    fun priceLabel(p: Int): String = if (p == 0) "무료" else "${String.format(Locale.KOREA, "%,d", p)}원"

    fun weeksLabel(w: Int): String = if (w >= WEEKS_RECURRING) "매주 반복" else "${w}주"
}

/** 위저드 입력 스냅샷 — iOS LessonWizardDraft(로컬 임시저장). */
data class LessonWizardDraft(
    var title: String = "",
    var genre: String = "",
    var level: String = "",
    var isExperience: Boolean = false,
    var intro: String = "",
    var images: List<String> = emptyList(),
    var detailType: String = "TEXT",
    var detailImages: List<String> = emptyList(),
    var isOnline: Boolean = false,
    var place: String = "",
    var address: String = "",
    var addressDetail: String = "",
    var region: String = "",
    var regionCode: String = "",
    var lat: Double? = null,
    var lng: Double? = null,
    var days: List<Int> = emptyList(),
    var startTime: String = "19:00",
    var duration: Int = 60,
    var capacity: Int = 8,
    var price: Int = 20000,
    var weeksAhead: Int = 8,
    var phone: String = "",
    var parkingInfo: String = "",
    var valetInfo: String = "",
    var homepage: String = "",
    var notice: String = "",
    var cancelPolicy: String = "",
) {
    /** 서버 payload — 키 이름은 iOS/웹과 동일해야 한다(레슨 개설·수정 공용). */
    fun toPayload(): JSONObject = JSONObject().apply {
        put("title", title)
        put("genre", genre)
        put("level", level)
        put("isExperience", isExperience)
        put("description", intro)
        put("images", JSONArray(images))
        put("detailType", detailType)
        put("detailImages", JSONArray(detailImages))
        put("isOnline", isOnline)
        put("place", place)
        put("address", listOf(address, addressDetail).filter { it.isNotEmpty() }.joinToString(" "))
        put("region", region)
        put("regionCode", regionCode)
        lat?.let { put("lat", it) }
        lng?.let { put("lng", it) }
        // schedule: 요일 × 시작시간(서버가 duration 으로 종료 계산 + weeksAhead 만큼 슬롯 생성)
        put(
            "schedule",
            JSONArray().apply {
                days.forEach { d -> put(JSONObject().put("dayOfWeek", d).put("startTime", startTime)) }
            },
        )
        put("duration", duration)
        put("maxParticipants", capacity)
        put("price", price)
        put("weeksAhead", weeksAhead)
        put("phone", phone)
        put("parkingInfo", parkingInfo)
        put("valetInfo", valetInfo)
        put("homepage", homepage)
        put("notice", notice)
        put("cancelPolicy", cancelPolicy)
    }
}

/** 수정 모드 프리필 — GET /lesson-products/:id 응답(필요 필드만). */
data class LessonProductDetail(
    val title: String?,
    val genre: String?,
    val level: String?,
    val isExperience: Boolean?,
    val description: String?,
    val images: List<String>?,
    val place: String?,
    val address: String?,
    val region: String?,
    val regionCode: String?,
    val lat: Double?,
    val lng: Double?,
    val schedule: List<Pair<Int, String>>?,   // (dayOfWeek, startTime)
    val duration: Int?,
    val maxParticipants: Int?,
    val price: Int?,
    val phone: String?,
    val parkingInfo: String?,
    val valetInfo: String?,
    val homepage: String?,
    val notice: String?,
    val cancelPolicy: String?,
    val calendarId: Int?,
    val detailType: String?,
    val detailImages: List<String>?,
    val purposes: List<String>?,
    val ageGroups: List<String>?,
    val formats: List<String>?,
) {
    /** 프리필 draft 로 변환. */
    fun toDraft() = LessonWizardDraft(
        title = title.orEmpty(), genre = genre.orEmpty(), level = level.orEmpty(),
        isExperience = isExperience == true, intro = description.orEmpty(),
        images = images ?: emptyList(),
        detailType = detailType ?: "TEXT", detailImages = detailImages ?: emptyList(),
        place = place.orEmpty(), address = address.orEmpty(),
        region = region.orEmpty(), regionCode = regionCode.orEmpty(), lat = lat, lng = lng,
        days = schedule?.map { it.first }?.distinct() ?: emptyList(),
        startTime = schedule?.firstOrNull()?.second ?: "19:00",
        duration = duration ?: 60, capacity = maxParticipants ?: 8, price = price ?: 20000,
        phone = phone.orEmpty(), parkingInfo = parkingInfo.orEmpty(), valetInfo = valetInfo.orEmpty(),
        homepage = homepage.orEmpty(), notice = notice.orEmpty(), cancelPolicy = cancelPolicy.orEmpty(),
    )

    companion object {
        fun from(o: JSONObject) = LessonProductDetail(
            title = o.stringOrNull("title"), genre = o.stringOrNull("genre"), level = o.stringOrNull("level"),
            isExperience = o.boolOrNull("isExperience"), description = o.stringOrNull("description"),
            images = o.stringList("images"),
            place = o.stringOrNull("place"), address = o.stringOrNull("address"),
            region = o.stringOrNull("region"), regionCode = o.stringOrNull("regionCode"),
            lat = o.doubleOrNull("lat"), lng = o.doubleOrNull("lng"),
            schedule = o.optJSONArray("schedule")?.map { it.optInt("dayOfWeek") to it.optString("startTime") },
            duration = o.intOrNull("duration"), maxParticipants = o.intOrNull("maxParticipants"),
            price = o.intOrNull("price"), phone = o.stringOrNull("phone"),
            parkingInfo = o.stringOrNull("parkingInfo"), valetInfo = o.stringOrNull("valetInfo"),
            homepage = o.stringOrNull("homepage"), notice = o.stringOrNull("notice"),
            cancelPolicy = o.stringOrNull("cancelPolicy"), calendarId = o.intOrNull("calendarId"),
            detailType = o.stringOrNull("detailType"), detailImages = o.stringList("detailImages"),
            purposes = o.stringList("purposes"), ageGroups = o.stringList("ageGroups"),
            formats = o.stringList("formats"),
        )
    }
}

/** 레슨 상품(개설/수정) API — iOS LessonWizardService. */
class LessonWizardApi(private val token: String?) {

    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    /** 개설 — POST /lesson-products (예약 슬롯 자동 생성). 성공 시 생성 id. */
    suspend fun createProduct(payload: JSONObject): Result<Int> =
        write("/lesson-products", "POST", payload, "레슨 개설에 실패했어요.")

    /** 수정 — PUT /lesson-products/:id. */
    suspend fun updateProduct(id: Int, payload: JSONObject): Result<Int> =
        write("/lesson-products/$id", "PUT", payload, "레슨 수정에 실패했어요.")

    suspend fun getProduct(id: Int): Result<LessonProductDetail> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url("$apiBase/lesson-products/$id")
                .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                .build()
            client.newCall(req).execute().use { res ->
                val text = res.body?.string().orEmpty()
                if (!res.isSuccessful) throw IllegalStateException("레슨 정보를 불러오지 못했어요.")
                LessonProductDetail.from(JSONObject(text))
            }
        }
    }

    /** 멤버십 상세이미지(DETAIL_IMAGE) 이용권 보유 여부. */
    suspend fun hasDetailImage(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url("$apiBase/monetization/entitlements/me")
                .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                .build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@use false
                val arr = JSONArray(res.body?.string().orEmpty().ifBlank { "[]" })
                (0 until arr.length()).any { i ->
                    val o = arr.optJSONObject(i) ?: return@any false
                    val status = o.stringOrNull("status")
                    o.optString("featureType") == "DETAIL_IMAGE" && (status == null || status == "ACTIVE")
                }
            }
        }.getOrDefault(false)
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

    private suspend fun write(path: String, method: String, payload: JSONObject, failMsg: String): Result<Int> =
        withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder().url(apiBase + path)
                    .method(method, payload.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("Content-Type", "application/json")
                    .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                    .build()
                client.newCall(req).execute().use { res ->
                    val text = res.body?.string().orEmpty()
                    if (!res.isSuccessful) {
                        val msg = runCatching { JSONObject(text).optString("message") }.getOrNull()
                        throw IllegalStateException(msg?.ifEmpty { null } ?: failMsg)
                    }
                    JSONObject(text.ifBlank { "{}" }).optInt("id", 0)
                }
            }
        }
}
