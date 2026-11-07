package com.muyeon.domain.model.recruit

/** 채용 탭 — 구인(채용공고) 카드. 웹 JobBoard hire 카드가 쓰는 값만. */
data class JobCard(
    val id: Long,
    val title: String,
    val academy: String?,
    val image: String?,
    val genre: String?,
    /** 직무(웹 job — 한글). */
    val job: String?,
    val region: String?,
    val days: String?,
    val time: String?,
    val pay: String?,
    /** SalaryRange 코드. */
    val salary: String?,
    /** TEACHING_FIELDS 코드. */
    val fields: List<String>,
    /** 마감일 'YYYY-MM-DD' 또는 '-'(상시). */
    val deadline: String?,
    val createdAt: String?,
)

/** 구직(이력서) 카드 — 강사·무용수 공용(웹 seek 카드). */
data class SeekerCard(
    val id: Long,
    /** TEACHER | DANCER. */
    val role: String,
    val name: String,
    val image: String?,
    val region: String?,
    val genres: List<String>,
    val fields: List<String>,
    val career: String?,
    val isNew: Boolean,
)

/** 대타공고 줄(웹 SubList 행 — 사진 없음). */
data class SubRow(
    val id: Long,
    val title: String?,
    val academy: String?,
    val region: String?,
    val genre: String?,
    val target: String?,
    val scheduleType: String?,
    val classCount: Int?,
    val classDate: String?,
    val classTime: String?,
    val salary: String?,
    val pay: String?,
    /** 최근 24시간 금액 변경: 1 올림 · -1 내림 · 0 없음(웹 payChangeOf). */
    val payChange: Int,
)

/** 구인 필터(여러 개 고름 — 비면 전체). */
data class JobFilter(val careers: Set<String> = emptySet(), val salaries: Set<String> = emptySet()) {
    val isActive get() = careers.isNotEmpty() || salaries.isNotEmpty()
}

/** 구직 필터 — 강사 유형은 types·fields, 그 밖은 fields·targets·careers(웹 JobBoard seek). */
data class SeekFilter(
    val types: Set<String> = emptySet(),
    val fields: Set<String> = emptySet(),
    val targets: Set<String> = emptySet(),
    val careers: Set<String> = emptySet(),
) {
    val isActive get() = types.isNotEmpty() || fields.isNotEmpty() || targets.isNotEmpty() || careers.isNotEmpty()
}

/** 대타 필터(한 개씩 고름 — null 이면 전체). 장르는 위 칩. */
data class SubFilter(
    val genre: String? = null,
    val scheduleType: String? = null,
    val minClassCount: Int? = null,
    val field: String? = null,
    val target: String? = null,
    val salary: String? = null,
) {
    val sheetActive get() = scheduleType != null || minClassCount != null || field != null || target != null || salary != null
}
