package com.muyeon.app.ui.quote

import com.muyeon.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * 견적요청 REST — iOS `QuoteService.swift` 1:1 이식.
 *  엔드포인트·HTTP 메서드·페이로드 키를 iOS 와 동일하게 유지해야 한다(서버 무변경).
 *
 *  iOS 는 URLSession + JWT 수동 헤더. Android 는 기존 ui/quote 패턴(OkHttp + org.json) 유지.
 *  결과는 Kotlin Result 로 감싸 호출부가 서버 거절 사유(4xx message)를 그대로 노출할 수 있게 한다
 *  (iOS APIMessageError 대응).
 */

/** 서버 거절 사유(HTTP 4xx 의 message)를 그대로 노출하는 예외 — 일반 문구로 뭉개지 않게. */
class ApiMessageException(override val message: String) : Exception(message)

class QuoteApi(private val token: String?) {

    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    // MARK: - 요청 생성

    /** 견적요청 생성. POST /quotes → { id } */
    suspend fun createQuote(body: JSONObject): Result<Int> =
        call("/quotes", "POST", body).map { it.asObject().optInt("id") }

    // MARK: - 받은견적(고객)

    /** 내 견적요청 목록. GET /quotes/me */
    suspend fun getMyQuotes(): Result<List<MyQuoteSummary>> =
        call("/quotes/me").map { it.asArray().map(MyQuoteSummary::from) }

    /** 견적요청 상세 + 받은 견적. GET /quotes/:id */
    suspend fun getQuote(quoteId: Int): Result<QuoteDetailResponse> =
        call("/quotes/$quoteId").map { QuoteDetailResponse.from(it.asObject()) }

    /** 바로 채팅(채택 없이 방 생성). POST /quotes/:id/responses/:rid/chat → { roomId } */
    suspend fun startQuoteChat(quoteId: Int, responseId: Int): Result<Int> =
        call("/quotes/$quoteId/responses/$responseId/chat", "POST").map { it.asObject().effectiveRoomId() }

    /** 견적 채택(1요청=1강사 → 요청 마감). POST /quotes/:id/responses/:rid/accept → { roomId } */
    suspend fun acceptQuote(quoteId: Int, responseId: Int): Result<Int> =
        call("/quotes/$quoteId/responses/$responseId/accept", "POST").map { it.asObject().effectiveRoomId() }

    /** 내 견적요청 취소. PATCH /quotes/:id/cancel (진행중 OPEN → CANCELED). */
    suspend fun cancelQuote(quoteId: Int): Result<Unit> =
        call("/quotes/$quoteId/cancel", "PATCH").map { }

    /** 내 견적요청 삭제. DELETE /quotes/:id (취소/마감된 것만, 백엔드 권한 재검증). */
    suspend fun deleteQuote(quoteId: Int): Result<Unit> =
        call("/quotes/$quoteId", "DELETE").map { }

    // MARK: - 추천 강사(무응답 요청)

    /** 무응답 요청 강사 추천. GET /quotes/:id/recommendations?exclude= */
    suspend fun getQuoteRecommendations(quoteId: Int, excludeIds: List<Int> = emptyList()): Result<List<RecommendedTeacher>> {
        var path = "/quotes/$quoteId/recommendations"
        if (excludeIds.isNotEmpty()) path += "?exclude=" + excludeIds.joinToString(",")
        return call(path).map { res ->
            res.asObject().optJSONArray("teachers")?.map(RecommendedTeacher::from) ?: emptyList()
        }
    }

    /** 추천 강사에게 기존 요청 그대로 지정 재요청. POST /quotes/:id/re-request { teacherId, force } */
    suspend fun reRequestQuote(quoteId: Int, teacherId: Int, force: Boolean = false): Result<Unit> =
        call("/quotes/$quoteId/re-request", "POST",
            JSONObject().put("teacherId", teacherId).put("force", force)).map { }

    /** 강사 찜 토글(scraps TEACHER 재사용). POST/DELETE /teachers/:id/scrap */
    suspend fun setTeacherScrap(teacherId: Int, on: Boolean): Result<Unit> =
        call("/teachers/$teacherId/scrap", if (on) "POST" else "DELETE").map { }

    /** 다시는 추천받지 않기. POST /quotes/recommend-blocks { teacherId } */
    suspend fun blockRecommendation(teacherId: Int): Result<Unit> =
        call("/quotes/recommend-blocks", "POST", JSONObject().put("teacherId", teacherId)).map { }

    /** 강사의 공개 레슨 목록(추천 카드 '레슨 보기'). GET /lesson-products?creatorId= */
    suspend fun getTeacherLessons(teacherId: Int): Result<List<TeacherLessonItem>> =
        call("/lesson-products?creatorId=$teacherId").map { it.asArray().map(TeacherLessonItem::from) }

    // MARK: - 보낸견적 / 모아보기(강사)

    /** 내가 보낸 견적 목록. GET /quotes/sent */
    suspend fun getSentQuotes(): Result<List<SentQuoteItem>> =
        call("/quotes/sent").map { it.asArray().map(SentQuoteItem::from) }

    /** 응답 가능한 견적요청 목록. GET /quotes/available?categoryId=&page=&applyPrefs= */
    suspend fun getAvailableQuotes(categoryId: String? = null, page: Int = 0, applyPrefs: Boolean = true): Result<AvailableQuotesResponse> {
        var path = "/quotes/available?page=$page"
        if (!categoryId.isNullOrEmpty()) path += "&categoryId=" + URLEncoder.encode(categoryId, "UTF-8")
        if (!applyPrefs) path += "&applyPrefs=0"   // 모아보기에서 조건 임시 해제(전체 보기)
        return call(path).map { AvailableQuotesResponse.from(it.asObject()) }
    }

    /**
     * 견적 보내기(응답). POST /quotes/:id/responses
     *  { message, attachmentType, priceAmount, priceUnit, paymentMode, depositAmount }
     *  attachmentType 에 따라 견적 카드에 기본 이력서(TEACHER) 또는 학원 기본정보(ACADEMY)가 붙는다.
     *  보유하지 않은 역할을 보내면 서버가 403 이므로 myAttachmentType() 으로 정한다.
     */
    suspend fun sendQuoteResponse(
        quoteId: Int,
        priceAmount: Int?,
        depositAmount: Int?,
        message: String,
        attachmentType: String,
    ): Result<Unit> {
        val body = JSONObject().put("message", message).put("attachmentType", attachmentType)
        if (priceAmount != null) body.put("priceAmount", priceAmount).put("priceUnit", "PER_SESSION")
        body.put("paymentMode", if (depositAmount == null) "NONE" else "DEPOSIT")
        body.put("depositAmount", depositAmount ?: 0)
        return call("/quotes/$quoteId/responses", "POST", body).map { }
    }

    /**
     * 첨부할 내 프로필 종류 — iOS `RoleGate.activeType == "ACADEMY" ? ACADEMY : TEACHER`.
     *  AOS 에는 activeType 캐시가 없어 GET /auth/me 로 읽고,
     *  활동유형이 GENERAL 이면 보유 역할로 보정한다(학원만 보유한 계정의 403 방지).
     */
    suspend fun myAttachmentType(): String = call("/auth/me").map { res ->
        val o = res.asObject()
        val active = o.optString("activeType")
        when (active) {
            "ACADEMY", "TEACHER" -> active
            else -> {
                val types = o.optJSONArray("memberTypes")
                val has = (0 until (types?.length() ?: 0)).map { i -> types?.optString(i).orEmpty() }
                if (has.contains("TEACHER")) "TEACHER" else if (has.contains("ACADEMY")) "ACADEMY" else "TEACHER"
            }
        }
    }.getOrDefault("TEACHER")

    /** 열람 기록(N 배지 제거). POST /quotes/browse/seen/:id — 실패해도 UI 영향 없음. */
    suspend fun markQuoteBrowseSeen(quoteId: Int) { call("/quotes/browse/seen/$quoteId", "POST") }

    /** 방문 종료(다음 N 기준 시각 갱신). POST /quotes/browse/visit */
    suspend fun endQuoteBrowseVisit() { call("/quotes/browse/visit", "POST") }

    // MARK: - 대시보드

    /** 견적관리 허브 대시보드(타일 카운트 + 다가오는 일정). GET /quotes/dashboard?role= */
    suspend fun getQuoteDashboard(role: String): Result<QuoteDashboardData> =
        call("/quotes/dashboard?role=$role").map { QuoteDashboardData.from(it.asObject()) }

    // MARK: - 공통 호출

    /** 성공 본문(JSON 문자열) 래퍼 — 배열/객체 어느 쪽이든 받는다. */
    @JvmInline
    value class Body(val raw: String) {
        fun asObject(): JSONObject = if (raw.isBlank()) JSONObject() else JSONObject(raw)
        fun asArray(): JSONArray = if (raw.isBlank()) JSONArray() else JSONArray(raw)
    }

    private suspend fun call(path: String, method: String = "GET", body: JSONObject? = null): Result<Body> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload: RequestBody? = when {
                    body != null -> body.toString().toRequestBody(JSON)
                    method != "GET" && method != "DELETE" -> "".toRequestBody(JSON)   // POST/PATCH 는 본문 필수
                    else -> null
                }
                val req = Request.Builder()
                    .url(apiBase + path)
                    .method(method, payload)
                    .addHeader("Content-Type", "application/json")
                    .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                    .build()
                client.newCall(req).execute().use { res ->
                    val text = res.body?.string().orEmpty()
                    if (!res.isSuccessful) throw ApiMessageException(serverMessage(text) ?: "요청에 실패했어요.")
                    Body(text)
                }
            }
        }

    /** 4xx 본문에서 message 추출 — 문자열/배열 양쪽 허용(NestJS ValidationPipe 는 배열). */
    private fun serverMessage(text: String): String? = runCatching {
        val o = JSONObject(text)
        o.optJSONArray("message")
            ?.let { arr -> (0 until arr.length()).joinToString("\n") { arr.optString(it) } }
            ?.ifEmpty { null }
            ?: o.stringOrNull("message")
    }.getOrNull()

    private companion object {
        val JSON = "application/json".toMediaType()
    }
}
