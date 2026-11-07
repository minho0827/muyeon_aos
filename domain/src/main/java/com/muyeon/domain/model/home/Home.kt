package com.muyeon.domain.model.home

/** 홈 레슨 카드 — `GET /lesson-products/home-preview` · `/lesson-products/mine`. */
data class HomeLesson(
    val id: Long,
    val title: String,
    val image: String?,
    val genre: String?,
    /** region || place. */
    val place: String?,
    val scheduleText: String?,
    /** null = 가격 문의, 0 = 무료. */
    val price: Int?,
    val promotedUntil: String?,
    val status: String?,
)

/** 홈 레슨 미리보기 세 묶음. */
data class HomeLessonPreview(val popular: List<HomeLesson>, val experience: List<HomeLesson>, val oneday: List<HomeLesson>)

/** 채용(JOB)·대타(SUB) 공고 요약 — `GET /jobs` · `/subs`(홈 카드·공지형 목록). */
data class HomeRecruit(
    val kind: Kind,
    val id: Long,
    val title: String?,
    val image: String?,
    val genre: String?,
    val region: String?,
    /** 급여 코드(SalaryRange) — 화면이 이름으로. */
    val salary: String?,
    val pay: String?,
    val createdAt: String?,
    /** 대타 수업일(공지형 목록 날짜). */
    val classDate: String?,
) { enum class Kind { JOB, SUB } }

data class HomeSpace(val id: Long, val name: String, val image: String?, val region: String?, val price: Int?)

data class HomeTransfer(val id: Long, val title: String, val image: String?, val region: String?, val areaPyeong: Double?, val deposit: Int?, val monthlyRent: Int?)

data class HomeNotice(val id: Long, val title: String, val createdAt: String?)

/** 레슨을 찾는 회원(견적 요청) 카드 — `GET /quotes/home`. 답은 (질문, 선택지 id) 그대로. */
data class HomeQuote(
    val id: Long,
    val categoryId: String?,
    val region: String?,
    val answers: List<QuoteAnswer>,
    val createdAt: String?,
    val isMine: Boolean,
    val canRespond: Boolean,
)

data class QuoteAnswer(val questionId: String, val optionIds: List<String>, val text: String?)

/** 학원 홈 — 학원 이름·원하는 조건이 있는지(`GET /auth/me/academy`). */
data class AcademyHomeProfile(val academyName: String?, val hasCriteria: Boolean)

/** 우리 학원 강사 모집 공고(`GET /me/postings` 중 JOB·OPEN). */
data class HomeMyJob(val id: Long, val title: String?, val imageUrl: String?, val genre: String?, val region: String?, val days: String?, val deadline: String?, val applicants: Int, val views: Int)

/** 강사 홈 추천 카드 — `GET /home/teacher-preview`. kind = JOB·SUB·CASTING. */
data class TeacherHomePosting(val kind: String, val id: Long, val title: String, val image: String?, val genre: String?, val sub: String?, val info: String?, val createdAt: String?)

data class TeacherHomePreview(val academies: List<TeacherHomePosting>, val postings: List<TeacherHomePosting>, val urgent: List<TeacherHomePosting>)

/** 클래식 홈 피드 카드 — `GET /home/feed/page`. id 는 "JOB-5" 꼴. */
data class HomeFeedItem(
    val id: String,
    val domain: String,
    val path: String,
    val image: String?,
    val title: String,
    val sub: String?,
    val info: List<String>,
    val status: String?,
    val statusColor: String?,
    val categories: List<String>,
) {
    /** 대상 번호(찜) — "JOB-5" → 5. */
    val entityId: Long? get() = id.substringAfter('-', "").toLongOrNull()
}

/**
 * 클래식 홈 영역 칩·세부 칩 — 웹 mock homeDomains 와 같은 코드(이름은 앱 strings.xml).
 *   무용수는 공간대관(SPACE)을 뺀다.
 */
object HomeDomains {
    val ALL_CODES = listOf("ALL", "JOB", "SUB", "CASTING", "SPACE", "CLASS")
}
