package com.muyeon.app.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteAvatar
import com.muyeon.app.ui.quote.QuoteNavBar
import java.util.Date

/**
 * 레슨 캘린더 — iOS `LessonCalendarView.swift` 이식.
 *  월 그리드(칩 최대 2 + "+N") + 캘린더 색 필터 + 선택일 아젠다.
 *  PENDING(날짜 미정) 배너로 강사의 '일정 확정 필요' 액션을 놓치지 않게 한다.
 */
private val WEEK_LABELS = listOf("일", "월", "화", "수", "목", "금", "토")

@Composable
fun LessonCalendarScreen(
    state: LessonCalendarState,
    onClose: () -> Unit,
    onOpenLesson: (Int) -> Unit,
    onManageCalendars: () -> Unit,
) {
    LaunchedEffect(Unit) { state.load() }

    val days = remember(state.monthAnchor, state.schedules, state.blocks, state.hiddenCalendarIds) { state.days() }
    val pending = state.schedules.filter { it.isPending && it.status != "CANCELED" }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "레슨 캘린더", onBack = onClose)

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // 월 이동
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.ChevronLeft, "이전 달", tint = MuyeonColors.textHead,
                    modifier = Modifier.size(28.dp).clickable { state.moveMonth(-1) },
                )
                Text(
                    state.monthTitle,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    lineHeight = 21.sp, color = MuyeonColors.textHead, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Filled.ChevronRight, "다음 달", tint = MuyeonColors.textHead,
                    modifier = Modifier.size(28.dp).clickable { state.moveMonth(1) },
                )
            }

            // 캘린더 색 필터 — 끄면 그리드/아젠다에서 제외
            if (state.calendars.isNotEmpty()) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    (listOf(UserCalendar.DEFAULT) + state.calendars).forEach { c ->
                        val on = !state.hiddenCalendarIds.contains(c.id)
                        Row(
                            Modifier.clip(RoundedCornerShape(50))
                                .background(if (on) c.uiColor.copy(alpha = 0.14f) else Color(0xFFF2F2F7))
                                .clickable { state.toggleCalendar(c.id) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(if (on) c.uiColor else MuyeonColors.secondary))
                            Text(
                                c.name,
                                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp,
                                color = if (on) MuyeonColors.textHead else MuyeonColors.secondary,
                            )
                        }
                    }
                    Text(
                        "관리",
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                        lineHeight = 15.sp, color = MuyeonColors.primary,
                        modifier = Modifier.clickable(onClick = onManageCalendars).padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }

            // 요일 헤더
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                WEEK_LABELS.forEachIndexed { i, w ->
                    Text(
                        w,
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
                        lineHeight = 14.sp, textAlign = TextAlign.Center,
                        color = if (i == 0) MuyeonColors.danger else MuyeonColors.textSub,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            if (state.loading) {
                Box(Modifier.fillMaxWidth().height(240.dp), Alignment.Center) {
                    CircularProgressIndicator(color = MuyeonColors.primary)
                }
            } else {
                // 월 그리드 6주 — key 는 날짜 문자열(UUID 금지: 재렌더마다 전체 교체돼 잔상)
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                    days.chunked(7).forEach { week ->
                        Row(Modifier.fillMaxWidth()) {
                            week.forEach { d -> DayCell(d, d.ymd == state.selectedYmd, Modifier.weight(1f)) { state.select(d.ymd) } }
                        }
                    }
                }
            }

            HorizontalDivider(Modifier.padding(top = 10.dp), color = MuyeonColors.border)

            // 날짜 미정 배너(강사 액션)
            if (pending.isNotEmpty()) {
                Text(
                    "일정 확정이 필요한 레슨 ${pending.size}건",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    lineHeight = 17.sp, color = MuyeonColors.primary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp).fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MuyeonColors.primary.copy(alpha = 0.08f))
                        .clickable { pending.firstOrNull()?.let { onOpenLesson(it.id) } }
                        .padding(12.dp),
                )
            }

            // 선택일 아젠다
            Column(Modifier.padding(horizontal = 20.dp).padding(top = 6.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    state.selectedYmd,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                )
                val lessons = state.selectedLessons
                val blocks = state.selectedBlocks
                if (lessons.isEmpty() && blocks.isEmpty()) {
                    Text(
                        "이 날 일정이 없어요.",
                        fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = MuyeonColors.textSub,
                    )
                }
                lessons.forEach { l -> AgendaRow(l, state.calendarOf(l)) { onOpenLesson(l.id) } }
                blocks.forEach { b ->
                    val cal = state.calendars.firstOrNull { it.id == b.calendarId } ?: UserCalendar.DEFAULT
                    BlockRow(b, cal)
                }
            }
        }
    }
}

@Composable
private fun DayCell(d: LessonDay, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .height(74.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MuyeonColors.primary.copy(alpha = 0.08f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            Modifier.size(20.dp).clip(CircleShape)
                .background(if (d.isToday) MuyeonColors.primary else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${d.day}",
                fontFamily = customFontFamily,
                fontWeight = if (d.isToday || selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp, lineHeight = 14.sp,
                color = when {
                    d.isToday -> Color.White
                    !d.isCurrentMonth -> MuyeonColors.chevron
                    d.isSunday -> MuyeonColors.danger
                    else -> MuyeonColors.textHead
                },
            )
        }
        d.chips.forEach { chip ->
            Text(
                chip.title,
                fontFamily = customFontFamily, fontSize = 8.sp, lineHeight = 11.sp,
                color = MuyeonColors.textHead, maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp))
                    .background(UserCalendar.hexToColor(chip.hex).copy(alpha = 0.18f))
                    .padding(horizontal = 2.dp, vertical = 1.dp),
            )
        }
        if (d.moreCount > 0) {
            Text(
                "+${d.moreCount}",
                fontFamily = customFontFamily, fontSize = 8.sp, lineHeight = 10.sp, color = MuyeonColors.secondary,
            )
        }
    }
}

@Composable
private fun AgendaRow(l: LessonSchedule, cal: UserCalendar, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).height(36.dp).clip(RoundedCornerShape(50)).background(cal.uiColor))
        QuoteAvatar(l.partner.image, l.partner.displayName, 32.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                l.partner.displayName,
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                lineHeight = 17.sp, color = MuyeonColors.textHead,
            )
            Text(
                listOfNotNull(
                    l.startMillis?.let { kstHm.format(Date(it)) },
                    l.serviceLabel,
                    l.place?.ifEmpty { null },
                ).joinToString(" · "),
                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp,
                color = MuyeonColors.textSub, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (l.isPending) {
            Text(
                "날짜 미정",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 12.sp,
                color = MuyeonColors.primary,
                modifier = Modifier.clip(RoundedCornerShape(50))
                    .background(MuyeonColors.primary.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun BlockRow(b: StudioBlock, cal: UserCalendar) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF7F7F7)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(3.dp).height(28.dp).clip(RoundedCornerShape(50)).background(cal.uiColor))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                b.title,
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                lineHeight = 17.sp, color = MuyeonColors.textHead,
            )
            Text(
                if (b.allDay) "종일" else listOfNotNull(b.startTime, b.endTime).joinToString(" ~ "),
                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp, color = MuyeonColors.textSub,
            )
        }
    }
}
