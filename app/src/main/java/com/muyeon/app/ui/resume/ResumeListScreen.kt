package com.muyeon.app.ui.resume

import com.muyeon.app.ui.common.muyeonCardBorder
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.outlined.Description
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteDialog
import kotlinx.coroutines.launch

/**
 * 이력서 관리 — iOS `ResumeListView.swift` 1:1.
 *  ★ 2026-11-03 [직접 작성하기] 안내 카드 → 총 N건 + 이력서 카드(수정하기·복사하기·⋯) → 유의사항.
 *   작성 입구가 상단 '강사프로필 등록/수정' 카드와 하단 '새 이력서 작성' 두 곳이라 헷갈려 하나로 합쳤다.
 *  기본 이력서가 강사프로필(무용수 프로필)의 원본이 된다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeListScreen(
    api: ResumeApi,
    mode: ResumeMode,
    onClose: () -> Unit,
    onEdit: (Int?) -> Unit,        // null = 신규
    onVisibility: () -> Unit,
    // 강사 모드 상단 진입 카드 — 기본 이력서를 열어 구직 프로필로 등록한다.
    onSeekProfile: (Int?) -> Unit = {},
) {
    var items by remember { mutableStateOf<List<ResumeListItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<ResumeListItem?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true
        items = api.list(mode).getOrDefault(emptyList())
        loading = false
    }

    LaunchedEffect(mode) { load() }

    // 기본 이력서(강사)는 강사프로필 편집 흐름으로 연다 — 종전 상단 카드와 같은 동작.
    fun open(item: ResumeListItem) {
        if (item.isDefault && !mode.isDancer) onSeekProfile(item.id) else onEdit(item.id)
    }
    // 강사의 첫 이력서는 곧 강사프로필 원본이라 강사프로필 편집 흐름으로 연다.
    fun create() {
        if (!mode.isDancer && items.isEmpty()) onSeekProfile(null) else onEdit(null)
    }

    // 10-27 목록 화면 틀(ui/common/MuyeonListKit) — ‹ + 왼쪽 굵은 제목, 회색 바탕 위 흰 카드(iOS 와 같다).
    Column(Modifier.fillMaxSize().background(com.muyeon.app.ui.common.MuyeonList.pageBg)) {
        com.muyeon.app.ui.common.MuyeonListHeader(mode.listTitle, onBack = onClose)

        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
        } else {
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { scope.launch { refreshing = true; load(); refreshing = false } },
                modifier = Modifier.weight(1f),
            ) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp).padding(top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CreateCard(mode.isDancer, onCreate = ::create)
                    if (mode.isDancer) VisibilityEntry(onVisibility)
                    Text(
                        "총 ${items.size}건",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                        lineHeight = 16.sp, color = MuyeonColors.textSub,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                    )
                    items.forEach { item ->
                        ResumeRow(
                            item = item,
                            isDancer = mode.isDancer,
                            onEdit = { open(item) },
                            onDuplicate = { scope.launch { api.duplicate(item.id).onFailure { errorMessage = it.message }; load() } },
                            onSetDefault = { scope.launch { api.setDefault(item.id).onFailure { errorMessage = it.message }; load() } },
                            onDelete = { deleteTarget = item },
                        )
                    }
                    if (items.isEmpty()) {
                        com.muyeon.app.ui.common.MuyeonEmptyState(
                            "아직 작성한 이력서가 없어요", "위의 [직접 작성하기]로 첫 이력서를 작성해 보세요.",
                            icon = androidx.compose.material.icons.Icons.Outlined.Description,
                        )
                    }
                    NoticeBox(mode.isDancer, Modifier.padding(top = 8.dp))
                }
            }
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

/** 상단 작성 안내 카드 — 작성 입구는 여기 하나(iOS `ResumeListView.createCard`). */
@Composable
private fun CreateCard(isDancer: Boolean, onCreate: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(com.muyeon.app.ui.common.MuyeonList.cardShape).muyeonCardBorder()
            .background(MuyeonColors.surface)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "이력서를 등록하고 싶다면?",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                lineHeight = 18.sp, color = MuyeonColors.textHead,
            )
            Text(
                if (isDancer) "이력서를 완성하고 원하는 공연에 바로 지원하세요" else "이력서를 완성하고 원하는 공고에 바로 지원하세요",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                lineHeight = 15.sp, color = MuyeonColors.textSub,
            )
        }
        Text(
            "직접 작성하기",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            lineHeight = 17.sp, color = Color.White,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MuyeonColors.primary)
                .clickable(onClick = onCreate)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

/** 하단 유의사항(iOS `ResumeListView.noticeBox`). */
@Composable
private fun NoticeBox(isDancer: Boolean, modifier: Modifier = Modifier) {
    val lines = if (isDancer) listOf(
        "기본 이력서가 공연팀·기획자에게 보이는 무용수 프로필이 돼요.",
        "공연에 지원할 때 제출할 이력서를 고를 수 있어요.",
        "지원에 쓴 이력서는 삭제할 수 없어요. 내용 수정이나 비공개로 관리해 주세요.",
    ) else listOf(
        "기본 이력서가 회원·원장님에게 보이는 강사프로필이 돼요.",
        "공고에 지원할 때 제출할 이력서를 고를 수 있어요.",
        "지원에 쓴 이력서는 삭제할 수 없어요. 내용 수정이나 비공개로 관리해 주세요.",
    )
    Column(
        modifier
            .fillMaxWidth()
            .clip(com.muyeon.app.ui.common.MuyeonList.cardShape)
            .background(MuyeonColors.tileIdle)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "유의사항",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.textSub,
        )
        lines.forEach { line ->
            Text(
                "• $line",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                lineHeight = 17.sp, color = MuyeonColors.textSub,
            )
        }
    }
}

/** 공개 범위 설정 진입 카드(무용수) — 강사는 강사프로필 편집 화면 안에 있다. */
@Composable
private fun VisibilityEntry(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(com.muyeon.app.ui.common.MuyeonList.cardShape).muyeonCardBorder()
            .background(MuyeonColors.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
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
                "기본 이력서에서 공연팀·기획자에게 공개할 항목을 고르세요.",
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
    isDancer: Boolean,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(com.muyeon.app.ui.common.MuyeonList.cardShape).muyeonCardBorder()
            .background(MuyeonColors.surface)
            .clickable(onClick = onEdit)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    item.title,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    lineHeight = 20.sp, color = MuyeonColors.textHead, maxLines = 2,
                )
                if (item.isDefault) {
                    Text(
                        if (isDancer) "기본 · 무용수 프로필에 보여요" else "기본 · 강사프로필에 보여요",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                        lineHeight = 13.sp, color = MuyeonColors.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MuyeonColors.primary.copy(alpha = 0.10f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
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
                    modifier = Modifier.size(32.dp).clickable { menuOpen = true }.padding(8.dp),
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RowButton("수정하기", Modifier.weight(1f), onEdit)
            RowButton("복사하기", Modifier.weight(1f), onDuplicate)
        }
    }
}

@Composable
private fun RowButton(title: String, modifier: Modifier, onClick: () -> Unit) {
    Text(
        title,
        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
        lineHeight = 16.sp, color = MuyeonColors.body, textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MuyeonColors.border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    )
}
