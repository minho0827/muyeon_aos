package com.muyeon.domain.model.member

/**
 * ★ 2026-11-16 기능별 필요 유형표 — "이 기능은 어떤 활성유형에서 쓸 수 있나"를 묻는 단 한 곳(서버 RequireActiveRole 기준과 같다).
 *   글쓰기 메뉴·캐스팅 ＋·공간 등록·양도 글·레슨 운영 입구가 모두 여기를 읽고, 공통 관문(TypeGate)이 판정한다.
 *   화면마다 `t in setOf(...)` 를 두지 않는다.
 */
enum class TypeFeature(
    /** 고정 필요 유형(버튼 순서 = 이 순서). [LESSON_PROVIDE] 는 레슨 제공 유형 명부를 읽는다. */
    private val fixed: List<String>,
    /** 창 문구의 동사 — "전환하고 작성할까요?" / "전환하고 이용할까요?". */
    val verb: String,
) {
    /** 채용공고 쓰기 — 원장·공연기획. */
    JOB_CREATE(listOf(ActiveType.ACADEMY, ActiveType.TEAM), "작성"),
    /** 이력서(구직 프로필) 등록 — 강사·무용수. */
    SEEK_PROFILE(listOf(ActiveType.TEACHER, ActiveType.DANCER), "작성"),
    /** 대타공고 쓰기 — 강사·원장·무용수·공연기획. */
    SUB_CREATE(listOf(ActiveType.TEACHER, ActiveType.ACADEMY, ActiveType.DANCER, ActiveType.TEAM), "작성"),
    /** 공연(캐스팅) 공고 쓰기 — 공연기획. */
    CASTING_CREATE(listOf(ActiveType.TEAM), "작성"),
    /** 대관 공간 등록 — 원장·강사·무용수. */
    SPACE_CREATE(listOf(ActiveType.ACADEMY, ActiveType.TEACHER, ActiveType.DANCER), "작성"),
    /** 양도양수 글 — 원장·강사. */
    TRANSFER_CREATE(listOf(ActiveType.ACADEMY, ActiveType.TEACHER), "작성"),
    /** 캐스팅(공연) 공고 지원 — 무용수. */
    CASTING_APPLY(listOf(ActiveType.DANCER), "지원"),
    /** 무용수에게 캐스팅 제안 — 공연기획. */
    CASTING_OFFER(listOf(ActiveType.TEAM), "제안"),
    /** 채용 채팅(이력서·무용수 프로필을 보고 먼저 연락) — 원장·공연기획·강사. 무용수 상세 열람 유형과 같다. */
    RECRUIT_CHAT(listOf(ActiveType.ACADEMY, ActiveType.TEAM, ActiveType.TEACHER), "이용"),
    /** 레슨 개설·운영·견적 응답 — 레슨 제공 유형 명부([LessonProviders]). */
    LESSON_PROVIDE(emptyList(), "이용"),
    ;

    /** 필요 유형(순서 있음). 레슨 제공은 강사 → 학원 → 그 밖 명부 유형. */
    val required: List<String>
        get() = if (this == LESSON_PROVIDE) {
            (listOf(ActiveType.TEACHER, ActiveType.ACADEMY) + LessonProviders.types.sorted()).distinct().filter { LessonProviders.canProvide(it) }
        } else fixed
}

object FeatureRoles {
    /** 화면에 쓰는 짧은 유형 이름(탭바 MY 칸 이름과 같은 말) — 유형 정보 표([ActiveTypeInfos]). */
    fun label(type: String): String = ActiveTypeInfos.find(ActiveType.visible(type))?.short ?: type

    fun required(feature: TypeFeature): List<String> = feature.required

    /** 이 활성유형으로 바로 쓸 수 있나. */
    fun allows(feature: TypeFeature, activeType: String?): Boolean =
        activeType != null && ActiveType.visible(activeType) in feature.required

    /** 회색 작은 글씨 — "강사·무용수". */
    fun caption(feature: TypeFeature): String = feature.required.joinToString("·") { label(it) }
}

/**
 * 공통 관문 판정 — 순수 함수. 화면은 결과에 맞는 창만 띄운다.
 *   1) 비회원 → 로그인  2) 지금 유형이 필요 유형 → 바로 진행  3) 필요 유형 중 가진 것 → 전환 창
 *   4) 필요 유형이 심사 중 → 심사 중 안내  5) 하나도 없음 → 유형 추가 안내
 */
sealed interface TypeGateDecision {
    data object Login : TypeGateDecision
    data object Allowed : TypeGateDecision
    /** [options] 가진 필요 유형(필요 유형 순서). 하나면 버튼 하나, 둘 이상이면 고르게 한다. */
    data class Switch(val current: String, val options: List<String>) : TypeGateDecision
    data class Pending(val types: List<String>) : TypeGateDecision
    data class AddType(val required: List<String>) : TypeGateDecision
}

object TypeGateRules {
    fun decide(
        feature: TypeFeature,
        loggedIn: Boolean,
        activeType: String?,
        held: Collection<String> = emptyList(),
        pending: Collection<String> = emptyList(),
    ): TypeGateDecision {
        if (!loggedIn) return TypeGateDecision.Login
        if (FeatureRoles.allows(feature, activeType)) return TypeGateDecision.Allowed
        val required = feature.required
        val heldSet = held.map { it.trim().uppercase() }.toSet()
        val options = required.filter { it in heldSet }
        if (options.isNotEmpty()) return TypeGateDecision.Switch(ActiveType.visible(activeType), options)
        val pendingSet = pending.map { it.trim().uppercase() }.toSet()
        val waiting = required.filter { it in pendingSet }
        if (waiting.isNotEmpty()) return TypeGateDecision.Pending(waiting)
        return TypeGateDecision.AddType(required)
    }

    /** 창 문구 — 해요체. */
    fun switchMessage(d: TypeGateDecision.Switch, feature: TypeFeature): String {
        val target = if (d.options.size == 1) "${FeatureRoles.label(d.options[0])} 유형으로" else "어느 유형으로"
        return "현재 회원님은 ${FeatureRoles.label(d.current)} 유형이에요. $target 전환하고 ${feature.verb}할까요?"
    }

    fun switchButton(type: String, feature: TypeFeature): String = "${FeatureRoles.label(type)} 유형으로 전환하고 ${feature.verb}"

    fun addTypeMessage(d: TypeGateDecision.AddType): String {
        val names = d.required.joinToString("·") { FeatureRoles.label(it) }
        val who = if (d.required.size == 1) "$names 유형이" else "$names 중 한 유형이"
        return "$who 있어야 쓸 수 있어요. 유형을 추가할까요?"
    }

    fun pendingMessage(d: TypeGateDecision.Pending): String =
        "${FeatureRoles.label(d.types.first())} 인증을 심사하고 있어요. 승인되면 바로 이용할 수 있어요."
}

/** 글쓰기 메뉴 한 줄의 판정 — [usable] false 면 회색 작은 글씨로 [caption](필요 유형)을 붙인다. */
data class WriteEntry(val feature: TypeFeature, val usable: Boolean, val caption: String?)

object WriteMenuRules {
    /** 채용 탭 [+ 글쓰기] — 4개 모두. 비회원은 빈 목록(버튼 숨김). */
    fun jobs(loggedIn: Boolean, activeType: String?): List<WriteEntry> =
        entries(loggedIn, activeType, listOf(TypeFeature.JOB_CREATE, TypeFeature.SEEK_PROFILE, TypeFeature.SUB_CREATE, TypeFeature.CASTING_CREATE))

    /** 공간 탭 [+ 글쓰기] — 대관·양도. 보고 있는 칸 항목이 먼저. */
    fun spaces(loggedIn: Boolean, activeType: String?, transfersFirst: Boolean): List<WriteEntry> =
        entries(loggedIn, activeType, if (transfersFirst) listOf(TypeFeature.TRANSFER_CREATE, TypeFeature.SPACE_CREATE) else listOf(TypeFeature.SPACE_CREATE, TypeFeature.TRANSFER_CREATE))

    /** 메뉴 맨 위 한 줄. */
    fun header(activeType: String?): String = "지금 ${FeatureRoles.label(activeType ?: ActiveType.GENERAL)} 유형이에요"

    private fun entries(loggedIn: Boolean, activeType: String?, features: List<TypeFeature>): List<WriteEntry> {
        if (!loggedIn) return emptyList()
        return features.map { f ->
            val ok = FeatureRoles.allows(f, activeType)
            WriteEntry(f, ok, if (ok) null else FeatureRoles.caption(f))
        }
    }
}

/** MY 탭 말풍선 — 전문 유형을 가졌는데 지금 일반일 때. */
object MyTypeHintRules {
    private val ORDER = listOf(ActiveType.TEACHER, ActiveType.ACADEMY, ActiveType.DANCER, ActiveType.TEAM)

    /** 말풍선에 쓸 유형(가진 것 중 강사 → 원장 → 무용수 → 공연기획 첫째). 안 띄우면 null. */
    fun bubbleType(loggedIn: Boolean, activeType: String?, held: Collection<String>, dismissed: Boolean): String? {
        if (!loggedIn || dismissed || ActiveType.visible(activeType) != ActiveType.GENERAL) return null
        val set = held.map { it.uppercase() }.toSet()
        return ORDER.firstOrNull { it in set }
    }

    fun text(type: String): String = "${FeatureRoles.label(type)} 유형도 있어요. MY를 두 번 누르면 바뀌어요"
}
