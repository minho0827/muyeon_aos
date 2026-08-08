package com.muyeon.app.ui.resume

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.ui.quote.QuoteNavBar
import kotlinx.coroutines.launch

/**
 * 이력서 관리 — iOS `ResumeListView.swift` 1:1.
 *  다중 이력서 목록(기본 지정/삭제) + [새 이력서 작성] + [공개 범위 설정].
 *  기본 이력서가 공개 프로필의 원본이 된다.
 */
@Composable
fun ResumeListScreen(
    api: ResumeApi,
    mode: ResumeMode,
    onClose: () -> Unit,
    onEdit: (Int?) -> Unit,        // null = 신규
    onVisibility: () -> Unit,
) {
    var items by remember { mutableStateOf<List<ResumeListItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<ResumeListItem?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true
        items = api.list().getOrDefault(emptyList())
        loading = false
    }

    LaunchedEffect(Unit) { load() }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = mode.listTitle, onBack = onClose)

        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
        } else {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                VisibilityEntry(onVisibility)
                items.forEach { item ->
                    ResumeRow(
                        item = item,
                        onClick = { onEdit(item.id) },
                        onSetDefault = { scope.launch { api.setDefault(item.id).onFailure { errorMessage = it.message }; load() } },
                        onDelete = { deleteTarget = item },
                    )
                }
                if (items.isEmpty()) {
                    Text(
                        "아직 작성한 이력서가 없어요.\n아래 버튼으로 첫 이력서를 작성해보세요.",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                        lineHeight = 20.sp, color = MuyeonColors.textSub, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    )
                }
            }
            Text(
                if (mode.isDancer) "새 무용수 프로필 작성" else "새 이력서 작성",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MuyeonColors.primary)
                    .clickable { onEdit(null) }
                    .padding(vertical = 16.dp),
            )
        }
    }

    deleteTarget?.let { t ->
        QuoteDialog(
            title = "이력서를 삭제할까요?",
            message = t.title,
            confirmText = "삭제",
            onConfirm = {
                deleteTarget = null
                scope.launch { api.remove(t.id).onFailure { errorMessage = it.message }; load() }
            },
            onDismiss = { deleteTarget = null },
        )
    }
    errorMessage?.let { msg ->
        QuoteDialog(
            title = "오류", message = msg, confirmText = "확인",
            onConfirm = { errorMessage = null }, onDismiss = { errorMessage = null },
        )
    }
}

/** 공개 범위 설정 진입 카드 — 기본 이력서 기반 공개 프로필 안내. */
@Composable
private fun VisibilityEntry(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF7F7F7))
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(MuyeonColors.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Tune, null, tint = MuyeonColors.primary, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "공개 범위 설정",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                lineHeight = 18.sp, color = MuyeonColors.textHead,
            )
            Text(
                "기본 이력서에서 일반회원에게 공개할 항목을 고르세요.",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                lineHeight = 14.sp, color = MuyeonColors.textSub,
            )
        }
        Icon(Icons.Filled.KeyboardArrowRight, null, tint = MuyeonColors.chevron, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ResumeRow(
    item: ResumeListItem,
    onClick: () -> Unit,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, MuyeonColors.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.title,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                )
                if (item.isDefault) {
                    Text(
                        "기본",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                        lineHeight = 13.sp, color = MuyeonColors.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MuyeonColors.primary.copy(alpha = 0.10f))
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
            }
            if (item.isDefault) {
                Text(
                    "공개 프로필의 원본이에요.",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                    lineHeight = 14.sp, color = MuyeonColors.chevron,
                )
            }
            // 비정형 기간 포함 — 열어서 휠피커로 다시 저장하면 정형화(검색·경력계산 반영)
            if (item.needsPeriodFix == true) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = MuyeonColors.orange, modifier = Modifier.size(10.dp))
                    Text(
                        "기간 형식 확인 필요",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                        lineHeight = 14.sp, color = MuyeonColors.orange,
                    )
                }
            }
        }
        Box {
            Icon(
                Icons.Filled.MoreVert, "더보기", tint = MuyeonColors.chevron,
                modifier = Modifier.size(36.dp).clickable { menuOpen = true }.padding(10.dp),
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                if (!item.isDefault) {
                    DropdownMenuItem(
                        text = { Text("기본으로 지정", fontFamily = customFontFamily, fontSize = 14.sp) },
                        onClick = { menuOpen = false; onSetDefault() },
                    )
                }
                DropdownMenuItem(
                    text = { Text("삭제", fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.danger) },
                    onClick = { menuOpen = false; onDelete() },
                )
            }
        }
    }
}
