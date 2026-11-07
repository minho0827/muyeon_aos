package com.muyeon.domain.model.teacher

/** 강사 카드 — profiles.service.ts teacherPreview(`GET /teachers/page`). */
data class TeacherCard(
    val id: Long,
    val name: String,
    val image: String?,
    /** 활동 지역 글자("서울 강남구, 경기 수원시"). */
    val region: String?,
    val genres: List<String>,
    /** 수업 분야 코드(BASIC·HOBBY·MAJOR … · 옛 BARRE·BALLET_FIT). */
    val fields: List<String>,
    /** 경력 코드(NEW·Y1_3 …). */
    val career: String?,
    /** 예시 프로필 — 문의 버튼을 막는다. */
    val isDemoProfile: Boolean,
    /** 상위노출권(부스트) 만료 시각. 지금보다 뒤면 '추천' 표시. */
    val promotedUntil: String?,
)

/**
 * 레슨 장르 칩 — 웹 mock JOB_GENRES. 바레·발레핏은 장르가 아니라 분야(field) 코드로 거른다(웹 GENRE_FIELD).
 *   문의 카테고리 id 는 웹 constants/quoteCategories.js 와 같다.
 */
object LessonGenres {
    val ALL = listOf("발레", "한국무용", "현대무용", "실용무용", "뮤지컬", "바레", "발레핏")
    val FIELD_OF = mapOf("바레" to "BARRE", "발레핏" to "BALLET_FIT")
    val QUOTE_CATEGORY = mapOf(
        "발레" to "ballet", "한국무용" to "korean", "현대무용" to "modern", "실용무용" to "practical",
        "뮤지컬" to "musical", "바레" to "barre", "발레핏" to "balletfit", "취미무용" to "hobby",
    )
    /** 지역 필터(시·도) — 웹 LessonTab REGION_FILTERS. 활동 지역 글자에 들어 있으면 통과(받은 목록 안에서 거른다). */
    val SIDO = listOf("서울", "경기", "인천", "부산", "대구", "대전", "광주", "울산", "세종", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주")
}
