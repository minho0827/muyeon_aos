package com.muyeon.app.ui.castingstage

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.EditNote
import com.muyeon.app.BuildConfig
import com.muyeon.app.webview.withActiveType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * '캐스팅·무대' 탭(10-26) — 캐스팅 공고 + 주간 무대 소식. iOS CastingStageModels.swift 와 같은 데이터·규칙.
 *  - 소식: GET /stage-news/latest (호가 없으면 빈 본문)
 *  - 캐스팅: GET /casting/page
 */
data class StageNewsItem(
    val id: Int, val title: String, val summary: String, val sourceName: String?, val sourceUrl: String,
    val imageUrl: String?, val publishedDate: String?, val eventDate: String?,
)

data class StageNewsIssue(
    val issueNo: Int, val weekStart: String, val headline: String, val intro: String?,
    val sections: Map<String, List<StageNewsItem>>,
)

data class StageCasting(
    val id: Int, val title: String, val team: String?, val genre: String?, val imageUrl: String?, val deadline: String?,
)

/** 칸 10개 + 캐스팅 — 코드·순서·색은 웹 constants/stageNews.js·backend·관리자 서버·iOS 와 같다. */
data class StageCategory(val code: String, val label: String, val icon: ImageVector, val bg: Color, val fg: Color) {
    companion object {
        val CASTING = StageCategory("CASTING", "캐스팅", Icons.Filled.Star, Color(0xFFFFF1E6), Color(0xFFE55E00))
        val NEWS = listOf(
            StageCategory("DOMESTIC", "국내소식", Icons.Filled.Newspaper, Color(0xFFEEF6F0), Color(0xFF2F6B45)),
            StageCategory("OVERSEAS", "해외소식", Icons.Filled.Public, Color(0xFFEFEAFD), Color(0xFF5B3FC4)),
            StageCategory("PERFORMANCE", "공연·축제", Icons.Filled.TheaterComedy, Color(0xFFF3F0FC), Color(0xFF5B3FC4)),
            StageCategory("CONTEST", "콩쿠르·대회", Icons.Filled.EmojiEvents, Color(0xFFFDF0F4), Color(0xFFA8335A)),
            StageCategory("WORKSHOP", "워크숍·특강", Icons.Filled.Groups, Color(0xFFEAF7F6), Color(0xFF1F6F68)),
            StageCategory("CERTIFICATE", "지도자·자격증", Icons.Filled.Verified, Color(0xFFF4F6E8), Color(0xFF5A6B14)),
            StageCategory("SUPPORT", "장학·지원·공모", Icons.Filled.CardGiftcard, Color(0xFFFFF1EA), Color(0xFFB4502A)),
            StageCategory("ADMISSION", "무용입시", Icons.Filled.EditNote, Color(0xFFFFF5E8), Color(0xFF8A5A14)),
            StageCategory("SCHOOL", "학교·교육과정", Icons.Filled.School, Color(0xFFEEF5FD), Color(0xFF2D5A8A)),
            StageCategory("ACADEMIC", "학술·세미나", Icons.Filled.MenuBook, Color(0xFFF2F2F4), Color(0xFF4A4A5A)),
        )
    }
}

class CastingStageApi(private val token: String?) {
    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    private suspend fun get(path: String, query: Map<String, String?> = emptyMap()): String = withContext(Dispatchers.IO) {
        val url = (apiBase + path).toHttpUrl().newBuilder().apply {
            query.forEach { (k, v) -> if (!v.isNullOrEmpty()) addQueryParameter(k, v) }
        }.build()
        val req = Request.Builder().url(url)
            .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }.withActiveType()
            .build()
        client.newCall(req).execute().use { res ->
            val text = res.body?.string().orEmpty()
            if (!res.isSuccessful) throw IllegalStateException("HTTP ${res.code}")
            text
        }
    }

    /** 최신 호. 아직 호가 없으면 null. */
    suspend fun latest(): StageNewsIssue? {
        val text = get("/stage-news/latest").trim()
        if (text.isEmpty() || text == "null") return null
        val o = JSONObject(text)
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

    suspend fun castings(genre: String?): List<StageCasting> {
        val o = JSONObject(get("/casting/page", mapOf("genre" to genre, "feeRange" to "ALL", "limit" to "12")))
        val items = o.optJSONArray("items") ?: return emptyList()
        return (0 until items.length()).map { i ->
            val c = items.getJSONObject(i)
            StageCasting(
                id = c.optInt("id"), title = c.optString("title"), team = c.str("team"), genre = c.str("genre"),
                imageUrl = c.str("imageUrl") ?: c.str("image"), deadline = c.optJSONObject("details")?.str("deadline"),
            )
        }
    }
}

/** 이미지 주소 — http 로 시작하면 그대로, 아니면 서버 주소를 붙인다(CLAUDE.md 절대 규칙). */
fun stageImageUrl(raw: String?): String? = when {
    raw.isNullOrBlank() -> null
    raw.startsWith("http") -> raw
    else -> BuildConfig.API_BASE_URL + (if (raw.startsWith("/")) raw else "/$raw")
}

private fun JSONObject.str(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() && it != "null" }

/** 날짜 표시 — iOS StageDate 와 같은 규칙(한국 날짜). */
object StageDate {
    private val kst: ZoneId = ZoneId.of("Asia/Seoul")
    private fun parse(ymd: String?): LocalDate? = runCatching { LocalDate.parse(ymd!!.take(10)) }.getOrNull()

    /** '10.28(수)' */
    fun short(ymd: String?): String? = parse(ymd)?.format(DateTimeFormatter.ofPattern("M.d(E)", Locale.KOREAN))

    /** 캐스팅 마감 — 오늘/내일/날짜. 지난 날짜는 null. */
    fun deadline(ymd: String?): String? {
        val d = parse(ymd) ?: return null
        val diff = ChronoUnit.DAYS.between(LocalDate.now(kst), d)
        return when {
            diff < 0 -> null
            diff == 0L -> "오늘 마감"
            diff == 1L -> "내일 마감"
            else -> short(ymd)?.let { "$it 마감" }
        }
    }

    data class Week(val year: Int, val monthEn: String, val week: Int, val label: String, val range: String)

    /** 그 주 목요일이 속한 달 기준(웹 stageWeek 와 같다). */
    fun week(weekStart: String): Week? {
        val start = parse(weekStart) ?: return null
        val thu = start.plusDays(3)
        val end = start.plusDays(6)
        val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
        val week = (thu.dayOfMonth - 1) / 7 + 1
        fun p(n: Int) = n.toString().padStart(2, '0')
        return Week(
            thu.year, months[thu.monthValue - 1], week, "${thu.monthValue}월 ${week}주차",
            "${start.year}.${p(start.monthValue)}.${p(start.dayOfMonth)}-${p(end.monthValue)}.${p(end.dayOfMonth)}",
        )
    }
}
