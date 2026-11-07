package com.muyeon.app.ui.format

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 서버 날짜(ISO) → 한국 날짜 문구. 웹 util/date.js(fmtDate·timeAgo)와 같은 결과.
 *   문구 조각("분 전" 등)은 화면이 strings.xml 로 붙인다 — 여기서는 숫자·날짜만.
 */
object KstDates {
    private val KST: ZoneId = ZoneId.of("Asia/Seoul")
    private val DOT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    private val DASH: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun parse(iso: String?): Instant? {
        if (iso.isNullOrBlank()) return null
        return runCatching { Instant.parse(iso) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(iso).toInstant() }.getOrNull()
            ?: runCatching { LocalDate.parse(iso.take(10)).atStartOfDay(KST).toInstant() }.getOrNull()
    }

    fun localDate(iso: String?): LocalDate? = parse(iso)?.atZone(KST)?.toLocalDate()

    /** "2026.11.07" (웹 fmtDate). */
    fun dot(iso: String?): String = localDate(iso)?.format(DOT).orEmpty()

    /** "2026-11-07". */
    fun dash(iso: String?): String = localDate(iso)?.format(DASH).orEmpty()

    /** 기간 "시작 ~ 끝"(같으면 하나) — 웹 PerformanceList period. */
    fun dashPeriod(start: String?, end: String?): String {
        val s = dash(start); val e = dash(end)
        return when {
            s.isNotEmpty() && e.isNotEmpty() && s != e -> "$s ~ $e"
            else -> s.ifEmpty { e }
        }
    }

    fun isToday(iso: String?): Boolean = localDate(iso) == LocalDate.now(KST)

    /** 지난 시간(초). 미래·실패면 null. */
    fun secondsAgo(iso: String?): Long? {
        val t = parse(iso) ?: return null
        val sec = (System.currentTimeMillis() - t.toEpochMilli()) / 1000
        return sec.takeIf { it >= 0 }
    }
}
