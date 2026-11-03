package com.muyeon.app.ui.space

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
import androidx.compose.material.icons.outlined.EventNote
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
import com.muyeon.app.ui.space.booking.HostCancelDialog
import com.muyeon.app.ui.space.booking.HostPaidReservationRow
import com.muyeon.app.ui.space.booking.HostReservationRow
import kotlinx.coroutines.launch

/** 호스트 — 예약 관리(날짜순, 상태 배지, 결제 완료 행 [취소(전액 환불)]). */
@Composable
fun SpaceHostReservationsScreen(
    api: SpaceApi,
    onBack: () -> Unit,
    onChanged: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var rows by remember { mutableStateOf<List<SpaceReservationRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    var cancelTarget by remember { mutableStateOf<SpaceReservationRow?>(null) }

    LaunchedEffect(tick) {
        if (!api.isLoggedIn) { error = "로그인 후 이용할 수 있어요."; loading = false; return@LaunchedEffect }
        api.hostReservations().onSuccess { list -> rows = list.sortedBy { "${it.date} ${it.startTime ?: ""}" }; error = null }
            .onFailure { error = it.message }
        loading = false
    }

    Column(Modifier.fillMaxSize().background(MuyeonList.pageBg)) {
        MuyeonListHeader("공간 예약 관리", onBack = onBack)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                loading -> CircularProgressIndicator(color = MuyeonColors.primary, modifier = Modifier.align(Alignment.Center))
                rows.isEmpty() -> MuyeonEmptyState(error ?: "들어온 예약이 없어요.", icon = Icons.Outlined.EventNote, modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(rows, key = { it.id }) { r ->
                        // 결제 완료만 [취소(전액 환불)] 줄이 붙는다.
                        if (r.status == "PAID") HostPaidReservationRow(r) { cancelTarget = r } else HostReservationRow(r)
                    }
                }
            }
        }
    }

    cancelTarget?.let { r ->
        HostCancelDialog(
            r,
            onConfirm = {
                scope.launch {
                    api.hostCancel(r.id).onSuccess { onMessage("예약을 취소하고 전액 환불했어요."); onChanged(); tick++ }
                        .onFailure { onMessage(it.message ?: "취소하지 못했어요.") }
                }
            },
            onDismiss = { cancelTarget = null },
        )
    }
}
