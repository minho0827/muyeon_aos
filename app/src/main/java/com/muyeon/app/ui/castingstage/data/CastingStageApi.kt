package com.muyeon.app.ui.castingstage.data

import com.muyeon.app.BuildConfig
import com.muyeon.app.webview.withActiveType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import com.muyeon.app.ui.castingstage.model.*
import com.muyeon.app.ui.castingstage.components.*
import com.muyeon.app.ui.castingstage.screens.*

/** 캐스팅·무대 API — iOS Services/CastingStageService 와 같다. */

class CastingStageApi(internal val token: String?) {
    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    private suspend fun get(path: String, query: Map<String, String?> = emptyMap()): String = call(path, "GET", query, null)

    private suspend fun call(path: String, method: String, query: Map<String, String?> = emptyMap(), body: JSONObject?): String =
        withContext(Dispatchers.IO) {
            val url = (apiBase + path).toHttpUrl().newBuilder().apply {
                query.forEach { (k, v) -> if (!v.isNullOrEmpty()) addQueryParameter(k, v) }
            }.build()
            val payload = when {
                body != null -> body.toString().toRequestBody("application/json".toMediaType())
                method == "POST" -> "{}".toRequestBody("application/json".toMediaType())
                else -> null
            }
            val req = Request.Builder().url(url).method(method, payload)
                .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }.withActiveType()
                .build()
            client.newCall(req).execute().use { res ->
                val text = res.body?.string().orEmpty()
                if (!res.isSuccessful) {
                    val msg = runCatching {
                        val o = JSONObject(text)
                        o.optJSONArray("message")?.let { a -> (0 until a.length()).joinToString("\n") { a.optString(it) } } ?: o.optString("message")
                    }.getOrNull()
                    throw StageMessageException(msg?.takeIf { it.isNotBlank() } ?: "요청에 실패했어요. 잠시 후 다시 시도해 주세요.")
                }
                text
            }
        }

    /** 최신 호. 아직 호가 없으면 null. */
    suspend fun latest(): StageNewsIssue? {
        val text = get("/stage-news/latest").trim()
        if (text.isEmpty() || text == "null") return null
        return parseIssue(JSONObject(text))
    }

    private fun parseIssue(o: JSONObject): StageNewsIssue {
        val sections = mutableMapOf<String, List<StageNewsItem>>()
        val arr = o.optJSONArray("sections")
        if (arr != null) for (i in 0 until arr.length()) {
            val s = arr.getJSONObject(i)
            val items = s.optJSONArray("items") ?: continue
            sections[s.optString("category")] = (0 until items.length()).map { j ->
                val n = items.getJSONObject(j)
                StageNewsItem(
                    id = n.optInt("id"), title = n.optString("title"), summary = n.optString("summary"),
                    sourceName = n.str("sourceName"), sourceUrl = n.optString("sourceUrl"), imageUrl = n.str("imageUrl"),
                    publishedDate = n.str("publishedDate"), eventDate = n.str("eventDate"),
                )
            }
        }
        return StageNewsIssue(o.optInt("issueNo"), o.optString("weekStart"), o.optString("headline"), o.str("intro"), sections)
    }

    private fun parseCastings(o: JSONObject): List<StageCasting> {
        val items = o.optJSONArray("items") ?: return emptyList()
        return (0 until items.length()).map { i ->
            val c = items.getJSONObject(i)
            StageCasting(
                id = c.optInt("id"), title = c.optString("title"), team = c.str("team"), genre = c.str("genre"),
                imageUrl = c.str("imageUrl") ?: c.str("image"), deadline = c.optJSONObject("details")?.str("deadline"),
            )
        }
    }

    suspend fun detail(id: Int): CastingDetail {
        val c = JSONObject(get("/casting/$id"))
        val det = c.optJSONObject("details")
        val map = buildMap { det?.keys()?.forEach { k -> det.str(k)?.let { put(k, it) } } }
        val imgs = c.optJSONArray("images")?.let { a -> (0 until a.length()).mapNotNull { a.optString(it).takeIf { s -> s.isNotBlank() } } }.orEmpty()
        return CastingDetail(
            id = c.optInt("id"), title = c.optString("title"), team = c.str("team"), genre = c.str("genre"), role = c.str("role"),
            place = c.str("place"), region = c.str("region"), schedule = c.str("schedule"), gender = c.str("gender"),
            height = c.str("height"), headcount = c.optInt("headcount"), videoRequired = c.optBoolean("videoRequired"),
            awardPreferred = c.optBoolean("awardPreferred"), status = c.str("status"),
            photos = imgs.ifEmpty { listOfNotNull(c.str("imageUrl")) }, details = map,
        )
    }

    suspend fun applied(id: Int): Boolean = runCatching { JSONObject(get("/casting/$id/applied")).optBoolean("applied") }.getOrDefault(false)

    /** 지원 — 실패하면 서버 안내문(StageMessageException). */
    suspend fun apply(id: Int, resumeId: Int?) {
        call("/casting/$id/apply", "POST", body = JSONObject().apply { if (resumeId != null) put("resumeId", resumeId) })
    }

    suspend fun dancerResumes(): List<StageResume> = runCatching {
        val a = org.json.JSONArray(get("/resumes", mapOf("roleIntent" to "DANCER")))
        (0 until a.length()).map { i -> a.getJSONObject(i).let { StageResume(it.optInt("id"), it.str("title"), it.optBoolean("isDefault")) } }
    }.getOrDefault(emptyList())

    suspend fun scrappedIds(): Set<Int> = runCatching {
        val a = org.json.JSONArray(get("/me/scraps", mapOf("targetType" to "CASTING")))
        (0 until a.length()).map { a.getJSONObject(it).optInt("targetId") }.toSet()
    }.getOrDefault(emptySet())

    suspend fun setScrap(id: Int, on: Boolean): Boolean =
        runCatching { call("/casting/$id/scrap", if (on) "POST" else "DELETE", body = null); true }.getOrDefault(false)

    suspend fun castingsPage(genre: String?, cursor: String?): StageCastingPage {
        val o = JSONObject(get("/casting/page", mapOf("genre" to genre, "feeRange" to "ALL", "cursor" to cursor, "limit" to "20")))
        return StageCastingPage(parseCastings(o), if (o.optBoolean("hasMore")) o.str("nextCursor") else null)
    }

    suspend fun issue(no: Int): StageNewsIssue = parseIssue(JSONObject(get("/stage-news/issues/$no")))

    suspend fun issues(): List<StageIssueSummary> = runCatching {
        val a = org.json.JSONArray(get("/stage-news/issues"))
        (0 until a.length()).map { i -> a.getJSONObject(i).let { StageIssueSummary(it.optInt("issueNo"), it.optString("weekStart"), it.optString("headline"), it.optInt("itemCount")) } }
    }.getOrDefault(emptyList())

    // ── 공연 공고 등록(공연팀) — POST /casting. 서버가 공연팀 인증·공고 한도·금칙어를 확인한다(iOS CastingStageService+Create).

    /** 계정 활동명(닉네임 → 이름). 못 받으면 빈 글자. */
    suspend fun activityName(): String = runCatching {
        val o = JSONObject(get("/auth/me"))
        o.str("nickname") ?: o.str("name") ?: ""
    }.getOrDefault("")

    /** 포스터 올리기 — 채용 공고와 같은 /uploads/image. */
    suspend fun uploadPoster(bytes: ByteArray): Result<String> =
        com.muyeon.app.ui.jobposting.JobPostingApi(token).uploadImage(bytes)

    /**
     * 등록하고 새 공고 id 를 돌려준다. 한도 초과(403 POSTING_MEMBERSHIP_REQUIRED)는 PostingLimitException,
     * 그 밖의 거절은 서버 문구(StageMessageException).
     */
    suspend fun createCasting(payload: JSONObject): Int? = withContext(Dispatchers.IO) {
        val req = Request.Builder().url("$apiBase/casting")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }.withActiveType()
            .build()
        client.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            val o = runCatching { JSONObject(text) }.getOrNull()
            if (!res.isSuccessful) {
                val msg = o?.optJSONArray("message")?.let { a -> (0 until a.length()).joinToString("\n") { a.optString(it) } }
                    ?: o?.str("message")
                if (o?.str("code") == com.muyeon.app.ui.jobposting.PostingLimitException.CODE) {
                    throw com.muyeon.app.ui.jobposting.PostingLimitException.from(msg, o.optJSONObject("data"))
                }
                throw StageMessageException(msg?.takeIf { it.isNotBlank() } ?: "등록에 실패했어요. 잠시 후 다시 시도해 주세요.")
            }
            o?.optInt("id")?.takeIf { it > 0 }
        }
    }

    suspend fun castings(genre: String?): List<StageCasting> =
        parseCastings(JSONObject(get("/casting/page", mapOf("genre" to genre, "feeRange" to "ALL", "limit" to "12"))))
}

private fun JSONObject.str(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() && it != "null" }
