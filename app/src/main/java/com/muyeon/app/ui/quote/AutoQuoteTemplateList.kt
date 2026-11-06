package com.muyeon.app.ui.quote

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonAddLink
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.common.MuyeonEmptyState
import com.muyeon.app.ui.common.MuyeonGroupSection
import com.muyeon.app.ui.common.MuyeonList
import com.muyeon.app.ui.common.MuyeonSectionTitle

/**
 * ★ 2026-11-06 '내 자동응답 메시지' — 묶음 제목 + "＋ 새 메시지" 링크 + 흰 카드 안 라디오 줄.
 *  누르면 바로 고른다(이미 고른 줄은 그대로). 비어 있으면 공용 빈 화면에서 바로 만든다.
 */
@Composable
fun AutoQuoteTemplateList(
    templates: List<AutoQuoteTemplate>,
    activeId: Int?,
    onAdd: () -> Unit,
    onActivate: (AutoQuoteTemplate) -> Unit,
    onPreview: (AutoQuoteTemplate) -> Unit,
    onEdit: (AutoQuoteTemplate) -> Unit,
    onDelete: (AutoQuoteTemplate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MuyeonSectionTitle("내 자동응답 메시지") {
            if (templates.isNotEmpty()) MuyeonAddLink("새 메시지", onAdd)
        }
        if (templates.isEmpty()) {
            MuyeonEmptyState(
                title = "저장된 자동응답 메시지가 없어요",
                message = "메시지와 금액을 만들어 두면 자동 견적에 그대로 쓰여요.",
                icon = Icons.Outlined.ChatBubbleOutline,
                actionTitle = "메시지 만들기",
                onAction = onAdd,
            )
        } else {
            MuyeonGroupSection {
                templates.forEach { t ->
                    AutoQuoteTemplateRow(
                        t = t,
                        isActive = activeId == t.id,
                        onActivate = { if (activeId != t.id) onActivate(t) },
                        onPreview = { onPreview(t) },
                        onEdit = { onEdit(t) },
                        onDelete = { onDelete(t) },
                    )
                }
            }
        }
    }
}

/** 한 줄 — 라디오 + 이름·사용 중 + 금액 + 메시지 두 줄 + 미리보기/편집/삭제. */
@Composable
private fun AutoQuoteTemplateRow(
    t: AutoQuoteTemplate,
    isActive: Boolean,
    onActivate: () -> Unit,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .selectable(selected = isActive, role = Role.RadioButton, onClick = onActivate)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            if (isActive) Icons.Filled.CheckCircle else Icons.Outlined.Circle, null,
            tint = if (isActive) MuyeonColors.primary else MuyeonList.ink300,
            modifier = Modifier.padding(top = 1.dp).size(22.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    t.title?.ifBlank { null } ?: "자동응답 메시지",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                    color = MuyeonColors.textHead, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isActive) {
                    Text(
                        "사용 중",
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
                        lineHeight = 13.sp, color = MuyeonColors.primary,
                        modifier = Modifier.clip(RoundedCornerShape(50))
                            .background(MuyeonColors.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            val hasPrice = !t.price.isNullOrBlank()
            Text(
                if (hasPrice) t.price!! else "금액 미입력",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                color = if (hasPrice) MuyeonColors.textHead else MuyeonColors.textSub,
            )
            Text(
                t.message,
                fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = MuyeonColors.textSub,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Row(Modifier.padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "미리보기",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                    color = MuyeonColors.primary, modifier = Modifier.clickable(onClick = onPreview),
                )
                Text(
                    "편집",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                    color = MuyeonColors.primary, modifier = Modifier.clickable(onClick = onEdit),
                )
                Text(
                    "삭제",
                    fontFamily = customFontFamily, fontSize = 13.sp, color = MuyeonColors.textSub,
                    modifier = Modifier.clickable(onClick = onDelete),
                )
            }
        }
    }
}
