package com.muyeon.app.ui.chat.dealbar

import com.muyeon.app.ui.chat.ChatSubContext

/**
 * ★ 2026-11-16 대타 카드 — '지금 무엇을 보여 줄지'만 정하는 순수 함수(화면 없음). 화면(SubContextCard)은 결과만 그린다.
 *  버튼은 서버가 준 할 수 있는 일(canConfirm·canWithdraw·canCancelConfirmed)만 켠다. 서버가 한 번 더 막는다.
 *   - 원장님: [지원서 보기][대타 확정] → 확정 뒤 [지원서 보기][확정 취소]
 *   - 지원자: [공고 보기][지원 취소] → 확정 뒤 [공고 보기][확정 취소](벌점 미리보기)
 *  잠긴 방은 보기 버튼만 남긴다.
 */
enum class SubAction(val label: String, val mutates: Boolean) {
    VIEW_APPLICATION("지원서 보기", false),
    CONFIRM("대타 확정", true),
    VIEW_POSTING("공고 보기", false),
    WITHDRAW("지원 취소", true),
    CANCEL_CONFIRMED("확정 취소", true),
}

/** 대타 건 한 줄(카드 안 한 칸). */
data class SubCardItem(
    val context: ChatSubContext,
    val title: String,
    val detail: String?,        // "11/20(목) · 19:00~20:00 · 60,000원"
    val status: String,
    val statusEmphasis: Boolean, // 내 차례(원장님 확정 대기)·확정됨이면 주황
    val actions: List<SubAction>,
)

/** 카드 전체 — 대표 한 건 + 나머지(여러 건이면 '진행 중 N건'으로 접는다). */
data class SubCardModel(val primary: SubCardItem, val items: List<SubCardItem>) {
    val count: Int get() = items.size
    val others: List<SubCardItem> get() = items.filter { it !== primary }
}

object SubContextResolver {

    fun resolve(contexts: List<ChatSubContext>, locked: Boolean): SubCardModel? {
        if (contexts.isEmpty()) return null
        val items = contexts.map { item(it, locked) }
        val primary = items.firstOrNull { i -> i.actions.any { it.mutates } }
            ?: items.firstOrNull { it.context.applicationStatus == "ACCEPTED" }
            ?: items.first()
        return SubCardModel(primary, items)
    }

    fun item(c: ChatSubContext, locked: Boolean): SubCardItem {
        val all = actions(c)
        return SubCardItem(
            context = c,
            title = c.title ?: "대타 공고",
            detail = detail(c),
            status = status(c),
            statusEmphasis = c.applicationStatus == "ACCEPTED" || (c.isAuthor && c.canConfirm),
            actions = if (locked) all.filterNot { it.mutates } else all,
        )
    }

    fun actions(c: ChatSubContext): List<SubAction> = if (c.isAuthor) listOfNotNull(
        SubAction.VIEW_APPLICATION.takeIf { c.postingId > 0 },
        SubAction.CONFIRM.takeIf { c.canConfirm },
        SubAction.CANCEL_CONFIRMED.takeIf { c.canCancelConfirmed },
    ) else listOfNotNull(
        SubAction.VIEW_POSTING.takeIf { c.postingId > 0 },
        SubAction.WITHDRAW.takeIf { c.canWithdraw && !c.canCancelConfirmed },
        SubAction.CANCEL_CONFIRMED.takeIf { c.canCancelConfirmed },
    )

    fun status(c: ChatSubContext): String = when (c.resultReason) {
        // 서버가 끝난 이유를 주면 그 말이 먼저다(다른 강사로 확정·마감 등은 지원 상태만으로 구분이 안 된다).
        "OTHER_CONFIRMED" -> "다른 강사로 확정"
        "REJECTED" -> "미선정"
        "CLOSED" -> "공고 마감"
        "WITHDRAWN" -> "지원 취소"
        "CANCELED" -> "확정 취소"
        else -> statusByApplication(c)
    }

    private fun statusByApplication(c: ChatSubContext): String = when (c.applicationStatus) {
        "ACCEPTED" -> "대타 확정"
        "CANCELED" -> if (c.confirmedAt != null) "확정 취소" else "지원 취소"
        "REJECTED" -> "미선정"
        else -> when {
            c.postingStatus == "CLOSED" || c.postingStatus == "ARCHIVED" -> "공고 마감"
            c.isAuthor -> "지원 도착"
            c.applicationStatus == "REVIEWING" -> "원장님 확인"
            else -> "지원함"
        }
    }

    /** 날짜 · 시간 · 금액 한 줄. 모두 없으면 null. */
    fun detail(c: ChatSubContext): String? =
        listOfNotNull(shortDate(c.classDate), c.classTime, c.pay?.let { won(it) })
            .joinToString(" · ").ifBlank { null }

    /** "2026-11-20" → "11/20(목)". 해석할 수 없으면 원문 그대로. */
    fun shortDate(raw: String?): String? {
        val s = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return runCatching {
            val d = java.time.LocalDate.parse(s.take(10))
            val dow = d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.KOREAN)
            "${d.monthValue}/${d.dayOfMonth}($dow)"
        }.getOrDefault(s)
    }

    private fun won(v: Int): String = String.format(java.util.Locale.KOREA, "%,d원", v)
}
