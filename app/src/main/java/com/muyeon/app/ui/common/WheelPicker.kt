package com.muyeon.app.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily

private val WHEEL_ITEM = 32.dp

/**
 * 스냅 휠 — 가운데 행이 선택값. iOS `.pickerStyle(.wheel)` 대응. 기본 3행(96dp), [visibleRows] 로 5행 등(홀수).
 *  이력서 기간 선택(ResumePeriodPicker)에서 쓰던 것을 2026-10-23 레슨 개설 휠(시간·정원·가격)과 함께 쓰려고 공용으로 옮겼다.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelColumn(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    enabled: Boolean,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    visibleRows: Int = 3,
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
    // 가운데 행 위아래 여백 = (보이는 행 - 1) / 2 행. 여백이 맞아야 맨 위 항목이 가운데 띠에 온다.
    Box(modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp).height(WHEEL_ITEM)
                .clip(RoundedCornerShape(8.dp)).background(MuyeonColors.tileIdle),
        )
        LazyColumn(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
            userScrollEnabled = enabled,
            contentPadding = PaddingValues(vertical = WHEEL_ITEM * ((visibleRows - 1) / 2)),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().testTag("wheel"),
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

/**
 * 값 하나를 휠로 고르는 바텀시트 — [취소 | 제목 | 완료]. 완료를 눌러야 반영된다(iOS MuyeonWheelSheet 와 같음).
 *  값이 선택지에 없으면(옛 데이터: 30분 수업 등) 그 값을 끼워 넣어 그대로 보인다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> WheelPickerSheet(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onDismiss: () -> Unit,
    onDone: (T) -> Unit,
) {
    val items = remember(options, selected) { if (selected in options) options else options + selected }
    var temp by remember { mutableStateOf(selected) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MuyeonColors.surface,
        dragHandle = null,
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("취소", fontFamily = customFontFamily, fontSize = 15.sp, color = MuyeonColors.secondary,
                    modifier = Modifier.clickable(onClick = onDismiss))
                Text(title, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("완료", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = MuyeonColors.primary, modifier = Modifier.clickable { onDone(temp) }.testTag("wheelDone"))
            }
            WheelColumn(items, temp, label, enabled = true, onSelect = { temp = it },
                modifier = Modifier.fillMaxWidth().height(WHEEL_ITEM * 5).padding(horizontal = 24.dp), visibleRows = 5)
            Box(Modifier.height(16.dp))
        }
    }
}

/** 휠 시트를 여는 값 칸 — 테두리 칸 안에 지금 값 + ⌄ (위저드 드롭다운 칸과 같은 모양). */
@Composable
fun <T> WheelField(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    onSelect: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
            .clickable { open = true }
            .semantics { contentDescription = "$title ${label(selected)}" }
            .testTag("wheelField.$title")
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label(selected), fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 18.sp,
            color = MuyeonColors.textHead, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.KeyboardArrowDown, null, tint = MuyeonColors.secondary, modifier = Modifier.size(16.dp))
    }
    if (open) {
        WheelPickerSheet(title, options, selected, label, onDismiss = { open = false }) { v ->
            open = false
            onSelect(v)
        }
    }
}
