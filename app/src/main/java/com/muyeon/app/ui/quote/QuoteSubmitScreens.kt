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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import kotlinx.coroutines.delay

private val cF7F7F7s = Color(0xFFF7F7F7)

/**
 * 견적 제출 후 매칭 로딩 — iOS `QuoteSubmitLoadingView.swift` 1:1.
 *  0→100% 를 28ms 간격(≈2.8s)으로 카운트업, 구간별 2줄 문구 전환, 상단 진행선(5dp),
 *  중앙 스켈레톤 펄스(0.85s autoreverse, opacity 0.4↔1). 100% 후 0.3s 뒤 onDone.
 */
@Composable
fun QuoteSubmitLoadingScreen(
    categoryTitle: String,
    isDirect: Boolean,
    onDone: () -> Unit,
) {
    var percent by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (percent < 100) {
            delay(28)
            percent += 1
        }
        delay(300)
        onDone()
    }

    val headTitle = if (categoryTitle.isEmpty()) "딱 맞는 강사님을" else "$categoryTitle 강사님을"
    val (line1, line2) = when {
        isDirect && percent < 34 -> "내 요청을" to "확인하고 있어요"
        isDirect && percent < 70 -> "강사님께 요청을" to "보내고 있어요"
        isDirect -> "요청 전달을" to "마무리하고 있어요"
        percent < 34 -> "내 요청을" to "확인하고 있어요"
        percent < 70 -> headTitle to "찾고 있어요"
        else -> "견적을" to "받고 있어요"
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(QuoteColors.white),
        horizontalAlignment = Alignment.Start,
    ) {
        // 단계 타이틀(2줄) — 28sp bold, spacing 6, top 28 / h24
        Column(
            Modifier.padding(top = 28.dp).padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(line1, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = QuoteColors.c101116)
            Text(line2, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, color = QuoteColors.c101116)
        }
        // 퍼센트 — 22sp bold 오렌지, top 14 / h24
        Text(
            "$percent%",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp,
            color = QuoteColors.f58232,
            modifier = Modifier.padding(top = 14.dp).padding(horizontal = 24.dp),
        )
        // 진행선(full-bleed) — 높이 5, top 24
        Box(
            Modifier
                .padding(top = 24.dp)
                .fillMaxWidth()
                .height(5.dp)
                .background(QuoteColors.cF4F4F4)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(percent / 100f)
                    .background(QuoteColors.f58232)
            )
        }

        Spacer(Modifier.weight(1f))

        // 스켈레톤 펄스 — 0.85s autoreverse, opacity 0.4↔1
        val pulse = rememberInfiniteTransition(label = "pulse")
        val alpha by pulse.animateFloat(
            initialValue = 0.4f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
            label = "alpha",
        )
        Column(
            Modifier.fillMaxWidth().alpha(alpha),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SkeletonRow(listOf(120.dp, 180.dp))
            SkeletonRow(listOf(150.dp, 210.dp))
        }

        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun SkeletonRow(lineWidths: List<androidx.compose.ui.unit.Dp>) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(QuoteColors.cEAEAEA))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            lineWidths.forEach { w ->
                Box(Modifier.width(w).height(12.dp).clip(RoundedCornerShape(6.dp)).background(QuoteColors.cEAEAEA))
            }
        }
    }
}

/**
 * 요청 완료 안내 — iOS `QuoteSubmittedView.swift` 1:1.
 *  체크 원 72 + 스파클, 제목 22sp bold, 부제 14sp medium, 안내 3행 카드(r16 EAEAEA),
 *  CTA 2종(v15 r12), TIP 카드(F7F7F7 r12 p14).
 */
@Composable
fun QuoteSubmittedScreen(
    onViewQuotes: () -> Unit,
    onGoHome: () -> Unit,
    onClose: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(QuoteColors.white)) {
        // navBar — 제목 18sp bold, X 18, p h20 v14
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), contentAlignment = Alignment.Center) {
            Text("레슨 요청", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = QuoteColors.c101116)
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterEnd)) {
                Icon(Icons.Filled.Close, "닫기", tint = QuoteColors.c37383B, modifier = Modifier.size(18.dp))
            }
        }
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CheckMark(Modifier.padding(top = 28.dp))
            Text(
                "요청이 전달되었어요!",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                color = QuoteColors.c101116, modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                "강사들이 견적을 준비 중이에요.\n견적이 도착하면 알림으로 알려드릴게요.",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                color = QuoteColors.c6D6E71, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )

            // 안내 3행 카드
            Column(
                Modifier
                    .padding(top = 24.dp)
                    .fillMaxWidth()
                    .border(1.dp, QuoteColors.cEAEAEA, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp),
            ) {
                InfoRow(Icons.Outlined.Notifications, "견적이 도착하면 알려드려요",
                    "새로운 견적이 오면 알림으로 바로 알려드려요.")
                Divider(color = QuoteColors.cEAEAEA)
                InfoRow(Icons.Outlined.Scale, "여러 강사의 견적을 비교해보세요",
                    "금액, 경력, 수업 방식 등을 비교해\n나에게 맞는 강사를 선택할 수 있어요.")
                Divider(color = QuoteColors.cEAEAEA)
                InfoRow(Icons.Outlined.ChatBubbleOutline, "마음에 드는 강사와 바로 채팅하세요",
                    "궁금한 점을 물어보고\n레슨을 확정할 수 있어요.")
            }

            // CTA 2종
            Box(
                Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(QuoteColors.f58232)
                    .clickable { onViewQuotes() }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("내 견적 보기", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = QuoteColors.white)
            }
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, QuoteColors.f58232, RoundedCornerShape(12.dp))
                    .clickable { onGoHome() }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("홈으로 이동", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = QuoteColors.f58232)
            }

            // TIP
            Row(
                Modifier
                    .padding(top = 16.dp, bottom = 24.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(cF7F7F7s)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Outlined.Lightbulb, null, tint = QuoteColors.f58232,
                    modifier = Modifier.padding(top = 2.dp).size(14.dp))
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = QuoteColors.f58232, fontWeight = FontWeight.Bold)) { append("TIP  ") }
                        withStyle(SpanStyle(color = QuoteColors.c6D6E71, fontWeight = FontWeight.Medium)) {
                            append("요청을 수정하거나 취소하고 싶다면\n마이페이지 > 레슨 요청 내역에서 가능해요.")
                        }
                    },
                    fontFamily = customFontFamily, fontSize = 13.sp,
                )
            }
        }
    }
}

/** 체크 원 72 + 스파클(45° 회전 사각, 오렌지 50%) — iOS checkMark. */
@Composable
private fun CheckMark(modifier: Modifier = Modifier) {
    Box(modifier.height(110.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Sparkle(-58, -30, 6); Sparkle(62, -38, 5); Sparkle(-70, 16, 4); Sparkle(70, 24, 6); Sparkle(12, -52, 4)
        Box(Modifier.size(72.dp).clip(CircleShape).background(QuoteColors.f58232), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, null, tint = QuoteColors.white, modifier = Modifier.size(30.dp))
        }
    }
}

@Composable
private fun Sparkle(x: Int, y: Int, size: Int) {
    Box(
        Modifier
            .offset(x = x.dp, y = y.dp)
            .size(size.dp)
            .rotate(45f)
            .background(QuoteColors.f58232.copy(alpha = 0.5f))
    )
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, desc: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(QuoteColors.f58232.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = QuoteColors.f58232, modifier = Modifier.size(18.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = QuoteColors.c101116)
            Text(desc, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = QuoteColors.c6D6E71)
        }
    }
}
