package com.muyeon.app.ui.castingstage.model

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

/** 칸 10개 + 캐스팅 — 코드·순서·색은 웹 constants/stageNews.js·backend·관리자 서버·iOS 와 같다.
 *  ★ 10-26 색은 무용연 주황 톤온톤(웹 \$primary-*) 하나로 — 보라는 경쟁사(발레매니아) 색이라 뺐다. */
data class StageCategory(val code: String, val label: String, val icon: ImageVector, val bg: Color, val fg: Color) {
    companion object {
        val CASTING = StageCategory("CASTING", "캐스팅", Icons.Filled.Star, Color(0xFFFFF3EA), Color(0xFFC95300))
        val NEWS = listOf(
            StageCategory("DOMESTIC", "국내소식", Icons.Filled.Newspaper, Color(0xFFFFF3EA), Color(0xFFC95300)),
            StageCategory("OVERSEAS", "해외소식", Icons.Filled.Public, Color(0xFFFFF3EA), Color(0xFFC95300)),
            StageCategory("PERFORMANCE", "공연·축제", Icons.Filled.TheaterComedy, Color(0xFFFFF3EA), Color(0xFFC95300)),
            StageCategory("CONTEST", "콩쿠르·대회", Icons.Filled.EmojiEvents, Color(0xFFFFF3EA), Color(0xFFC95300)),
            StageCategory("WORKSHOP", "워크숍·특강", Icons.Filled.Groups, Color(0xFFFFF3EA), Color(0xFFC95300)),
            StageCategory("CERTIFICATE", "지도자·자격증", Icons.Filled.Verified, Color(0xFFFFF3EA), Color(0xFFC95300)),
            StageCategory("SUPPORT", "장학·지원·공모", Icons.Filled.CardGiftcard, Color(0xFFFFF3EA), Color(0xFFC95300)),
            StageCategory("ADMISSION", "무용입시", Icons.Filled.EditNote, Color(0xFFFFF3EA), Color(0xFFC95300)),
            StageCategory("SCHOOL", "학교·교육과정", Icons.Filled.School, Color(0xFFFFF3EA), Color(0xFFC95300)),
            StageCategory("ACADEMIC", "학술·세미나", Icons.Filled.MenuBook, Color(0xFFFFF3EA), Color(0xFFC95300)),
        )
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
