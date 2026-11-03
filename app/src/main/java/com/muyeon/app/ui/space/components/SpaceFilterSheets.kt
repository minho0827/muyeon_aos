package com.muyeon.app.ui.space.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.*
import com.muyeon.app.ui.common.market.*
import com.muyeon.app.ui.transfer.TransferLabels
import kotlinx.coroutines.launch

/** 지역 시트 — 시도 칩 + [초기화][적용]. */
@Composable
fun SpaceRegionSheet(current: String?, onApply: (String?) -> Unit, onDismiss: () -> Unit) = FilterSheet("지역", onDismiss) {
    var pick by remember { mutableStateOf(current) }
    Column(Modifier.padding(horizontal = 20.dp)) {
        FormChipWrap(TransferLabels.regions.map { (it.second ?: "") to it.first }, { (pick ?: "") == it }) { pick = it.ifEmpty { null } }
    }
    SheetActions(onReset = { onApply(null); onDismiss() }, onApply = { onApply(pick); onDismiss() })
}

/** 날짜 시트 — 달력(오늘 이전 불가) + [초기화][적용]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceDateSheet(current: String?, onApply: (String?) -> Unit, onDismiss: () -> Unit) = FilterSheet("날짜", onDismiss) {
    val today = remember { MarketDates.todayKst() }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = MarketDates.toMillis(current),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = MarketDates.ymd(utcTimeMillis) >= today
        },
    )
    DatePicker(state, title = null, headline = null, showModeToggle = false)
    SheetActions(onReset = { onApply(null); onDismiss() }, onApply = { onApply(state.selectedDateMillis?.let { MarketDates.ymd(it) }); onDismiss() })
}

/** 인원 시트 — −/＋ + [초기화][적용]. */
@Composable
fun SpaceHeadcountSheet(current: Int?, onApply: (Int?) -> Unit, onDismiss: () -> Unit) = FilterSheet("인원", onDismiss) {
    var n by remember { mutableIntStateOf(current ?: 1) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleStep(Icons.Filled.Remove, n > 1) { n -= 1 }
        Text("${n}명", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MuyeonColors.textHead)
        CircleStep(Icons.Filled.Add, n < 300) { n += 1 }
    }
    SheetActions(onReset = { onApply(null); onDismiss() }, onApply = { onApply(n); onDismiss() })
}

/** 정렬 시트 — 추천순·가격 낮은순·가격 높은순(누르면 바로 적용). */
@Composable
fun SpaceSortSheet(current: String, onApply: (String) -> Unit, onDismiss: () -> Unit) = FilterSheet("정렬", onDismiss) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SPACE_SORTS.forEach { (k, label) -> RadioRow(label, current == k) { onApply(k); onDismiss() } }
    }
    Spacer(Modifier.height(16.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Text(
                title, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MuyeonColors.textHead,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            content()
        }
    }
}

@Composable
private fun CircleStep(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).alpha(if (enabled) 1f else 0.35f).clip(CircleShape).border(1.dp, MuyeonList.ink300, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        Alignment.Center,
    ) { Icon(icon, null, tint = MuyeonColors.textHead) }
}
