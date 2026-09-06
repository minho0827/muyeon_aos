package com.muyeon.app.ui.academy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteAvatar

/** 학원↔강사 소속 화면 공통 조각 — iOS `AcademySharedUI.swift` 1:1. */

/** 섹션 제목 + 개수(개수는 primary). */
@Composable
fun AcademySectionHead(title: String, count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = MuyeonColors.textHead,
        )
        Text(
            "$count",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = MuyeonColors.primary,
        )
    }
}

@Composable
fun AcademyStatusChip(status: String?) {
    val label = AcademyTeacherStatus.label(status)
    if (label.isEmpty()) return
    val fg: Color
    val bg: Color
    when (status) {
        "ACTIVE" -> { fg = MuyeonColors.primary; bg = MuyeonColors.primary.copy(alpha = 0.10f) }
        "REQUESTED", "INVITED" -> { fg = MuyeonColors.textSub; bg = Color(0xFFF7F7F7) }
        else -> { fg = MuyeonColors.chevron; bg = Color(0xFFF4F4F4) }
    }
    Text(
        label,
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp,
        lineHeight = 14.sp, color = fg,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

/** 사람 카드 — 아바타 + 이름 + 장르 + 상태칩 + 신청 메시지 + 하단 액션. */
@Composable
fun AcademyPersonCard(row: AcademyTeacher, actions: @Composable RowScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .border(1.dp, MuyeonColors.border, RoundedCornerShape(14.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            QuoteAvatar(row.userImage, row.displayName, 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    row.displayName,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                )
                if (row.genreLine.isNotEmpty()) {
                    Text(
                        row.genreLine,
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                        lineHeight = 16.sp, color = MuyeonColors.textSub,
                    )
                }
            }
            AcademyStatusChip(row.status)
        }
        row.message?.takeIf { it.isNotEmpty() }?.let {
            Text(
                it,
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                lineHeight = 18.sp, color = MuyeonColors.body,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF7F7F7)).padding(10.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
    }
}

/** 카드 안 액션 버튼 — 채움(primary) / 외곽선(primary) / 위험(회색 외곽선). */
@Composable
fun RowScope.AcademyActionButton(
    title: String,
    filled: Boolean = false,
    destructive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val fg = when {
        filled -> Color.White
        destructive -> MuyeonColors.textSub
        else -> MuyeonColors.primary
    }
    Text(
        title,
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
        lineHeight = 17.sp, color = fg, textAlign = TextAlign.Center,
        modifier = Modifier
            .weight(1f)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (filled) Modifier.background(MuyeonColors.primary)
                else Modifier.border(
                    1.dp,
                    if (destructive) MuyeonColors.border else MuyeonColors.primary,
                    RoundedCornerShape(10.dp),
                ),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 11.dp),
    )
}

/** 빈 상태 문구. */
@Composable
fun AcademyEmptyText(text: String) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
        lineHeight = 20.sp, color = MuyeonColors.textSub, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
    )
}