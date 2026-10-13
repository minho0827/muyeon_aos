package com.muyeon.app.ui.resume

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import java.util.Calendar

/**
 * 이력서 기간 선택 — iOS `PeriodWheelPicker.swift` 의 `PeriodPickerRow` + `PeriodWheelPickerSheet` 1:1.
 *  "YYYY.MM ~ YYYY.MM" / "YYYY.MM ~ 현재" 문자열 계약은 [ResumePeriod.format] 그대로.
 */
@Composable
fun PeriodPickerRow(label: String, period: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable { open = true },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.textSub, modifier = Modifier.width(64.dp),
        )
        if (period.isEmpty()) {
            Text(
                "기간 선택",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                lineHeight = 17.sp, color = MuyeonColors.hint,
            )
        } else {
            Text(
                period,
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                lineHeight = 17.sp, color = MuyeonColors.textHead,
            )
            ResumePeriod.durationLabel(period)?.let { dur ->
                Text(
                    dur,
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                    lineHeight = 14.sp, color = MuyeonColors.primary,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Icon(Icons.Filled.KeyboardArrowDown, null, tint = MuyeonColors.hint, modifier = Modifier.size(14.dp))
    }
    if (open) {
        PeriodWheelPickerSheet(
            title = "$label 선택", period = period,
            onDone = { onChange(it); open = false }, onDismiss = { open = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodWheelPickerSheet(title: String, period: String, onDone: (String) -> Unit, onDismiss: () -> Unit) {
    val cal = remember { Calendar.getInstance() }
    val thisYear = cal.get(Calendar.YEAR)
    val thisMonth = cal.get(Calendar.MONTH) + 1
    val parsed = remember(period) { ResumePeriod.parse(period) }
    var startYear by remember { mutableIntStateOf(parsed?.sy ?: (thisYear - 1)) }
    var startMonth by remember { mutableIntStateOf(parsed?.sm ?: 3) }
    var isCurrent by remember { mutableStateOf(parsed != null && parsed.ey == null) }
    var endYear by remember { mutableIntStateOf(parsed?.ey ?: thisYear) }
    var endMonth by remember { mutableIntStateOf(parsed?.em ?: thisMonth) }

    val years = remember { (1980..thisYear).reversed().toList() }
    // 종료 휠은 시작 이후만 노출 — 시작보다 앞선 종료를 아예 고를 수 없게(자동 보정과 이중 안전)
    val endYears = years.filter { it >= startYear }
    val endMonths = if (endYear == startYear) (startMonth..12).toList() else (1..12).toList()

    // 시작을 바꾸면 종료가 그 이전이 되지 않게 자동으로 따라온다
    LaunchedEffect(startYear, startMonth, endYear) {
        if (endYear < startYear) endYear = startYear
        if (endYear == startYear && endMonth < startMonth) endMonth = startMonth
    }

    val draft = ResumePeriod.format(
        startYear, startMonth, if (isCurrent) null else endYear, if (isCurrent) null else endMonth,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MuyeonColors.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp)) {
            Box(Modifier.fillMaxWidth().height(44.dp)) {
                Text(
                    "취소",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                    color = MuyeonColors.primary,
                    modifier = Modifier.align(Alignment.CenterStart).clickable(onClick = onDismiss).padding(4.dp),
                )
                Text(
                    title,
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
                    color = MuyeonColors.textHead, modifier = Modifier.align(Alignment.Center),
                )
                Text(
                    "완료",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = MuyeonColors.primary,
                    modifier = Modifier.align(Alignment.CenterEnd).clickable { onDone(draft) }.padding(4.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                WheelRow("시작", years, startYear, (1..12).toList(), startMonth, enabled = true,
                    onYear = { startYear = it }, onMonth = { startMonth = it })
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp).clickable { isCurrent = !isCurrent },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "현재까지 (재직·재학 중)",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                        color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = isCurrent, onCheckedChange = { isCurrent = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = MuyeonColors.primary),
                    )
                }
                WheelRow("종료", endYears, endYear, endMonths, endMonth, enabled = !isCurrent,
                    onYear = { endYear = it }, onMonth = { endMonth = it })

                // 자동 계산 — 선택 즉시 몇 년/몇 개월인지 표시
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MuyeonColors.tileIdle)
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                ) {
                    Text(
                        draft,
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        color = MuyeonColors.textHead,
                    )
                    ResumePeriod.durationLabel(draft)?.let {
                        Text(
                            "· $it",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            color = MuyeonColors.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WheelRow(
    label: String,
    years: List<Int>, year: Int,
    months: List<Int>, month: Int,
    enabled: Boolean,
    onYear: (Int) -> Unit, onMonth: (Int) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(96.dp).alpha(if (enabled) 1f else 0.3f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            color = MuyeonColors.textSub, modifier = Modifier.width(40.dp),
        )
        WheelColumn(years, year, { "${it}년" }, enabled, onYear, Modifier.weight(1f))
        WheelColumn(months, month, { "${it}월" }, enabled, onMonth, Modifier.weight(1f))
    }
}

private val WHEEL_ITEM = 32.dp

/** 3행(96dp) 스냅 휠 — 가운데 행이 선택값. iOS `.pickerStyle(.wheel)` 대응. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    items: List<Int>,
    selected: Int,
    label: (Int) -> String,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = items.indexOf(selected).coerceAtLeast(0))
    val itemPx = with(LocalDensity.current) { WHEEL_ITEM.toPx() }
    val center by remember(items) {
        derivedStateOf {
            val idx = state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0
            idx.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        }
    }
    // 스크롤이 멈추면 가운데 값을 확정
    LaunchedEffect(state.isScrollInProgress, items) {
        if (!state.isScrollInProgress && items.isNotEmpty()) {
            val v = items[center]
            if (v != selected) onSelect(v)
        }
    }
    // 외부 보정(종료 clamp)·목록 변경 시 휠 위치 동기화
    LaunchedEffect(selected, items) {
        val idx = items.indexOf(selected)
        if (idx >= 0 && !state.isScrollInProgress && idx != center) state.scrollToItem(idx)
    }
    Box(modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp).height(WHEEL_ITEM)
                .clip(RoundedCornerShape(8.dp)).background(MuyeonColors.tileIdle),
        )
        LazyColumn(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
            userScrollEnabled = enabled,
            contentPadding = PaddingValues(vertical = WHEEL_ITEM),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(items) { i, v ->
                val on = i == center
                Box(Modifier.height(WHEEL_ITEM).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        label(v),
                        fontFamily = customFontFamily,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = if (on) 17.sp else 15.sp,
                        color = if (on) MuyeonColors.textHead else MuyeonColors.hint,
                    )
                }
            }
        }
    }
}
