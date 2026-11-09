package com.muyeon.app.ui.castingstage.model

import com.muyeon.app.BuildConfig
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import com.muyeon.app.ui.castingstage.data.*
import com.muyeon.app.ui.castingstage.components.*
import com.muyeon.app.ui.castingstage.screens.*

/** 캐스팅·무대 데이터 모양 — iOS Models/(StageNewsModels·StageCastingModels·StageCategory·StageDate) 와 같다. */

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

/** 캐스팅 상세 — details 는 공고마다 칸이 달라 글자 맵으로 받는다. */
data class CastingDetail(
    val id: Int, val title: String, val team: String?, val genre: String?, val role: String?, val place: String?,
    val region: String?, val schedule: String?, val gender: String?, val height: String?, val headcount: Int,
    val videoRequired: Boolean, val awardPreferred: Boolean, val status: String?, val photos: List<String>,
    val details: Map<String, String>,
) {
    fun d(key: String): String? = details[key]
    val isExternal get() = d("source") == "WEB"
}

data class StageResume(val id: Int, val title: String?, val isDefault: Boolean)

data class StageIssueSummary(val issueNo: Int, val weekStart: String, val headline: String, val itemCount: Int)

data class StageCastingPage(val items: List<StageCasting>, val nextCursor: String?)

/** 서버 거절 사유를 그대로 보여 주는 예외. */
class StageMessageException(override val message: String) : Exception(message)

data class StageCasting(
    val id: Int, val title: String, val team: String?, val genre: String?, val imageUrl: String?, val deadline: String?,
)

/**
 * 캐스팅·무대 분류 — ★ 2026-11-09 서버가 준다(`GET /stage-news/categories`, 보이는 것만·서버 순서). 관리자가 NEWS 칸을 언제든 더한다.
 *   아이콘·색은 없다(사용자 결정: 글자만). [kind] CASTING = 캐스팅 공고 칸, NEWS = 무대 소식 칸.
 */
data class StageCategory(val code: String, val label: String, val kind: String) {
    val isCasting: Boolean get() = kind == KIND_CASTING

    companion object {
        const val KIND_CASTING = "CASTING"
        const val KIND_NEWS = "NEWS"
        const val CASTING_CODE = "CASTING"

        /** 서버를 못 받았을 때(실패·404) — 국내소식·콩쿠르·대회는 뺐다(11-09 사용자 요청). */
        val FALLBACK = listOf(
            StageCategory(CASTING_CODE, "캐스팅 공고", KIND_CASTING),
            StageCategory("PERFORMANCE", "공연·축제", KIND_NEWS),
            StageCategory("ADMISSION", "무용입시", KIND_NEWS),
            StageCategory("SCHOOL", "학교·교육과정", KIND_NEWS),
        )

        /** 서버 목록 정리 — 빈 코드·모르는 kind·겹친 코드는 뺀다. null(실패)·빈 목록이면 [FALLBACK]. */
        fun resolve(server: List<StageCategory>?): List<StageCategory> {
            val clean = server.orEmpty()
                .filter { it.code.isNotBlank() && (it.kind == KIND_CASTING || it.kind == KIND_NEWS) }
                .map { if (it.label.isBlank()) it.copy(label = it.code) else it }
                .distinctBy { it.code }
            return clean.ifEmpty { FALLBACK }
        }

        /** 보일 소식 칸 — 목록에 있고(서버 순서) 이번 호에 글이 있는 칸만. */
        fun newsSections(cats: List<StageCategory>, sections: Map<String, List<StageNewsItem>>?): List<Pair<StageCategory, List<StageNewsItem>>> =
            cats.filter { it.kind == KIND_NEWS }.mapNotNull { c -> sections?.get(c.code)?.takeIf { it.isNotEmpty() }?.let { c to it } }
    }
}

/** 이미지 주소 — http 로 시작하면 그대로, 아니면 서버 주소를 붙인다(CLAUDE.md 절대 규칙). */
fun stageImageUrl(raw: String?): String? = when {
    raw.isNullOrBlank() -> null
    raw.startsWith("http") -> raw
    else -> BuildConfig.API_BASE_URL + (if (raw.startsWith("/")) raw else "/$raw")
}

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
