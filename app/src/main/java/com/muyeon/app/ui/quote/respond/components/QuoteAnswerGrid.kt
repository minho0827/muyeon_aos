package com.muyeon.app.ui.quote.respond.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/** 문진 답변 한 칸 — wide 면 한 줄을 다 쓴다(웹 .is-wide: 추가 요청 · 14자 넘는 답). */
data class QuoteAnswerCell(val label: String, val value: String, val wide: Boolean)

/**
 * 문진 답변 2열 — 웹 .quote-answer-grid(1fr 1fr · 세로 16 / 가로 12). dt 13 회색 · dd 14 medium.
 *  CSS grid 자동 배치와 같다: 좁은 칸은 두 개씩, 넓은 칸은 새 줄을 통째로(앞 칸이 하나 남으면 오른쪽은 비운다).
 */
@Composable
fun QuoteAnswerGrid(cells: List<QuoteAnswerCell>, modifier: Modifier = Modifier) {
    val rows = remember(cells) { gridRows(cells) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(ROW_GAP)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(COL_GAP)) {
                row.forEach { c -> AnswerCell(c, Modifier.weight(if (c.wide) 2f else 1f)) }
                if (row.size == 1 && !row[0].wide) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AnswerCell(c: QuoteAnswerCell, modifier: Modifier) {
    Column(modifier) {
        Text(c.label, style = AppTypography.Sub13, color = AppColors.TextSub)
        Text(c.value, style = AppTypography.Body14.copy(fontWeight = FontWeight.Medium), color = AppColors.TextHead, modifier = Modifier.padding(top = 2.dp))
    }
}

private fun gridRows(cells: List<QuoteAnswerCell>): List<List<QuoteAnswerCell>> {
    val out = mutableListOf<List<QuoteAnswerCell>>()
    var pending: QuoteAnswerCell? = null
    for (c in cells) {
        if (c.wide) {
            pending?.let { out.add(listOf(it)); pending = null }
            out.add(listOf(c))
        } else if (pending == null) {
            pending = c
        } else {
            out.add(listOf(pending!!, c)); pending = null
        }
    }
    pending?.let { out.add(listOf(it)) }
    return out
}

private val ROW_GAP = 16.dp
private val COL_GAP = 12.dp
