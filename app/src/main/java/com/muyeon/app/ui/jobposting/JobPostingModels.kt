package com.muyeon.app.ui.jobposting

import com.muyeon.app.BuildConfig
import com.muyeon.app.ui.quote.boolOrNull
import com.muyeon.app.ui.quote.intOrNull
import com.muyeon.app.ui.quote.map
import com.muyeon.app.ui.quote.stringList
import com.muyeon.app.ui.quote.stringOrNull
import com.muyeon.app.ui.resume.ResumeOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 채용 공고 — iOS `JobPosting/JobPostingModels.swift` 1:1.
 *  공고 종류 3종(JOB 채용 / SUB 대타 / CASTING 공연)을 하나의 목록에서 관리한다.
 */
data class MyPosting(
    val kind: String,      // JOB | SUB | CASTING
    val id: Int,
    val title: String?,
    val status: String?,   // OPEN | CLOSED | HOLD | DRAFT
    val genre: String?,
    val region: String?,
    val fields: List<String>?,
    val days: String?,
    val target: String?,
    val deadline: String?,
    val applicants: Int?,
    val views: Int?,
    val updatedAt: String?,
    val createdAt: String?,
    // 본인 전용 작성값 원본(서버 postingCard.preview) — 상세 화면을 GET 응답 전에 바로 그린다(2026-10-04).
    val preview: JSONObject? = null,
) {
    /** kind 가 달라도 id 가 겹칠 수 있어 목록 key 는 조합. */
    val uid: String get() = "$kind-$id"

    /** D-day — OPEN + 마감일이 있을 때만. "-"(미정)은 null. */
    val dday: Int?
        get() {
            if (status != "OPEN") return null
            val d = deadline ?: return null
            if (d == "-" || d.length < 10) return null
            val due = runCatching { ymd.parse(d.take(10))?.time }.getOrNull() ?: return null
            return TimeUnit.MILLISECONDS.toDays(due - System.currentTimeMillis()).toInt()
        }

    /** 카드 서브라인 — 모집분야(최대 2) 또는 장르 + 요일. */
    val subLine: String
        get() {
            val parts = mutableListOf<String>()
            val fs = fields
            if (!fs.isNullOrEmpty()) parts.add(fs.take(2).joinToString(", ") { ResumeOptions.fieldLabel(it) })
            else genre?.takeIf { it.isNotEmpty() }?.let { parts.add(it) }
            days?.takeIf { it.isNotEmpty() }?.let { parts.add(it) }
            return parts.joinToString(" · ")
        }

    companion object {
        private val ymd = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        fun from(o: JSONObject) = MyPosting(
            o.optString("kind").ifEmpty { "JOB" }, o.optInt("id"),
            o.stringOrNull("title"), o.stringOrNull("status"), o.stringOrNull("genre"),
            o.stringOrNull("region"), o.stringList("fields"), o.stringOrNull("days"),
            o.stringOrNull("target"), o.stringOrNull("deadline"),
            o.intOrNull("applicants"), o.intOrNull("views"),
            o.stringOrNull("updatedAt"), o.stringOrNull("createdAt"),
            o.optJSONObject("preview"),
        )
    }
}

object JobPostingOptions {
    val kindLabel = mapOf("JOB" to "채용", "SUB" to "대타", "CASTING" to "공연")

    fun statusLabel(s: String?): String = when (s) {
        "OPEN" -> "채용중"
        "CLOSED" -> "마감"
        "HOLD" -> "보류"
        "DRAFT" -> "임시저장"
        else -> ""
    }

    fun statusPriority(status: String?): Int = when (status) {
        "OPEN" -> 0
        "HOLD" -> 1
        "CLOSED" -> 2
        else -> 3
    }

    val tabs = listOf("ALL" to "전체", "OPEN" to "채용중", "HOLD" to "보류", "CLOSED" to "마감")
}

/** 등록 폼 옵션 — 웹 jobOptions/subOptions 와 **값 계약**. */
object JobFormOptions {
    val genres = listOf("발레", "한국무용", "현대무용", "실용무용", "바레", "발레핏")
    // ANY(상관없음)는 채용공고 허용 경력 전용 — 고르면 다른 경력은 해제된다(iOS·웹과 동일).
    val careerLevels = listOf(
        "ANY" to "상관없음", "NEW" to "신입", "Y1_3" to "1~3년", "Y3_5" to "3~5년", "Y5_10" to "5~10년", "Y10" to "10년 이상",
    )
    val salaryRanges = listOf(
        "W1_2" to "1만~2만원", "W2_3" to "2만~3만원", "W3_4" to "3만~4만원",
        "W4_5" to "4만~5만원", "W5_6" to "5만~6만원", "NEGOTIABLE" to "추후 협의",
    )
    val employments = listOf(
        "FULLTIME" to "정규직", "CONTRACT" to "계약직", "PARTTIME" to "파트타임", "FREELANCE" to "프리랜서",
    )
    val weekDays = listOf("월", "화", "수", "목", "금", "토", "일")
    // 모집분야·수업대상은 ResumeOptions.teachingFields / classTargets 재사용

    // ── 코드 → 표시 문구 (iOS JobFormOptions 와 같은 규칙) ──
    fun salaryLabel(v: String?): String =
        v?.let { code -> salaryRanges.firstOrNull { it.first == code }?.second } ?: ""

    fun employmentLabel(v: String?): String =
        v?.let { code -> employments.firstOrNull { it.first == code }?.second } ?: ""

    fun careerLevelLabel(v: String): String =
        careerLevels.firstOrNull { it.first == v }?.second ?: v

    fun careerLevelsLabel(list: List<String>?): String =
        (list ?: emptyList()).joinToString(", ") { careerLevelLabel(it) }
}

/** 등록 폼 — 서버 create/update payload 와 키 일치. */
data class JobForm(
    var title: String = "",
    var academy: String? = null,
    var genre: String? = null,
    var region: String? = null,
    var regionCode: String? = null,
    var fields: List<String>? = null,
    var target: String? = null,
    var imageUrl: String? = null,
    var images: List<String>? = null,
    var address: String? = null,
    var subway: String? = null,
    var days: String? = null,       // "월·수·금"
    var time: String? = null,
    var employment: String? = null,
    var headcount: Int? = null,
    var deadline: String? = null,
    var salary: String? = null,
    var pay: String? = null,
    var careerLevels: List<String>? = null,
    var careerText: String? = null,
    var description: String? = null,
    var status: String? = null,     // DRAFT(임시저장) | OPEN
    // 원하는 강사 조건 — 웹 JobCreate·iOS JobPreferences 와 **키 이름까지 동일**해야 한다.
    //  한쪽만 바꾸면 저장은 되는데 다른 화면에서 안 보이는 형태로 조용히 어긋난다.
    var pref: JobPref = JobPref(),
) {
    fun toJson(isEdit: Boolean = false): JSONObject = JSONObject().apply {
        put("title", title)
        putOpt("academy", academy); putOpt("genre", genre)
        putOpt("region", region); putOpt("regionCode", regionCode)
        fields?.let { put("fields", JSONArray(it)) }
        putOpt("target", target)
        if (isEdit) put("imageUrl", imageUrl.orEmpty()) else putOpt("imageUrl", imageUrl)
        if (isEdit) put("images", JSONArray(images ?: emptyList<String>()))
        else images?.let { put("images", JSONArray(it)) }
        putOpt("address", address); putOpt("subway", subway)
        putOpt("days", days); putOpt("time", time)
        putOpt("employment", employment)
        headcount?.let { put("headcount", it) }
        putOpt("deadline", deadline); putOpt("salary", salary); putOpt("pay", pay)
        careerLevels?.let { put("careerLevels", JSONArray(it)) }
        putOpt("careerText", careerText); putOpt("description", description)
        putOpt("status", status)
        pref.toJson()?.let { put("preferences", it) }
    }

    companion object {
        /**
         * 서버 findOne 응답 → 폼.
         *  ⚠️ 일부 필드는 최상위가 아니라 **details 안**에 있다(target/address/subway/employment/
         *   headcount/deadline) — iOS loadJob 과 동일하게 갈라 읽는다.
         */
        fun from(o: JSONObject): JobForm {
            val d = o.optJSONObject("details") ?: JSONObject()
            return JobForm(
                title = o.optString("title"),
                academy = o.stringOrNull("academy"), genre = o.stringOrNull("genre"),
                region = o.stringOrNull("region"), regionCode = o.stringOrNull("regionCode"),
                fields = o.stringList("fields"),
                imageUrl = o.stringOrNull("imageUrl"), images = o.stringList("images"),
                days = o.stringOrNull("days"), time = o.stringOrNull("time"),
                salary = o.stringOrNull("salary"), pay = o.stringOrNull("pay"),
                careerLevels = o.stringList("careerLevels"), careerText = o.stringOrNull("careerText"),
                description = o.stringOrNull("description"),
                target = d.stringOrNull("target"), address = d.stringOrNull("address"),
                subway = d.stringOrNull("subway"), employment = d.stringOrNull("employment"),
                headcount = d.intOrNull("headcount"), deadline = d.stringOrNull("deadline"),
                // 서버는 preferences 를 details 안에 넣는다(웹과 동일 위치).
                pref = JobPref.from(d.optJSONObject("preferences")),
            )
        }
    }
}

/** 원하는 강사 조건 — null = 조건 없음(예/아니오 미선택). */
data class JobPref(
    val artHigh: Boolean? = null,        // 예고 출신 우대
    val university: Boolean? = null,     // 대학 졸업 우대
    val universityName: String? = null,  // 우대 대학명
    val company: Boolean? = null,        // 무용단 출신 우대
    val fields: List<String>? = null,    // 지도 가능 분야(우대)
    val certRequired: Boolean? = null,   // 자격증 필수
    val videoRequired: Boolean? = null,  // 영상 포트폴리오 필수
    val note: String? = null,            // 기타 우대 조건
) {
    /** 아무것도 안 골랐으면 null — 빈 객체를 보내 details 를 지저분하게 만들지 않는다. */
    fun toJson(): JSONObject? {
        val o = JSONObject()
        artHigh?.let { o.put("artHigh", it) }
        university?.let { o.put("university", it) }
        universityName?.takeIf { it.isNotBlank() }?.let { o.put("universityName", it) }
        company?.let { o.put("company", it) }
        fields?.takeIf { it.isNotEmpty() }?.let { o.put("fields", JSONArray(it)) }
        certRequired?.let { o.put("certRequired", it) }
        videoRequired?.let { o.put("videoRequired", it) }
        note?.takeIf { it.isNotBlank() }?.let { o.put("note", it) }
        return if (o.length() == 0) null else o
    }

    companion object {
        fun from(o: JSONObject?): JobPref {
            if (o == null) return JobPref()
            return JobPref(
                artHigh = o.boolOrNull("artHigh"), university = o.boolOrNull("university"),
                universityName = o.stringOrNull("universityName"), company = o.boolOrNull("company"),
                fields = o.stringList("fields"),
                certRequired = o.boolOrNull("certRequired"), videoRequired = o.boolOrNull("videoRequired"),
                note = o.stringOrNull("note"),
            )
        }
    }
}

/**
 * 공고 공개 한도 초과(403 POSTING_MEMBERSHIP_REQUIRED).
 *  서버 data = { kind: JOB|SUB|CASTING, tier: BASIC|STANDARD|PRO|null, limit, used } — 한도는 종류별(2026-10-01).
 *  tier == null 이면 무료 회원(가입 유도), 있으면 이미 멤버(공고 정리·등급 올리기 유도).
 *  data 가 없으면(구 서버) [hasData] = false → 종전 무료 회원 안내 그대로.
 */
class PostingLimitException(
    val serverMessage: String?,
    val hasData: Boolean,
    val kind: String?,
    val tier: String?,
    val limit: Int?,
    val used: Int?,
) : Exception(serverMessage ?: CODE) {

    /** 종류 한글 이름 — 서버 POSTING_KINDS.label 과 같은 표. */
    val kindLabel: String
        get() = when (kind) { "SUB" -> "대타"; "CASTING" -> "캐스팅"; else -> "채용" }

    companion object {
        const val CODE = "POSTING_MEMBERSHIP_REQUIRED"

        fun from(message: String?, data: JSONObject?) = PostingLimitException(
            serverMessage = message,
            hasData = data != null,
            kind = data?.stringOrNull("kind"),
            tier = data?.stringOrNull("tier"),
            limit = data?.intOrNull("limit"),
            used = data?.intOrNull("used"),
        )
    }
}

/** 서버 공통 에러 { code, message, data } — message 는 그대로 사용자 문구로 쓴다. */
class PostingApiException(val code: String?, message: String, val data: JSONObject?) : IllegalStateException(message)

/** 공고 종류 → 웹·API 경로 조각(웹 KIND_PATH 와 같은 규약). */
object PostingKind {
    fun seg(kind: String): String = when (kind) { "SUB" -> "subs"; "CASTING" -> "casting"; else -> "jobs" }
}

class JobPostingApi(internal val token: String?) {

    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    suspend fun myPostings(): Result<List<MyPosting>> =
        call("/me/postings").map { JSONArray(it.ifBlank { "[]" }).map(MyPosting::from) }

    /** 복사 → DRAFT 사본 id. */
    suspend fun duplicate(kind: String, id: Int): Result<Int> =
        call("/me/postings/$kind/$id/duplicate", "POST").map { JSONObject(it.ifBlank { "{}" }).optInt("id", 0) }

    suspend fun setStatus(kind: String, id: Int, status: String): Result<Unit> =
        call("/me/postings/$kind/$id/status", "PATCH", JSONObject().put("status", status)).map { }

    /** 삭제 = 보관(ARCHIVED) 소프트 처리. 지원 이력은 서버에 그대로 남는다. */
    suspend fun remove(kind: String, id: Int): Result<Unit> =
        call("/me/postings/$kind/$id", "DELETE").map { }

    /** 보관(삭제)한 공고 목록 — 보관함/복원 UI용. */
    suspend fun archived(): Result<List<MyPosting>> =
        call("/me/postings/archived").map { JSONArray(it.ifBlank { "[]" }).map(MyPosting::from) }

    /** 보관 해제(복원) — 삭제 직전 상태로 되돌린다(무조건 공개되지 않는다). */
    suspend fun restore(kind: String, id: Int): Result<Unit> =
        call("/me/postings/$kind/$id/restore", "POST").map { }

    suspend fun loadJob(id: Int): Result<JobForm> = call("/jobs/$id").map { JobForm.from(JSONObject(it)) }

    /**
     * 공고 상세 원본 — 종류별 GET /{jobs|subs|casting}/:id (인증 헤더 포함).
     *  ⚠️ 대타(SUB)는 토큰이 있어야 올린 분 전용 dispatchStats 가 붙는다.
     */
    suspend fun loadPosting(kind: String, id: Int): Result<JSONObject> =
        call("/${PostingKind.seg(kind)}/$id").map { JSONObject(it.ifBlank { "{}" }) }

    /** 마감 시 미선정 처리될 대기 지원자 수 — 채용·대타만(iOS pendingApplicantCount 동일). */
    suspend fun pendingApplicantCount(kind: String, id: Int): Result<Int> =
        call("/${PostingKind.seg(kind)}/$id/pending-applicants/count")
            .map { JSONObject(it.ifBlank { "{}" }).optInt("count", 0) }

    /** 마감 + 남은 지원자 일괄 미선정 안내 → 미선정 처리된 인원. */
    suspend fun closeWithApplicantResults(kind: String, id: Int): Result<Int> =
        call("/${PostingKind.seg(kind)}/$id/close", "PATCH", JSONObject().put("rejectPending", true))
            .map { JSONObject(it.ifBlank { "{}" }).optInt("rejectedCount", 0) }

    // ★ 2026-10-05 금액 올려 다시 보내기(raiseDispatch) 폐지 — 금액은 공고 수정으로 바꾼다.

    /** 긴급 대타 — 발송 중지(재발송·금액 권유 멈춤). */
    suspend fun stopDispatch(id: Int): Result<Unit> = call("/subs/$id/dispatch/stop", "POST").map { }

    /**
     * 대타 지원자 한 명의 이름 — 공고 상세 '○○ 강사로 확정' 문장용.
     *  지원자 상세(/applicants/:appId)는 열람 시각을 찍으므로 목록에서 찾는다.
     */
    suspend fun subApplicantName(postingId: Int, applicationId: Int): Result<String?> =
        call("/subs/$postingId/applicants").map { text ->
            JSONArray(text.ifBlank { "[]" }).map { it }
                .firstOrNull { it.optInt("id") == applicationId }?.stringOrNull("applicantName")
        }

    /** 공고 대표·상세 이미지 업로드 — 이력서·견적과 같은 /uploads/image. */
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
                if (!res.isSuccessful) error("이미지 업로드에 실패했어요.")
                JSONObject(text).optString("url").ifEmpty { error("이미지 업로드에 실패했어요.") }
            }
        }
    }

    suspend fun saveJob(id: Int?, form: JobForm): Result<Int> {
        val path = if (id != null) "/jobs/$id" else "/jobs"
        val method = if (id != null) "PATCH" else "POST"
        return call(path, method, form.toJson(isEdit = id != null)).map {
            JSONObject(it.ifBlank { "{}" }).optInt("id", id ?: 0)
        }
    }

    private suspend fun call(path: String, method: String = "GET", body: JSONObject? = null): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = when {
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
                        val obj = runCatching { JSONObject(text) }.getOrNull()
                        val msg = obj?.optString("message")?.ifEmpty { null }
                        // 공고 공개 한도 — 무료/멤버 구분은 data.tier 로 한다(403, 서버 공통 에러 필터 { code, message, data }).
                        if (obj?.optString("code") == PostingLimitException.CODE) {
                            throw PostingLimitException.from(msg, obj.optJSONObject("data"))
                        }
                        // 긴급 대타 등 코드로 분기하는 화면이 있어 code·data 를 함께 싣는다.
                        throw PostingApiException(
                            obj?.optString("code")?.ifEmpty { null }, msg ?: "요청에 실패했어요.", obj?.optJSONObject("data"),
                        )
                    }
                    text
                }
            }
        }

    private companion object { val JSON = "application/json".toMediaType() }
}
