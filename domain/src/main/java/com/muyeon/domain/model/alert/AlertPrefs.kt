package com.muyeon.domain.model.alert

/**
 * ★ 2026-11-08 (b4) 관심 조건 알림 — 웹 pages/muyeon/my/AlertSettings.js · `GET/PATCH /me/alert-prefs`(alerts.controller.ts).
 *   [regions] 는 지역 코드 배열(웹은 첫 코드 하나만 쓴다). 빈 배열 = 그 조건은 따지지 않음.
 */
data class AlertPrefs(
    val regions: List<String> = emptyList(),
    val fields: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val targets: List<String> = emptyList(),
    val types: List<String> = emptyList(),
    val push: Boolean = true,
)

/** 화면을 여는 값 — 저장된 조건 + 보유 유형(고를 수 있는 항목이 유형에 따라 다르다). */
data class AlertForm(val prefs: AlertPrefs, val heldTypes: List<String>)

/**
 * 고를 값 — 웹 AlertSettings ALERT_TYPES · constants/subOptions(TEACHING_FIELDS·CLASS_TARGETS) 와 같은 값·같은 순서.
 *   ⚠️ 받는 종류는 백엔드 AlertType(common/enums.ts)과 1:1, [TYPE_ROLES] 는 ALERT_REQUIRED_ROLES 와 같은 규칙.
 */
object AlertOptions {
    val TYPES = listOf("JOB", "SUB", "CASTING", "TEACHER")
    val TYPE_ROLES = mapOf(
        "JOB" to listOf("TEACHER", "DANCER"),
        "SUB" to listOf("TEACHER", "DANCER"),
        "CASTING" to listOf("DANCER"),
        "TEACHER" to listOf("ACADEMY", "TEAM"),
    )
    val FIELDS = listOf("BASIC", "HOBBY", "MAJOR", "EXAM", "REPERTOIRE", "CONCOURS", "AUDITION", "PILATES", "STRETCH", "OTHER")
    val TARGETS = listOf("KIDS", "ELEM", "TEEN", "ADULT", "EXAM", "MAJOR")

    /** 보유 유형으로 고를 수 있는 종류 — 유형이 아직 없으면 전부(웹: 고를 게 하나도 없는 화면보다 낫다). */
    fun typesFor(held: List<String>): List<String> =
        if (held.isEmpty()) TYPES else TYPES.filter { t -> TYPE_ROLES[t].orEmpty().any { it in held } }

    fun showFields(held: List<String>) = held.any { it in listOf("TEACHER", "ACADEMY", "TEAM") }
    /** 강사면 '관심 수업 분야', 학원·공연팀만이면 '원하는 강사 분야'. */
    fun fieldsAsTeacher(held: List<String>) = "TEACHER" in held
    fun showTargets(held: List<String>) = held.any { it in listOf("TEACHER", "DANCER") }
}
