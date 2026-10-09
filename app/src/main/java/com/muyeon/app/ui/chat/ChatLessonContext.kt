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

/**
 * 채팅방 상단 레슨 컨텍스트 영역 — iOS `ChatRoomView.lessonContextArea` 이식.
 *  표시 순서·조건·문구는 iOS 와 같다. 동작(화면 이동·시트·확인창)은 모두 호출부 콜백이 담당한다.
 *
 *  1) 레슨 사이클 2건 이상 → 요약 한 줄(탭 → 레슨 목록 시트) / 1건 → 진행 카드
 *  2) 레거시(사이클 없음 + progress) → 대표 진행 카드
 *  3) 대기 중 약속 제안(재입장으로 카드가 숨겨진 경우) 상단 고정
 *  4) 회원 일정 잡기 고정 버튼
 *  5) 견적 헤더("발레 레슨 · 견적 N개 도착")
 *  6) 확정 일정 배너 / 채택 유도 / 채택 완료 안내 중 하나
 */
@Composable
fun LessonContextArea(
    vm: ChatRoomViewModel,
    onOpenCycles: () -> Unit,
    onCycleTimeline: (ChatLessonCycle) -> Unit,
    onCyclePrimary: (ChatLessonCycle) -> Unit,
    onLegacyTimeline: () -> Unit,
    onLegacyPrimary: (ChatLessonProgress) -> Unit,
    onProposalChanged: () -> Unit,
    onOpenProposalPayment: (Int) -> Unit,
    onMemberSchedule: () -> Unit,
    onQuoteHeader: () -> Unit,
    onOpenLesson: (Int) -> Unit,
    onAcceptQuote: () -> Unit,
    onMatchedAction: () -> Unit,
) {
    val cycles = vm.lessonCycles
    val progress = vm.progress
    Column(Modifier.fillMaxWidth()) {
        // 양방향 레슨 표시: 2건 이상은 요약 한 줄(카드 여러 장으로 인한 혼란 방지), 1건은 단일 진행 카드.
        if (cycles.size >= 2) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MuyeonColors.groupedBg)
                    .clickable(onClick = onOpenCycles)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.List, null, tint = MuyeonColors.primary, modifier = Modifier.size(15.dp))
                Text(
                    "진행 중인 레슨 ${cycles.size}건",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    lineHeight = 17.sp, color = MuyeonColors.textHead,
                )
                if (cycles.any { it.needsMyAction }) {
                    Text(
                        "일정 확정 필요",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                        lineHeight = 13.sp, color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50)).background(MuyeonColors.primary)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                    tint = MuyeonColors.secondary, modifier = Modifier.size(14.dp),
                )
            }
        } else if (cycles.size == 1) {
            val cycle = cycles.first()
            LessonProgressCard(
                progress = cycle.progress,
                context = cycle.asContext,
                category = cycle.title ?: "레슨",
                isExpired = false,
                isProposal = cycle.isProposal,
                onTimeline = { onCycleTimeline(cycle) },
                onPrimary = { onCyclePrimary(cycle) },
            )
        }
        // 레거시(구백엔드: lessonCycles 없음) — 대표 단일 진행 카드 폴백
        if (cycles.isEmpty() && progress != null) {
            LessonProgressCard(
                progress = progress,
                context = vm.quoteContext,
                category = vm.contextCategory,
                isExpired = vm.isQuoteExpired,
                onTimeline = onLegacyTimeline,
                onPrimary = { onLegacyPrimary(progress) },
            )
        }

        // 재입장(나가기)으로 제안 카드가 대화에서 숨겨진 경우 — 대기 중 제안을 상단에 고정 노출.
        //  강사 판정은 서버 isProposer(제안자=열람자) 값을 쓴다 — 견적 없는 방에서 오판 방지.
        vm.pendingProposal?.let { pp ->
            Box(
                Modifier.fillMaxWidth().background(MuyeonColors.groupedBg)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                LessonProposalBubble(
                    contentJson = pp.json,
                    isProposer = pp.isProposer,
                    currentUserId = vm.currentUserId,
                    token = vm.tokenForCards,
                    onChanged = onProposalChanged,
                    onOpenPayment = onOpenProposalPayment,
                )
            }
        }

        // 일반회원 핵심 CTA 는 진행 카드 내부 상태에 의존하지 않는다 — 채택 직후 바로 날짜·시간을 제안할 수 있어야 한다.
        if (vm.memberNeedsSchedule && vm.pendingProposal == null) {
            MemberScheduleCTA(onMemberSchedule)
        }

        // 견적 헤더 — 진행 카드/사이클 카드가 둘 다 없는 방(공간 문의 등)만 폴백.
        if (cycles.isEmpty() && progress == null && vm.quoteCount > 0) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MuyeonColors.groupedBg)
                    .clickable(onClick = onQuoteHeader)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                vm.contextCategory?.let {
                    Text(
                        it, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        lineHeight = 17.sp, color = MuyeonColors.textHead,
                    )
                }
                if (vm.isQuoteExpired) QuoteExpiredPill()
                Spacer(Modifier.weight(1f))
                Text(
                    buildAnnotatedString {
                        append("견적 ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MuyeonColors.primary)) {
                            append("${vm.quoteCount}개")
                        }
                        append(" 도착")
                    },
                    fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp, color = MuyeonColors.secondary,
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                    tint = MuyeonColors.secondary, modifier = Modifier.size(14.dp),
                )
            }
        }

        // 확정 일정 배너 — 사이클/진행이 전혀 없을 때만(사이클 카드가 있으면 각 카드가 일정 상태를 담당).
        val sch = vm.lessonSchedule
        if (sch != null && sch.confirmed && cycles.isEmpty() && progress == null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MuyeonColors.primary.copy(alpha = 0.07f))
                    .clickable { onOpenLesson(sch.lessonId) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(9.dp))
                        .background(MuyeonColors.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.CalendarMonth, null, tint = MuyeonColors.primary, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        sch.title ?: "레슨",
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                        lineHeight = 15.sp, color = MuyeonColors.primary,
                    )
                    Text(
                        scheduleBannerLabel(sch.startAt) ?: "일정 확정됨",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        lineHeight = 17.sp, color = MuyeonColors.textHead, maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    sch.place?.takeIf { it.isNotEmpty() }?.let {
                        Text(
                            it, fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 14.sp,
                            color = MuyeonColors.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                    tint = MuyeonColors.primary, modifier = Modifier.size(18.dp),
                )
            }
        } else if (cycles.isEmpty() && vm.canAcceptQuote) {
            // 고객(미채택): 진행 CTA — 사이클이 있으면 각 카드가 채택을 담당하므로 폴백만.
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MuyeonColors.primary.copy(alpha = 0.08f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        "이 강사와 진행하시겠어요?",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                        lineHeight = 16.sp, color = MuyeonColors.textHead,
                    )
                    vm.quoteContext?.priceText?.let {
                        Text(
                            "받은 제안 $it",
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                            lineHeight = 15.sp, color = MuyeonColors.primary,
                        )
                    }
                }
                CapsuleButton("이 강사로 진행하기", fontSize = 13, hPad = 12, vPad = 7, onClick = onAcceptQuote)
            }
        } else if (progress == null && vm.isQuoteMatched) {
            // 채택됐지만 일정 미확정 → 양쪽 모두 채팅 안에서 일정 진행.
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(MuyeonColors.primary.copy(alpha = 0.06f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Verified, null, tint = MuyeonColors.primary, modifier = Modifier.size(15.dp))
                Text(
                    if (vm.isTeacherSide) "채택 완료 — 일정을 확정해 주세요" else "채택 완료 — 원하는 일정을 제안해 주세요",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                    lineHeight = 16.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                )
                CapsuleButton(
                    if (vm.isTeacherSide) "일정 확정" else "일정 잡기",
                    fontSize = 12, hPad = 10, vPad = 6, onClick = onMatchedAction,
                )
            }
        }
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
                "진행 중인 레슨 ${cycles.size}건",
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
    val reviewAction = if (reviewDisabledStyle || progress.stepIndex < 4) null
    else onReview?.let { TimelineAction("후기 쓰기", it) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MuyeonColors.surface,
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Text(
                "${category ?: "레슨"} 진행 현황",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                lineHeight = 21.sp, color = MuyeonColors.textHead,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 14.dp),
            )
            Column(Modifier.padding(horizontal = 20.dp)) {
                val startLabel = lessonProgressDateTime(progress.scheduleStartAt)
                if (isProposal) {
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
