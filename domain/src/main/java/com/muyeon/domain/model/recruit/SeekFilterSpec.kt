package com.muyeon.domain.model.recruit

import com.muyeon.domain.model.member.ActiveType

/** 구직(이력서) 목록 필터 칸. 서버 쿼리 이름과 같다(types·fields·targets·careers). */
enum class SeekFilterField(val key: String) { TYPES("types"), FIELDS("fields"), TARGETS("targets"), CAREERS("careers") }

/**
 * ★ 2026-11-16 구직 탭 필터 규칙 — 지금 유형으로 어떤 칸을 보일지·보낼지·셀지를 한 곳에서 정한다(웹 JobBoard seek).
 *   강사는 [유형·분야], 그 밖은 [분야·대상·경력]. 화면이 처음 한 번만 유형을 읽던 잔상을 없애려고
 *   유형이 바뀌면 [forType] 으로 새로 만들고 [clean] 으로 안 보이는 칸 값을 지운다.
 */
data class SeekFilterSpec(val fields: List<SeekFilterField>) {
    /** 강사 시점(유형 칸이 있다). */
    val isTeacherView: Boolean get() = SeekFilterField.TYPES in fields

    /** 보이지 않는 칸의 값은 지운다 — 안 보이는 조건으로 목록이 걸러지지 않게. */
    fun clean(f: SeekFilter): SeekFilter = SeekFilter(
        types = if (SeekFilterField.TYPES in fields) f.types else emptySet(),
        fields = if (SeekFilterField.FIELDS in fields) f.fields else emptySet(),
        targets = if (SeekFilterField.TARGETS in fields) f.targets else emptySet(),
        careers = if (SeekFilterField.CAREERS in fields) f.careers else emptySet(),
    )

    /** 고른 값 개수(보이는 칸만). */
    fun count(f: SeekFilter): Int = clean(f).let { it.types.size + it.fields.size + it.targets.size + it.careers.size }

    /** 시트가 돌려준 칸별 값 → 필터(보이는 칸만). */
    fun fromSelection(m: Map<String, Set<String>>): SeekFilter = clean(
        SeekFilter(m[SeekFilterField.TYPES.key].orEmpty(), m[SeekFilterField.FIELDS.key].orEmpty(), m[SeekFilterField.TARGETS.key].orEmpty(), m[SeekFilterField.CAREERS.key].orEmpty()),
    )

    /** 필터 → 시트에 미리 채울 칸별 값. */
    fun selection(f: SeekFilter): Map<String, Set<String>> = mapOf(
        SeekFilterField.TYPES.key to f.types, SeekFilterField.FIELDS.key to f.fields,
        SeekFilterField.TARGETS.key to f.targets, SeekFilterField.CAREERS.key to f.careers,
    )

    companion object {
        private val TEACHER = SeekFilterSpec(listOf(SeekFilterField.TYPES, SeekFilterField.FIELDS))
        private val DEFAULT = SeekFilterSpec(listOf(SeekFilterField.FIELDS, SeekFilterField.TARGETS, SeekFilterField.CAREERS))

        fun forType(activeType: String?): SeekFilterSpec =
            if (ActiveType.visible(activeType) == ActiveType.TEACHER) TEACHER else DEFAULT
    }
}
