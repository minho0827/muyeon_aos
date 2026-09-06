package com.muyeon.app.ui.lesson

import android.content.SharedPreferences
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

/** 아젠다 월 섹션 — iOS `AgendaSection`. */
data class AgendaSection(val key: String, val title: String, val lessons: List<LessonSchedule>)

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
    internal val calendarApi: UserCalendarApi,
    /** 배지 상태 저장 — iOS 는 UserDefaults. 계정 id 를 로컬에 안 두므로 키에 계정을 붙이지 않는다. */
    private val prefs: SharedPreferences? = null,
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
    /** 탭한 날짜(null = 미선택 → 월 아젠다). iOS 와 동일하게 처음엔 선택 없음. */
    var selectedYmd by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(true)
        private set
    /** 최초 로드 완료 — '일정 없음' 오인 방지(로딩 중에는 스켈레톤). */
    var didLoad by mutableStateOf(false)
        private set

    /** 달력 펼침/접힘 — 접히면 그리드를 감추고 아젠다가 화면 전체를 쓴다. */
    var calendarExpanded by mutableStateOf(true)
        private set

    /** 상세를 열어본 조율 중 id(파란 점 제거용). */
    var seenPendingIds by mutableStateOf(setOf<Int>())
        private set
    /** 지난 방문 이후 새로 들어온 예약(신규 배지). */
    var newBookingIds by mutableStateOf(setOf<Int>())
        private set
    private var baselineCaptured = false
    private var seenBaseline: Long? = null

    /** ymd → (칩 최대 2, 나머지 수). 렌더는 이 캐시만 조회한다. */
    private var chipsByYmd: Map<String, Pair<List<DayChip>, Int>> = emptyMap()
    private var ymdSet: Set<String> = emptySet()

    val monthTitle: String get() = monthTitleFormatter.format(Date(monthAnchor))

    /** 확정 일정(취소 포함 — 회색 표시). iOS `visibleScheduled`. */
    val visibleScheduled: List<LessonSchedule>
        get() = schedules.filter { !it.isPending && isVisible(it) }

    /**
     * 조율 중(날짜 미정) — iOS `visiblePending`.
     * 캘린더 미배정이라 '기본' 숨김에 딸려 사라지면 [일정 정하기] 진입로가 없어진다 → 칩 필터 예외.
     */
    val pending: List<LessonSchedule>
        get() = schedules.filter { it.isPending && it.status != "CANCELED" }

    /**
     * 리스트 모드 '예정' — (ymd, 일정들). 오늘 이후·비취소만, 날짜 오름차순.
     * iOS `listGroups`.
     */
    fun upcomingGroups(): List<Pair<String, List<LessonSchedule>>> {
        val today = todayYmdKST()
        return visibleScheduled
            .filter { it.status != "CANCELED" && (it.ymdKST ?: "") >= today }
            .groupBy { it.ymdKST!! }
            .toSortedMap()
            .map { (ymd, list) -> ymd to list.sortedBy { it.startMillis ?: Long.MAX_VALUE } }
    }

    /** 리스트 모드 '취소' — 최근 취소가 위(날짜 내림차순). iOS `canceledLessons`. */
    val canceledLessons: List<LessonSchedule>
        get() = visibleScheduled.filter { it.status == "CANCELED" }
            .sortedByDescending { it.ymdKST ?: "" }

    /** 아직 상세를 안 열어본 조율 중 건이 있는가(상태 탭 파란 점). */
    val hasUnseenPending: Boolean get() = pending.any { !seenPendingIds.contains(it.id) }

    /** 선택한 날짜의 일정(시간순, 취소 포함). */
    val selectedLessons: List<LessonSchedule>
        get() = visibleScheduled.filter { it.ymdKST == selectedYmd }
            .sortedBy { it.startMillis ?: Long.MAX_VALUE }

    val selectedBlocks: List<StudioBlock>
        get() = blocks.filter { it.date == selectedYmd && !hiddenCalendarIds.contains(it.calendarId ?: 0) }

    /** "8월 12일 (화)" — 선택일 아젠다 제목. */
    val selectedDayTitle: String
        get() = selectedYmd?.let { dayTitleFormatter.format(Date(millisOf(it))) } ?: ""

    /**
     * 월별 아젠다 — 표시 중인 달부터 일정이 있는 마지막 달까지. 빈 달도 섹션을 만들어
     * "일정이 없습니다"를 보여준다(iOS `agenda`). 최대 24개월에서 끊는다.
     */
    val agenda: List<AgendaSection>
        get() {
            val startYm = ymKST(monthAnchor)
            val maxYm = (listOf(startYm) + visibleScheduled.mapNotNull { it.ymKST }).max()
            val out = mutableListOf<AgendaSection>()
            var cursor = monthAnchor
            var guard = 0
            while (ymKST(cursor) <= maxYm && guard < 24) {
                val key = ymKST(cursor)
                out.add(
                    AgendaSection(
                        key = key,
                        title = monthTitleFormatter.format(Date(cursor)),
                        lessons = visibleScheduled.filter { it.ymKST == key }
                            .sortedBy { it.startMillis ?: Long.MAX_VALUE },
                    )
                )
                cursor = addMonths(cursor, 1)
                guard++
            }
            return out
        }

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
            restoreSeenPending()
            recomputeNewBookings()
            didLoad = true
            loading = false
        }
    }

    private fun restoreSeenPending() {
        if (prefs == null) return
        if (seenPendingIds.isEmpty()) {
            seenPendingIds = prefs.getStringSet(KEY_PENDING_SEEN, emptySet())
                ?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
        }
        // 조율이 끝난 id 는 정리(무한 누적 방지) — iOS syncPendingSeen 과 동일.
        val alive = pending.map { it.id }.toSet()
        val trimmed = seenPendingIds intersect alive
        if (trimmed != seenPendingIds) {
            seenPendingIds = trimmed
            prefs.edit().putStringSet(KEY_PENDING_SEEN, trimmed.map { it.toString() }.toSet()).apply()
        }
    }

    /**
     * 지난 방문 이후 생성된 예약(BOOKING)에 '신규' 배지.
     * 기준선은 세션 내내 고정하고(재로딩에도 배지 유지) 저장값만 '지금'으로 전진시켜
     * 다음 실행부터 이번에 본 예약이 신규에서 빠지게 한다(iOS recomputeNewBookings).
     */
    private fun recomputeNewBookings() {
        if (prefs == null) return
        if (!baselineCaptured) {
            baselineCaptured = true
            seenBaseline = prefs.getLong(KEY_LAST_SEEN_BOOKING, 0L).takeIf { it > 0L }
            prefs.edit().putLong(KEY_LAST_SEEN_BOOKING, System.currentTimeMillis()).apply()
        }
        val baseline = seenBaseline ?: run { newBookingIds = emptySet(); return } // 첫 실행은 기준선만
        newBookingIds = schedules
            .filter { it.isBooking && it.status != "CANCELED" && (it.createdMillis ?: 0L) > baseline }
            .map { it.id }.toSet()
    }

    fun moveMonth(delta: Int) {
        monthAnchor = addMonths(monthAnchor, delta)
        load()
    }

    /** 같은 날 재탭이면 선택 해제(iOS `selectDay`). 다른 달 셀이면 그 달로 이동. */
    fun select(ymd: String) {
        if (selectedYmd == ymd) { selectedYmd = null; return }
        selectedYmd = ymd
        if (ymKST(millisOf(ymd)) != ymKST(monthAnchor)) moveToMonthOf(ymd)
    }

    fun clearSelection() { selectedYmd = null }

    /** [오늘] 칩 — 이번 달로 돌아가고 선택 해제. */
    fun goToday() {
        val now = startOfMonthKST(System.currentTimeMillis())
        selectedYmd = null
        if (now != monthAnchor) { monthAnchor = now; load() }
    }

    private fun moveToMonthOf(ymd: String) {
        monthAnchor = startOfMonthKST(millisOf(ymd))
        load()
    }

    fun toggleCalendar() { calendarExpanded = !calendarExpanded }

    /** iOS `setCalendarExpanded` — 프로퍼티 setter 와 JVM 시그니처가 겹쳐 이름만 다르게. */
    fun updateCalendarExpanded(on: Boolean) {
        if (calendarExpanded == on) return
        calendarExpanded = on
    }

    /** 조율 중 상세를 열었을 때 — 파란 점에서 제외(기기 저장). */
    fun markPendingSeen(id: Int) {
        if (seenPendingIds.contains(id)) return
        seenPendingIds = seenPendingIds + id
        prefs?.edit()?.putStringSet(KEY_PENDING_SEEN, seenPendingIds.map { it.toString() }.toSet())?.apply()
    }

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
        private const val KEY_PENDING_SEEN = "muyeon.calendar.pendingSeen"
        private const val KEY_LAST_SEEN_BOOKING = "muyeon.calendar.lastSeenBookingAt"
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

private val dayTitleFormatter: SimpleDateFormat =
    SimpleDateFormat("M월 d일 (E)", Locale.KOREA).apply { timeZone = KST }

private val ymFormatter: SimpleDateFormat =
    SimpleDateFormat("yyyy-MM", Locale.US).apply { timeZone = KST }

internal fun ymKST(millis: Long): String = ymFormatter.format(Date(millis))

/** "yyyy-MM-dd" → millis(KST 자정). 파싱 실패 시 현재 시각. */
internal fun millisOf(ymd: String): Long = runCatching { kstYmd.parse(ymd)!!.time }.getOrDefault(System.currentTimeMillis())

internal fun addMonths(millis: Long, delta: Int): Long =
    kstCalendar().apply { timeInMillis = millis; add(Calendar.MONTH, delta) }.timeInMillis
