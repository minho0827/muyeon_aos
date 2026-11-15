package com.muyeon.domain.model.member

/**
 * ★ 2026-11-16 레슨 제공 유형 명부 — "이 활성유형이 레슨 요청을 받고 레슨을 운영할 수 있나"를 묻는 단 한 곳.
 *
 *   서버가 GET /auth/me · GET /app-gate 의 `lessonProviderTypes`(예: TEACHER, ACADEMY, DANCER)로 알려 주고,
 *   값이 없으면(옛 서버·조회 실패) [DEFAULT] = 강사·학원으로 본다.
 *   강사·학원만 하드코딩하던 판정(견적 응답·견적 목록·MY 레슨 메뉴·레슨 운영 화면 등)은 모두 여기를 읽는다.
 *
 *   무용수 레슨 제공 추가 기능의 등록 내용과 빼는 방법은 [DancerLessonProviderAddon] 한 파일에 모아 두었다.
 */
object LessonProviders {
    /** 서버가 값을 안 줄 때의 기본 — 예전 규칙(강사·학원). */
    val DEFAULT: Set<String> = setOf(ActiveType.TEACHER, ActiveType.ACADEMY)

    @Volatile private var current: Set<String> = DEFAULT

    /** 바뀔 때마다 부른다 — 앱이 저장(SharedPreferences)에 쓴다. 도메인은 저장소를 모른다. */
    @Volatile var onChanged: ((Set<String>) -> Unit)? = null

    /** 지금 레슨을 제공할 수 있는 활성유형 목록. */
    val types: Set<String> get() = current

    /**
     * 서버 값으로 바꾼다. null·빈 목록이면 기본값으로 돌아간다.
     *  @return 바뀐 뒤 목록(저장용)
     */
    fun update(serverTypes: List<String>?): Set<String> {
        val cleaned = serverTypes.orEmpty().map { it.trim().uppercase() }.filter { it.isNotEmpty() }.toSet()
        val next = cleaned.ifEmpty { DEFAULT }
        val changed = next != current
        current = next
        if (changed) onChanged?.invoke(next)
        return next
    }

    /** 기본값으로 되돌린다(테스트·로그아웃 후 재설정용). */
    fun reset() { current = DEFAULT }

    /** 이 활성유형이 레슨 제공자인가(견적 응답·레슨 운영·MY 레슨 메뉴). */
    fun canProvide(type: String?): Boolean = type?.trim()?.uppercase() in current

    /**
     * 견적 응답에 붙일 내 프로필 종류 — 학원은 "ACADEMY"(학원 정보), 그 밖 제공자(강사·무용수)는 "TEACHER"(개인 이력서).
     */
    fun attachmentType(type: String?): String =
        if (type?.trim()?.uppercase() == ActiveType.ACADEMY) ActiveType.ACADEMY else ActiveType.TEACHER

    /** 이력서 수정 화면에 레슨 항목(레슨 가능 시간·강의 경력·자격증·대상·희망 조건)을 보일까 — 학원은 이력서가 없다. */
    fun showsLessonFields(type: String?): Boolean =
        canProvide(type) && type?.trim()?.uppercase() != ActiveType.ACADEMY
}
