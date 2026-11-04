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

/** 내 예약 한 줄(버튼 없음). */
@Composable
fun MyBookingRow(r: SpaceReservationRow, onClick: () -> Unit) = ReservationCardFrame(r, bookerName = null, onClick = onClick)

/** 결제 대기 예약 — [결제하기] [예약 취소]. */
@Composable
fun MyPendingBookingRow(r: SpaceReservationRow, onClick: () -> Unit, onPay: () -> Unit, onCancel: () -> Unit) =
    ReservationCardFrame(r, bookerName = null, onClick = onClick) {
        ReservationActions {
            MarketRowButton("결제하기", Modifier.weight(1f), onClick = onPay)
            MarketRowButton("예약 취소", Modifier.weight(1f), danger = true, onClick = onCancel)
        }
    }

/** 호스트 응답 대기(결제 완료·수락 전) — "응답 대기" 배지 + "{기한}까지 호스트 응답". 취소는 상세 시트에서(전액). */
@Composable
fun MyAwaitingBookingRow(r: SpaceReservationRow, onClick: () -> Unit) =
    ReservationCardFrame(r, bookerName = null, onClick = onClick, statusLabel = r.bookerStatusLabel) {
        r.respondByText?.let { ReservationDeadlineLine("${it}까지 호스트 응답") }
    }
