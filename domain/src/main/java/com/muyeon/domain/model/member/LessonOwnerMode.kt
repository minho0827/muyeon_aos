package com.muyeon.domain.model.member

/**
 * ★ 2026-11-16 레슨을 여는 주체 — 학원(ACADEMY) / 개인(강사·명부의 무용수 등).
 *   레슨 개설·관리·시간표 화면이 `isAcademy` 로 갈라 쓰던 규칙을 한 곳에 모았다.
 *   화면은 [of] 로 고른 모드의 값만 읽는다.
 */
enum class LessonOwnerMode(
    /** 개설 때 명의 칸 이름 — 학원은 "이 강사 이름으로"(asTeacherId), 개인은 "이 학원이 함께 관리"(atAcademyId). */
    val bylineField: String,
    /** 명의 후보 API — 학원은 소속 강사, 개인은 소속 학원. */
    val affiliationPath: String,
    /** 체험 클래스를 늘 고를 수 있나 — 학원 수업만 체험이 있다(개인은 학원 명의를 고를 때만). */
    val experienceAlways: Boolean,
    /** 레슨 설정(노출·장르) 메뉴가 있나 — 학원에겐 효과가 없는 화면이다. */
    val hasLessonSettings: Boolean,
) {
    ACADEMY("asTeacherId", "/academy-teachers/mine", experienceAlways = true, hasLessonSettings = false),
    PERSONAL("atAcademyId", "/academy-teachers/invites", experienceAlways = false, hasLessonSettings = true),
    ;

    val isAcademy: Boolean get() = this == ACADEMY

    /** 학원이 관리하는 레슨의 시간표를 이 주체가 못 고치나(개인은 읽기만). */
    fun slotsReadOnly(ownerAcademyId: Int?): Boolean = ownerAcademyId != null && !isAcademy

    /** 체험 클래스를 보일까 — 학원 본인, 학원 명의를 고른 개인, 이미 체험으로 켜 둔 레슨 수정. */
    fun experienceVisible(hasByline: Boolean, isEdit: Boolean, isExperience: Boolean): Boolean =
        experienceAlways || hasByline || (isEdit && isExperience)

    companion object {
        fun of(activeType: String?): LessonOwnerMode =
            if (ActiveType.visible(activeType) == ActiveType.ACADEMY) ACADEMY else PERSONAL
    }
}

/**
 * ★ 2026-11-16 레슨 제공 유형별 문구 표 — 학원은 "수업", 개인(강사·무용수)은 "레슨".
 */
data class LessonProviderCopy(
    /** 수업 / 레슨 */
    val noun: String,
    /** 관리 화면 아래 만들기 버튼. */
    val createButton: String,
    /** 개설 위저드 제목(새로). */
    val createTitle: String,
    /** 개설 위저드 제목(수정). */
    val editTitle: String,
    /** 이름 칸 — 수업명 / 레슨명. */
    val nameField: String,
    /** 명의 칸 질문. */
    val bylineQuestion: String,
    /** 명의 칸 첫 선택지(내 이름으로). */
    val selfByline: String,
    /** 명의 칸 도움말. */
    val bylineHint: String,
) {
    /** 명의 후보 한 줄 — 학원은 "OOO 강사", 개인은 학원 이름 그대로. */
    fun partyLabel(mode: LessonOwnerMode, partyName: String): String = if (mode.isAcademy) "$partyName 강사" else partyName

    companion object {
        private val ACADEMY = LessonProviderCopy(
            noun = "수업", createButton = "새 수업 등록", createTitle = "수업 등록", editTitle = "수업 수정",
            nameField = "수업명", bylineQuestion = "누구 이름으로 낼까요?", selfByline = "우리 학원 이름으로",
            bylineHint = "회원에게는 선택한 강사가 보이고, 후기도 그 강사에게 쌓여요. 관리는 학원이 합니다.",
        )
        private val PERSONAL = LessonProviderCopy(
            noun = "레슨", createButton = "새 레슨 개설", createTitle = "레슨 개설", editTitle = "레슨 수정",
            nameField = "레슨명", bylineQuestion = "어느 학원 수업인가요?", selfByline = "개인 레슨",
            bylineHint = "학원을 고르면 관리는 학원이 함께 해요. 레슨은 그대로 내 이름으로 보여요.",
        )

        fun of(mode: LessonOwnerMode): LessonProviderCopy = if (mode.isAcademy) ACADEMY else PERSONAL
        fun of(activeType: String?): LessonProviderCopy = of(LessonOwnerMode.of(activeType))

        /** 레슨 관리 화면의 내 유형 이름 — 학원은 "학원", 그 밖은 긴 유형 이름(강사·무용수). */
        fun roleLabel(activeType: String?): String =
            if (LessonOwnerMode.of(activeType).isAcademy) "학원" else ActiveTypeInfos.find(activeType)?.long ?: activeType.orEmpty()
    }
}
