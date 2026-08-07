package com.muyeon.app.ui.quote

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily

/**
 * 견적 문진 컴포넌트 — iOS 컴포넌트 1:1 이식(색/여백/크기/폰트 동일).
 *  대응: QuoteChatBubble.swift · QuoteOptionRow.swift · QuoteProgressBar.swift · QuoteTypingBubble.swift
 */

// iOS Asset.Colors 와 동일한 hex (이름=hex 규약)
object QuoteColors {
    val f58232 = Color(0xFFF58232)   // 브랜드 오렌지
    val c101116 = Color(0xFF101116)  // 본문 진한 텍스트
    val c6D6E71 = Color(0xFF6D6E71)  // 보조 텍스트
    val cF4F4F4 = Color(0xFFF4F4F4)  // 질문 말풍선 배경
    val cEAEAEA = Color(0xFFEAEAEA)  // 테두리/트랙
    val c37383B = Color(0xFF37383B)  // 답변 말풍선 배경
    val c8E8E8E = Color(0xFF8E8E8E)  // '수정' 링크, 타이핑 점, primaryLine 테두리
    val cFFE4D2 = Color(0xFFFFE4D2)  // 선택된 옵션 배경
    val cC5C4C4 = Color(0xFFC5C4C4)  // 미선택 인디케이터 테두리
    val white = Color(0xFFFFFFFF)
}

/** 질문(좌측 회색) / 답변(우측 다크) 말풍선 — iOS QuoteChatBubble. */
@Composable
fun QuoteChatBubble(
    text: String,
    isQuestion: Boolean,
    onEdit: (() -> Unit)? = null,
) {
    if (isQuestion) {
        Row(Modifier.fillMaxWidth()) {
            Bubble(text, QuoteColors.cF4F4F4, QuoteColors.c101116, Modifier.weight(1f, fill = false))
            Spacer(Modifier.width(40.dp)) // iOS Spacer(minLength: 40)
        }
    } else {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Spacer(Modifier.width(40.dp))
                Bubble(text, QuoteColors.c37383B, QuoteColors.white, Modifier.weight(1f, fill = false))
            }
            if (onEdit != null) {
                Spacer(Modifier.height(4.dp)) // iOS VStack spacing 4
                Text(
                    text = "수정",
                    fontFamily = customFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = QuoteColors.c8E8E8E,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { onEdit() },
                )
            }
        }
    }
}

@Composable
private fun Bubble(text: String, bg: Color, fg: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontFamily = customFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        color = fg,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

/** 선택지 행 — multi=체크박스(사각 r6), single=라디오(원). iOS QuoteOptionRow. */
@Composable
fun QuoteOptionRow(
    label: String,
    isSelected: Boolean,
    isMulti: Boolean,
    onTap: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) QuoteColors.cFFE4D2 else QuoteColors.white)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) QuoteColors.f58232 else QuoteColors.cEAEAEA,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable { onTap() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OptionIndicator(isSelected, isMulti)
        Text(
            text = label,
            fontFamily = customFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = QuoteColors.c101116,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun OptionIndicator(isSelected: Boolean, isMulti: Boolean) {
    val borderColor = if (isSelected) QuoteColors.f58232 else QuoteColors.cC5C4C4
    if (isMulti) {
        Box(
            Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isSelected) QuoteColors.f58232 else QuoteColors.white)
                .border(1.5.dp, borderColor, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = QuoteColors.white, modifier = Modifier.size(14.dp))
            }
        }
    } else {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(1.5.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(QuoteColors.f58232))
            }
        }
    }
}

/** 상단 진행바 — 트랙 EAEAEA / 채움 F58232, 높이 6, 우측 "2/12" 13sp semibold. iOS QuoteProgressBar. */
@Composable
fun QuoteProgressBar(progress: Float, stepText: String) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(QuoteColors.cEAEAEA),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(3.dp))
                    .background(QuoteColors.f58232),
            )
        }
        Text(
            text = stepText,
            fontFamily = customFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = QuoteColors.f58232,
            modifier = Modifier.width(40.dp),
        )
    }
}

/**
 * '입력 중…' 타이핑 인디케이터 — 회색 말풍선 안 3점 바운스.
 *  iOS 는 원래 Lottie(typing_dots)였으나 렌더 실패로 **순수 SwiftUI 3점**으로 교체됨(2026-08-06).
 *  여기서도 동일하게 외부 라이브러리 없이 Compose 애니메이션으로 1:1 구현:
 *  점 8dp / 간격 6 / 색 8E8E8E / 배경 F4F4F4 / padding h16 v14 / radius 16 /
 *  easeInOut 0.6s repeatForever, 점마다 0.2s 지연, opacity 0.3→1, scale 0.6→1.
 */
@Composable
fun QuoteTypingBubble() {
    Row(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(QuoteColors.cF4F4F4)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val transition = rememberInfiniteTransition(label = "typing")
            repeat(3) { i ->
                val anim by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 600, delayMillis = 0),
                        repeatMode = RepeatMode.Reverse,
                        initialStartOffset = androidx.compose.animation.core.StartOffset(i * 200),
                    ),
                    label = "dot$i",
                )
                Box(
                    Modifier
                        .size(8.dp)
                        .scale(0.6f + 0.4f * anim)
                        .alpha(0.3f + 0.7f * anim)
                        .clip(CircleShape)
                        .background(QuoteColors.c8E8E8E),
                )
            }
        }
        Spacer(Modifier.width(40.dp))
    }
}

/** 하단 CTA — iOS PrimaryButton(height 48, radius 12, primaryFill/primaryLine). */
@Composable
fun QuotePrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val bg = when {
        !enabled && filled -> Color(0xFFD0D0D0)   // primaryFill disable
        !enabled -> Color(0xFFF7F7F7)             // primaryLine disable
        filled -> QuoteColors.f58232
        else -> QuoteColors.white
    }
    val fg = when {
        !enabled && filled -> QuoteColors.c6D6E71
        !enabled -> QuoteColors.c8E8E8E
        filled -> QuoteColors.white
        else -> QuoteColors.c37383B
    }
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .then(if (filled) Modifier else Modifier.border(0.5.dp, QuoteColors.c8E8E8E, RoundedCornerShape(12.dp)))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontFamily = customFontFamily,
            fontWeight = if (filled) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 16.sp,
            color = fg,
        )
    }
}
