package com.muyeon.app.ui.quote

import org.json.JSONArray
import org.json.JSONObject

/**
 * 받은견적(내 견적요청 목록 + 상세) 응답 모델 — iOS `QuoteReceivedModels.swift` 1:1.
 *  muyeon-backend /quotes 계약 매칭. **필드명은 서버 응답 키 그대로**(변경 금지).
 *   - GET /quotes/me      → [MyQuoteSummary]
 *   - GET /quotes/:id     → QuoteDetailResponse { quote, responses }
 *   - GET /quotes/sent    → [SentQuoteItem]
 *   - GET /quotes/available → AvailableQuotesResponse
 *
 *  Gson/Retrofit 대신 org.json 수동 파싱 — 기존 ui/quote(ExitRecommend, QuoteWizardActivity)와
 *  동일 패턴이고, 서버가 필드를 누락해도 optXXX 가 조용히 기본값을 주어 크래시가 없다.
 */

internal fun JSONObject.stringOrNull(key: String): String? =
    if (isNull(key)) null else optString(key).ifEmpty { null }

internal fun JSONObject.intOrNull(key: String): Int? = if (has(key) && !isNull(key)) optInt(key) else null

internal fun JSONObject.doubleOrNull(key: String): Double? = if (has(key) && !isNull(key)) optDouble(key) else null

internal fun JSONObject.boolOrNull(key: String): Boolean? = if (has(key) && !isNull(key)) optBoolean(key) else null

internal fun JSONObject.stringList(key: String): List<String>? {
    val arr = optJSONArray(key) ?: return null
    return (0 until arr.length()).mapNotNull { arr.optString(it).ifEmpty { null } }
}

internal fun JSONObject.intList(key: String): List<Int>? {
    val arr = optJSONArray(key) ?: return null
    return (0 until arr.length()).map { arr.optInt(it) }
}

internal inline fun <T> JSONArray.map(transform: (JSONObject) -> T): List<T> =
    (0 until length()).mapNotNull { optJSONObject(it) }.map(transform)

/** 견적 응답 강사 프로필 요약 — topResponder / responder 공통. */
data class QuotePro(
    val id: Int?,
    val name: String?,
    val nickname: String?,
    val image: String?,
    val career: String?,
    val region: String?,
    val genres: List<String>?,
    val service: String?,     // 카테고리 한글(예: 발레)
    val intro: String?,
    val rating: Double?,
    val reviewCount: Int?,
) {
    val displayName: String get() = name?.ifEmpty { null } ?: nickname ?: "강사"

    companion object {
        fun from(o: JSONObject?): QuotePro? = o?.let {
            QuotePro(
                id = it.intOrNull("id"), name = it.stringOrNull("name"), nickname = it.stringOrNull("nickname"),
                image = it.stringOrNull("image"), career = it.stringOrNull("career"), region = it.stringOrNull("region"),
                genres = it.stringList("genres"), service = it.stringOrNull("service"), intro = it.stringOrNull("intro"),
                rating = it.doubleOrNull("rating"), reviewCount = it.intOrNull("reviewCount"),
            )
        }
    }
}

/** GET /quotes/me — 내 견적요청 목록 1건. */
data class MyQuoteSummary(
    val id: Int,
    val categoryId: String?,
    val region: String?,
    val status: String?,          // OPEN | MATCHED | CLOSED | CANCELED
    val createdAt: String?,
    val responseCount: Int?,
    val lastResponseAt: String?,
    val expired: Boolean?,
    val acceptedResponseId: Int?,
    val topResponder: QuotePro?,
    val targetTeacherId: Int?,        // 지정(1:1) 요청 대상 강사 id. null 이면 브로드캐스트.
    val targetTeacher: QuotePro?,
    val responderImages: List<String>?,   // 겹침 아바타용(최대 4, 중복 제거)
) {
    /** 지정(1:1) 견적요청 여부. */
    val isDirect: Boolean get() = targetTeacherId != null

    /** 행 아바타에 쓸 이미지 목록 — 응답자 이미지 우선, 없으면 대표강사 1개. */
    val avatarImages: List<String?>
        get() = responderImages?.takeIf { it.isNotEmpty() } ?: listOf(topResponder?.image)

    companion object {
        fun from(o: JSONObject) = MyQuoteSummary(
            id = o.optInt("id"),
            categoryId = o.stringOrNull("categoryId"),
            region = o.stringOrNull("region"),
            status = o.stringOrNull("status"),
            createdAt = o.stringOrNull("createdAt"),
            responseCount = o.intOrNull("responseCount"),
            lastResponseAt = o.stringOrNull("lastResponseAt"),
            expired = o.boolOrNull("expired"),
            acceptedResponseId = o.intOrNull("acceptedResponseId"),
            topResponder = QuotePro.from(o.optJSONObject("topResponder")),
            targetTeacherId = o.intOrNull("targetTeacherId"),
            targetTeacher = QuotePro.from(o.optJSONObject("targetTeacher")),
            responderImages = o.stringList("responderImages"),
        )
    }
}

/** 문진 답변 1건(서버 저장본). 선택형 답(optionIds)까지 담아 작성 내용을 되짚을 수 있게 한다. */
data class QuoteAnswerRaw(
    val questionId: String?,
    val optionIds: List<String>?,
    val text: String?,
    val region: String?,
    val date: String?,
    val images: List<String>?,
) {
    companion object {
        fun from(o: JSONObject) = QuoteAnswerRaw(
            questionId = o.stringOrNull("questionId"),
            optionIds = o.stringList("optionIds"),
            text = o.stringOrNull("text"),
            region = o.stringOrNull("region"),
            date = o.stringOrNull("date"),
            images = o.stringList("images"),
        )
    }
}

data class QuoteFull(
    val id: Int,
    val categoryId: String?,
    val region: String?,
    val status: String?,          // OPEN | MATCHED | CLOSED | CANCELED
    val createdAt: String?,
    val acceptedResponseId: Int?,
    val answers: List<QuoteAnswerRaw>?,
    val targetTeacherId: Int?,
    val targetTeacher: QuotePro?,
) {
    val isDirect: Boolean get() = targetTeacherId != null

    companion object {
        fun from(o: JSONObject) = QuoteFull(
            id = o.optInt("id"),
            categoryId = o.stringOrNull("categoryId"),
            region = o.stringOrNull("region"),
            status = o.stringOrNull("status"),
            createdAt = o.stringOrNull("createdAt"),
            acceptedResponseId = o.intOrNull("acceptedResponseId"),
            answers = o.optJSONArray("answers")?.map { QuoteAnswerRaw.from(it) },
            targetTeacherId = o.intOrNull("targetTeacherId"),
            targetTeacher = QuotePro.from(o.optJSONObject("targetTeacher")),
        )
    }
}

/** 받은 견적 1건(강사가 보낸 응답). */
data class QuoteResponseItem(
    val id: Int,
    val price: String?,           // 가격 안내 메모(보조)
    val priceAmount: Int?,        // 구조화 금액(원) — 정렬/표시
    val priceUnit: String?,       // PER_SESSION | PER_MONTH | TOTAL
    val message: String?,
    val status: String?,          // SENT | ACCEPTED | REJECTED
    val createdAt: String?,
    val responder: QuotePro?,
) {
    companion object {
        fun from(o: JSONObject) = QuoteResponseItem(
            id = o.optInt("id"),
            price = o.stringOrNull("price"),
            priceAmount = o.intOrNull("priceAmount"),
            priceUnit = o.stringOrNull("priceUnit"),
            message = o.stringOrNull("message"),
            status = o.stringOrNull("status"),
            createdAt = o.stringOrNull("createdAt"),
            responder = QuotePro.from(o.optJSONObject("responder")),
        )
    }
}

/** GET /quotes/:id — 요청 상세 + 받은 견적. */
data class QuoteDetailResponse(val quote: QuoteFull, val responses: List<QuoteResponseItem>) {
    companion object {
        fun from(o: JSONObject) = QuoteDetailResponse(
            quote = QuoteFull.from(o.optJSONObject("quote") ?: JSONObject()),
            responses = o.optJSONArray("responses")?.map { QuoteResponseItem.from(it) } ?: emptyList(),
        )
    }
}

/** GET /quotes/:id/recommendations — 무응답 요청 강사 추천 1건. */
data class RecommendedTeacher(
    val id: Int,
    val name: String?,
    val nickname: String?,
    val image: String?,
    val career: String?,
    val region: String?,
    val genres: List<String>?,
    val service: String?,
    val intro: String?,
    val rating: Double?,
    val reviewCount: Int?,
    val reasons: List<String>?,        // 추천 근거 라벨(지역 일치·바로 견적·평점 우수)
    val alreadyRequested: Boolean?,    // 이미 이 강사에게 지정 요청을 보냄
    val lastActiveAt: String?,
    val isOnline: Boolean?,
    val isScrapped: Boolean?,
    val existingRoomId: Int?,          // 나↔강사 기존 1:1 채팅방(분기 팝업)
    val activeQuoteId: Int?,           // 진행 중 지정요청 id
    val activeQuoteStatus: String?,    // OPEN | MATCHED — MATCHED 는 새 요청 차단(채택 보호)
) {
    val displayName: String get() = name?.ifEmpty { null } ?: nickname ?: "강사"

    companion object {
        fun from(o: JSONObject) = RecommendedTeacher(
            id = o.optInt("id"),
            name = o.stringOrNull("name"), nickname = o.stringOrNull("nickname"), image = o.stringOrNull("image"),
            career = o.stringOrNull("career"), region = o.stringOrNull("region"), genres = o.stringList("genres"),
            service = o.stringOrNull("service"), intro = o.stringOrNull("intro"),
            rating = o.doubleOrNull("rating"), reviewCount = o.intOrNull("reviewCount"),
            reasons = o.stringList("reasons"), alreadyRequested = o.boolOrNull("alreadyRequested"),
            lastActiveAt = o.stringOrNull("lastActiveAt"), isOnline = o.boolOrNull("isOnline"),
            isScrapped = o.boolOrNull("isScrapped"), existingRoomId = o.intOrNull("existingRoomId"),
            activeQuoteId = o.intOrNull("activeQuoteId"), activeQuoteStatus = o.stringOrNull("activeQuoteStatus"),
        )
    }
}

/** GET /quotes/available — 강사가 응답 가능한 견적요청 목록(견적 모아보기, 페이징). */
data class AvailableQuotesResponse(
    val needsGenre: Boolean?,      // 전공 미등록 안내 필요
    val items: List<QuoteFull>,
    val total: Int?,
    val page: Int?,
    val hasMore: Boolean?,
    val prefsApplied: Boolean?,    // 견적 수신 조건(레슨 설정) 적용 상태 — 배너용
    val prefsSummary: List<String>?,
    val prefsFilteredOut: Int?,
    val hasPrefs: Boolean?,
    val myCategoryIds: List<String>?,  // 내 전공 — 전공 외 견적 발송 확인 팝업용
    val newIds: List<Int>?,            // 직전 방문 이후 미열람(N 배지) — 서버 판정
) {
    companion object {
        fun from(o: JSONObject) = AvailableQuotesResponse(
            needsGenre = o.boolOrNull("needsGenre"),
            items = o.optJSONArray("items")?.map { QuoteFull.from(it) } ?: emptyList(),
            total = o.intOrNull("total"), page = o.intOrNull("page"), hasMore = o.boolOrNull("hasMore"),
            prefsApplied = o.boolOrNull("prefsApplied"), prefsSummary = o.stringList("prefsSummary"),
            prefsFilteredOut = o.intOrNull("prefsFilteredOut"), hasPrefs = o.boolOrNull("hasPrefs"),
            myCategoryIds = o.stringList("myCategoryIds"), newIds = o.intList("newIds"),
        )
    }
}

/** 견적 요청자(고객) 요약 — 강사가 보는 '내 보낸 견적' 카드용. */
data class QuoteCustomer(val id: Int?, val name: String?, val nickname: String?, val image: String?) {
    val displayName: String get() = name?.ifEmpty { null } ?: nickname ?: "회원"

    companion object {
        fun from(o: JSONObject?): QuoteCustomer? = o?.let {
            QuoteCustomer(it.intOrNull("id"), it.stringOrNull("name"), it.stringOrNull("nickname"), it.stringOrNull("image"))
        }
    }
}

/** GET /quotes/sent — 내가(강사·원장) 보낸 견적 1건. */
data class SentQuoteItem(
    val id: Int,
    val quoteId: Int,
    val price: String?,
    val priceAmount: Int?,
    val priceUnit: String?,
    val message: String?,
    val status: String?,          // 내 응답: SENT | ACCEPTED | REJECTED
    val chatRoomId: Int?,
    val createdAt: String?,
    val categoryId: String?,
    val region: String?,
    val quoteStatus: String?,     // 요청 상태: OPEN | MATCHED | EXPIRED | CANCELED
    val customer: QuoteCustomer?,
) {
    companion object {
        fun from(o: JSONObject) = SentQuoteItem(
            id = o.optInt("id"), quoteId = o.optInt("quoteId"),
            price = o.stringOrNull("price"), priceAmount = o.intOrNull("priceAmount"),
            priceUnit = o.stringOrNull("priceUnit"), message = o.stringOrNull("message"),
            status = o.stringOrNull("status"), chatRoomId = o.intOrNull("chatRoomId"),
            createdAt = o.stringOrNull("createdAt"), categoryId = o.stringOrNull("categoryId"),
            region = o.stringOrNull("region"), quoteStatus = o.stringOrNull("quoteStatus"),
            customer = QuoteCustomer.from(o.optJSONObject("customer")),
        )
    }
}

/** GET /lesson-products?creatorId= — 추천 카드 '레슨 보기' 시트용. */
data class TeacherLessonItem(val id: Int, val title: String?, val genre: String?, val region: String?, val price: Int?) {
    companion object {
        fun from(o: JSONObject) = TeacherLessonItem(
            o.optInt("id"), o.stringOrNull("title"), o.stringOrNull("genre"),
            o.stringOrNull("region"), o.intOrNull("price"),
        )
    }
}

/** GET /quotes/dashboard?role= — 견적관리 허브 대시보드(iOS QuoteDashboardView.swift 모델). */
data class QuoteDashboardData(
    val role: String?,
    // teacher
    val todayLessons: Int?,
    val newRequests: Int?,
    val sentPending: Int?,
    val accepted: Int?,
    val pendingSchedules: Int?,
    // customer
    val upcomingReservations: Int?,
    val unreadQuotes: Int?,
    val openRequests: Int?,
    val doneLessons: Int?,
    val upcoming: List<QuoteDashUpcoming>?,
) {
    companion object {
        fun from(o: JSONObject) = QuoteDashboardData(
            role = o.stringOrNull("role"),
            todayLessons = o.intOrNull("todayLessons"), newRequests = o.intOrNull("newRequests"),
            sentPending = o.intOrNull("sentPending"), accepted = o.intOrNull("accepted"),
            pendingSchedules = o.intOrNull("pendingSchedules"),
            upcomingReservations = o.intOrNull("upcomingReservations"), unreadQuotes = o.intOrNull("unreadQuotes"),
            openRequests = o.intOrNull("openRequests"), doneLessons = o.intOrNull("doneLessons"),
            upcoming = o.optJSONArray("upcoming")?.map { QuoteDashUpcoming.from(it) },
        )
    }
}

data class QuoteDashUpcoming(
    val id: Int,
    val date: String?,
    val time: String?,
    val title: String?,
    val counterpart: String?,
    val lessonId: Int?,
    val reservationId: Int?,
) {
    companion object {
        fun from(o: JSONObject) = QuoteDashUpcoming(
            id = o.optInt("id"), date = o.stringOrNull("date"), time = o.stringOrNull("time"),
            title = o.stringOrNull("title"), counterpart = o.stringOrNull("counterpart"),
            lessonId = o.intOrNull("lessonId"), reservationId = o.intOrNull("reservationId"),
        )
    }
}

/** accept / startChat 공통 응답 — { roomId } 또는 { chatRoomId, roomId }. */
internal fun JSONObject.effectiveRoomId(): Int = intOrNull("roomId") ?: intOrNull("chatRoomId") ?: 0

internal fun JSONArray.mapObjects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }
