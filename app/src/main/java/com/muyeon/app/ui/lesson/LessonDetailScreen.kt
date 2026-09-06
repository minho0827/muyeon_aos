package com.muyeon.app.ui.lesson

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteAvatar
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.ui.quote.QuoteUi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 레슨 일정 상세 — iOS `LessonDetailView.swift` 이식.
 *  일정 확정(PENDING → SCHEDULED) · 장소·메모 · 캘린더 배정 · 완료/완료취소 · 취소 · 변경 이력.
 *
 * ⚠️ 저장 시 place/placeAddress 는 항상 전송된다(LessonApi.setSchedule) — 빈 문자열이 '해제'다.
 */
@Composable
fun LessonDetailScreen(
    api: LessonApi,
    calendarApi: UserCalendarApi,
    lessonId: Int,
    onClose: () -> Unit,
    onOpenChat: (Int) -> Unit,
    onOpenReservation: (Int) -> Unit,
) {
    var lesson by remember { mutableStateOf<LessonSchedule?>(null) }
    var history by remember { mutableStateOf<List<LessonHistoryItem>>(emptyList()) }
    var calendars by remember { mutableStateOf<List<UserCalendar>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    var confirmCancel by remember { mutableStateOf(false) }

    // 편집 상태
    var startAt by remember { mutableStateOf<String?>(null) }
    var placeName by remember { mutableStateOf("") }
    var placeAddress by remember { mutableStateOf("") }
    // 검색으로 고른 장소의 좌표. 손으로 고치면 좌표는 버린다(옛 핀이 남지 않도록).
    var placeLat by remember { mutableStateOf<Double?>(null) }
    var placeLng by remember { mutableStateOf<Double?>(null) }
    var showLocationSearch by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    var memo by remember { mutableStateOf("") }
    var calendarId by remember { mutableStateOf<Int?>(null) }

    val scope = rememberCoroutineScope()

    suspend fun load() {
        api.get(lessonId).onSuccess { l ->
            lesson = l
            startAt = l.startAt
            placeName = l.place.orEmpty()
            placeAddress = l.placeAddress.orEmpty()
            placeLat = l.placeLat; placeLng = l.placeLng
            memo = l.memo.orEmpty()
            calendarId = l.calendarId
        }
        history = api.history(lessonId).getOrDefault(emptyList())
        calendars = calendarApi.list().getOrDefault(emptyList())
        loading = false
    }

    LaunchedEffect(lessonId) { load() }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "레슨 일정", onBack = onClose)

        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            return@Column
        }
        val l = lesson
        if (l == null) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                Text("일정을 찾을 수 없어요.", fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.textSub)
            }
            return@Column
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // 상대 + 상태
            Row(
                Modifier.padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuoteAvatar(l.partner.image, l.partner.displayName, 52.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        l.partner.displayName,
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                        lineHeight = 20.sp, color = MuyeonColors.textHead,
                    )
                    Text(
                        listOfNotNull(l.serviceLabel, statusLabel(l)).joinToString(" · "),
                        fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp, color = MuyeonColors.textSub,
                    )
                }
                l.roomId?.takeIf { it > 0 }?.let { rid ->
                    Text(
                        "채팅",
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                        lineHeight = 16.sp, color = MuyeonColors.primary,
                        modifier = Modifier.clip(RoundedCornerShape(50))
                            .background(MuyeonColors.primary.copy(alpha = 0.08f))
                            .clickable { onOpenChat(rid) }.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }

            // 시간슬롯 예약에서 온 일정이면 예약 상세로 갈 수 있다.
            if (l.isBooking) {
                Text(
                    "예약 내역 확인하기",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                    lineHeight = 17.sp, color = MuyeonColors.primary,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .background(MuyeonColors.primary.copy(alpha = 0.08f))
                        .clickable { l.reservationId?.let(onOpenReservation) }
                        .padding(12.dp),
                )
            }

            DetailField("일시") {
                Text(
                    startAt?.let { fullDateTime(it) } ?: "날짜 미정",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = if (startAt == null) MuyeonColors.secondary else MuyeonColors.textHead,
                )
                if (l.isPending) {
                    Text(
                        "강사가 날짜를 확정하면 알림으로 알려드려요.",
                        fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp, color = MuyeonColors.textSub,
                    )
                }
            }

            DetailField("장소") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        // 이름을 손으로 고치면 검색 좌표는 무효 — 옛 핀이 새 장소에 붙는 걸 막는다.
                        value = placeName,
                        onValueChange = { placeName = it; placeLat = null; placeLng = null },
                        singleLine = true,
                        placeholder = { Text("장소명", fontFamily = customFontFamily, fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Filled.Search, "장소 검색", tint = MuyeonColors.primary,
                        modifier = Modifier.size(20.dp).clickable { showLocationSearch = true },
                    )
                }
                OutlinedTextField(
                    value = placeAddress,
                    onValueChange = { placeAddress = it; placeLat = null; placeLng = null },
                    singleLine = true,
                    placeholder = { Text("주소", fontFamily = customFontFamily, fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (placeLat != null && placeLng != null) {
                    LocationPreviewCard(LessonPlace(placeName, placeAddress.ifEmpty { null }, placeLat, placeLng)) {
                        openLessonNaverMap(ctx, placeName, placeLat, placeLng)
                    }
                }
                Text(
                    "비우고 저장하면 장소가 해제돼요.",
                    fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 14.sp, color = MuyeonColors.secondary,
                )
            }

            DetailField("메모") {
                OutlinedTextField(
                    value = memo, onValueChange = { memo = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                )
            }

            // 캘린더 배정(색 상속)
            DetailField("캘린더") {
                var open by remember { mutableStateOf(false) }
                val current = calendars.firstOrNull { it.id == calendarId } ?: UserCalendar.DEFAULT
                Box {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
                            .clickable { open = true }.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(current.uiColor))
                        Text(
                            current.name,
                            fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 18.sp,
                            color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Filled.KeyboardArrowDown, null, tint = MuyeonColors.secondary, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        (listOf(UserCalendar.DEFAULT) + calendars).forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name, fontFamily = customFontFamily, fontSize = 14.sp) },
                                onClick = {
                                    open = false
                                    calendarId = c.id.takeIf { it != 0 }
                                    // 캘린더 이동은 조용한 변경(알림·이력 없음) — 별도 엔드포인트.
                                    scope.launch { api.setCalendar(lessonId, calendarId).onFailure { toast = it.message } }
                                },
                            )
                        }
                    }
                }
            }

            // 변경 이력
            if (history.isNotEmpty()) {
                HorizontalDivider(color = MuyeonColors.border)
                DetailField("변경 이력") {
                    history.forEach { h ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.padding(top = 6.dp).size(5.dp).clip(CircleShape).background(MuyeonColors.border))
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    "${h.actorName ?: if (h.isMe) "나" else "상대"} · ${h.actionLabel}",
                                    fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 17.sp,
                                    color = MuyeonColors.textHead,
                                )
                                Text(
                                    QuoteUi.relativeTime(h.createdAt),
                                    fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 14.sp,
                                    color = MuyeonColors.secondary,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // 액션
        Column(Modifier.fillMaxWidth().background(MuyeonColors.surface)) {
            HorizontalDivider(color = MuyeonColors.border)
            Row(
                Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DetailAction("취소", filled = false, danger = true, enabled = !busy, modifier = Modifier.weight(1f)) {
                    confirmCancel = true
                }
                // 강사만 — 회원이 낸 레슨 전 설문 모아보기(iOS 레슨 상세 액션과 같은 자리).
                if (l.iAmTeacher && l.partner.id > 0) {
                    DetailAction("설문 프로필", filled = false, enabled = !busy, modifier = Modifier.weight(1f)) {
                        com.muyeon.app.ui.survey.SurveyActivity.startProfile(ctx, l.partner.id)
                    }
                }
                // 강사만 완료/완료취소
                if (l.iAmTeacher) {
                    if (l.status == "DONE") {
                        DetailAction("완료 취소", filled = false, enabled = !busy, modifier = Modifier.weight(1f)) {
                            busy = true
                            scope.launch {
                                api.uncomplete(lessonId).onSuccess { toast = "완료를 취소했어요." }.onFailure { toast = it.message }
                                load(); busy = false
                            }
                        }
                    } else {
                        DetailAction("레슨 완료", filled = false, enabled = !busy, modifier = Modifier.weight(1f)) {
                            busy = true
                            scope.launch {
                                api.complete(lessonId).onSuccess { toast = "완료 처리했어요." }.onFailure { toast = it.message }
                                load(); busy = false
                            }
                        }
                    }
                }
                DetailAction("저장", filled = true, enabled = !busy, modifier = Modifier.weight(1f)) {
                    busy = true
                    scope.launch {
                        api.setSchedule(
                            id = lessonId,
                            startAt = startAt,
                            place = LessonPlace(
                                name = placeName, address = placeAddress.ifEmpty { null },
                                lat = placeLat, lng = placeLng,
                            ),
                            memo = memo,
                        ).onSuccess { toast = "저장했어요." }.onFailure { toast = it.message }
                        load(); busy = false
                    }
                }
            }
        }
    }

    if (showLocationSearch) {
        LocationSearchSheet(
            initialQuery = placeName.ifEmpty { placeAddress },
            onSelect = { p ->
                placeName = p.name; placeAddress = p.address.orEmpty()
                placeLat = p.lat; placeLng = p.lng
            },
            onDismiss = { showLocationSearch = false },
        )
    }

    if (confirmCancel) {
        QuoteDialog(
            title = "이 일정을 취소할까요?",
            message = "상대에게 취소 알림이 전달돼요. 되돌릴 수 없어요.",
            confirmText = "취소하기",
            onConfirm = {
                confirmCancel = false
                busy = true
                scope.launch {
                    api.cancel(lessonId).onSuccess { onClose() }.onFailure { toast = it.message }
                    busy = false
                }
            },
            onDismiss = { confirmCancel = false },
        )
    }
    toast?.let { msg ->
        QuoteDialog("알림", msg, "확인", onConfirm = { toast = null }, onDismiss = { toast = null })
    }
}

private fun statusLabel(l: LessonSchedule): String = when {
    l.status == "CANCELED" -> "취소됨"
    l.status == "DONE" -> "완료"
    l.isPending -> "날짜 미정"
    else -> "확정"
}

private val fullFmt = SimpleDateFormat("yyyy년 M월 d일(E) a h:mm", Locale.KOREA).apply {
    timeZone = TimeZone.getTimeZone("Asia/Seoul")
}

private fun fullDateTime(iso: String): String =
    QuoteUi.parseDate(iso)?.let { fullFmt.format(Date(it)) } ?: iso

@Composable
private fun DetailField(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            lineHeight = 18.sp, color = MuyeonColors.textHead,
        )
        content()
    }
}

@Composable
private fun DetailAction(
    text: String,
    filled: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 17.sp,
        color = when {
            filled -> Color.White
            danger -> MuyeonColors.danger
            else -> MuyeonColors.primary
        },
        textAlign = TextAlign.Center,
        modifier = modifier.clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    filled -> MuyeonColors.primary
                    danger -> MuyeonColors.danger.copy(alpha = 0.08f)
                    else -> MuyeonColors.primary.copy(alpha = 0.08f)
                },
            )
            .clickable(enabled = enabled, onClick = onClick).padding(vertical = 13.dp),
    )
}
