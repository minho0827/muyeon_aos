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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 개인 일정 편집기 — iOS `TimetreeEventEditor.swift` 대응(핵심 필드만).
 *  제목 · 캘린더 · 하루종일/시간 · 장소 · 메모 · 알림 + 삭제.
 *
 * ⚠️ 종전 AOS 는 개인 일정을 **조회만** 하고 추가·수정·삭제가 아예 없었다
 *   (UserCalendarApi 에 schedule() 만 있었다). 캘린더에 남의 일정처럼 뜨기만 했다.
 *
 * iOS 편집기의 반복 일정은 여기서 제외했다 — 서버 DTO 에도 반복 필드가 없다.
 * 장소는 iOS 처럼 검색(LocationSearchSheet)으로 좌표까지 받아
 * place/placeAddress/placeLat/placeLng 를 함께 저장한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleBlockEditor(
    api: UserCalendarApi,
    /** 신규면 null, 수정이면 대상 블록. */
    block: StudioBlock?,
    /** 신규 기본 날짜(yyyy-MM-dd). */
    defaultYmd: String,
    calendars: List<UserCalendar>,
    onSaved: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf(block?.title.orEmpty()) }
    var ymd by remember { mutableStateOf(block?.date ?: defaultYmd) }
    var allDay by remember { mutableStateOf(block?.allDay ?: false) }
    var startTime by remember { mutableStateOf(block?.startTime ?: "10:00") }
    var endTime by remember { mutableStateOf(block?.endTime ?: "11:00") }
    var calendarId by remember { mutableStateOf(block?.calendarId) }
    var memo by remember { mutableStateOf(block?.memo.orEmpty()) }
    var place by remember { mutableStateOf(block?.place.orEmpty()) }
    // 좌표를 가진 '검색으로 고른' 장소. 직접입력이면 null 이고 place 텍스트만 남는다.
    var selectedPlace by remember {
        mutableStateOf(
            block?.place?.takeIf { block.placeLat != null }?.let {
                LessonPlace(it, block.placeAddress, block.placeLat, block.placeLng)
            },
        )
    }
    var showLocationSearch by remember { mutableStateOf(false) }
    var remindMinutes by remember { mutableStateOf<Int?>(null) }
    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var showStart by remember { mutableStateOf(false) }
    var showEnd by remember { mutableStateOf(false) }
    var showCalendarPicker by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    val selectedCalendar = calendars.firstOrNull { it.id == calendarId } ?: UserCalendar.DEFAULT
    val canSave = title.isNotBlank() && !saving

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(44.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "취소",
                    fontFamily = customFontFamily, fontSize = 15.sp, color = MuyeonColors.textSub,
                    modifier = Modifier.clickable(onClick = onDismiss),
                )
                Text(
                    if (block == null) "일정 추가" else "일정 수정",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    lineHeight = 20.sp, color = MuyeonColors.textHead,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
                Text(
                    if (saving) "저장 중…" else "저장",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = if (canSave) MuyeonColors.primary else MuyeonColors.chevron,
                    modifier = Modifier.clickable(enabled = canSave) {
                        saving = true
                        scope.launch {
                            val result = if (block == null) {
                                api.createBlock(
                                    ymd, title.trim(), calendarId, allDay,
                                    if (allDay) null else startTime, if (allDay) null else endTime,
                                    memo.trim().ifEmpty { null }, remindMinutes,
                                    placePayload(place, selectedPlace),
                                )
                            } else {
                                api.updateBlock(
                                    block.id, ymd, title.trim(), calendarId, allDay,
                                    if (allDay) null else startTime, if (allDay) null else endTime,
                                    memo.trim().ifEmpty { null }, remindMinutes,
                                    placePayload(place, selectedPlace),
                                )
                            }
                            result.onSuccess { onSaved() }
                                .onFailure { errorMessage = it.message ?: "저장에 실패했어요." }
                            saving = false
                        }
                    },
                )
            }

            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                EditorField("제목") {
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        placeholder = {
                            Text(
                                "예: 개인 연습, 병원",
                                fontFamily = customFontFamily, fontSize = 15.sp, color = MuyeonColors.chevron,
                            )
                        },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                }

                EditorField("캘린더") {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF2F2F7))
                            .clickable { showCalendarPicker = true }.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(selectedCalendar.uiColor))
                        Text(
                            selectedCalendar.name,
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                            lineHeight = 18.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                        )
                        Text("›", fontFamily = customFontFamily, fontSize = 15.sp, color = MuyeonColors.textSub)
                    }
                }

                Row(
                    Modifier.fillMaxWidth().clickable { allDay = !allDay },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "하루 종일",
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        lineHeight = 18.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = allDay, onCheckedChange = { allDay = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = MuyeonColors.primary),
                    )
                }

                EditorField("날짜") {
                    EditorBox(prettyYmd(ymd)) { showDate = true }
                }
                if (!allDay) {
                    EditorField("시간") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            EditorBox(startTime, Modifier.weight(1f)) { showStart = true }
                            EditorBox(endTime, Modifier.weight(1f)) { showEnd = true }
                        }
                    }
                }

                EditorField("알림") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 서버가 받는 값은 0/10/30/60/1440 — 미전송이면 알림 없음.
                        listOf(null to "없음", 10 to "10분 전", 60 to "1시간 전", 1440 to "하루 전").forEach { (v, label) ->
                            val on = remindMinutes == v
                            Text(
                                label,
                                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                                lineHeight = 16.sp, color = if (on) Color.White else MuyeonColors.body,
                                textAlign = TextAlign.Center, maxLines = 1,
                                modifier = Modifier.weight(1f).clip(RoundedCornerShape(50))
                                    .background(if (on) MuyeonColors.primary else MuyeonColors.surface)
                                    .then(
                                        if (on) Modifier
                                        else Modifier.border(1.dp, MuyeonColors.border, RoundedCornerShape(50)),
                                    )
                                    .clickable { remindMinutes = v }
                                    .padding(vertical = 8.dp),
                            )
                        }
                    }
                }

                EditorField("장소") {
                    val picked = selectedPlace
                    if (picked != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    picked.name,
                                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    Icons.Filled.Cancel, "장소 해제", tint = MuyeonColors.chevron,
                                    modifier = Modifier.size(18.dp)
                                        .clickable { selectedPlace = null; place = "" },
                                )
                            }
                            if (picked.hasCoord) {
                                LocationPreviewCard(picked) { openLessonNaverMap(ctx, picked.name, picked.lat, picked.lng) }
                            }
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = place, onValueChange = { place = it },
                                placeholder = {
                                    Text(
                                        "장소 (선택)",
                                        fontFamily = customFontFamily, fontSize = 15.sp,
                                        color = MuyeonColors.chevron,
                                    )
                                },
                                singleLine = true, modifier = Modifier.weight(1f),
                            )
                            Icon(
                                Icons.Filled.Search, "장소 검색", tint = MuyeonColors.primary,
                                modifier = Modifier.size(20.dp).clickable { showLocationSearch = true },
                            )
                        }
                    }
                }

                EditorField("메모") {
                    OutlinedTextField(
                        value = memo, onValueChange = { memo = it },
                        placeholder = {
                            Text(
                                "메모 (선택)",
                                fontFamily = customFontFamily, fontSize = 15.sp, color = MuyeonColors.chevron,
                            )
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                    )
                }

                if (block != null) {
                    Text(
                        "일정 삭제",
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        lineHeight = 18.sp, color = MuyeonColors.danger, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
                            .clickable { confirmDelete = true }
                            .padding(vertical = 12.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showLocationSearch) {
        LocationSearchSheet(
            initialQuery = place,
            onSelect = { picked -> selectedPlace = picked; place = picked.name },
            onDismiss = { showLocationSearch = false },
        )
    }

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = millisOfYmd(ymd))
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ymd = ymdOfUtcMidnight(it) }
                    showDate = false
                }) { Text("완료", fontFamily = customFontFamily, color = MuyeonColors.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showDate = false }) {
                    Text("취소", fontFamily = customFontFamily, color = MuyeonColors.textSub)
                }
            },
        ) { DatePicker(state) }
    }
    if (showStart || showEnd) {
        val current = if (showStart) startTime else endTime
        val parts = current.split(":").mapNotNull { it.toIntOrNull() }
        val state = rememberTimePickerState(
            initialHour = parts.getOrElse(0) { 10 },
            initialMinute = parts.getOrElse(1) { 0 },
            is24Hour = true,
        )
        DatePickerDialog(
            onDismissRequest = { showStart = false; showEnd = false },
            confirmButton = {
                TextButton(onClick = {
                    val v = String.format(Locale.KOREA, "%02d:%02d", state.hour, state.minute)
                    if (showStart) {
                        startTime = v
                        // 시작이 종료보다 늦으면 종료를 한 시간 뒤로 밀어 잘못된 구간 저장을 막는다.
                        if (v >= endTime) endTime = plusHour(v)
                    } else {
                        endTime = v
                    }
                    showStart = false; showEnd = false
                }) { Text("완료", fontFamily = customFontFamily, color = MuyeonColors.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showStart = false; showEnd = false }) {
                    Text("취소", fontFamily = customFontFamily, color = MuyeonColors.textSub)
                }
            },
        ) { Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) { TimePicker(state) } }
    }
    if (showCalendarPicker) {
        ModalBottomSheet(onDismissRequest = { showCalendarPicker = false }) {
            Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                (listOf(UserCalendar.DEFAULT) + calendars).forEach { c ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { calendarId = c.id.takeIf { it > 0 }; showCalendarPicker = false }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(c.uiColor))
                        Text(
                            c.name,
                            fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 18.sp,
                            color = MuyeonColors.textHead,
                        )
                    }
                }
            }
        }
    }
    if (confirmDelete && block != null) {
        QuoteDialog(
            "이 일정을 삭제할까요?", "되돌릴 수 없어요.", "삭제",
            onConfirm = {
                confirmDelete = false
                scope.launch {
                    api.deleteBlock(block.id)
                        .onSuccess { onSaved() }
                        .onFailure { errorMessage = it.message ?: "삭제에 실패했어요." }
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
    errorMessage?.let { msg ->
        QuoteDialog("오류", msg, "확인", onConfirm = { errorMessage = null }, onDismiss = { errorMessage = null })
    }
}

@Composable
private fun EditorField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 17.sp, color = MuyeonColors.textHead,
        )
        content()
    }
}

@Composable
private fun EditorBox(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
        lineHeight = 18.sp, color = MuyeonColors.textHead, textAlign = TextAlign.Center,
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFFF2F2F7))
            .clickable(onClick = onClick).padding(12.dp),
    )
}

// ── 날짜/시간 헬퍼 (전부 KST) ──

private val KST: TimeZone = TimeZone.getTimeZone("Asia/Seoul")
private val YMD = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).apply { timeZone = KST }
private val PRETTY = SimpleDateFormat("yyyy.MM.dd (E)", Locale.KOREA).apply { timeZone = KST }

private fun millisOfYmd(ymd: String): Long =
    runCatching { YMD.parse(ymd)?.time }.getOrNull() ?: System.currentTimeMillis()

private fun prettyYmd(ymd: String): String =
    runCatching { PRETTY.format(Date(millisOfYmd(ymd))) }.getOrDefault(ymd)

/** DatePicker 는 UTC 자정을 돌려준다 — 그대로 KST 로 포맷하면 하루가 밀린다. */
private fun ymdOfUtcMidnight(millis: Long): String {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = millis }
    return String.format(
        Locale.KOREA, "%04d-%02d-%02d",
        utc.get(Calendar.YEAR), utc.get(Calendar.MONTH) + 1, utc.get(Calendar.DAY_OF_MONTH),
    )
}

private fun plusHour(hhmm: String): String {
    val p = hhmm.split(":").mapNotNull { it.toIntOrNull() }
    if (p.size < 2) return hhmm
    val h = (p[0] + 1).coerceAtMost(23)
    return String.format(Locale.KOREA, "%02d:%02d", h, p[1])
}

/**
 * 저장용 장소 — 검색으로 고른 게 있으면 그대로, 없으면 입력 텍스트만(좌표 없이).
 *  iOS TimetreeEventEditor.save() 의 placeObj 폴백과 동일.
 */
private fun placePayload(text: String, selected: LessonPlace?): LessonPlace? =
    selected ?: text.trim().takeIf { it.isNotEmpty() }?.let { LessonPlace(name = it) }
