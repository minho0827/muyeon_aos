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
import com.muyeon.app.ui.space.SpaceDesign
import com.muyeon.app.ui.space.SpaceReservationRow
import kotlinx.coroutines.launch

/** 예약자 취소 확인 — 결제 대기면 안내, 결제됐으면 "환불 예정 금액 N원(R%)". */
@Composable
fun CancelConfirmDialog(r: SpaceReservationRow, rate: Int, refund: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    MuyeonDialog(
        title = "예약을 취소할까요?",
        message = if (r.status == "PENDING_PAYMENT") "아직 결제하지 않은 예약이에요." else "환불 예정 금액 ${SpaceDesign.won(refund)}원(${rate}%)",
        actions = listOf(
            MuyeonDialogAction("예약 취소", MuyeonDialogKind.DESTRUCTIVE) { onConfirm() },
            MuyeonDialogAction("닫기", MuyeonDialogKind.CANCEL),
        ),
        onDismiss = onDismiss,
    )
}

/** 호스트 취소 확인 — 전액 환불. */
@Composable
fun HostCancelDialog(r: SpaceReservationRow, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    MuyeonDialog(
        title = "이 예약을 취소할까요?",
        message = "${r.bookerName ?: "예약자"}님 · ${r.whenText}\n결제 금액 ${wonOrDash(r.amount)}을 전액 환불해요.",
        actions = listOf(
            MuyeonDialogAction("취소(전액 환불)", MuyeonDialogKind.DESTRUCTIVE) { onConfirm() },
            MuyeonDialogAction("닫기", MuyeonDialogKind.CANCEL),
        ),
        onDismiss = onDismiss,
    )
}

/** 내 공간 삭제 확인. */
@Composable
fun DeleteSpaceDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    MuyeonDialog(
        title = "공간을 삭제할까요?",
        message = "결제된 예약이 있으면 삭제할 수 없어요.",
        actions = listOf(
            MuyeonDialogAction("삭제", MuyeonDialogKind.DESTRUCTIVE) { onConfirm() },
            MuyeonDialogAction("취소", MuyeonDialogKind.CANCEL),
        ),
        onDismiss = onDismiss,
    )
}
