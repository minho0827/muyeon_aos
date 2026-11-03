package com.muyeon.app.ui.transfer

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
import androidx.compose.material.icons.outlined.Storefront
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
import com.muyeon.app.ui.transfer.components.MyTransferRow
import com.muyeon.app.ui.transfer.components.MyTransferRowSkeleton
import com.muyeon.app.ui.transfer.components.TransferDeleteDialog
import com.muyeon.app.ui.transfer.components.TransferStatusDialog
import kotlinx.coroutines.launch

/** 내 양도 글 — 카드 + 각 행 [수정] [상태 변경] [삭제(확인)]. */
@Composable
fun MyTransfersScreen(
    api: TransferApi,
    reloadTick: Int,
    onBack: () -> Unit,
    onOpen: (Int) -> Unit,
    onEdit: (Int) -> Unit,
    onCreate: () -> Unit,
    onChanged: () -> Unit,
    onMessage: (String) -> Unit,
) {
    var items by remember { mutableStateOf<List<TransferCard>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var statusTarget by remember { mutableStateOf<TransferCard?>(null) }
    var deleteTarget by remember { mutableStateOf<TransferCard?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reloadTick) {
        if (!api.isLoggedIn) { error = "로그인 후 이용할 수 있어요."; loading = false; return@LaunchedEffect }
        api.mine().onSuccess { items = it; error = null }.onFailure { error = it.message }
        loading = false
    }

    Column(Modifier.fillMaxSize().background(MuyeonList.pageBg)) {
        MuyeonListHeader("내 양도 글", onBack = onBack) { MuyeonHeaderIcon(Icons.Filled.Add, "양도 글 등록", onClick = onCreate) }
        val phase = when {
            loading -> ListPhase.Loading
            items.isEmpty() && error != null -> ListPhase.Error
            items.isEmpty() -> ListPhase.Empty
            else -> ListPhase.Items
        }
        StableSwap(phase, Modifier.weight(1f).fillMaxWidth()) { p ->
            Box(Modifier.fillMaxSize()) {
                when (p) {
                    ListPhase.Loading -> Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(5) { MyTransferRowSkeleton() }
                    }
                    ListPhase.Error -> MuyeonEmptyState(error ?: "", icon = Icons.Outlined.Storefront, modifier = Modifier.align(Alignment.Center))
                    ListPhase.Empty -> MuyeonEmptyState(
                        "아직 올린 양도 글이 없어요.", icon = Icons.Outlined.Storefront,
                        actionTitle = "양도 글 등록하기", onAction = onCreate, modifier = Modifier.align(Alignment.Center),
                    )
                    ListPhase.Items -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(items, key = { it.id }) { c ->
                            MyTransferRow(c, onOpen = { onOpen(c.id) }, onEdit = { onEdit(c.id) }, onStatus = { statusTarget = c }, onDelete = { deleteTarget = c })
                        }
                    }
                }
            }
        }
    }

    statusTarget?.let { t ->
        TransferStatusDialog(t.status, onPick = { s ->
            scope.launch {
                api.setStatus(t.id, s).onSuccess { items = items.map { if (it.id == t.id) it.copy(status = s) else it }; onChanged() }
                    .onFailure { onMessage(it.message ?: "상태를 바꾸지 못했어요.") }
            }
        }, onDismiss = { statusTarget = null })
    }
    deleteTarget?.let { t ->
        TransferDeleteDialog(
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
