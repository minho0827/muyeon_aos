package com.muyeon.app.ui.space.booking

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
import com.muyeon.app.ui.space.SpaceReservationRow
import kotlinx.coroutines.launch

/** 호스트 예약 한 줄(예약자 이름). 줄을 누르면 예약 상세 시트. */
@Composable
fun HostReservationRow(r: SpaceReservationRow, onClick: () -> Unit) = ReservationCardFrame(r, bookerName = r.bookerName, onClick = onClick)

/** 결제 완료 예약 — [취소(전액 환불)]. */
@Composable
fun HostPaidReservationRow(r: SpaceReservationRow, onClick: () -> Unit, onCancel: () -> Unit) =
    ReservationCardFrame(r, bookerName = r.bookerName, onClick = onClick) {
        ReservationActions { MarketRowButton("취소(전액 환불)", Modifier.weight(1f), danger = true, onClick = onCancel) }
    }
