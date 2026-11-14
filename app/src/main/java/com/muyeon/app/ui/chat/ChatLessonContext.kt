package com.muyeon.app.ui.chat

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ★ 2026-11-14 LessonContextArea(10겹 상단 영역)는 dealbar/ChatDealBarArea 로 대체됐다.

/**
 * 대화 종료 카운트다운 한 줄 — "D-N 후 대화 종료"/"오늘 대화 종료" + 보조 안내.
 *  평소 보조색, 7일 이내면 포인트색(견적 만료 'N일 후 만료'와 같은 primary).
 */
@Composable
internal fun LockCountdownLine(days: Int) {
    val warn = ChatLockCountdown.isWarn(days)
    Column(
        Modifier
            .fillMaxWidth()
            .background(MuyeonColors.groupedBg)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("chat.lockCountdown"),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            ChatLockCountdown.roomLabel(days),
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            lineHeight = 16.sp, color = if (warn) MuyeonColors.primary else MuyeonColors.secondary,
        )
        Text(
            ChatLockCountdown.SUBTEXT,
            fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 14.sp, color = MuyeonColors.secondary,
        )
    }
}

/** 채택 직후 일반회원에게 항상 보이는 일정 진입 버튼 — iOS MemberScheduleCTA. */
@Composable
fun MemberScheduleCTA(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MuyeonColors.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.EventAvailable, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Text(
            "레슨 일정 잡기",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            lineHeight = 18.sp, color = Color.White,
        )
        Spacer(Modifier.weight(1f))
        Text(
            "날짜·시간 선택",
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
            lineHeight = 15.sp, color = Color.White.copy(alpha = 0.9f),
        )
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun CapsuleButton(text: String, fontSize: Int, hPad: Int, vPad: Int, onClick: () -> Unit) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = fontSize.sp,
        lineHeight = (fontSize + 3).sp, color = Color.White,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MuyeonColors.primary)
            .clickable(onClick = onClick)
            .padding(horizontal = hPad.dp, vertical = vPad.dp),
    )
}

/** 확정 일정 배너 시각 — iOS SurveyTimeFormat.lessonLabel("M월 d일 (E) a h시 m분"). */
private fun scheduleBannerLabel(iso: String?): String? {
    val t = QuoteUi.parseDate(iso) ?: return null
    return SimpleDateFormat("M월 d일 (E) a h시 m분", Locale.KOREA)
        .apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Seoul") }
        .format(Date(t))
}

// ============================================================
// 레슨 목록 바텀시트 (양방향: 방에 레슨 2건 이상일 때) — iOS LessonCyclesSheet
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonCyclesSheet(
    cycles: List<ChatLessonCycle>,
    opponentImage: String?,
    waitingProposal: Boolean = false,
    acceptBlocked: Boolean = false,
    onTimeline: (ChatLessonCycle) -> Unit,
    onPrimary: (ChatLessonCycle) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MuyeonColors.surface,
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Text(
                "진행 중인 ${cyclesNoun(cycles)} ${cycles.size}건",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                lineHeight = 21.sp, color = MuyeonColors.textHead,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 4.dp),
            )
            Text(
                "이 대화 상대와 진행 중인 레슨이에요. 방향(강사/수강생)을 확인하세요.",
                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp, color = MuyeonColors.secondary,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 14.dp),
            )
            Column(
                Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                cycles.forEach { cycle ->
                    LessonProgressCard(
                        progress = cycle.progress,
                        context = cycle.asContext,
                        category = cycle.title ?: "레슨",
                        isExpired = false,
                        isProposal = cycle.isProposal,
                        personName = cycle.opponentName,
                        personImage = opponentImage,
                        personRole = cycle.opponentRoleLabel,
                        waitingProposal = waitingProposal,
                        acceptBlocked = acceptBlocked,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MuyeonColors.border, RoundedCornerShape(12.dp)),
                        onTimeline = { onTimeline(cycle) },
                        onPrimary = { onPrimary(cycle) },
                    )
                }
            }
        }
    }
}

// ============================================================
// 진행 타임라인 시트 — iOS LessonTimelineSheet
// ============================================================

/** 타임라인 행의 액션(라벨 + 동작). */
private data class TimelineAction(val label: String, val run: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonTimelineSheet(
    progress: ChatLessonProgress,
    context: ChatQuoteContext?,
    category: String?,
    isProposal: Boolean = false,              // 약속잡기 레슨 — 견적 단계 없이 확정→완료만 표시
    onQuoteDetail: (() -> Unit)? = null,      // 고객: 견적 보기
    onSetSchedule: (() -> Unit)? = null,      // 강사: 일정 정하기
    onOpenCalendar: (() -> Unit)? = null,     // 일정 상세
    onReview: (() -> Unit)? = null,           // 고객: 후기 쓰기
    reviewDisabledStyle: Boolean = false,     // 강사 방향 — 후기 액션 미노출
    onDismiss: () -> Unit,
) {
    // ★ 11-10 학원 채용(afterAccept=HIRE) — 요청 → 제안 → 채용 확정 3단계. 일정·후기 행이 없다.
    val hire = context?.isHire == true
    val reviewAction = if (hire || reviewDisabledStyle || progress.stepIndex < 4) null
    else onReview?.let { TimelineAction("후기 쓰기", it) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MuyeonColors.surface,
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Text(
                "${if (hire) hireCategory(category) else category ?: "레슨"} 진행 현황",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                lineHeight = 21.sp, color = MuyeonColors.textHead,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 14.dp),
            )
            if (hire && progress.stepIndex >= 2) {
                Text(
                    "학원 채용은 레슨 일정·예약금·후기가 없어요. 근무 일정과 조건은 채팅으로 이야기해 주세요.",
                    fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 17.sp, color = MuyeonColors.secondary,
                    modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp),
                )
            }
            Column(Modifier.padding(horizontal = 20.dp)) {
                val startLabel = lessonProgressDateTime(progress.scheduleStartAt)
                if (hire) {
                    TimelineRow(progress, 0, "학원 요청", lessonProgressDateTime(progress.requestedAt), null)
                    TimelineRow(
                        progress, 1,
                        "제안 ${progress.responseCount ?: 1}건 도착" + (context?.priceText?.let { " · $it" } ?: ""),
                        lessonProgressDateTime(progress.firstResponseAt),
                        onQuoteDetail?.let { TimelineAction("제안 보기", it) },
                    )
                    TimelineRow(
                        progress, 2, "채용 확정", lessonProgressDateTime(progress.acceptedAt), null, isLast = true,
                    )
                } else if (isProposal) {
                    // 약속잡기 — 견적/채택 단계 없이 '약속 확정 → 완료' 2단계.
                    TimelineRow(
                        progress, 3,
                        if (progress.scheduleStartAt != null) "레슨 약속 확정 · ${startLabel.orEmpty()}" else "레슨 약속 확정",
                        lessonProgressDateTime(progress.scheduledAt),
                        onOpenCalendar?.let { TimelineAction("일정 보기", it) },
                    )
                    TimelineRow(
                        progress, 4, "레슨 완료", lessonProgressDateTime(progress.completedAt),
                        reviewAction, isLast = true,
                    )
                } else {
                    TimelineRow(progress, 0, "견적 요청", lessonProgressDateTime(progress.requestedAt), null)
                    TimelineRow(
                        progress, 1,
                        "견적 ${progress.responseCount ?: 1}건 도착" + (context?.priceText?.let { " · $it" } ?: ""),
                        lessonProgressDateTime(progress.firstResponseAt),
                        onQuoteDetail?.let { TimelineAction("견적 보기", it) },
                    )
                    TimelineRow(
                        progress, 2, "강사 채택", lessonProgressDateTime(progress.acceptedAt),
                        if (progress.stepIndex == 2) onSetSchedule?.let { TimelineAction("일정 정하기", it) } else null,
                    )
                    TimelineRow(
                        progress, 3,
                        if (progress.scheduleStartAt != null) "일정 확정 · ${startLabel.orEmpty()}" else "일정 확정",
                        lessonProgressDateTime(progress.scheduledAt),
                        if (progress.stepIndex >= 3) onOpenCalendar?.let { TimelineAction("일정 보기", it) } else null,
                    )
                    TimelineRow(
                        progress, 4, "레슨 완료", lessonProgressDateTime(progress.completedAt),
                        reviewAction, isLast = true,
                    )
                }
            }
        }
    }
}

/** 타임라인 행 — 점·연결선 + 라벨/시각 + 액션. 현재 단계 점은 링이 퍼져나가는 파동으로 강조한다. */
@Composable
private fun TimelineRow(
    progress: ChatLessonProgress,
    index: Int,
    title: String,
    time: String?,
    action: TimelineAction?,
    isLast: Boolean = false,
) {
    val reached = index <= progress.stepIndex
    val isCurrent = index == progress.stepIndex
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.width(10.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            val dotModifier = if (isCurrent) {
                val pulse = rememberInfiniteTransition(label = "pulse")
                val p by pulse.animateFloat(
                    0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "p",
                )
                Modifier.drawBehind {
                    val r = size.minDimension / 2f * (1f + 1.6f * p)
                    drawCircle(MuyeonColors.primary.copy(alpha = 0.7f * (1f - p)), radius = r, style = Stroke(2.dp.toPx()))
                }
            } else Modifier
            Box(
                dotModifier.size(10.dp).clip(CircleShape)
                    .background(if (reached) MuyeonColors.primary else Color(0xFFD1D1D6)),
            )
            if (!isLast) {
                Box(
                    Modifier.width(2.dp).weight(1f).heightIn(min = 30.dp)
                        .background(if (index < progress.stepIndex) MuyeonColors.primary else MuyeonColors.border),
                )
            }
        }
        Column(
            Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                title,
                fontFamily = customFontFamily,
                fontWeight = if (reached) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp, lineHeight = 17.sp,
                color = if (reached) MuyeonColors.textHead else MuyeonColors.secondary,
            )
            time?.let {
                Text(it, fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp, color = MuyeonColors.secondary)
            }
        }
        action?.let { a ->
            Text(
                a.label,
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 15.sp,
                color = MuyeonColors.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, MuyeonColors.primary, RoundedCornerShape(50))
                    .clickable(onClick = a.run)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

// ============================================================
// 재입장 방 견적 요약 시트 — iOS QuoteContextSummarySheet
//  (카드 메시지가 leftAt 으로 숨겨진 방에서 서버 컨텍스트로 과목·금액·상태를 보여 준다)
// ============================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteContextSummarySheet(
    context: ChatQuoteContext?,
    category: String?,
    onOpenDetail: (() -> Unit)?,   // 고객만: 받은 제안 상세로
    onDismiss: () -> Unit,
) {
    val statusLabel = when {
        context == null -> "-"
        context.matched -> if (context.responseStatus == "ACCEPTED") "채택됨 · 진행 중" else "다른 견적 채택됨"
        context.quoteStatus == "EXPIRED" -> "견적 마감"
        else -> "응답 대기 중"
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MuyeonColors.surface) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "진행 중 견적",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                lineHeight = 21.sp, color = MuyeonColors.textHead,
            )
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MuyeonColors.groupedBg).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SummaryRow("과목", category ?: "레슨")
                context?.priceText?.let { SummaryRow("견적 금액", it, accent = true) }
                context?.quoteCount?.takeIf { it > 1 }?.let { SummaryRow("받은 제안", "${it}건") }
                SummaryRow("상태", statusLabel)
            }
            Text(
                "이전 대화를 정리한 방이라 견적 카드 메시지는 보이지 않지만, 견적은 그대로 진행 중이에요.",
                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp, color = MuyeonColors.secondary,
            )
            onOpenDetail?.let { open ->
                Text(
                    "받은 제안 상세 보기",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp,
                    color = Color.White, textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MuyeonColors.primary)
                        .clickable(onClick = open)
                        .padding(vertical = 13.dp),
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, accent: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp, color = MuyeonColors.secondary)
        Spacer(Modifier.weight(1f))
        Text(
            value,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 17.sp,
            color = if (accent) MuyeonColors.primary else MuyeonColors.textHead,
        )
    }
}
