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

    val tabs = listOf("ALL" to "전체", "OPEN" to "채용중", "CLOSED" to "마감", "HOLD" to "보류")
}

/** 등록 폼 옵션 — 웹 jobOptions/subOptions 와 **값 계약**. */
object JobFormOptions {
    val genres = listOf("발레", "한국무용", "현대무용", "실용무용", "바레", "발레핏")
    val careerLevels = listOf(
        "NEW" to "신입", "Y1_3" to "1~3년", "Y3_5" to "3~5년", "Y5_10" to "5~10년", "Y10" to "10년 이상",
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
    fun toJson(): JSONObject = JSONObject().apply {
        put("title", title)
        putOpt("academy", academy); putOpt("genre", genre)
        putOpt("region", region); putOpt("regionCode", regionCode)
        fields?.let { put("fields", JSONArray(it)) }
        putOpt("target", target); putOpt("imageUrl", imageUrl)
        images?.let { put("images", JSONArray(it)) }
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

class JobPostingApi(private val token: String?) {

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

    suspend fun loadJob(id: Int): Result<JobForm> = call("/jobs/$id").map { JobForm.from(JSONObject(it)) }

    suspend fun saveJob(id: Int?, form: JobForm): Result<Int> {
        val path = if (id != null) "/jobs/$id" else "/jobs"
        val method = if (id != null) "PATCH" else "POST"
        return call(path, method, form.toJson()).map { JSONObject(it.ifBlank { "{}" }).optInt("id", id ?: 0) }
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
                        val msg = runCatching { JSONObject(text).optString("message") }.getOrNull()
                        throw IllegalStateException(msg?.ifEmpty { null } ?: "요청에 실패했어요.")
                    }
                    text
                }
            }
        }

    private companion object { val JSON = "application/json".toMediaType() }
}
