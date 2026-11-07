package com.muyeon.app.ui.recruit

import androidx.annotation.StringRes
import com.muyeon.app.R
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 채용 탭 문구 — 웹 constants/jobOptions.js(CAREER·SALARY) · subOptions.js(SCHEDULE·TARGET·CLASS_COUNT) · util/date deadlineInfo 와 같은 값.
 *   경력·분야 이름은 LessonLabels, 급여 이름은 HomeLabels.salary 를 같이 쓴다.
 */
object RecruitLabels {
    val CAREERS = listOf("NEW", "Y1_3", "Y3_5", "Y5_10", "Y10")
    val SALARIES = listOf("W1_2", "W2_3", "W3_4", "W4_5", "W5_6", "W6_8", "W8_UP", "NEGOTIABLE")
    val FIELDS = listOf("BASIC", "HOBBY", "MAJOR", "EXAM", "REPERTOIRE", "CONCOURS", "AUDITION", "PILATES", "STRETCH", "OTHER")
    val TARGETS = listOf("KIDS", "ELEM", "TEEN", "EXAM", "MAJOR", "ADULT")
    val SCHEDULES = listOf("ONE_DAY", "SHORT", "REGULAR")
    val CLASS_COUNTS = listOf(1, 2, 3, 5)
    val SEEK_TYPES = listOf("TEACHER", "DANCER")

    @StringRes
    fun target(code: String?): Int? = when (code) {
        "KIDS" -> R.string.target_kids
        "ELEM" -> R.string.target_elem
        "TEEN" -> R.string.target_teen
        "ADULT" -> R.string.target_adult
        "EXAM" -> R.string.target_exam
        "MAJOR" -> R.string.target_major
        else -> null
    }

    @StringRes
    fun schedule(code: String?): Int? = when (code) {
        "ONE_DAY" -> R.string.sub_schedule_one_day
        "SHORT" -> R.string.sub_schedule_short
        "REGULAR" -> R.string.sub_schedule_regular
        else -> null
    }

    @StringRes
    fun seekType(code: String): Int = if (code == "DANCER") R.string.recruit_type_dancer else R.string.recruit_type_teacher

    /** 마감 표시 — '-' 상시 모집 · 0일 오늘 마감 · 그 밖 D-n. 지났거나 없으면 null(웹 deadlineInfo). [urgent] 3일 이하. */
    data class Deadline(@StringRes val res: Int, val days: Long, val urgent: Boolean)

    fun deadline(value: String?): Deadline? {
        val v = value?.trim().orEmpty()
        if (v.isEmpty()) return null
        if (v == "-") return Deadline(R.string.recruit_deadline_always, -1, false)
        val date = runCatching { LocalDate.parse(v.take(10)) }.getOrNull() ?: return null
        val days = ChronoUnit.DAYS.between(LocalDate.now(ZoneId.of("Asia/Seoul")), date)
        if (days < 0) return null
        return Deadline(if (days == 0L) R.string.recruit_deadline_today else R.string.recruit_deadline_d, days, days <= 3)
    }

    /** 날짜 '-' = 미정(웹 tbd). */
    fun tbd(v: String?): String? = v?.let { if (it == "-") null else it }
}
