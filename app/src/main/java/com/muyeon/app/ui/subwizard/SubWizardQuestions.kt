package com.muyeon.app.ui.subwizard

/**
 * 대타공고 등록 항목 — 값은 웹 constants/subOptions.js · jobOptions.js 와 같은 코드(서버 CreateSubDto 계약). 문구는 화면이 리소스로 단다.
 *   ★ 2026-11-08 사용자 요청으로 설문형(한 번에 하나씩)에서 **한 화면 입력 양식**으로 바꿨다.
 *   수업 시간은 시작·끝을 휠로 10분 단위(16:10 처럼)로 고른다.
 */
enum class SubFieldType { SINGLE, MULTI, CHIPS, DATE, TIME, REGION, TEXT, IMAGE }

data class SubField(val id: String, val type: SubFieldType, val options: List<String> = emptyList(), val required: Boolean = true)

object SubWizardQuestions {
    const val GENRE = "genre"
    const val TARGET = "target"
    const val FIELDS = "fields"
    const val SCHEDULE = "scheduleType"
    const val DATE = "classDate"
    const val START = "start"
    const val END = "end"
    const val TIME = "time"
    const val COUNT = "classCount"
    const val REGION = "region"
    const val SALARY = "salary"
    const val CAREER = "career"
    const val DESC = "description"
    const val IMAGE = "image"

    /** 시간 휠 — 0~23시, 분은 10분 단위. */
    val HOURS: List<Int> = (0..23).toList()
    val MINUTES: List<Int> = (0..50 step 10).toList()
    const val DEFAULT_START = "19:00"
    const val DEFAULT_END = "20:00"

    /** 화면 순서 그대로. */
    val fields: List<SubField> = listOf(
        SubField(GENRE, SubFieldType.CHIPS, com.muyeon.domain.model.teacher.LessonGenres.ALL),
        SubField(TARGET, SubFieldType.CHIPS, listOf("KIDS", "ELEM", "TEEN", "ADULT", "EXAM", "MAJOR")),
        SubField(FIELDS, SubFieldType.MULTI, com.muyeon.app.ui.recruit.RecruitLabels.FIELDS, required = false),
        SubField(SCHEDULE, SubFieldType.CHIPS, com.muyeon.app.ui.recruit.RecruitLabels.SCHEDULES),
        SubField(DATE, SubFieldType.DATE),
        SubField(TIME, SubFieldType.TIME),
        SubField(COUNT, SubFieldType.CHIPS, (1..8).map { it.toString() }),
        SubField(REGION, SubFieldType.REGION),
        SubField(SALARY, SubFieldType.CHIPS, com.muyeon.app.ui.recruit.RecruitLabels.SALARIES),
        SubField(CAREER, SubFieldType.CHIPS, listOf("ANY") + com.muyeon.app.ui.recruit.RecruitLabels.CAREERS, required = false),
        SubField(DESC, SubFieldType.TEXT, required = false),
        SubField(IMAGE, SubFieldType.IMAGE, required = false),
    )
}
