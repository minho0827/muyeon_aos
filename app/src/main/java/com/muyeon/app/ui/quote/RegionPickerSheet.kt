package com.muyeon.app.ui.quote

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors

/**
 * 지역 **단일 선택** 시트 — iOS `RegionPickerSheet.swift` 1:1.
 *  데이터는 견적용 복수선택기와 같은 RegionRepo(GET /regions)를 쓴다.
 *
 * ⚠️ 왜 자유 텍스트를 쓰면 안 되나 —
 *   activeRegion 은 regions.name("서울 강남구") 포맷으로 저장돼야 지역 강사수(prefix 매칭)·
 *   목록 지역 필터·견적 지역 매칭이 전부 싱크된다. "강남"·"서울 강남" 처럼 적으면
 *   저장은 되는데 검색·매칭에서 조용히 빠진다(iOS 주석과 같은 근거).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionPickerSheet(
    token: String?,
    title: String,
    /** "서울 전체" 같은 시/도 단위 선택 허용(활동·희망지역용). */
    allowSidoAll: Boolean = true,
    onPick: (name: String, code: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var groups by remember { mutableStateOf<List<RegionSidoGroup>>(emptyList()) }
    var expanded by remember { mutableStateOf<Set<String>>(emptySet()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val regions = RegionRepo.fetchRegions(token)
        groups = RegionRepo.group(regions.ifEmpty { RegionRepo.fallback })
        loading = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth()) {
            Text(
                title,
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                lineHeight = 20.sp, color = MuyeonColors.textHead, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            )
            HorizontalDivider(color = MuyeonColors.border)

            if (loading) {
                Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                    CircularProgressIndicator(color = MuyeonColors.primary)
                }
            } else {
                LazyColumn(Modifier.weight(1f, fill = false).heightIn(max = 520.dp)) {
                    groups.forEach { g ->
                        val open = expanded.contains(g.sido)
                        item(key = "sido-${g.sido}") {
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable {
                                        expanded = if (open) expanded - g.sido else expanded + g.sido
                                    }
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    g.sido,
                                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null,
                                    tint = MuyeonColors.chevron, modifier = Modifier.size(12.dp),
                                )
                            }
                            HorizontalDivider(color = MuyeonColors.border)
                        }
                        if (open) {
                            if (allowSidoAll) {
                                item(key = "all-${g.sido}") {
                                    // 시도 전체도 코드를 준다 — 법정동 코드 앞 2자리(prefix 매칭용).
                                    RegionRow("${g.sido} 전체") {
                                        onPick("${g.sido} 전체", g.items.firstOrNull()?.code?.take(2))
                                        onDismiss()
                                    }
                                }
                            }
                            items(g.items.size, key = { i -> g.items[i].code }) { i ->
                                val r = g.items[i]
                                RegionRow(r.name) { onPick(r.name, r.code); onDismiss() }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RegionRow(name: String, onClick: () -> Unit) {
    Text(
        name,
        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
        lineHeight = 17.sp, color = MuyeonColors.body,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(start = 32.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
    )
}

/** 편집 화면용 지역 행 — 탭 → 선택 시트. iOS `RegionPickRow`. */
@Composable
fun RegionPickRow(
    token: String?,
    placeholder: String,
    name: String,
    allowSidoAll: Boolean = true,
    onPick: (name: String, code: String?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
            .clickable { open = true }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name.ifEmpty { placeholder },
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
            lineHeight = 17.sp,
            color = if (name.isEmpty()) MuyeonColors.chevron else MuyeonColors.textHead,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Filled.ExpandMore, null, tint = MuyeonColors.chevron, modifier = Modifier.size(11.dp))
    }
    if (open) {
        RegionPickerSheet(
            token = token, title = placeholder, allowSidoAll = allowSidoAll,
            onPick = onPick, onDismiss = { open = false },
        )
    }
}

/** 활동 지역 [+ 지역 추가] 점선 행 — 다중 지역용. iOS `ActiveRegionAddRow`. */
@Composable
fun RegionAddRow(
    token: String?,
    label: String = "지역 추가 (최대 3개)",
    enabled: Boolean = true,
    onPick: (name: String, code: String?) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .border(1.dp, MuyeonColors.primary.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) { open = true }
            .padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, null, tint = MuyeonColors.primary, modifier = Modifier.size(12.dp))
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 17.sp, color = MuyeonColors.primary,
        )
    }
    if (open) {
        RegionPickerSheet(
            token = token, title = "활동 지역 선택", allowSidoAll = true,
            onPick = onPick, onDismiss = { open = false },
        )
    }
}
