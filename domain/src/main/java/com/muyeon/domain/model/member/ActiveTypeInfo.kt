package com.muyeon.domain.model.member

/**
 * ★ 2026-11-16 유형 정보 표 — 유형 이름·아이콘·인증 방식·서류 안내를 모은 단 한 곳.
 *   탭바 MY 칸 이름, 유형 시트, MY 머리말, 인증 화면, 전환 토스트가 모두 여기를 읽는다.
 *   화면마다 `when (type) { "ACADEMY" -> "학원·원장" … }` 을 두지 않는다.
 *
 *   이름은 두 벌만 쓴다.
 *   - 짧은 이름: 일반·강사·무용수·원장·공연기획(탭바·관문 창·글쓰기 메뉴)
 *   - 긴 이름: 일반·강사·무용수·학원·원장·공연팀·기획자(유형 시트·인증 화면·전환 토스트)
 */
data class ActiveTypeInfo(
    val type: String,
    val short: String,
    val long: String,
    /** MY 머리말 이름 — 일반만 "일반회원", 나머지는 긴 이름. */
    val title: String,
    /** 앱 아이콘 열쇠 — :app 이 그림으로 바꾼다(TypeIcons). */
    val iconKey: String,
    /** 유형 추가·관리 화면 그림 글자. */
    val emoji: String,
    /** 서류(사업자 등) 심사를 받아야 생기는 유형. */
    val documentVerification: Boolean,
    /** 서류 없이 휴대폰 본인 인증만으로 생기는 유형. */
    val identityOnly: Boolean,
    /** 인증 서류 안내 문구. */
    val docGuide: String,
)

object ActiveTypeInfos {
    val GENERAL = ActiveTypeInfo(
        ActiveType.GENERAL, "일반", "일반", "일반회원", "person", "🙂",
        documentVerification = false, identityOnly = false,
        docGuide = "회원유형을 확인할 수 있는 서류를 첨부해 주세요.",
    )
    val TEACHER = ActiveTypeInfo(
        ActiveType.TEACHER, "강사", "강사", "강사", "school", "🩰",
        documentVerification = false, identityOnly = true,
        docGuide = "강사 경력이나 무용 전공을 확인할 수 있는 서류를 첨부해 주세요.",
    )
    val DANCER = ActiveTypeInfo(
        ActiveType.DANCER, "무용수", "무용수", "무용수", "dancer", "💃",
        documentVerification = false, identityOnly = true,
        docGuide = "공연 경력, 소속 또는 무용 전공을 확인할 수 있는 서류를 첨부해 주세요.",
    )
    val ACADEMY = ActiveTypeInfo(
        ActiveType.ACADEMY, "원장", "학원·원장", "학원·원장", "academy", "🏫",
        documentVerification = true, identityOnly = false,
        docGuide = "사업자등록번호와 최근 3개월 이내 발급한 사업자등록증명원을 확인해요.",
    )
    val TEAM = ActiveTypeInfo(
        ActiveType.TEAM, "공연기획", "공연팀·기획자", "공연팀·기획자", "team", "🎭",
        documentVerification = true, identityOnly = false,
        docGuide = "사업자등록증, 고유번호증 또는 공연 증빙을 첨부해 주세요.",
    )
    /** 폐기된 공간 유형 — 예전 인증 기록을 보여 줄 때만 쓴다. */
    val SPACE = ActiveTypeInfo(
        ActiveType.SPACE, "공간", "공간 보유자", "공간 보유자", "space", "🏠",
        documentVerification = true, identityOnly = false,
        docGuide = "사업자등록증 또는 임대차계약서를 첨부해 주세요.",
    )

    private val ALL = listOf(GENERAL, TEACHER, DANCER, ACADEMY, TEAM, SPACE).associateBy { it.type }

    /** 표에 있는 유형만(없으면 null). 대소문자는 가리지 않는다. */
    fun find(type: String?): ActiveTypeInfo? = type?.trim()?.uppercase()?.let { ALL[it] }

    /** 화면에 쓰는 유형(취미·빈 값은 일반)으로 찾는다. */
    fun of(type: String?): ActiveTypeInfo = ALL[ActiveType.visible(type)] ?: GENERAL

    /** 서류 심사를 받는 전문 유형 — 학원·공연팀(가입 직후 MY 로 보낸다). */
    val DOCUMENT_ROLES: Set<String> = ALL.values.filter { it.documentVerification && ActiveType.isPro(it.type) }.map { it.type }.toSet()

    /** 본인 인증만으로 생기는 유형 — 강사·무용수. */
    val IDENTITY_ROLES: Set<String> = ALL.values.filter { it.identityOnly }.map { it.type }.toSet()

    /** 서류 인증 화면을 거치는 유형 — 학원·공연팀·(옛)공간. */
    fun needsDocuments(type: String?): Boolean = find(type)?.documentVerification == true
}
