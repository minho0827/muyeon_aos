package com.muyeon.app.ui.lesson

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * 원데이·체험 클래스 '진행 날짜' 여러 개 고르기(2026-10-23) — iOS LessonMultiDateCalendar · 웹 MultiDateCalendar 와 같은 규칙.
 *  오늘~6개월(183일) 안, 최대 30개. 지난 날짜는 회색으로 고를 수 없다. 고른 날은 주황 띠(테두리) 정원으로 표시.
 */
@Composable
internal fun LessonMultiDateCalendar(selected: List<String>, onToggle: (String) -> Unit) {
    val today = remember { LocalDate.now(ZoneId.of("Asia/Seoul")) }
    val last = remember { today.plusDays(LessonOptions.MAX_DAYS_AHEAD.toLong()) }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { month = month.minusMonths(1) }, enabled = month > YearMonth.from(today)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "이전 달")
            }
            Text("${month.year}년 ${month.monthValue}월", fontFamily = customFontFamily, fontWeight = FontWeight.Bold,
                fontSize = 16.sp, color = MuyeonColors.textHead, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            IconButton(onClick = { month = month.plusMonths(1) }, enabled = month < YearMonth.from(last)) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "다음 달")
            }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("일", "월", "화", "수", "목", "금", "토").forEach {
                Text(it, fontFamily = customFontFamily, fontSize = 12.sp, color = MuyeonColors.textSub,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        val lead = month.atDay(1).dayOfWeek.value % 7
        val cells: List<LocalDate?> = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                (0 until 7).forEach { i ->
                    val date = week.getOrNull(i)
                    Box(Modifier.weight(1f).height(44.dp), contentAlignment = Alignment.Center) {
                        if (date != null) {
                            val ymd = date.toString()
                            val on = ymd in selected
                            val enabled = !date.isBefore(today) && !date.isAfter(last)
                            Box(
                                Modifier.size(38.dp)
                                    .border(2.dp, if (on) MuyeonColors.primary else Color.Transparent, CircleShape)
                                    .clickable(enabled = enabled) { onToggle(ymd) }
                                    .semantics { contentDescription = if (on) "$ymd 선택됨" else ymd }
                                    .testTag("date.$ymd"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${date.dayOfMonth}",
                                    fontFamily = customFontFamily,
                                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = when {
                                        on -> MuyeonColors.primary
                                        enabled -> MuyeonColors.textHead
                                        else -> MuyeonColors.hint
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
