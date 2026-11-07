package com.muyeon.app.ui.performance

import androidx.annotation.StringRes
import com.muyeon.app.R
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.domain.model.performance.PerformanceStatus

/** 공연 코드 → 화면 이름·배지 색(웹 PerformanceList STATUS_LABEL·STATUS_CLASS, mock performanceSubGenreLabel). */
object PerformanceLabels {
    @StringRes
    fun status(s: PerformanceStatus?): Int = when (s) {
        PerformanceStatus.UPCOMING -> R.string.performance_status_upcoming
        PerformanceStatus.ONGOING -> R.string.performance_status_ongoing
        PerformanceStatus.ENDED -> R.string.performance_status_ended
        null -> R.string.performance_status_unknown
    }

    fun statusLevel(s: PerformanceStatus?): CategoryColors.Level = when (s) {
        PerformanceStatus.UPCOMING -> CategoryColors.Level.Row
        PerformanceStatus.ONGOING -> CategoryColors.Level.High
        else -> CategoryColors.Level.Middle
    }

    /** 세부 분류·태그 이름. 모르는 코드면 null(웹은 빈 문자열 → 배지 없음). */
    @StringRes
    fun code(code: String?): Int? = when (code) {
        "CLASSIC_FULL" -> R.string.performance_sub_classic_full
        "GALA" -> R.string.performance_sub_gala
        "CONTEMPORARY_BALLET" -> R.string.performance_sub_contemporary_ballet
        "TRADITIONAL" -> R.string.performance_sub_traditional
        "CREATIVE_KOREAN" -> R.string.performance_sub_creative_korean
        "STREET" -> R.string.performance_sub_street
        "KPOP" -> R.string.performance_sub_kpop
        "FESTIVAL" -> R.string.performance_tag_festival
        "FAMILY" -> R.string.performance_tag_family
        else -> null
    }

    /** 목록 제목 — 20자 넘으면 말줄임(웹 ellipsize). */
    fun listTitle(title: String): String = if (title.length > 20) title.take(20) + "…" else title
}
