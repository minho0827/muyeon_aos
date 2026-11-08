package com.muyeon.app.ui.dancer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/**
 * 레슨 가능 시간 격자 — 웹 DancerDetail LessonTimeGrid(월~일 × 오전·오후·저녁). 고른 요일·시간대 칸만 주황.
 *   시간대가 비면 그 줄 전체, 요일이 비면 그 칸 전체를 켠다(웹과 같다).
 */
@Composable
fun LessonTimeGrid(days: List<String>, slots: List<String>) {
    val week = stringArrayResource(R.array.dancer_week_days)
    val rows = stringArrayResource(R.array.dancer_time_rows)
    val rowSlots = listOf(setOf("dawn", "morning"), setOf("noon", "afternoon"), setOf("evening", "night"))
    val selDays = days.map { it.take(1) }.toSet()
    val selSlots = slots.toSet()
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXS)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingXS), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(LABEL_W))
            week.forEach { Text(it, style = AppTypography.Sub13SemiBold, color = AppColors.TextBody, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
        rows.forEachIndexed { i, label ->
            val rowOn = selSlots.isEmpty() || rowSlots[i].any { it in selSlots }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingXS), verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = AppTypography.Sub13SemiBold, color = AppColors.TextBody, modifier = Modifier.width(LABEL_W))
                week.forEach { d ->
                    val on = rowOn && (selDays.isEmpty() || d in selDays)
                    Box(Modifier.weight(1f).aspectRatio(1.4f).background(if (on) AppColors.Primary else AppColors.SurfaceMuted, RoundedCornerShape(6.dp)))
                }
            }
        }
    }
}

private val LABEL_W = 36.dp
