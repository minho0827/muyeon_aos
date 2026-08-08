package com.muyeon.app.ui.lesson

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 레슨 캘린더 상태 — iOS `LessonCalendarViewModel.swift` + `LessonCalendarModels.swift` 이식.
 *  월 그리드(42칸) + 일정/개인일정 통합 칩 + 캘린더 색 필터.
 *
 * ⚠️ 성능: iOS 는 렌더마다 칩을 계산하다 렉이 나서 **캐시 재구축(rebuildDayCaches) 방식**으로 바꿨다.
 *   여기서도 일정/필터/캘린더가 바뀔 때 1회만 재계산하고, 그리드 렌더는 조회만 한다.
 * ⚠️ 그리드 셀의 key 는 UUID 가 아니라 **날짜 문자열** — UUID 를 쓰면 재렌더마다 42칸이
 *   전부 교체돼 잔상·재드로우가 생긴다(iOS LessonDay.id 주석과 동일 이유).
 */

/** 날짜 셀 일정 칩 — 제목(상대명)과 캘린더 색. */
data class DayChip(val title: String, val hex: String)

/** 월 그리드 한 칸. */
data class LessonDay(
    val day: Int,
    val ymd: String,          // yyyy-MM-dd (KST)
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSunday: Boolean,
    val hasLesson: Boolean,
    val chips: List<DayChip>, // 최대 2
    val moreCount: Int,       // "+N"
)

class LessonCalendarState(
    private val lessonApi: LessonApi,
    private val calendarApi: UserCalendarApi,
) : ViewModel() {

    var schedules by mutableStateOf<List<LessonSchedule>>(emptyList())
        private set
    var blocks by mutableStateOf<List<StudioBlock>>(emptyList())
        private set
    var calendars by mutableStateOf<List<UserCalendar>>(emptyList())
        private set
    var hiddenCalendarIds by mutableStateOf(setOf<Int>())
        private set

    /** 표시 중인 달의 1일(KST). */
    var monthAnchor by mutableStateOf(startOfMonthKST(System.currentTimeMillis()))
        private set
    var selectedYmd by mutableStateOf(todayYmdKST())
        private set
    var loading by mutableStateOf(true)
        private set

    /** ymd → (칩 최대 2, 나머지 수). 렌더는 이 캐시만 조회한다. */
    private var chipsByYmd: Map<String, Pair<List<DayChip>, Int>> = emptyMap()
    private var ymdSet: Set<String> = emptySet()

    val monthTitle: String get() = monthTitleFormatter.format(Date(monthAnchor))

    /** 선택한 날짜의 일정(시간순). */
    val selectedLessons: List<LessonSchedule>
        get() = schedules.filter { isVisible(it) && it.status != "CANCELED" && it.ymdKST == selectedYmd }
            .sortedBy { it.startMillis ?: Long.MAX_VALUE }

    val selectedBlocks: List<StudioBlock>
        get() = blocks.filter { it.date == selectedYmd && !hiddenCalendarIds.contains(it.calendarId ?: 0) }

    fun load() {
        viewModelScope.launch {
            loading = true
            calendarApi.list().onSuccess { calendars = it }
            // 일정은 표시 달 기준 넉넉히(-1 ~ +2개월)
            val from = ymdKST(addMonths(monthAnchor, -1))
            val to = ymdKST(addMonths(monthAnchor, 2))
            lessonApi.list(from, to).onSuccess { schedules = it }
            // 개인 일정은 광범위(-6 ~ +12개월). 실패 시 기존값 유지 — 캘린더가 사라지지 않게(iOS 규칙).
            calendarApi.schedule(ymdKST(addMonths(monthAnchor, -6)), ymdKST(addMonths(monthAnchor, 12)))
                .onSuccess { blocks = it }
            rebuildDayCaches()
            loading = false
        }
    }

    fun moveMonth(delta: Int) {
        monthAnchor = addMonths(monthAnchor, delta)
        load()
    }

    fun select(ymd: String) { selectedYmd = ymd }

    fun toggleCalendar(id: Int) {
        hiddenCalendarIds = if (hiddenCalendarIds.contains(id)) hiddenCalendarIds - id else hiddenCalendarIds + id
        rebuildDayCaches()
    }

    private fun isVisible(l: LessonSchedule) = !hiddenCalendarIds.contains(l.calendarId ?: 0)

    fun calendarOf(l: LessonSchedule): UserCalendar =
        calendars.firstOrNull { it.id == (l.calendarId ?: -1) } ?: UserCalendar.DEFAULT

    /**
     * 일정/필터/캘린더 변경 시 1회 호출 — 날짜별 칩·날짜 Set 재계산.
     *  레슨 + 개인 일정을 (정렬키, 칩)으로 통합. 종일 개인 일정이 맨 앞("00:00").
     */
    private fun rebuildDayCaches() {
        val groups = mutableMapOf<String, MutableList<Pair<String, DayChip>>>()

        schedules.filter { isVisible(it) && it.status != "CANCELED" }.forEach { l ->
            val ymd = l.ymdKST ?: return@forEach
            val title = l.partner.displayName.ifEmpty { l.serviceLabel }
            val key = l.startMillis?.let { kstHm.format(Date(it)) } ?: "99:99"
            groups.getOrPut(ymd) { mutableListOf() }.add(key to DayChip(title, calendarOf(l).color))
        }
        blocks.filter { !hiddenCalendarIds.contains(it.calendarId ?: 0) }.forEach { b ->
            val hex = (calendars.firstOrNull { it.id == b.calendarId } ?: UserCalendar.DEFAULT).color
            val key = if (b.allDay) "00:00" else (b.startTime ?: "00:01")
            groups.getOrPut(b.date) { mutableListOf() }.add(key to DayChip(b.title, hex))
        }

        chipsByYmd = groups.mapValues { (_, list) ->
            val sorted = list.sortedBy { it.first }
            sorted.take(2).map { it.second } to maxOf(0, sorted.size - 2)
        }
        ymdSet = groups.keys.toSet()
    }

    /** 월 그리드 42칸 — 일요일 시작. */
    fun days(): List<LessonDay> {
        val cal = kstCalendar().apply { timeInMillis = monthAnchor }
        val monthIndex = cal.get(Calendar.MONTH)
        // 그리드 시작 = 이 달 1일이 속한 주의 일요일
        cal.add(Calendar.DAY_OF_MONTH, -(cal.get(Calendar.DAY_OF_WEEK) - 1))
        val today = todayYmdKST()

        return (0 until 42).map { i ->
            val ymd = ymdKST(cal.timeInMillis)
            val cached = chipsByYmd[ymd]
            LessonDay(
                day = cal.get(Calendar.DAY_OF_MONTH),
                ymd = ymd,
                isCurrentMonth = cal.get(Calendar.MONTH) == monthIndex,
                isToday = ymd == today,
                isSunday = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY,
                hasLesson = ymdSet.contains(ymd),
                chips = cached?.first ?: emptyList(),
                moreCount = cached?.second ?: 0,
            ).also { cal.add(Calendar.DAY_OF_MONTH, 1) }
        }
    }

    companion object {
        fun todayYmdKST(): String = kstYmd.format(Date())
    }
}

// ============================================================
// KST 날짜 헬퍼 — 포매터는 최상위 단일 인스턴스(할당 비용 제거)
// ============================================================

private val KST: TimeZone = TimeZone.getTimeZone("Asia/Seoul")

internal val kstHm: SimpleDateFormat = SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = KST }

private val monthTitleFormatter: SimpleDateFormat =
    SimpleDateFormat("yyyy년 M월", Locale.KOREA).apply { timeZone = KST }

internal fun kstCalendar(): Calendar = Calendar.getInstance(KST)

internal fun ymdKST(millis: Long): String = kstYmd.format(Date(millis))

internal fun startOfMonthKST(millis: Long): Long = kstCalendar().apply {
    timeInMillis = millis
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

internal fun addMonths(millis: Long, delta: Int): Long =
    kstCalendar().apply { timeInMillis = millis; add(Calendar.MONTH, delta) }.timeInMillis
