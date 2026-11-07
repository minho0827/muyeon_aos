package com.muyeon.app.ui.home

import androidx.annotation.StringRes
import com.muyeon.app.R
import com.muyeon.app.ui.format.KstDates
import java.text.NumberFormat
import java.util.Locale

/** 홈 문구 도우미 — 웹 Home·jobOptions·monetization 과 같은 규칙. */
object HomeLabels {
    /** 급여 코드 이름(웹 salaryLabel — SalaryRange). 모르면 null. */
    @StringRes
    fun salary(code: String?): Int? = when (code) {
        "ANY" -> R.string.salary_any
        "W1_2" -> R.string.salary_w1_2
        "W2_3" -> R.string.salary_w2_3
        "W3_4" -> R.string.salary_w3_4
        "W4_5" -> R.string.salary_w4_5
        "W5_6" -> R.string.salary_w5_6
        "W6_8" -> R.string.salary_w6_8
        "W8_UP" -> R.string.salary_w8_up
        "NEGOTIABLE" -> R.string.salary_negotiable
        else -> null
    }

    fun won(n: Int): String = NumberFormat.getNumberInstance(Locale.KOREA).format(n)

    /** 상위노출(부스트) 중인가 — 웹 isBoosted(promotedUntil). */
    fun isBoosted(promotedUntil: String?): Boolean =
        KstDates.parse(promotedUntil)?.let { it.toEpochMilli() > System.currentTimeMillis() } == true

    /** 3일 안에 올라온 글(신규 표시) — 웹 isRecent / isNewPosting. */
    fun isRecent(iso: String?): Boolean = KstDates.secondsAgo(iso)?.let { it < 3 * 24 * 3600 } == true
}
