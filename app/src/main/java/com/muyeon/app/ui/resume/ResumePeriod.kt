package com.muyeon.app.ui.resume

import java.util.Calendar

/**
 * 이력서 기간 문자열 파싱/포맷 — iOS `PeriodWheelPicker.swift` 의 `ResumePeriod` 1:1.
 *
 * ⚠️ `mergedTotalMonths` / `careerBucket` 은 **서버 계약**과 같아야 한다
 *   (서버 mergedCareerMonths·careerRank, 웹 CareerLevel). 목록 필터/정렬 싱크의 핵심이라
 *   한쪽만 바꾸면 검색 결과가 조용히 어긋난다.
 */
object ResumePeriod {

    const val CURRENT_LABEL = "현재"

    data class Parsed(val sy: Int, val sm: Int, val ey: Int?, val em: Int?)   // ey==null → 현재

    /**
     * "2022.03 ~ 현재" → 시작/종료 연·월.
     *  구분자 유연 수용(".", "-", "/"), "2022-03-15"의 일은 무시, 종료 빈 값 = 현재(웹 DateSelect 호환).
     *  종료가 있는데 파싱 불가면 **비정형**으로 보고 null(자동 산출 보류).
     */
    fun parse(s: String): Parsed? {
        val parts = s.replace(" ", "").split("~")
        val first = parts.firstOrNull() ?: return null

        fun ym(t: String): Pair<Int, Int>? {
            val c = t.split('.', '-', '/').mapNotNull { it.toIntOrNull() }
            if (c.size < 2 || c[0] <= 1900 || c[1] !in 1..12) return null
            return c[0] to c[1]
        }

        val start = ym(first) ?: return null
        val endText = if (parts.size >= 2) parts[1] else CURRENT_LABEL
        if (endText != CURRENT_LABEL && endText.isNotEmpty()) {
            val end = ym(endText) ?: return null   // 종료가 있는데 파싱 불가 = 비정형
            return Parsed(start.first, start.second, end.first, end.second)
        }
        return Parsed(start.first, start.second, null, null)
    }

    fun format(sy: Int, sm: Int, ey: Int?, em: Int?): String {
        val start = String.format("%d.%02d", sy, sm)
        return if (ey != null && em != null) "$start ~ ${String.format("%d.%02d", ey, em)}"
        else "$start ~ $CURRENT_LABEL"
    }

    /** 개월 수(종료 미지정=현재 기준, 종료월 포함). */
    fun months(s: String): Int? {
        val p = parse(s) ?: return null
        val cal = Calendar.getInstance()
        val ey = p.ey ?: cal.get(Calendar.YEAR)
        val em = p.em ?: (cal.get(Calendar.MONTH) + 1)
        val diff = (ey - p.sy) * 12 + (em - p.sm) + 1
        return if (diff > 0) diff else null
    }

    fun label(months: Int): String {
        val y = months / 12
        val r = months % 12
        return when {
            y > 0 && r > 0 -> "${y}년 ${r}개월"
            y > 0 -> "${y}년"
            else -> "${r}개월"
        }
    }

    fun durationLabel(s: String): String? = months(s)?.let { label(it) }

    /**
     * 겹침 구간 병합(union) 총 개월 — 동시 재직 중복 합산 방지.
     *  하나라도 파싱 실패하면 null(자동 산출 보류 — 부분합으로 과소 저장되는 것 방지).
     */
    fun mergedTotalMonths(periods: List<String>): Int? {
        val texts = periods.filter { it.trim().isNotEmpty() }
        if (texts.isEmpty()) return null
        val cal = Calendar.getInstance()
        val nowIdx = cal.get(Calendar.YEAR) * 12 + cal.get(Calendar.MONTH)

        val intervals = mutableListOf<Pair<Int, Int>>()
        for (t in texts) {
            val p = parse(t) ?: return null
            val s = p.sy * 12 + (p.sm - 1)
            val e = p.ey?.let { y -> y * 12 + ((p.em ?: 1) - 1) } ?: nowIdx
            if (e < s) return null
            intervals.add(s to e)
        }
        intervals.sortBy { it.first }

        var total = 0
        var curS = intervals[0].first
        var curE = intervals[0].second
        for ((s, e) in intervals.drop(1)) {
            if (s <= curE + 1) {
                curE = maxOf(curE, e)
            } else {
                total += curE - curS + 1
                curS = s; curE = e
            }
        }
        total += curE - curS + 1
        return if (total > 0) total else null
    }

    /** 총 개월 → 경력 버킷(웹 CareerLevel·서버 careerRank 와 동일 계약). */
    fun careerBucket(totalMonths: Int): String = when {
        totalMonths < 12 -> "NEW"
        totalMonths < 36 -> "Y1_3"
        totalMonths < 60 -> "Y3_5"
        totalMonths < 120 -> "Y5_10"
        else -> "Y10"
    }
}
