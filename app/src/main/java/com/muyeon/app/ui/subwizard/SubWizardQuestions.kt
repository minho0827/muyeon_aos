package com.muyeon.app.ui.subwizard

/**
 * ★ 2026-11-08 대타공고 설문 — 레슨 견적처럼 한 번에 하나씩 묻는다(사용자 요청: 공고 양식은 너무 불편).
 *   값은 웹 constants/subOptions.js · jobOptions.js 와 같은 코드(서버 CreateSubDto 계약). 문구는 화면이 리소스로 단다.
 */
enum class SubStepType { SINGLE, MULTI, CHIPS, DATE, REGION, TEXT, IMAGE, CONFIRM }

data class SubStep(val id: String, val type: SubStepType, val options: List<String> = emptyList(), val required: Boolean = true)

object SubWizardQuestions {
    const val GENRE = "genre"
    const val TARGET = "target"
    const val FIELDS = "fields"
    const val SCHEDULE = "scheduleType"
    const val DATE = "classDate"
    const val START = "start"
    const val LENGTH = "length"
    const val COUNT = "classCount"
    const val REGION = "region"
    const val SALARY = "salary"
    const val CAREER = "career"
    const val DESC = "description"
    const val IMAGE = "image"
    const val CONFIRM = "confirm"

    /** 수업 시작 — 06:00~22:30, 30분 간격. */
    val START_TIMES: List<String> = (12..45).map { h2 -> "%02d:%02d".format(h2 / 2, (h2 % 2) * 30) }
    /** 수업 길이(분). */
    val LENGTHS = listOf("50", "60", "90", "120", "150", "180")

    val steps: List<SubStep> = listOf(
        SubStep(GENRE, SubStepType.SINGLE, com.muyeon.domain.model.teacher.LessonGenres.ALL),
        SubStep(TARGET, SubStepType.SINGLE, listOf("KIDS", "ELEM", "TEEN", "ADULT", "EXAM", "MAJOR")),
        SubStep(FIELDS, SubStepType.MULTI, com.muyeon.app.ui.recruit.RecruitLabels.FIELDS, required = false),
        SubStep(SCHEDULE, SubStepType.SINGLE, com.muyeon.app.ui.recruit.RecruitLabels.SCHEDULES),
        SubStep(DATE, SubStepType.DATE),
        SubStep(START, SubStepType.CHIPS, START_TIMES),
        SubStep(LENGTH, SubStepType.SINGLE, LENGTHS),
        SubStep(COUNT, SubStepType.CHIPS, (1..8).map { it.toString() }),
        SubStep(REGION, SubStepType.REGION),
        SubStep(SALARY, SubStepType.SINGLE, com.muyeon.app.ui.recruit.RecruitLabels.SALARIES),
        // 경력 — 웹 JOB_CAREER_LEVELS(상관없음 + CAREER_LEVELS). 건너뛸 수 있다.
        SubStep(CAREER, SubStepType.SINGLE, listOf("ANY") + com.muyeon.app.ui.recruit.RecruitLabels.CAREERS, required = false),
        SubStep(DESC, SubStepType.TEXT, required = false),
        // 대표 사진 1장(웹 SubCreate image) — 건너뛸 수 있다.
        SubStep(IMAGE, SubStepType.IMAGE, required = false),
        SubStep(CONFIRM, SubStepType.CONFIRM),
    )
}
