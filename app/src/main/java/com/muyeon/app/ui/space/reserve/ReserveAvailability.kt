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
fun ReserveSlotLoading() {
    Box(Modifier.fillMaxWidth().height(48.dp), Alignment.Center) { CircularProgressIndicator(color = com.muyeon.app.ui.space.SpaceDesign.primary, modifier = Modifier.size(24.dp)) }
}

@Composable
fun ReserveSlotFailed(message: String) = Notice(message)

@Composable
fun ReserveSlotClosed() = Notice("이날은 예약할 수 없어요. 다른 날짜를 골라 주세요.")
