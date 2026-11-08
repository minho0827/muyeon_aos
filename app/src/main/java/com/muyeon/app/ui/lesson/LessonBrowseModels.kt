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
    // ★ 2026-11-08 웹 LessonDetail.js 와 같은 '레슨 정보'(레슨 상품 = lesson_products 그대로).
    val level: String? = null,
    val lessonKind: String? = null,      // REGULAR | ONEDAY | EXPERIENCE
    val scheduleText: String? = null,
    val duration: Int? = null,           // 분
    val maxParticipants: Int? = null,
    val priceUnit: String? = null,       // PER_PERSON | PER_BOOKING
    val place: String? = null,
    val purposes: List<String>? = null,  // 견적 문진 옵션과 같은 id(견적 추천에서 연 경우에만 보여 준다)
    val formats: List<String>? = null,
    val ageGroups: List<String>? = null,
    /** 서버 bookable — 개설자가 지금 레슨을 제공할 수 있는 사람인가(false 면 웹처럼 예약·요청 버튼 없음). */
    val bookable: Boolean = true,
) {
    /** 웹 상세의 진행 일정/시간대 라벨 기준 — 원데이·체험은 날짜형. */
    val isDatedKind: Boolean get() = lessonKind == "ONEDAY" || lessonKind == "EXPERIENCE"

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
                level = o.optString("level").ifEmpty { null }.takeIf { !o.isNull("level") },
                lessonKind = o.optString("lessonKind").ifEmpty { null }.takeIf { !o.isNull("lessonKind") },
                scheduleText = o.optString("scheduleText").ifEmpty { null }.takeIf { !o.isNull("scheduleText") },
                duration = if (o.has("duration") && !o.isNull("duration")) o.optInt("duration") else null,
                maxParticipants = if (o.has("maxParticipants") && !o.isNull("maxParticipants")) o.optInt("maxParticipants") else null,
                priceUnit = o.optString("priceUnit").ifEmpty { null }.takeIf { !o.isNull("priceUnit") },
                place = o.optString("place").ifEmpty { null }.takeIf { !o.isNull("place") },
                purposes = strings("purposes"),
                formats = strings("formats"),
                ageGroups = strings("ageGroups"),
                bookable = !(o.has("bookable") && !o.optBoolean("bookable", true)),
            )
        }
    }
}

class LessonBrowseApi(private val token: String?) {

    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    data class FeedPage(val items: List<BrowseFeedItem>, val hasMore: Boolean, val nextCursor: String?)

    suspend fun feedPage(filter: LessonBrowseFilter, cursor: String? = null): Result<FeedPage> = runCatching {
        val q = mutableListOf("limit=30")
        filter.genre?.let { q += "genre=" + URLEncoder.encode(it, "UTF-8") }
        filter.sidoCode?.let { q += "regionCode=" + URLEncoder.encode(it, "UTF-8") }
        filter.purposes.takeIf { it.isNotEmpty() }?.let { q += "purpose=" + URLEncoder.encode(it.joinToString(","), "UTF-8") }
        filter.ageGroups.takeIf { it.isNotEmpty() }?.let { q += "ageGroup=" + URLEncoder.encode(it.joinToString(","), "UTF-8") }
        cursor?.let { q += "cursor=" + URLEncoder.encode(it, "UTF-8") }
        val body = get("/lesson-products/feed/page?" + q.joinToString("&")) ?: error("목록을 불러오지 못했어요.")
        val page = JSONObject(body)
        val rows = page.getJSONArray("items")
        FeedPage((0 until rows.length()).map { BrowseFeedItem.from(rows.getJSONObject(it)) },
            page.getBoolean("hasMore"), if (page.isNull("nextCursor")) null else page.getString("nextCursor"))
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

    /**
     * 지금 예약할 수 있는 회차 수 — GET /lesson-products/:id/available-slots(공개, OPEN·FULL 미래 회차).
     *  웹·앱 예약 시트가 쓰는 것과 같은 API. 읽지 못하면 null(버튼은 웹처럼 예약으로 둔다).
     */
    suspend fun availableSlotCount(id: Int): Int? {
        val body = get("/lesson-products/$id/available-slots") ?: return null
        return runCatching { JSONArray(body.ifBlank { "[]" }).length() }.getOrNull()
    }

    /** '같은 조건으로 견적 요청' · '이 강사님께 레슨 요청' 프리필 — GET /lesson-products/:id/quote-prefill. */
    suspend fun quotePrefill(id: Int): com.muyeon.app.ui.review.LessonQuotePrefill? {
        val body = get("/lesson-products/$id/quote-prefill") ?: return null
        return runCatching { com.muyeon.app.ui.review.LessonQuotePrefill.from(JSONObject(body.ifBlank { "{}" })) }.getOrNull()
    }

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

/**
 * ★ 2026-11-08 레슨 상세를 어디서 열었는지 — 아래 버튼이 달라진다(사용자 결정).
 *  QUOTE_RECOMMEND = 견적(레슨 요청)을 그만둘 때 띄우는 추천 레슨에서 연 경우 → [같은 조건으로 견적 요청하기].
 *  BROWSE = 그 밖의 모든 진입(홈 레슨 탭·전체보기·목록·둘러보기·찜·링크/푸시 /lessons/:id) → 웹 LessonDetail 과 같은 예약 버튼.
 */
enum class LessonDetailSource(val key: String) {
    BROWSE("browse"), QUOTE_RECOMMEND("quoteRecommend");

    companion object {
        fun from(key: String?) = entries.firstOrNull { it.key == key } ?: BROWSE
    }
}

/** 레슨 상세 아래 버튼. NONE = 버튼 없음(웹: 개설자가 지금 레슨을 받을 수 없는 회원 → bookable=false). */
enum class LessonDetailCta { SAME_QUOTE, MANAGE, BOOK, REQUEST, NONE }

/**
 * 버튼 고르기 — iOS LessonDetailCta.decide 와 같은 규칙.
 *  1) 견적 추천에서 열었으면 같은 조건 견적(내 레슨이면 관리).
 *  2) 내 레슨 → 관리(웹: 자기 레슨 예약 방지).
 *  3) bookable=false → 없음(웹과 같다).
 *  4) 수업료가 정해졌고 예약할 회차가 있으면(회차 수를 못 읽으면 웹처럼 예약) → 레슨 예약하기(기존 예약 화면).
 *  5) 수업료나 회차가 아직 없으면 → 이 강사님께 레슨 요청하기(지정 요청).
 */
fun lessonDetailCta(source: LessonDetailSource, d: LessonContentDetail, myUserId: Int?, slotCount: Int?): LessonDetailCta {
    val mine = myUserId != null && d.creatorId != null && d.creatorId == myUserId
    return when {
        mine -> LessonDetailCta.MANAGE
        source == LessonDetailSource.QUOTE_RECOMMEND -> LessonDetailCta.SAME_QUOTE
        !d.bookable -> LessonDetailCta.NONE
        d.price != null && (slotCount == null || slotCount > 0) -> LessonDetailCta.BOOK
        else -> LessonDetailCta.REQUEST
    }
}
