package com.muyeon.app.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteDialog
import kotlinx.coroutines.launch

/**
 * 한 회차(수업)의 예약자 명단 + 출석 체크 — iOS `ClassDetailView.swift` 1:1.
 *  상단 요약(레슨명·일시·예약인원/출석/결석) + 회원별 예약/출석/결석 드롭다운.
 *  출석 상태는 정원을 바꾸지 않는다(자리 유지).
 *
 * ⚠️ 종전 AOS 는 이 화면이 통째로 없었다. `LessonSlotManageScreen` 은
 *   "예약자 명단 확인과 출석 체크는 그대로 하실 수 있어요" 라고 안내하면서
 *   회차를 탭할 수조차 없었고, setAttendance API 는 호출하는 곳이 하나도 없었다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailSheet(
    api: LessonSlotApi,
    productTitle: String,
    slot: LessonSlot,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var roster by remember { mutableStateOf<List<LessonSlotReservation>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // 출석인데 수강권이 여러 개면 '어느 권에서 차감할지' 물어본다(iOS confirmationDialog).
    var passPickerFor by remember { mutableStateOf<LessonSlotReservation?>(null) }

    suspend fun load() {
        // 실패해도 기존 명단은 유지한다 — iOS 와 동일(빈 화면으로 되돌아가지 않는다).
        api.slotReservations(slot.id).onSuccess { roster = it }
        loading = false
    }

    LaunchedEffect(slot.id) { load() }

    /** 낙관적 갱신 후 서버 반영, 실패하면 되돌린다. */
    fun apply(r: LessonSlotReservation, state: SlotAttendance, passId: Int? = null) {
        val previous = r.status
        roster = roster.map { if (it.id == r.id) it.copy(status = state.name) else it }
        scope.launch {
            api.setAttendance(slot.id, r.id, state.name, passId).onFailure { e ->
                roster = roster.map { if (it.id == r.id) it.copy(status = previous) else it }
                errorMessage = e.message ?: "출석 처리에 실패했어요."
            }
        }
    }

    fun choose(r: LessonSlotReservation, state: SlotAttendance) {
        if (state == SlotAttendance.ATTENDED && r.passes.size > 1) passPickerFor = r
        else apply(r, state)
    }

    val attended = roster.count { it.attendance == SlotAttendance.ATTENDED }
    val noshow = roster.count { it.attendance == SlotAttendance.NOSHOW }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    "예약자 · 출석",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    lineHeight = 20.sp, color = MuyeonColors.textHead,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "닫기",
                    fontFamily = customFontFamily, fontSize = 15.sp, color = MuyeonColors.textSub,
                    modifier = Modifier.align(Alignment.CenterStart).clickable(onClick = onDismiss),
                )
            }

            Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 560.dp)) {
                // 상단 요약 카드
                Column(
                    Modifier.padding(horizontal = 20.dp, vertical = 6.dp).fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)).background(MuyeonColors.primary).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        productTitle,
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                        lineHeight = 22.sp, color = Color.White,
                    )
                    Text(
                        "${slot.date} · ${LessonTimeFmt.ampm(slot.startTime)}~${LessonTimeFmt.ampm(slot.endTime)}",
                        fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                    Row(
                        Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        HeaderStat("예약 인원", "${slot.reservedCount}/${slot.capacity}")
                        if (roster.isNotEmpty()) {
                            HeaderStat("출석", "$attended")
                            HeaderStat("결석", "$noshow")
                        }
                    }
                }

                when {
                    loading -> Box(Modifier.fillMaxWidth().height(160.dp), Alignment.Center) {
                        CircularProgressIndicator(color = MuyeonColors.primary)
                    }
                    roster.isEmpty() -> Column(
                        Modifier.fillMaxWidth().padding(horizontal = 30.dp, vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "아직 예약한 수강생이 없어요",
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                            lineHeight = 18.sp, color = MuyeonColors.textHead,
                        )
                        Text(
                            "수강생이 이 회차를 예약하면 여기에 표시돼요.",
                            fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 17.sp,
                            color = MuyeonColors.textSub, textAlign = TextAlign.Center,
                        )
                    }
                    else -> {
                        Text(
                            "예약자 명단",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            lineHeight = 19.sp, color = MuyeonColors.textHead,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 4.dp),
                        )
                        roster.forEach { r ->
                            MemberRow(
                                r = r,
                                onPick = { st -> choose(r, st) },
                                onDecide = { approved ->
                                    scope.launch {
                                        api.decideReschedule(r.id, approved)
                                            .onSuccess { load() }
                                            .onFailure { errorMessage = it.message ?: "요청을 처리하지 못했어요." }
                                    }
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    passPickerFor?.let { r ->
        PassPickerSheet(
            passes = r.passes,
            onPick = { passId -> passPickerFor = null; apply(r, SlotAttendance.ATTENDED, passId) },
            onAuto = { passPickerFor = null; apply(r, SlotAttendance.ATTENDED) },
            onDismiss = { passPickerFor = null },
        )
    }

    errorMessage?.let { msg ->
        QuoteDialog("알림", msg, "확인", onConfirm = { errorMessage = null }, onDismiss = { errorMessage = null })
    }
}

@Composable
private fun HeaderStat(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 14.sp,
            color = Color.White.copy(alpha = 0.85f),
        )
        Text(
            value,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            lineHeight = 18.sp, color = Color.White,
        )
    }
}

/** 회원 한 줄 — 이름/전화/인원 + 출석 드롭다운 (+ 일정 변경 요청 승인·거절). */
@Composable
private fun MemberRow(
    r: LessonSlotReservation,
    onPick: (SlotAttendance) -> Unit,
    onDecide: (Boolean) -> Unit,
) {
    Column {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        r.name,
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        lineHeight = 18.sp, color = MuyeonColors.textHead,
                    )
                    if (r.headcount > 1) {
                        Text(
                            "${r.headcount}명",
                            fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp,
                            color = MuyeonColors.textSub,
                        )
                    }
                }
                r.phone?.takeIf { it.isNotEmpty() }?.let {
                    Text(
                        it,
                        fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 15.sp,
                        color = MuyeonColors.textSub,
                    )
                }
            }
            AttendanceMenu(r.attendance, onPick)
        }
        if (r.rescheduleStatus == "REQUESTED") {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "일정 변경 요청",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    lineHeight = 16.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                )
                Text(
                    "거절",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    lineHeight = 16.sp, color = MuyeonColors.textSub,
                    modifier = Modifier.clip(RoundedCornerShape(50))
                        .border(1.dp, MuyeonColors.border, RoundedCornerShape(50))
                        .clickable { onDecide(false) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
                Text(
                    "승인",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                    lineHeight = 16.sp, color = Color.White,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(MuyeonColors.primary)
                        .clickable { onDecide(true) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        HorizontalDivider(color = MuyeonColors.border, modifier = Modifier.padding(horizontal = 20.dp))
    }
}

/** 예약/출석/결석 드롭다운 — 현재 상태를 색 캡슐로 표시(iOS Menu). */
@Composable
private fun AttendanceMenu(current: SlotAttendance, onPick: (SlotAttendance) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.clip(RoundedCornerShape(50)).background(current.tint)
                .clickable { open = true }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                current.label,
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                lineHeight = 16.sp, color = Color.White,
            )
            Icon(Icons.Filled.ExpandMore, null, tint = Color.White, modifier = Modifier.size(11.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SlotAttendance.entries.forEach { st ->
                DropdownMenuItem(
                    text = {
                        Text(
                            st.label,
                            fontFamily = customFontFamily,
                            fontWeight = if (st == current) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp, color = MuyeonColors.textHead,
                        )
                    },
                    leadingIcon = if (st == current) {
                        { Icon(Icons.Filled.Check, null, tint = MuyeonColors.primary, modifier = Modifier.size(15.dp)) }
                    } else null,
                    onClick = { open = false; onPick(st) },
                )
            }
        }
    }
}

/** 차감할 수강권 선택 — iOS confirmationDialog("차감할 수강권 선택"). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PassPickerSheet(
    passes: List<RosterPass>,
    onPick: (Int) -> Unit,
    onAuto: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                "차감할 수강권 선택",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                lineHeight = 19.sp, color = MuyeonColors.textHead,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            )
            HorizontalDivider(color = MuyeonColors.border)
            passes.forEach { p ->
                Text(
                    p.label,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                    modifier = Modifier.fillMaxWidth().clickable { onPick(p.id) }
                        .padding(horizontal = 20.dp, vertical = 13.dp),
                )
            }
            Text(
                "자동 선택 (만료 임박순)",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                lineHeight = 18.sp, color = MuyeonColors.primary,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onAuto)
                    .padding(horizontal = 20.dp, vertical = 13.dp),
            )
        }
    }
}
