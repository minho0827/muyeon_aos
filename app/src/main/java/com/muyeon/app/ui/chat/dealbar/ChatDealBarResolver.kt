package com.muyeon.app.ui.chat.dealbar

/**
 * ★ 2026-11-14 채팅방 상단 고정 바(당근 거래 바) — '지금 무엇을 보여 줄지'만 정하는 순수 함수(화면 없음, iOS ChatDealBarResolver 1:1).
 *  화면(ChatDealBar)은 이 결과만 그린다. 짝(pair)·내 역할·레슨 단계별 버튼 규칙은 여기 한 곳.
 *
 *  회원↔강사(MEMBER_TEACHER)
 *   - 회원: 제안 도착 [받은 제안 보기][채택하기] → 채택 [약속잡기|약속 확인하기][받은 제안 보기] → 확정 [일정 보기][전화] → 완료 [후기 보내기][일정 보기]
 *   - 강사: 채택 전 [견적 보기] → 채택 [약속잡기|약속 확인하기] → 확정 [일정 보기][레슨 완료] → 완료 [일정 보기]
 *  학원↔강사(ACADEMY_TEACHER, 채택 = 함께하기): 학원 [받은 제안 보기][함께하기] / 강사 [학원 정보 보기], 함께한 뒤 [전화]
 *  약속잡기 방(PROPOSAL): 강사 [약속잡기] … 확정 [일정 보기][레슨 완료] / 회원 [약속 확인하기] … 완료 [후기 보내기]
 *  공간·양도·채용·그 밖, 잠긴 방: 바를 숨긴다(첫 안내만).
 */
enum class DealAction(val label: String) {
    VIEW_QUOTES("받은 제안 보기"),
    VIEW_MY_QUOTE("견적 보기"),
    ACCEPT("채택하기"),
    HIRE("함께하기"),
    SCHEDULE("약속잡기"),
    CHECK_PROPOSAL("약속 확인하기"),
    VIEW_LESSON("일정 보기"),
    COMPLETE_LESSON("레슨 완료"),
    REVIEW("후기 보내기"),
    CALL("전화"),
    ACADEMY_INFO("학원 정보 보기"),
}

/** 바 위 요약 줄 + 버튼(최대 3개). */
data class DealBarModel(
    val title: String,
    val status: String,
    val statusEmphasis: Boolean,   // 내 차례(채택·약속 확인·레슨 완료·후기)면 주황 강조
    val detail: String?,           // 가격 또는 일정 한 줄
    val actions: List<DealAction>,
    val cyclesCount: Int,
)

/** 방 상세에서 모은 값(VM 이 채운다). 단계 값은 서버 progress.step 그대로. */
data class DealBarInput(
    val pair: String?,             // 서버 room-pair. 예전 서버(null)면 견적 유무로 추정한다
    val myRole: String?,           // customer | pro
    val locked: Boolean,
    val step: String?,             // RESPONDED | ACCEPTED | SCHEDULED | DONE (대표 사이클)
    val title: String?,            // "발레 레슨"
    val priceText: String?,
    val scheduleText: String?,     // "11/26(목) 오후 3:00"
    val lessonId: Int?,
    val canAccept: Boolean,        // 채택 가능(마감·잠금 아님)
    val proposalWaiting: Boolean,  // 대기 중인 약속 제안이 있다(누가 보냈든)
    val proposalAwaitingMe: Boolean, // 내가 수락할 제안이 있다
    val contactRevealed: Boolean,
    val cyclesCount: Int,
    val hasQuote: Boolean,         // 예전 서버 추정용
)

object ChatDealBarResolver {

    fun resolve(i: DealBarInput): DealBarModel? {
        if (i.locked) return null
        val pair = i.pair ?: if (i.hasQuote) "MEMBER_TEACHER" else null
        val pro = i.myRole == "pro"
        val step = i.step ?: return null
        val actions: List<DealAction> = when (pair) {
            "MEMBER_TEACHER" -> if (pro) teacherActions(i, step) else memberActions(i, step)
            "PROPOSAL" -> if (pro) proposalTeacherActions(i, step) else proposalMemberActions(i, step)
            "ACADEMY_TEACHER" -> academyActions(i, step, pro)
            else -> return null
        }
        if (actions.isEmpty()) return null
        return DealBarModel(
            title = i.title?.takeIf { it.isNotBlank() } ?: if (pair == "ACADEMY_TEACHER") "학원 강사 제안" else "레슨",
            status = statusText(pair, step, pro),
            statusEmphasis = actions.firstOrNull() in setOf(DealAction.ACCEPT, DealAction.HIRE, DealAction.CHECK_PROPOSAL, DealAction.COMPLETE_LESSON, DealAction.REVIEW),
            detail = if (step == "SCHEDULED" || step == "DONE") i.scheduleText ?: i.priceText else i.priceText,
            actions = actions.take(3),
            cyclesCount = i.cyclesCount,
        )
    }

    private fun scheduleAction(i: DealBarInput): DealAction? = when {
        i.proposalAwaitingMe || i.proposalWaiting -> DealAction.CHECK_PROPOSAL
        else -> DealAction.SCHEDULE
    }

    private fun memberActions(i: DealBarInput, step: String): List<DealAction> = when (step) {
        "RESPONDED" -> listOfNotNull(DealAction.VIEW_QUOTES, DealAction.ACCEPT.takeIf { i.canAccept })
        "ACCEPTED" -> listOfNotNull(scheduleAction(i), DealAction.VIEW_QUOTES, DealAction.CALL.takeIf { i.contactRevealed })
        "SCHEDULED" -> listOfNotNull(DealAction.VIEW_LESSON.takeIf { i.lessonId != null }, DealAction.CALL.takeIf { i.contactRevealed })
        "DONE" -> listOfNotNull(DealAction.REVIEW, DealAction.VIEW_LESSON.takeIf { i.lessonId != null })
        else -> emptyList()
    }

    private fun teacherActions(i: DealBarInput, step: String): List<DealAction> = when (step) {
        "RESPONDED" -> listOf(DealAction.VIEW_MY_QUOTE)
        "ACCEPTED" -> listOfNotNull(scheduleAction(i), DealAction.CALL.takeIf { i.contactRevealed })
        "SCHEDULED" -> if (i.lessonId != null) listOfNotNull(DealAction.VIEW_LESSON, DealAction.COMPLETE_LESSON, DealAction.CALL.takeIf { i.contactRevealed }) else emptyList()
        "DONE" -> listOfNotNull(DealAction.VIEW_LESSON.takeIf { i.lessonId != null })
        else -> emptyList()
    }

    private fun proposalTeacherActions(i: DealBarInput, step: String): List<DealAction> = when (step) {
        "SCHEDULED", "DONE" -> teacherActions(i, step)
        else -> listOfNotNull(scheduleAction(i))
    }

    private fun proposalMemberActions(i: DealBarInput, step: String): List<DealAction> = when (step) {
        "SCHEDULED", "DONE" -> memberActions(i, step)
        else -> listOfNotNull(DealAction.CHECK_PROPOSAL.takeIf { i.proposalAwaitingMe || i.proposalWaiting })
    }

    private fun academyActions(i: DealBarInput, step: String, pro: Boolean): List<DealAction> {
        val hired = step != "RESPONDED"
        return when {
            !hired && !pro -> listOfNotNull(DealAction.VIEW_QUOTES, DealAction.HIRE.takeIf { i.canAccept })
            !hired && pro -> listOf(DealAction.ACADEMY_INFO)
            pro -> listOfNotNull(DealAction.ACADEMY_INFO, DealAction.CALL.takeIf { i.contactRevealed })
            else -> listOfNotNull(DealAction.CALL.takeIf { i.contactRevealed })
        }
    }

    private fun statusText(pair: String, step: String, pro: Boolean): String = when {
        pair == "ACADEMY_TEACHER" -> if (step == "RESPONDED") (if (pro) "제안 보냄" else "제안 도착") else "함께하기로 했어요"
        step == "RESPONDED" -> if (pro) "견적 보냄" else "제안 도착"
        step == "ACCEPTED" -> if (pair == "PROPOSAL") "약속 잡는 중" else "채택됨"
        step == "SCHEDULED" -> "레슨 예정"
        step == "DONE" -> "레슨 완료"
        else -> ""
    }
}
