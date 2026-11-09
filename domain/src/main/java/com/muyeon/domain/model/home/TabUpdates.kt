package com.muyeon.domain.model.home

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * ★ 2026-11-09 하단 [공연] 칸 새 글 점 — `GET /home/tab-updates`.
 *   칸마다 가장 최근에 올라온 시각(ISO). 없으면 null.
 */
data class TabUpdates(val performance: String?, val casting: String?, val stageNews: String?) {
    /** 셋 중 가장 늦은 시각(원문 그대로). 다 없으면 null. */
    val latest: String? get() = listOfNotNull(performance, casting, stageNews).maxByOrNull { TabUpdateDot.millis(it) ?: Long.MIN_VALUE }
        ?.takeIf { TabUpdateDot.millis(it) != null }
}

/** 점 규칙 — 순수 함수(테스트용). */
object TabUpdateDot {
    /** 점을 켤지 · 마지막으로 본 시각을 무엇으로 저장할지(null = 그대로). */
    data class Decision(val dot: Boolean, val saveSeen: String?)

    /**
     * @param latest 서버 최신 시각(실패·없음 = null)
     * @param lastSeen 기기에 저장한 마지막으로 본 시각(처음 = null)
     * @param onTab 지금 공연·캐스팅 칸을 보고 있음
     */
    fun decide(latest: String?, lastSeen: String?, onTab: Boolean): Decision {
        val l = latest?.let(::millis) ?: return Decision(false, null)
        val seen = lastSeen?.let(::millis)
        // 처음 켰다(저장값 없음·깨짐) — 지금 것을 본 것으로 두고 점은 안 켠다.
        if (seen == null) return Decision(false, latest)
        if (l <= seen) return Decision(false, null)
        // 새 글이 있지만 이미 그 칸을 보고 있다 — 본 것으로 저장.
        return if (onTab) Decision(false, latest) else Decision(true, null)
    }

    /** ISO 시각 → 밀리초. 못 읽으면 null(초 소수점 있음·없음, Z·+09:00 둘 다). */
    fun millis(iso: String): Long? {
        val s = iso.trim()
        if (s.isEmpty()) return null
        for (p in PATTERNS) {
            val f = SimpleDateFormat(p, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC"); isLenient = false }
            runCatching { return f.parse(s)!!.time }
        }
        return null
    }

    private val PATTERNS = listOf("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "yyyy-MM-dd'T'HH:mm:ssXXX")
}
