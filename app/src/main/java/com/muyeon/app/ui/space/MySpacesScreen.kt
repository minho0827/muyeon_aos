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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.MeetingRoom
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
import com.muyeon.app.ui.space.booking.DeleteSpaceDialog
import com.muyeon.app.ui.space.booking.MySpaceRow
import com.muyeon.app.ui.space.booking.MySpaceListSkeleton
import kotlinx.coroutines.launch

/** 호스트 — 내 공간 목록([수정][삭제]) + 제목줄 [예약 관리]·＋. */
@Composable
fun SpaceMineScreen(
    api: SpaceApi,
    reloadTick: Int,
    onBack: () -> Unit,
    onOpen: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onCreate: () -> Unit,
    onReservations: () -> Unit,
    onChanged: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<SpaceListItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<SpaceListItem?>(null) }

    LaunchedEffect(reloadTick) {
        if (!api.isLoggedIn) { error = "로그인 후 이용할 수 있어요."; loading = false; return@LaunchedEffect }
        api.mySpaces().onSuccess { items = it; error = null }.onFailure { error = it.message }
        loading = false
    }

    Column(Modifier.fillMaxSize().background(MuyeonList.pageBg)) {
        MuyeonListHeader("내 공간", onBack = onBack) {
            Text(
                "예약 관리", fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MuyeonColors.primary,
                modifier = Modifier.clickable(onClick = onReservations).padding(horizontal = 8.dp, vertical = 10.dp),
            )
            MuyeonHeaderIcon(Icons.Filled.Add, "공간 등록", onClick = onCreate)
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                loading -> MySpaceListSkeleton()  // 실제 카드 높이의 자리
                items.isEmpty() && error != null -> MuyeonEmptyState(error ?: "", icon = Icons.Outlined.MeetingRoom, modifier = Modifier.align(Alignment.Center))
                items.isEmpty() -> MuyeonEmptyState(
                    "아직 등록한 공간이 없어요.", icon = Icons.Outlined.MeetingRoom,
                    actionTitle = "공간 등록하기", onAction = onCreate, modifier = Modifier.align(Alignment.Center),
                )
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items, key = { it.id }) { s ->
                        MySpaceRow(s, onOpen = { onOpen(s.id) }, onEdit = { onEdit(s.id) }, onDelete = { deleteTarget = s })
                    }
                }
            }
        }
    }

    deleteTarget?.let { t ->
        DeleteSpaceDialog(
            onConfirm = {
                scope.launch {
                    api.remove(t.id).onSuccess { items = items.filter { it.id != t.id }; onChanged() }
                        .onFailure { onMessage(it.message ?: "삭제하지 못했어요.") }
                }
            },
            onDismiss = { deleteTarget = null },
        )
    }
}
