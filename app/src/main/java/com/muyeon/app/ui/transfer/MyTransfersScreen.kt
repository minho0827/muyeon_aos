package com.muyeon.app.ui.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.ui.common.MuyeonDialogKind
import com.muyeon.app.ui.common.MuyeonEmptyState
import com.muyeon.app.ui.common.MuyeonHeaderIcon
import com.muyeon.app.ui.common.MuyeonList
import com.muyeon.app.ui.common.MuyeonListHeader
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
    var localTick by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reloadTick, localTick) {
        if (!api.isLoggedIn) { error = "로그인 후 이용할 수 있어요."; loading = false; return@LaunchedEffect }
        api.mine().onSuccess { items = it; error = null }.onFailure { error = it.message }
        loading = false
    }

    Column(Modifier.fillMaxSize().background(MuyeonList.pageBg)) {
        MuyeonListHeader("내 양도 글", onBack = onBack) { MuyeonHeaderIcon(Icons.Filled.Add, "양도 글 등록", onClick = onCreate) }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                loading -> CircularProgressIndicator(color = MuyeonColors.primary, modifier = Modifier.align(Alignment.Center))
                items.isEmpty() -> MuyeonEmptyState(
                    error ?: "아직 올린 양도 글이 없어요.", icon = Icons.Outlined.Storefront,
                    actionTitle = if (error == null) "양도 글 등록하기" else null, onAction = if (error == null) onCreate else null,
                    modifier = Modifier.align(Alignment.Center),
                )
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(items, key = { it.id }) { c ->
                        Column(Modifier.fillMaxWidth().clip(MuyeonList.cardShape).background(Color.White)) {
                            TransferCardRow(c, mine = true, onClick = { onOpen(c.id) }, onHeart = null, onChat = null)
                            if (!c.isExposed) {
                                Text("관리자 확인으로 숨겨진 글이에요.", fontFamily = customFontFamily, fontSize = 12.sp, color = MuyeonColors.danger,
                                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 8.dp))
                            }
                            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RowButton("수정", Modifier.weight(1f)) { onEdit(c.id) }
                                RowButton("상태 변경", Modifier.weight(1f)) { statusTarget = c }
                                RowButton("삭제", Modifier.weight(1f), danger = true) { deleteTarget = c }
                            }
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
        MuyeonDialog(
            title = "양도 글을 삭제할까요?",
            message = "삭제한 글은 되돌릴 수 없어요.",
            actions = listOf(
                MuyeonDialogAction("삭제", MuyeonDialogKind.DESTRUCTIVE) {
                    scope.launch {
                        api.remove(t.id).onSuccess { items = items.filter { it.id != t.id }; onChanged() }
                            .onFailure { onMessage(it.message ?: "삭제하지 못했어요.") }
                    }
                },
                MuyeonDialogAction("취소", MuyeonDialogKind.CANCEL),
            ),
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
internal fun RowButton(label: String, modifier: Modifier, danger: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier.height(40.dp).clip(RoundedCornerShape(10.dp)).border(1.dp, MuyeonList.ink200, RoundedCornerShape(10.dp)).clickable(onClick = onClick),
        Alignment.Center,
    ) {
        Text(label, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = if (danger) MuyeonColors.danger else MuyeonColors.textHead)
    }
}
