package com.muyeon.app.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.market.SheetActions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/*
 * 목록 필터 시트(2026-11-06) — 웹·iOS 와 같은 계약.
 *  칩 줄 오른쪽 끝 필터 아이콘 → "필터" 시트(기간 프리셋 + 화면별 상태) → [초기화][적용]
 *  → 칩 줄 맨 앞에 적용한 조건마다 검은 칩 "최근 3개월 ×". × 는 그 조건만 기본값으로 돌린다.
 *  ⚠️ 조건은 화면을 떠나면 버린다(remember 로만 들고, 저장하지 않는다).
 *  ⚠️ 기간은 프리셋만 — 날짜 직접 고르기는 이번 범위가 아니다.
 */

/** 기간 프리셋. param = 서버 `period` 값(null = 보내지 않음 = 전체). */
enum class ListPeriod(val param: String?, val label: String, val months: Int) {
    ALL(null, "전체", 0),
    M1("1m", "1개월", 1),
    M3("3m", "3개월", 3),
    M6("6m", "6개월", 6);

    /** 적용 칩 문구. 전체는 칩을 만들지 않는다. */
    val chipLabel: String? get() = if (this == ALL) null else "최근 $label"
}

/** 상태 섹션 한 칸 — value 는 서버 `status` 값(화면별 계약 표). "전체"는 시트가 앞에 붙인다(value null). */
data class ListStatusOption(val value: String, val label: String)

/** 목록 필터 조건 {period, status}. status null = 전체. */
data class ListFilter(val period: ListPeriod = ListPeriod.ALL, val status: String? = null) {
    val isActive: Boolean get() = period != ListPeriod.ALL || status != null

    /** 서버 파라미터 — 기본값은 보내지 않는다. 상태 파라미터 이름·값이 다른 화면(알림 unreadOnly)은 [statusParam] 으로 바꾼다. */
    fun queryParams(statusParam: (String) -> Pair<String, String>? = { "status" to it }): List<Pair<String, String>> = buildList {
        period.param?.let { add("period" to it) }
        status?.let { s -> statusParam(s)?.let(::add) }
    }

    /** "period=3m&status=ACTIVE" 형태(앞에 & 없음). 비었으면 "". */
    fun queryString(statusParam: (String) -> Pair<String, String>? = { "status" to it }): String =
        queryParams(statusParam).joinToString("&") { (k, v) -> "$k=${java.net.URLEncoder.encode(v, "UTF-8")}" }

    /** 클라이언트에서 거르는 화면용 — createdAt 이 기간 안이면 true. 기간 전체이거나 날짜를 못 읽으면 true(숨기지 않는다). */
    fun withinPeriod(createdAt: String?, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (period == ListPeriod.ALL) return true
        val t = parseListDate(createdAt) ?: return true
        val since = Calendar.getInstance().apply { timeInMillis = nowMillis; add(Calendar.MONTH, -period.months) }.timeInMillis
        return t >= since
    }
}

private val LIST_DATE_PATTERNS = listOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX" to null,
    "yyyy-MM-dd'T'HH:mm:ssXXX" to null,
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" to "UTC",
    "yyyy-MM-dd'T'HH:mm:ss'Z'" to "UTC",
    "yyyy-MM-dd'T'HH:mm:ss" to "Asia/Seoul",
    "yyyy-MM-dd HH:mm:ss" to "Asia/Seoul",
    "yyyy-MM-dd" to "Asia/Seoul",
)

/** 서버 날짜 문자열 → epoch ms. minSdk 24 라 java.time 대신 SimpleDateFormat. */
fun parseListDate(s: String?): Long? {
    if (s.isNullOrBlank()) return null
    for ((pattern, tz) in LIST_DATE_PATTERNS) {
        val f = SimpleDateFormat(pattern, Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone(tz ?: "UTC")
        }
        val pos = java.text.ParsePosition(0)
        val d = runCatching { f.parse(s, pos) }.getOrNull()
        if (d != null && pos.index == s.length) return d.time
    }
    return null
}

/**
 * 적용 조건 → 칩 줄 맨 앞 알약들([MuyeonPillTabs] leadingPills). × 는 그 조건만 기본값으로 돌린다.
 *  기본값(전체)인 조건은 알약을 만들지 않는다.
 */
fun ListFilter.appliedPills(statusOptions: List<ListStatusOption>, onChange: (ListFilter) -> Unit): List<MuyeonRemovablePill> = buildList {
    period.chipLabel?.let { add(MuyeonRemovablePill("period", it) { onChange(copy(period = ListPeriod.ALL)) }) }
    status?.let { s ->
        val label = statusOptions.firstOrNull { it.value == s }?.label ?: s
        add(MuyeonRemovablePill("status", label) { onChange(copy(status = null)) })
    }
}

/**
 * 적용 조건 칩만 따로 한 줄로 그릴 때(탭 줄이 없는 화면). 탭 줄이 있으면 [appliedPills] 를 leadingPills 로 넘긴다.
 *  조건이 없으면 아무것도 그리지 않는다.
 */
@Composable
fun AppliedFilterChips(
    filter: ListFilter,
    statusOptions: List<ListStatusOption> = emptyList(),
    onChange: (ListFilter) -> Unit,
    tagPrefix: String? = null,
    trailing: (@Composable androidx.compose.foundation.layout.RowScope.() -> Unit)? = null,
) {
    val pills = filter.appliedPills(statusOptions, onChange)
    if (pills.isEmpty() && trailing == null) return
    MuyeonPillTabs<String>(
        emptyList(), "", {}, tagPrefix = tagPrefix, compact = true, leadingPills = pills, trailing = trailing,
    )
}

/**
 * 칩 줄 오른쪽 끝 필터 아이콘 — 양도양수 TransferRegionChips 의 Tune 버튼과 같은 모양.
 *  조건이 걸려 있으면 주황 + 작은 점.
 */
@Composable
fun ListFilterButton(active: Boolean, onClick: () -> Unit, tag: String = "list.filter") {
    Box(
        Modifier.padding(end = 8.dp).size(40.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
            .semantics { contentDescription = if (active) "$tag.active" else tag },
        Alignment.Center,
    ) {
        Icon(Icons.Filled.Tune, "필터", tint = if (active) MuyeonColors.primary else MuyeonColors.textHead, modifier = Modifier.size(22.dp))
        if (active) Box(Modifier.align(Alignment.TopEnd).offset(x = (-6).dp, y = 6.dp).size(6.dp).clip(CircleShape).background(MuyeonColors.primary))
    }
}

/**
 * "필터" 바텀시트 — 기간(전체·1개월·3개월·6개월) + 상태(statusOptions 가 있을 때만, 첫 칸 "전체").
 *  시트 안 선택은 임시값 — [적용]을 눌러야 [onApply]. 닫으면 버린다. [초기화]는 시트 안 선택만 기본값으로.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListFilterSheet(
    initial: ListFilter,
    statusOptions: List<ListStatusOption> = emptyList(),
    onApply: (ListFilter) -> Unit,
    onDismiss: () -> Unit,
    statusTitle: String = "상태",
) {
    var f by remember { mutableStateOf(initial) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Text(
                "필터", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MuyeonColors.textHead,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                FormField("기간") {
                    FormChipWrap(ListPeriod.entries.map { it.name to it.label }, { f.period.name == it }) { v ->
                        f = f.copy(period = ListPeriod.valueOf(v))
                    }
                }
                if (statusOptions.isNotEmpty()) {
                    FormField(statusTitle) {
                        FormChipWrap(listOf("" to "전체") + statusOptions.map { it.value to it.label }, { (f.status ?: "") == it }) { v ->
                            f = f.copy(status = v.ifEmpty { null })
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            SheetActions(onReset = { f = ListFilter() }, onApply = { onApply(f) })
        }
    }
}

/** 조건을 걸었는데 결과가 없을 때 — 기존 빈 화면 + [필터 초기화]. */
@Composable
fun ListFilterEmpty(onReset: () -> Unit, modifier: Modifier = Modifier) {
    MuyeonEmptyState("조건에 맞는 내역이 없어요", actionTitle = "필터 초기화", onAction = onReset, modifier = modifier)
}
