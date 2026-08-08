package com.muyeon.app.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.muyeon.app.ui.quote.QuoteNavBar
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

/**
 * 예약 가능 시간 관리 — iOS `LessonSlotManageView.swift` 1:1.
 *  레슨 선택 → 요일 규칙(템플릿) 추가/삭제 → 서버가 실제 슬롯 생성 → 생성된 슬롯 확인.
 *
 * ⚠️ 규칙을 지우면 아직 예약이 없는 미래 슬롯만 사라진다(서버 규칙). 이미 예약된 슬롯은 남는다.
 */
@Composable
fun LessonSlotManageScreen(
    api: LessonSlotApi,
    initialProductId: Int?,
    onClose: () -> Unit,
) {
    var products by remember { mutableStateOf<List<MyLessonProduct>>(emptyList()) }
    var selected by remember { mutableStateOf<MyLessonProduct?>(null) }
    var templates by remember { mutableStateOf<List<LessonSlotTemplate>>(emptyList()) }
    var slots by remember { mutableStateOf<List<LessonSlot>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<LessonSlotTemplate?>(null) }

    // 새 규칙 입력값 — iOS 기본값(수요일 19:00~20:00, 정원 8)
    var newDay by remember { mutableIntStateOf(3) }
    var newStart by remember { mutableStateOf("19:00") }
    var newEnd by remember { mutableStateOf("20:00") }
    var newCapacity by remember { mutableIntStateOf(8) }

    val scope = rememberCoroutineScope()
    val (from, to) = remember { slotRange() }

    suspend fun reload() {
        val p = selected ?: return
        templates = api.templates(p.id).getOrDefault(emptyList())
        slots = api.slots(p.id, from, to).getOrDefault(emptyList())
    }

    LaunchedEffect(Unit) {
        products = api.myProducts().getOrDefault(emptyList())
        selected = products.firstOrNull { it.id == initialProductId } ?: products.firstOrNull()
        selected?.let { newCapacity = it.maxParticipants ?: 8 }
        reload()
        loading = false
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "예약 가능 시간", onBack = onClose)

        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            return@Column
        }
        if (products.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                Text(
                    "개설한 레슨이 없어요.\n레슨을 먼저 개설해 주세요.",
                    fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp,
                    color = MuyeonColors.textSub, textAlign = TextAlign.Center,
                )
            }
            return@Column
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // 안내
            Column(
                Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(MuyeonColors.primary.copy(alpha = 0.07f)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                GuideStep(1, "레슨을 고르고")
                GuideStep(2, "요일·시간 규칙을 추가하면")
                GuideStep(3, "예약 가능한 시간이 자동으로 만들어져요")
            }

            // 레슨 선택
            SectionHeader("레슨 선택")
            var menuOpen by remember { mutableStateOf(false) }
            Box {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
                        .clickable { menuOpen = true }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        selected?.title ?: "레슨을 선택하세요",
                        fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 18.sp,
                        color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Filled.KeyboardArrowDown, null, tint = MuyeonColors.secondary, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    products.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p.title, fontFamily = customFontFamily, fontSize = 14.sp) },
                            onClick = {
                                menuOpen = false
                                selected = p
                                newCapacity = p.maxParticipants ?: 8
                                scope.launch { reload() }
                            },
                        )
                    }
                }
            }

            // 규칙 추가
            SectionHeader("요일·시간 규칙 추가")
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                LessonOptions.weekdays.forEach { (value, label) ->
                    val on = newDay == value
                    Text(
                        label,
                        fontFamily = customFontFamily,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp, lineHeight = 16.sp, textAlign = TextAlign.Center,
                        color = if (on) Color.White else MuyeonColors.textSub,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(50))
                            .background(if (on) MuyeonColors.primary else Color(0xFFF2F2F7))
                            .clickable { newDay = value }.padding(vertical = 9.dp),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeMenu("시작", newStart, Modifier.weight(1f)) {
                    newStart = it
                    // 시작을 바꾸면 종료를 60분 뒤로 자동 보정(역전 방지)
                    newEnd = LessonOptions.addMinutes(it, 60)
                }
                TimeMenu("종료", newEnd, Modifier.weight(1f)) { newEnd = it }
                CapacityMenu(newCapacity, Modifier.weight(1f)) { newCapacity = it }
            }
            Text(
                "규칙 추가",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                lineHeight = 18.sp, color = Color.White, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(MuyeonColors.primary)
                    .clickable {
                        val p = selected ?: return@clickable
                        scope.launch {
                            api.createTemplate(p.id, newDay, newStart, newEnd, newCapacity)
                                .onFailure { errorMessage = it.message }
                            reload()
                        }
                    }
                    .padding(vertical = 13.dp),
            )

            // 등록된 규칙
            SectionHeader("등록된 규칙", "${templates.size}개")
            if (templates.isEmpty()) {
                Text(
                    "아직 규칙이 없어요.",
                    fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = MuyeonColors.textSub,
                )
            }
            templates.forEach { t ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${t.dayLabel} ${t.timeLabel} · 정원 ${t.capacity}명",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                        lineHeight = 17.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Filled.Close, "삭제", tint = MuyeonColors.secondary,
                        modifier = Modifier.size(16.dp).clickable { deleteTarget = t },
                    )
                }
            }

            HorizontalDivider(color = MuyeonColors.border)

            // 생성된 슬롯(앞으로 4주)
            SectionHeader("생성된 예약 시간", "$from ~ $to")
            if (slots.isEmpty()) {
                Text(
                    "생성된 예약 시간이 없어요. 규칙을 추가하면 자동으로 만들어져요.",
                    fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = MuyeonColors.textSub,
                )
            }
            slots.forEach { s -> SlotRow(s) }
            Spacer(Modifier.height(12.dp))
        }
    }

    deleteTarget?.let { t ->
        QuoteDialog(
            title = "이 규칙을 삭제할까요?",
            message = "${t.dayLabel} ${t.timeLabel}\n예약이 없는 앞으로의 시간만 사라져요. 이미 예약된 시간은 남습니다.",
            confirmText = "삭제",
            onConfirm = {
                deleteTarget = null
                scope.launch { api.deleteTemplate(t.id).onFailure { errorMessage = it.message }; reload() }
            },
            onDismiss = { deleteTarget = null },
        )
    }
    errorMessage?.let { msg ->
        QuoteDialog("알림", msg, "확인", onConfirm = { errorMessage = null }, onDismiss = { errorMessage = null })
    }
}

/** 조회 구간 — 오늘부터 4주(iOS range()). */
private fun slotRange(): Pair<String, String> {
    val cal = kstCalendar()
    val from = kstYmd.format(Date(cal.timeInMillis))
    cal.add(Calendar.DAY_OF_MONTH, 28)
    return from to kstYmd.format(Date(cal.timeInMillis))
}

@Composable
private fun SectionHeader(title: String, caption: String? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = MuyeonColors.textHead,
        )
        caption?.let {
            Text(it, fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 14.sp, color = MuyeonColors.textSub)
        }
    }
}

@Composable
private fun GuideStep(n: Int, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$n",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 13.sp,
            color = Color.White, textAlign = TextAlign.Center,
            modifier = Modifier.size(16.dp).clip(RoundedCornerShape(50)).background(MuyeonColors.primary)
                .padding(top = 1.dp),
        )
        Text(text, fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = MuyeonColors.textHead)
    }
}

@Composable
private fun TimeMenu(label: String, value: String, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
                .clickable { open = true }.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 13.sp, color = MuyeonColors.textSub)
            Text(value, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 17.sp, color = MuyeonColors.textHead)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            LessonOptions.startTimes.forEach { t ->
                DropdownMenuItem(
                    text = { Text(t, fontFamily = customFontFamily, fontSize = 14.sp) },
                    onClick = { open = false; onSelect(t) },
                )
            }
        }
    }
}

@Composable
private fun CapacityMenu(value: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
                .clickable { open = true }.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text("정원", fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 13.sp, color = MuyeonColors.textSub)
            Text("${value}명", fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 17.sp, color = MuyeonColors.textHead)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            LessonOptions.capacities.forEach { c ->
                DropdownMenuItem(
                    text = { Text("${c}명", fontFamily = customFontFamily, fontSize = 14.sp) },
                    onClick = { open = false; onSelect(c) },
                )
            }
        }
    }
}

@Composable
private fun SlotRow(s: LessonSlot) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF7F7F7)).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${s.date} ${s.timeLabel}",
            fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp,
            color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
        )
        Text(
            if (s.isOpen) "예약 가능 ${s.remaining}/${s.capacity}" else if (s.status == "FULL") "마감" else "종료",
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 15.sp,
            color = if (s.isOpen) MuyeonColors.primary else MuyeonColors.secondary,
        )
    }
}
