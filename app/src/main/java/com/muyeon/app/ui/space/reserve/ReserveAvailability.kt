package com.muyeon.app.ui.space.reserve

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
import com.muyeon.app.ui.space.SpaceAvailability
import kotlinx.coroutines.launch

/** 고른 날짜의 예약 가능 상태 — 화면은 이 중 하나만 그린다. */
sealed interface SlotState {
    data object Loading : SlotState
    data class Failed(val message: String) : SlotState
    data object Closed : SlotState
    data class Open(val a: SpaceAvailability) : SlotState
}

/** (불러오는 중, 오류, 결과) → 상태. 날짜를 아직 안 골랐으면 null. */
fun slotState(date: String?, loading: Boolean, error: String?, avail: SpaceAvailability?): SlotState? = when {
    date == null -> null
    loading -> SlotState.Loading
    error != null -> SlotState.Failed(error)
    avail == null -> null
    !avail.open -> SlotState.Closed
    else -> SlotState.Open(avail)
}

@Composable
fun ReserveSlotFailed(message: String) = Notice(message)

@Composable
fun ReserveSlotClosed() = Notice("이날은 예약할 수 없어요. 다른 날짜를 골라 주세요.")

/**
 * 가능 시간을 읽기 전, 공간 정보만으로 만든 "예상" 영업시간 — 서버 parseBusinessHours 와 같은 규칙
 *  ("09~23시", "10:00~22:00", 못 읽으면 하루 종일, 끝 ≤ 시작이면 자정까지). 칩 자리 높이를 미리 잡는 데만 쓴다.
 */
fun expectedAvailability(businessHours: String?, hourlyMinHours: Int?): SpaceAvailability {
    val m = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*시?\\s*[~\\-–]\\s*(\\d{1,2})(?::(\\d{2}))?").find(businessHours.orEmpty())
    var start = 0
    var end = 24 * 60
    if (m != null) {
        start = m.groupValues[1].toInt() * 60 + (m.groupValues[2].toIntOrNull() ?: 0)
        end = m.groupValues[3].toInt() * 60 + (m.groupValues[4].toIntOrNull() ?: 0)
        if (end <= start) end = 24 * 60
        start = start.coerceAtLeast(0); end = end.coerceAtMost(24 * 60)
    }
    return SpaceAvailability(open = true, start = start, end = end, minHours = (hourlyMinHours?.takeIf { it > 0 } ?: 1).toDouble(), blocked = emptyList())
}

/**
 * 시작 시간·이용 시간 칸 — 날짜를 고르기 전·읽는 중·쉬는 날·오류여도 늘 같은 높이.
 *  뒤에 예상 영업시간 기준 칩 배치를 투명하게 깔아 높이를 잡고, 그 위에 지금 상태 하나만 그린다.
 */
@Composable
fun ReserveSlotArea(
    expected: SpaceAvailability,
    slot: SlotState?,
    startMin: Int?,
    hours: Double,
    onPickStart: (SpaceAvailability, Int) -> Unit,
    onPickHours: (Double) -> Unit,
) {
    Box(Modifier.fillMaxWidth()) {
        ReserveSlotChips(expected, expected.minHours, null, hours, ChipMode.Reserve, {}, {})
        when (slot) {
            null -> Unit
            SlotState.Loading -> ReserveSlotChips(expected, expected.minHours, null, hours, ChipMode.Skeleton, {}, {})
            is SlotState.Failed -> ReserveSlotFailed(slot.message)
            SlotState.Closed -> ReserveSlotClosed()
            is SlotState.Open -> ReserveSlotChips(slot.a, slot.a.minHours, startMin, hours, ChipMode.Live, { onPickStart(slot.a, it) }, onPickHours)
        }
    }
}

@Composable
private fun ReserveSlotChips(a: SpaceAvailability, minHours: Double, startMin: Int?, hours: Double, mode: ChipMode, onStart: (Int) -> Unit, onHours: (Double) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        StartTimeChips(a, minHours, startMin, mode, onStart)
        DurationChips(a, minHours, startMin, hours, mode, onHours)
    }
}
