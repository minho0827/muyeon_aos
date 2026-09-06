package com.muyeon.app.ui.survey

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import kotlinx.coroutines.launch

/**
 * 강사: 채팅 +시트 "설문지" → 발송할 설문지 선택 → 회원에게 발송(SURVEY_CARD 생성).
 *  iOS `SurveyPickerView.swift` 1:1.
 *
 * ⚠️ 종전 AOS 는 이 화면이 없어 첨부 시트의 '설문지' 가 토스트만 띄우는 죽은 버튼이었다.
 *   발송 API(SurveyApi.dispatch)는 이미 있었고 부르는 곳만 없었다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurveyPickerSheet(
    api: SurveyApi,
    roomId: Int,
    recipientId: Int,
    onSent: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var templates by remember { mutableStateOf<List<SurveyTemplate>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var sendingId by remember { mutableStateOf<Int?>(null) }
    var expanded by remember { mutableStateOf<Set<Int>>(emptySet()) }
    // 펼칠 때만 문항을 받아 캐시한다(목록 진입에서 전체를 다 받지 않게).
    var previews by remember { mutableStateOf<Map<Int, List<SurveyQuestion>>>(emptyMap()) }

    LaunchedEffect(Unit) {
        // 실패 시 기존 목록 유지(iOS 와 동일) — 빈 화면으로 덮어쓰지 않는다.
        api.listTemplates().onSuccess { templates = it }
        loading = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().background(MuyeonColors.groupedBg)) {
            Row(
                Modifier.fillMaxWidth().background(MuyeonColors.surface)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Close, "닫기", tint = MuyeonColors.textHead,
                    modifier = Modifier.size(15.dp).clickable(onClick = onDismiss),
                )
                Text(
                    "레슨 전 설문지 보내기",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    lineHeight = 19.sp, color = MuyeonColors.textHead,
                )
            }
            HorizontalDivider(color = MuyeonColors.border)

            when {
                loading -> Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                    CircularProgressIndicator(color = MuyeonColors.primary)
                }
                templates.isEmpty() -> Box(Modifier.fillMaxWidth().height(200.dp), Alignment.Center) {
                    Text(
                        "등록된 설문지가 없습니다.",
                        fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.textSub,
                    )
                }
                else -> Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    templates.forEach { t ->
                        TemplateCard(
                            template = t,
                            expanded = expanded.contains(t.id),
                            questions = previews[t.id],
                            sending = sendingId == t.id,
                            enabled = sendingId == null,
                            onToggle = {
                                expanded = if (expanded.contains(t.id)) expanded - t.id else expanded + t.id
                                if (previews[t.id] == null) {
                                    scope.launch {
                                        api.getTemplate(t.id).onSuccess { d ->
                                            previews = previews + (t.id to d.questions)
                                        }
                                    }
                                }
                            },
                            onSend = {
                                if (sendingId != null) return@TemplateCard
                                sendingId = t.id
                                scope.launch {
                                    api.dispatch(t.id, roomId, recipientId)
                                        .onSuccess { sendingId = null; onSent() }
                                        // 실패도 닫는다 — 방으로 돌아가야 원인(권한 안내 등)을 볼 수 있다(iOS 동일).
                                        .onFailure { sendingId = null; onDismiss() }
                                }
                            },
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    template: SurveyTemplate,
    expanded: Boolean,
    questions: List<SurveyQuestion>?,
    sending: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    onSend: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(MuyeonColors.surface).padding(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                template.genre?.takeIf { it.isNotEmpty() }?.let {
                    Text(
                        it,
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                        lineHeight = 14.sp, color = MuyeonColors.primary,
                    )
                }
                Text(
                    template.title,
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                    lineHeight = 19.sp, color = MuyeonColors.textHead,
                )
                template.description?.takeIf { it.isNotEmpty() }?.let {
                    Text(
                        it,
                        fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp,
                        color = MuyeonColors.textSub, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, "문항 미리보기",
                tint = MuyeonColors.textSub,
                modifier = Modifier.size(29.dp).clickable(onClick = onToggle).padding(8.dp),
            )
        }
        if (expanded && questions != null) {
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MuyeonColors.border)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                questions.forEachIndexed { i, q ->
                    Text(
                        "${i + 1}. ${q.title}",
                        fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 14.sp,
                        color = MuyeonColors.textSub,
                    )
                }
            }
        }
        Text(
            if (sending) "보내는 중…" else "이 설문지 보내기",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            lineHeight = 17.sp, color = Color.White, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp).fillMaxWidth()
                .clip(RoundedCornerShape(10.dp)).background(MuyeonColors.primary)
                .clickable(enabled = enabled, onClick = onSend)
                .padding(vertical = 11.dp),
        )
    }
}
