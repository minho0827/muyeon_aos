package com.muyeon.app.ui.lesson

import com.muyeon.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * '강사 콘텐츠 둘러보기' — 레슨상품(lesson_products)을 카드/상세로 표시.
 *  iOS `LessonContentModels.swift` / `LessonContentService.swift` 1:1.
 *  백엔드 GET /lesson-products(browse=1&feed=1), /:id, /:id/similar.
 */

/** 둘러보기 필터 상태. */
data class LessonBrowseFilter(
    val genre: String? = null,        // 한글 장르 — null=전체
    val sidoName: String? = null,     // 지역(시/도) 표시명
    val sidoCode: String? = null,     // 지역 코드(2자리, regionCode LIKE 프리픽스)
    val purposes: List<String> = emptyList(),
    val ageGroups: List<String> = emptyList(),
)

object LessonBrowseOptions {
    val genres = listOf("발레", "바레", "한국무용", "현대무용", "실용무용", "발레핏", "뮤지컬")

    /** 시/도 — 코드 2자리(regionCode LIKE 프리픽스). 표준 행정구역 코드. */
    val sidos = listOf(
        "서울" to "11", "부산" to "26", "대구" to "27", "인천" to "28", "광주" to "29",
        "대전" to "30", "울산" to "31", "세종" to "36", "경기" to "41", "강원" to "51",
        "충북" to "43", "충남" to "44", "전북" to "52", "전남" to "46", "경북" to "47",
        "경남" to "48", "제주" to "50",
    )
    val ages = listOf(
        "preschool" to "미취학 아동", "elem" to "초등학생", "middle" to "중학생",
        "high" to "고등학생", "20s" to "20대", "30s" to "30대", "40s+" to "40대 이상",
    )
}

/**
 * 혼합 둘러보기 피드 1건 — 포트폴리오 또는 리뷰.
 *  숨고식: 부스트 포트폴리오에 강사 포토리뷰를 인터리브. type 으로 렌더/라우팅을 가른다.
 */
data class BrowseFeedItem(
    val type: String,             // "portfolio" | "review"
    val itemId: Int,
    val title: String?,
    val genre: String?,
    val region: String?,
    val images: List<String>?,
    val viewCount: Int?,
    val creatorName: String?,
    // 리뷰
    val content: String?,
    val rating: Int?,
    val teacherId: Int?,
    val teacherName: String?,
    val image: String?,
) {
    val isReview: Boolean get() = type == "review"

    /** 카드 썸네일(포트폴리오=images.first, 리뷰=image). */
    val thumbnail: String? get() = if (isReview) image else images?.firstOrNull()

    /** 카드 제목(포트폴리오=title, 리뷰=후기 내용). */
    val cardTitle: String get() = (if (isReview) content else title).orEmpty()

    val displayName: String? get() = if (isReview) teacherName else creatorName

    companion object {
        fun from(o: JSONObject) = BrowseFeedItem(
            type = o.optString("type").ifEmpty { "portfolio" },
            // ⚠️ 서버 키는 id 다(itemId 아님). 잘못 읽으면 카드 탭이 전부 0번으로 간다.
            itemId = o.optInt("id"),
            title = o.optString("title").ifEmpty { null },
            genre = o.optString("genre").ifEmpty { null },
            region = o.optString("region").ifEmpty { null },
            images = o.optJSONArray("images")?.let { a ->
                (0 until a.length()).map { a.optString(it) }.filter { it.isNotEmpty() }
            },
            viewCount = if (o.has("viewCount") && !o.isNull("viewCount")) o.optInt("viewCount") else null,
            creatorName = o.optJSONObject("creator")?.optString("name")?.ifEmpty { null },
            content = o.optString("content").ifEmpty { null },
            rating = if (o.has("rating") && !o.isNull("rating")) o.optInt("rating") else null,
            teacherId = if (o.has("teacherId") && !o.isNull("teacherId")) o.optInt("teacherId") else null,
            teacherName = o.optString("teacherName").ifEmpty { null },
            image = o.optString("image").ifEmpty { null },
        )
    }
}

/** 콘텐츠 상세. */
data class LessonContentDetail(
    val id: Int,
    val title: String,
    val description: String?,
    val genre: String?,
    val region: String?,
    val price: Int?,
    val images: List<String>?,
    val detailImages: List<String>?,
    val viewCount: Int?,
    val creatorId: Int?,
    val creatorName: String?,
    val creatorImage: String?,
) {
    companion object {
        fun from(o: JSONObject): LessonContentDetail {
            fun strings(k: String) = o.optJSONArray(k)?.let { a ->
                (0 until a.length()).map { a.optString(it) }.filter { it.isNotEmpty() }
            }
            val c = o.optJSONObject("creator")
            return LessonContentDetail(
                id = o.optInt("id"),
                title = o.optString("title").ifEmpty { "레슨" },
                description = o.optString("description").ifEmpty { null },
                genre = o.optString("genre").ifEmpty { null },
                region = o.optString("region").ifEmpty { null },
                price = if (o.has("price") && !o.isNull("price")) o.optInt("price") else null,
                images = strings("images"),
                detailImages = strings("detailImages"),
                viewCount = if (o.has("viewCount") && !o.isNull("viewCount")) o.optInt("viewCount") else null,
                creatorId = c?.optInt("id"),
                creatorName = c?.optString("name")?.ifEmpty { null },
                creatorImage = c?.optString("image")?.ifEmpty { null },
            )
        }
    }
}

class LessonBrowseApi(private val token: String?) {

    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    /** 숨고식 혼합 둘러보기 피드(포트폴리오+리뷰). browse=1&feed=1. */
    suspend fun feed(filter: LessonBrowseFilter): Result<List<BrowseFeedItem>> = runCatching {
        val q = mutableListOf("browse=1", "feed=1")
        filter.genre?.let { q += "genre=" + URLEncoder.encode(it, "UTF-8") }
        filter.sidoCode?.let { q += "regionCode=$it" }
        filter.purposes.takeIf { it.isNotEmpty() }?.let {
            q += "purposes=" + URLEncoder.encode(it.joinToString(","), "UTF-8")
        }
        filter.ageGroups.takeIf { it.isNotEmpty() }?.let {
            q += "ageGroups=" + URLEncoder.encode(it.joinToString(","), "UTF-8")
        }
        val body = get("/lesson-products?" + q.joinToString("&")) ?: error("목록을 불러오지 못했어요.")
        val arr = JSONArray(body.ifBlank { "[]" })
        (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let(BrowseFeedItem::from) }
    }

    /** 상세. view=1 은 조회수 증가(첫 진입에만). */
    suspend fun detail(id: Int, countView: Boolean): Result<LessonContentDetail> = runCatching {
        val path = if (countView) "/lesson-products/$id?view=1" else "/lesson-products/$id"
        val body = get(path) ?: error("상세를 불러오지 못했어요.")
        LessonContentDetail.from(JSONObject(body.ifBlank { "{}" }))
    }

    /** 비슷한 콘텐츠 — 상세 하단 추천. */
    suspend fun similar(id: Int): List<BrowseFeedItem> = runCatching {
        val body = get("/lesson-products/$id/similar").orEmpty()
        val arr = JSONArray(body.ifBlank { "[]" })
        (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.let(BrowseFeedItem::from) }
    }.getOrDefault(emptyList())

    private suspend fun get(path: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder().url(apiBase + path)
                .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                .build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@use null
                res.body?.string()
            }
        }.getOrNull()
    }
}
