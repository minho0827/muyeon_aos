package com.muyeon.app.ui.quote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.common.MuyeonInfoRow

/**
 * ★ 2026-11-06 '지금 안 나가는 이유' — 서버 순서 그대로 한 줄씩(⚠ + 이유 + 고치러 가기).
 *  NO_TEMPLATE·NO_ACTIVE 는 아래 메시지 목록(빈 화면·라디오)이 그 자리에서 풀어 주므로 여기선 뺀다.
 */
private val LIST_HANDLED = setOf("NO_TEMPLATE", "NO_ACTIVE")

fun visibleAutoQuoteBlockers(blockers: List<AutoQuoteBlocker>): List<AutoQuoteBlocker> =
    blockers.filter { it.code !in LIST_HANDLED }

@Composable
fun AutoQuoteBlockerList(blockers: List<AutoQuoteBlocker>, onAction: (AutoQuoteBlocker) -> Unit) {
    if (blockers.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blockers.forEach { b ->
            MuyeonInfoRow(
                text = b.title,
                highlight = "⚠",
                detail = b.action?.label?.ifEmpty { null },
                onClick = { onAction(b) },
            )
        }
    }
}
