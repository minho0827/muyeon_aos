package com.muyeon.app.ui.lesson

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteAvatar
import com.muyeon.app.ui.quote.QuoteNavBar
import java.util.Date

/**
 * 레슨 캘린더 — iOS `LessonCalendarView.swift` 이식.
 *
 *  구조(iOS 와 동일): 상단 고정(오늘칩·월이동·캘린더필터·요일·그리드·화살표) + 하단 아젠다 독립 스크롤.
 *   ⚠️ 종전 AOS 는 전체가 하나의 verticalScroll 이라 '접기'가 의미를 갖지 못했다. 레이아웃 분리가 선행.
 *
 *  접기/펼치기 진입점 3개(iOS 그대로):
 *   1) 화살표 탭         → toggleCalendar()
 *   2) 화살표 줄 드래그  → 아래 25dp 초과 펼침 / 위 25dp 초과 접힘 (떼기 전 실시간)
 *   3) 아젠다 위로 드래그 → 펼침 상태에서만 접힘
 *  펼침 상태에서는 아젠다 스크롤을 끈다(iOS scrollDisabled) — 그래야 위로 끄는 동작이 접기로 잡힌다.
 *
 *  접히면: 그리드를 감추고 상태 필터 칩(전체/예정/조율 중/취소)이 뜨며 아젠다가 화면 전체를 쓴다.
 *  다시 펼치면 필터를 '전체'로 되돌린다 — 필터가 남으면 "일정이 사라졌다"는 오해가 생긴다(iOS 주석).
 *
 *  상단 우측 아이콘으로 월간 ↔ 리스트 전환(iOS 드롭다운 메뉴 대응).
 *   리스트 모드는 달력을 감추고 상태 탭(전체/예정/조율 중/취소)별 목록만 보여준다.
 *
 *  미이식(원본에 있음): 날짜 재탭 시 그날 시트, 개인 일정 추가(+), 딥링크 펄스, 미니맵.
 *   사유는 docs/lesson-calendar-port.md.
 */
private val WEEK_LABELS = listOf("일", "월", "화", "수", "목", "금", "토")

/** 접힘 상태에서만 노출되는 상태 필터 — iOS `ListStatusTab`. */
private enum class StatusTab(val label: String) {
    ALL("전체"), UPCOMING("예정"), PENDING("조율 중"), CANCELED("취소")
}

/** 드래그 판정 임계값 — iOS 25pt. */
private val FOLD_THRESHOLD = 25.dp

/** 보기 모드 — iOS `CalViewMode`. */
private enum class CalViewMode { MONTH, LIST }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonCalendarScreen(
    state: LessonCalendarState,
    onClose: () -> Unit,
    onOpenLesson: (Int) -> Unit,
    onManageCalendars: () -> Unit,
    onOpenChat: (Int) -> Unit = {},
) {
    LaunchedEffect(Unit) { state.load() }

    var refreshing by remember { mutableStateOf(false) }
    val refreshScope = rememberCoroutineScope()

    val days = remember(state.monthAnchor, state.schedules, state.blocks, state.hiddenCalendarIds) { state.days() }
    var statusTab by remember { mutableStateOf(StatusTab.ALL) }
    var viewMode by remember { mutableStateOf(CalViewMode.MONTH) }
    val expanded = state.calendarExpanded

    // 다시 펼치면 필터 초기화 — 필터가 남은 채 칩이 숨으면 "일정이 사라졌다"는 오해가 생긴다.
    // 월간 모드에서만 초기화 — 리스트는 상태 탭이 상시 노출이라 유지한다(iOS 와 동일).
    LaunchedEffect(expanded, viewMode) {
        if (expanded && viewMode == CalViewMode.MONTH && statusTab != StatusTab.ALL) statusTab = StatusTab.ALL
    }

    val density = LocalDensity.current
    val thresholdPx = with(density) { FOLD_THRESHOLD.toPx() }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(
            title = "레슨 일정",
            onBack = onClose,
            trailing = {
                Icon(
                    if (viewMode == CalViewMode.MONTH) Icons.Filled.CalendarMonth else Icons.Filled.FormatListBulleted,
                    if (viewMode == CalViewMode.MONTH) "리스트 보기로 전환" else "월간 보기로 전환",
                    tint = MuyeonColors.textHead,
                    modifier = Modifier.size(40.dp).clickable {
                        viewMode = if (viewMode == CalViewMode.MONTH) CalViewMode.LIST else CalViewMode.MONTH
                    }.padding(9.dp),
                )
            },
        )

        // 리스트 모드 — 달력 없이 상태 탭 + 목록만(iOS viewMode == .list).
        if (viewMode == CalViewMode.LIST) {
            HorizontalDivider(color = MuyeonColors.border)
            StatusChipsRow(statusTab, state.hasUnseenPending) { statusTab = it }
            HorizontalDivider(color = MuyeonColors.border)
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { refreshScope.launch { refreshing = true; state.load(); refreshing = false } },
                modifier = Modifier.weight(1f),
            ) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
                ) {
                    when {
                        state.loading && !state.didLoad -> LoadingAgenda()
                        else -> ListBody(state, statusTab, onOpenLesson, onOpenChat) { statusTab = it }
                    }
                }
            }
            return@Column
        }

        // ── [오늘] + < 월 > ──
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "오늘",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                lineHeight = 17.sp, color = MuyeonColors.textHead,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(MuyeonColors.groupedBg)
                    .clickable { state.goToday() }.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(
                Modifier.weight(1f), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.ChevronLeft, "이전 달", tint = MuyeonColors.textHead,
                    modifier = Modifier.size(26.dp).clickable { state.moveMonth(-1) },
                )
                Text(
                    state.monthTitle,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    lineHeight = 21.sp, color = MuyeonColors.textHead,
                    modifier = Modifier.padding(horizontal = 14.dp),
                )
                Icon(
                    Icons.Filled.ChevronRight, "다음 달", tint = MuyeonColors.textHead,
                    modifier = Modifier.size(26.dp).clickable { state.moveMonth(1) },
                )
            }
            Spacer(Modifier.width(56.dp)) // 오늘 칩과 좌우 균형(iOS Color.clear 56pt)
        }

        // ── 캘린더 색 필터 — 끄면 그리드/아젠다에서 제외 ──
        if (state.calendars.isNotEmpty()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                (listOf(UserCalendar.DEFAULT) + state.calendars).forEach { c ->
                    val on = !state.hiddenCalendarIds.contains(c.id)
                    Row(
                        Modifier.clip(RoundedCornerShape(50))
                            .background(if (on) c.uiColor.copy(alpha = 0.14f) else MuyeonColors.groupedBg)
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

        // ── 요일 헤더 + 월 그리드 (접히면 사라짐) ──
        AnimatedVisibility(visible = expanded, enter = androidx.compose.animation.expandVertically(tween(200)),
            exit = androidx.compose.animation.shrinkVertically(tween(200))) {
            Column {
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
                if (state.loading && !state.didLoad) {
                    Box(Modifier.fillMaxWidth().height(240.dp), Alignment.Center) {
                        CircularProgressIndicator(color = MuyeonColors.primary)
                    }
                } else {
                    // 6주 42칸 — key 는 날짜 문자열(UUID 금지: 재렌더마다 전체 교체돼 잔상)
                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        days.chunked(7).forEach { week ->
                            Row(Modifier.fillMaxWidth()) {
                                week.forEach { d ->
                                    DayCell(d, d.ymd == state.selectedYmd, Modifier.weight(1f)) { state.select(d.ymd) }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 화살표 줄 — 탭 토글 + 드래그(실시간) ──
        Box(
            Modifier.fillMaxWidth()
                .clickable { state.toggleCalendar() }
                .pointerInput(expanded) {
                    var acc = 0f
                    detectVerticalDragGestures(
                        onDragStart = { acc = 0f },
                        onDragEnd = { acc = 0f },
                    ) { _, dragAmount ->
                        acc += dragAmount
                        if (acc > thresholdPx && !state.calendarExpanded) state.updateCalendarExpanded(true)
                        else if (acc < -thresholdPx && state.calendarExpanded) state.updateCalendarExpanded(false)
                    }
                }
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                if (expanded) "달력 접기" else "달력 펼치기",
                tint = MuyeonColors.chevron, modifier = Modifier.size(24.dp),
            )
        }
        HorizontalDivider(color = MuyeonColors.border)

        // ── 상태 필터 칩 — 달력이 접혀 아젠다가 전체를 쓸 때만(iOS 와 동일) ──
        if (!expanded) {
            StatusChipsRow(statusTab, state.hasUnseenPending) { statusTab = it }
            HorizontalDivider(color = MuyeonColors.border)
        }

        // ── 아젠다 — 펼침 상태에선 스크롤 OFF + 위로 끌면 접힘 ──
        val agendaScroll = rememberScrollState()
        Column(
            Modifier.weight(1f)
                .verticalScroll(agendaScroll, enabled = !expanded)
                // ⚠️ 드래그 감지기는 **펼침 상태에서만** 붙인다. 항상 붙여두면 detectVerticalDragGestures
                //   가 이벤트를 소비해 접힘 상태의 아젠다 스크롤이 먹통이 된다.
                .then(
                    if (expanded) Modifier.pointerInput(Unit) {
                        var acc = 0f
                        detectVerticalDragGestures(
                            onDragStart = { acc = 0f },
                            onDragEnd = { acc = 0f },
                        ) { _, dragAmount ->
                            acc += dragAmount
                            if (acc < -thresholdPx) state.updateCalendarExpanded(false)
                        }
                    } else Modifier
                )
                .padding(bottom = 24.dp),
        ) {
            when {
                state.loading && !state.didLoad -> LoadingAgenda()
                state.selectedYmd != null -> SelectedDayAgenda(state, onOpenLesson, onOpenChat)
                else -> MonthAgenda(state, statusTab, onOpenLesson, onOpenChat)
            }
        }
    }
}

// ── 선택일 아젠다: 그 날 일정 + [전체 보기] ──
@Composable
private fun SelectedDayAgenda(
    state: LessonCalendarState,
    onOpenLesson: (Int) -> Unit,
    onOpenChat: (Int) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            state.selectedDayTitle,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            lineHeight = 17.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
        )
        Text(
            "전체 보기",
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.primary,
            modifier = Modifier.clickable { state.clearSelection() },
        )
    }
    val lessons = state.selectedLessons
    val blocks = state.selectedBlocks
    if (lessons.isEmpty() && blocks.isEmpty()) {
        EmptyRow("이 날은 일정이 없습니다.")
    } else {
        lessons.forEach { LessonCard(it, state, pending = false, onOpenLesson = onOpenLesson, onOpenChat = onOpenChat) }
        blocks.forEach { b ->
            val cal = state.calendars.firstOrNull { it.id == b.calendarId } ?: UserCalendar.DEFAULT
            Box(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) { BlockRow(b, cal) }
        }
    }
}

// ── 기본 아젠다: 조율 중 + 월별 섹션 ──
@Composable
private fun MonthAgenda(
    state: LessonCalendarState,
    tab: StatusTab,
    onOpenLesson: (Int) -> Unit,
    onOpenChat: (Int) -> Unit,
) {
    if (tab == StatusTab.ALL || tab == StatusTab.PENDING) {
        if (state.pending.isNotEmpty()) {
            SectionHeader("일정 조율 중")
            state.pending.forEach {
                LessonCard(it, state, pending = true, onOpenLesson = onOpenLesson, onOpenChat = onOpenChat)
            }
        } else if (tab == StatusTab.PENDING) {
            EmptyRow("조율 중인 레슨이 없습니다.")
        }
    }
    if (tab == StatusTab.PENDING) return

    state.agenda.forEach { section ->
        val items = when (tab) {
            StatusTab.UPCOMING -> section.lessons.filter { it.status == "SCHEDULED" }
            StatusTab.CANCELED -> section.lessons.filter { it.status == "CANCELED" }
            else -> section.lessons
        }
        if (tab == StatusTab.ALL) {
            SectionHeader(section.title)
            if (items.isEmpty()) EmptyRow("일정이 없습니다.")
            else items.forEach { LessonCard(it, state, false, onOpenLesson, onOpenChat) }
        } else if (items.isNotEmpty()) {
            // 필터 탭은 빈 달 섹션을 생략한다(소음 방지) — iOS 와 동일.
            SectionHeader(section.title)
            items.forEach { LessonCard(it, state, false, onOpenLesson, onOpenChat) }
        }
    }
}

/** 상태 필터 칩 줄 — 접힘(월간) / 리스트 모드 공용. iOS `statusChipsRow`. */
@Composable
private fun StatusChipsRow(current: StatusTab, hasUnseenPending: Boolean, onSelect: (StatusTab) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusTab.entries.forEach { tab ->
            val selected = current == tab
            Row(
                Modifier.clip(RoundedCornerShape(50))
                    .background(if (selected) MuyeonColors.primary else MuyeonColors.groupedBg)
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    tab.label,
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    lineHeight = 16.sp, color = if (selected) Color.White else MuyeonColors.textHead,
                )
                // 숫자 대신 파란 점 — 아직 상세를 안 열어본 조율 중 건이 있을 때만.
                if (tab == StatusTab.PENDING && hasUnseenPending) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(MuyeonColors.info))
                }
            }
        }
    }
}

/**
 * 리스트 모드 본문 — iOS `listBody` 4종.
 *  전체: 조율 중 최대 2건(+모두 보기) → 날짜별 예정. 취소는 소음 방지로 제외(취소 탭 전용).
 *  예정: 오늘 이후 확정 일정을 날짜별로. 조율: 전량. 취소: 최근 취소가 위(날짜 내림차순).
 */
@Composable
private fun ListBody(
    state: LessonCalendarState,
    tab: StatusTab,
    onOpenLesson: (Int) -> Unit,
    onOpenChat: (Int) -> Unit,
    onSelectTab: (StatusTab) -> Unit,
) {
    val groups = remember(state.schedules, state.hiddenCalendarIds) { state.upcomingGroups() }
    when (tab) {
        StatusTab.ALL -> {
            if (state.pending.isNotEmpty()) {
                SectionHeader("일정 조율 중")
                state.pending.take(2).forEach { LessonCard(it, state, true, onOpenLesson, onOpenChat) }
                if (state.pending.size > 2) {
                    Text(
                        "${state.pending.size}건 모두 보기 →",
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                        lineHeight = 16.sp, color = MuyeonColors.primary,
                        modifier = Modifier.clickable { onSelectTab(StatusTab.PENDING) }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }
            if (groups.isEmpty() && state.pending.isEmpty() && state.didLoad) {
                ListEmpty("예정된 일정이 없습니다.")
            }
            groups.forEach { (ymd, lessons) ->
                SectionHeader(listDateTitle(ymd))
                lessons.forEach { LessonCard(it, state, false, onOpenLesson, onOpenChat) }
            }
        }
        StatusTab.UPCOMING -> {
            if (groups.isEmpty() && state.didLoad) ListEmpty("예정된 레슨이 없습니다.")
            groups.forEach { (ymd, lessons) ->
                SectionHeader(listDateTitle(ymd))
                lessons.forEach { LessonCard(it, state, false, onOpenLesson, onOpenChat) }
            }
        }
        StatusTab.PENDING -> {
            if (state.pending.isEmpty() && state.didLoad) ListEmpty("조율 중인 레슨이 없습니다.")
            else {
                SectionHeader("일정 조율 중 ${state.pending.size}건")
                state.pending.forEach { LessonCard(it, state, true, onOpenLesson, onOpenChat) }
            }
        }
        StatusTab.CANCELED -> {
            val items = state.canceledLessons
            if (items.isEmpty() && state.didLoad) ListEmpty("취소된 레슨이 없습니다.")
            else {
                SectionHeader("취소된 레슨 ${items.size}건")
                items.forEach { LessonCard(it, state, false, onOpenLesson, onOpenChat) }
            }
        }
    }
}

@Composable
private fun ListEmpty(t: String) {
    Text(
        t,
        fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 18.sp, color = MuyeonColors.textSub,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
    )
}

/** "2026-08-12" → "2026년 8월 12일 (화)" — iOS `listDateTitle`. */
private fun listDateTitle(ymd: String): String =
    runCatching { listTitleFormatter.format(Date(millisOf(ymd))) }.getOrDefault(ymd)

@Composable
private fun SectionHeader(t: String) {
    Text(
        t,
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 17.sp,
        color = MuyeonColors.textHead,
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
    )
}

@Composable
private fun EmptyRow(t: String) {
    Text(
        t,
        fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 18.sp, color = MuyeonColors.textSub,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
    )
}

// ── 최초 로드 스켈레톤 — '일정 없음' 오인 방지(iOS loadingAgenda) ──
@Composable
private fun LoadingAgenda() {
    Row(
        Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(color = MuyeonColors.primary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Text(
            "레슨 일정을 불러오는 중이에요",
            fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 17.sp, color = MuyeonColors.textSub,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
    repeat(2) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp)).background(MuyeonColors.groupedBg).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SkeletonBar(96.dp, 13.dp)
            SkeletonBar(150.dp, 22.dp)
            SkeletonBar(180.dp, 15.dp)
        }
    }
}

@Composable
private fun SkeletonBar(w: androidx.compose.ui.unit.Dp, h: androidx.compose.ui.unit.Dp) {
    Box(Modifier.width(w).height(h).clip(RoundedCornerShape(h / 3)).background(MuyeonColors.placeholder))
}

/**
 * 일정 카드 — iOS `lessonCard`.
 *  상대 라인 → 날짜(+취소/완료/신규 배지) → 컬러바+과목·장소+시간 → 액션(일정 정하기·채팅).
 *  ⚠️ [일정 정하기/수정] 은 iOS 의 전용 편집 시트 대신 **상세 화면**으로 보낸다 —
 *    AOS 상세가 이미 일정 확정·취소를 담당한다(죽은 버튼 금지).
 */
@Composable
private fun LessonCard(
    l: LessonSchedule,
    state: LessonCalendarState,
    pending: Boolean,
    onOpenLesson: (Int) -> Unit,
    onOpenChat: (Int) -> Unit,
) {
    val isCanceled = l.status == "CANCELED"
    val isNew = !isCanceled && state.newBookingIds.contains(l.id)
    val barColor = if (isCanceled) MuyeonColors.chevron else state.calendarOf(l).uiColor

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isNew) MuyeonColors.primary.copy(alpha = 0.06f) else MuyeonColors.groupedBg)
            .then(
                if (isNew) Modifier.border(1.dp, MuyeonColors.primary, RoundedCornerShape(16.dp))
                else Modifier.border(1.dp, MuyeonColors.border, RoundedCornerShape(16.dp))
            )
            .clickable {
                if (pending) state.markPendingSeen(l.id)  // 조율 중 확인 → 파란 점 제거
                onOpenLesson(l.id)
            }
            .padding(16.dp),
    ) {
        // 상대 라인
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            QuoteAvatar(l.partner.image, l.partner.displayName, 20.dp)
            Text(
                (if (l.iAmTeacher) "고객 " else "강사 ") + l.partner.displayName,
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.textSub,
            )
        }
        // 날짜 + 배지
        Row(
            Modifier.padding(top = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                l.startMillis?.let { cardDateFormatter.format(Date(it)) } ?: "날짜 미정",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp,
                color = if (isCanceled) MuyeonColors.textSub else MuyeonColors.textHead,
                textDecoration = if (isCanceled) TextDecoration.LineThrough else null,
            )
            when {
                isCanceled -> Badge("취소됨", MuyeonColors.placeholder, MuyeonColors.secondary)
                l.status == "DONE" -> Badge("완료", MuyeonColors.primary, Color.White)
            }
            if (isNew) Badge(if (l.isBooking) "신규 예약" else "새 일정", MuyeonColors.primary, Color.White)
        }
        // 컬러바 + 내용
        Row(
            Modifier.padding(top = 10.dp).heightIn(min = 34.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.width(3.dp).heightIn(min = 34.dp).clip(RoundedCornerShape(2.dp)).background(barColor))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    l.serviceLabel + (l.place?.takeIf { it.isNotEmpty() }?.let { " · $it" } ?: ""),
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    lineHeight = 18.sp,
                    color = if (isCanceled) MuyeonColors.textSub else MuyeonColors.textHead,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
                val sub = when {
                    isCanceled -> "취소된 일정이에요"
                    pending -> if (l.iAmTeacher) "날짜 미정 — 일정을 정해주세요" else "강사가 일정을 정할 예정이에요"
                    else -> l.startMillis?.let { cardTimeFormatter.format(Date(it)) } ?: ""
                }
                if (sub.isNotEmpty()) {
                    Text(
                        sub,
                        fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp,
                        color = if (pending && !isCanceled) MuyeonColors.primary else MuyeonColors.textSub,
                    )
                }
            }
        }
        // 액션 — 취소된 일정엔 노출하지 않는다(iOS 와 동일).
        if (!isCanceled) {
            val canSchedule = l.iAmTeacher && !l.isBooking && l.status != "DONE"
            val roomId = l.roomId
            if (canSchedule || roomId != null) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (canSchedule) {
                        Text(
                            if (pending) "일정 정하기" else "일정 수정",
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                            lineHeight = 16.sp, color = Color.White, textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .background(MuyeonColors.primary)
                                .clickable { state.markPendingSeen(l.id); onOpenLesson(l.id) }
                                .padding(vertical = 9.dp),
                        )
                    }
                    if (roomId != null) {
                        Text(
                            "채팅",
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                            lineHeight = 16.sp, color = MuyeonColors.primary, textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .border(1.dp, MuyeonColors.primary, RoundedCornerShape(10.dp))
                                .clickable { onOpenChat(roomId) }
                                .padding(vertical = 9.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, bg: Color, fg: Color) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 14.sp,
        color = fg,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun DayCell(d: LessonDay, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .height(74.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // ★ 원 유무와 무관하게 항상 같은 크기 — 원 없는 셀에서 숫자 기준선이 어긋나던 문제(iOS 주석).
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(
                when {
                    selected -> MuyeonColors.primary
                    d.isToday && d.isCurrentMonth -> MuyeonColors.primary.copy(alpha = 0.16f)
                    else -> Color.Transparent
                }
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${d.day}",
                fontFamily = customFontFamily,
                fontWeight = if (d.isToday || selected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp, lineHeight = 14.sp,
                color = when {
                    selected -> Color.White
                    !d.isCurrentMonth -> MuyeonColors.chevron
                    d.isToday -> MuyeonColors.primary
                    d.isSunday -> MuyeonColors.danger
                    else -> MuyeonColors.textHead
                },
            )
        }
        d.chips.forEach { chip ->
            Text(
                chip.title,
                fontFamily = customFontFamily, fontSize = 8.sp, lineHeight = 11.sp,
                color = UserCalendar.hexToColor(chip.hex), maxLines = 1, overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp))
                    .background(UserCalendar.hexToColor(chip.hex).copy(alpha = if (d.isCurrentMonth) 0.18f else 0.08f))
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
private fun BlockRow(b: StudioBlock, cal: UserCalendar) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(MuyeonColors.groupedBg).padding(12.dp),
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

private val listTitleFormatter: java.text.SimpleDateFormat =
    java.text.SimpleDateFormat("yyyy년 M월 d일 (E)", java.util.Locale.KOREA)
        .apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul") }

private val cardDateFormatter: java.text.SimpleDateFormat =
    java.text.SimpleDateFormat("M월 d일 (E)", java.util.Locale.KOREA)
        .apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul") }

private val cardTimeFormatter: java.text.SimpleDateFormat =
    java.text.SimpleDateFormat("a h:mm", java.util.Locale.KOREA)
        .apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul") }
