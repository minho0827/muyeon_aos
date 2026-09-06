package com.muyeon.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.BuildConfig
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.lesson.UserCalendar
import com.muyeon.app.ui.lesson.UserCalendarApi
import com.muyeon.app.ui.quote.QuoteDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** 겹침(409) — "그래도 보낼까요?" 경고용. */
private class ProposalConflict(message: String) : Exception(message)

/**
 * 레슨 약속 제안 — POST /lesson-proposals. iOS `LessonProposalService.create` 1:1.
 *  겹침이면 409 → 경고 후 force 재시도.
 */
class LessonProposalApi(private val token: String?) {

    private val client = OkHttpClient()
    private val json = "application/json; charset=utf-8".toMediaType()

    suspend fun create(
        roomId: Int,
        startAtMillis: Long,
        durationMin: Int,
        place: String?,
        memo: String,
        calendarId: Int?,
        force: Boolean,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("roomId", roomId)
                .put("startAt", ISO.format(Date(startAtMillis)))
                .put("durationMin", durationMin)
                .put("force", force)
            place?.takeIf { it.isNotBlank() }?.let { body.put("place", it) }
            memo.trim().takeIf { it.isNotEmpty() }?.let { body.put("memo", it) }
            calendarId?.takeIf { it > 0 }?.let { body.put("calendarId", it) }

            val req = Request.Builder()
                .url(BuildConfig.API_BASE_URL + "/api/lesson-proposals")
                .post(body.toString().toRequestBody(json))
                .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                .build()
            client.newCall(req).execute().use { res ->
                val text = res.body?.string().orEmpty()
                val msg = runCatching { JSONObject(text).optString("message") }.getOrNull()
                // 409 는 실패가 아니라 "겹치는데 강행할래?" 물음이다 — 따로 구분한다.
                if (res.code == 409) throw ProposalConflict(msg?.ifEmpty { null } ?: "그 시간엔 이미 일정이 있어요.")
                if (!res.isSuccessful) error(msg?.ifEmpty { null } ?: "제안을 보내지 못했어요. 다시 시도해 주세요.")
            }
        }
    }

    companion object {
        private val ISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
}

/**
 * 레슨 약속 제안 작성 — iOS `LessonProposalComposer.swift` 1:1.
 *  채택된 견적의 회원·강사 모두 날짜를 제안할 수 있다.
 *
 * ⚠️ 종전 AOS 는 채팅 첨부 시트의 '레슨 약속잡기' 가 토스트만 띄우는 죽은 버튼이었다
 *   (수락/거절만 있고 제안 생성이 없었다).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonProposalComposer(
    api: LessonProposalApi,
    calendarApi: UserCalendarApi,
    roomId: Int,
    isTeacher: Boolean,
    totalPrice: Int,
    depositAmount: Int,
    /** 캘린더에서 열 때 편집기에 이미 적어둔 값을 그대로 싣는다(iOS 는 편집기가 제안 모드로 바뀐다). */
    initialStartAt: Long? = null,
    initialDurationMin: Int? = null,
    initialPlace: String = "",
    initialMemo: String = "",
    initialCalendarId: Int? = null,
    onSent: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // 기본값: 내일 오후 3시(KST) — iOS 와 같다.
    var startAt by remember { mutableLongStateOf(initialStartAt ?: defaultStart()) }
    var durationMin by remember { mutableIntStateOf(initialDurationMin ?: 60) }
    var place by remember { mutableStateOf(initialPlace) }
    var memo by remember { mutableStateOf(initialMemo) }
    var calendars by remember { mutableStateOf<List<UserCalendar>>(emptyList()) }
    var calendarId by remember { mutableStateOf(initialCalendarId) }
    var policyAgreed by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var conflictText by remember { mutableStateOf<String?>(null) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var showCalendarPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { calendars = calendarApi.list().getOrDefault(emptyList()) }

    val selectedCalendar = calendars.firstOrNull { it.id == calendarId } ?: UserCalendar.DEFAULT
    // 회원은 예약금 규정 동의 전에는 못 보낸다(iOS 와 동일).
    val canSend = !sending && (isTeacher || policyAgreed)

    fun submit(force: Boolean) {
        sending = true
        errorText = null
        scope.launch {
            api.create(
                roomId, startAt, durationMin,
                place.ifBlank { null }, memo,
                if (isTeacher) calendarId else null, force,
            )
                .onSuccess { sending = false; onSent() }
                .onFailure { e ->
                    sending = false
                    if (e is ProposalConflict) conflictText = e.message else errorText = e.message
                }
        }
    }

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
                    "레슨 약속잡기",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    lineHeight = 20.sp, color = MuyeonColors.textHead,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
                Text(
                    if (sending) "보내는 중…" else "제안 보내기",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = if (canSend) MuyeonColors.primary else MuyeonColors.chevron,
                    modifier = Modifier.clickable(enabled = canSend) { submit(force = false) },
                )
            }

            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    if (isTeacher) "회원이 수락하면 레슨이 확정되고 캘린더에 등록돼요."
                    else "강사가 수락하면 레슨이 확정되고 내 일정에도 등록돼요.",
                    fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp,
                    color = MuyeonColors.textSub,
                )

                ComposerField("날짜 · 시간") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BoxButton(KST_DATE.format(Date(startAt)), Modifier.weight(1f)) { showDate = true }
                        BoxButton(KST_TIME.format(Date(startAt)), Modifier.weight(1f)) { showTime = true }
                    }
                }

                ComposerField("수업 길이") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(30, 60, 90, 120).forEach { m ->
                            val on = durationMin == m
                            Text(
                                "${m}분",
                                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                                lineHeight = 17.sp, color = if (on) Color.White else MuyeonColors.textHead,
                                modifier = Modifier.clip(RoundedCornerShape(50))
                                    .background(if (on) MuyeonColors.primary else Color(0xFFF2F2F7))
                                    .clickable { durationMin = m }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
                }

                if (isTeacher) {
                    ComposerField("캘린더") {
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
                            Text(
                                "›",
                                fontFamily = customFontFamily, fontSize = 15.sp, color = MuyeonColors.textSub,
                            )
                        }
                    }
                }

                ComposerField("장소") {
                    OutlinedTextField(
                        value = place, onValueChange = { place = it },
                        placeholder = {
                            Text(
                                "예: 강남 무용연 연습실 (선택)",
                                fontFamily = customFontFamily, fontSize = 15.sp, color = MuyeonColors.chevron,
                            )
                        },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                }

                ComposerField("메모") {
                    OutlinedTextField(
                        value = memo, onValueChange = { memo = it },
                        placeholder = {
                            Text(
                                "예: 기초 스트레칭 준비해 오세요",
                                fontFamily = customFontFamily, fontSize = 15.sp, color = MuyeonColors.chevron,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (!isTeacher) {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                            .border(1.dp, MuyeonColors.border, RoundedCornerShape(14.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            "예약금 안내",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            lineHeight = 19.sp, color = MuyeonColors.textHead,
                        )
                        Text(
                            if (depositAmount > 0) "예약금 결제 후 일정이 확정돼요."
                            else "이 견적은 결제 없이 일정이 확정돼요.",
                            fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp,
                            color = MuyeonColors.textSub,
                        )
                        PriceRow("총 레슨비", totalPrice)
                        PriceRow("지금 결제할 예약금", depositAmount, accent = true)
                        PriceRow("현장 결제 예정액", maxOf(0, totalPrice - depositAmount))
                        HorizontalDivider(color = MuyeonColors.border)
                        Text(
                            "예약금은 레슨비에 포함됩니다.\n· 24시간 전까지 예약금 전액 환불\n" +
                                "· 24시간 이내 취소 시 예약금 미환불\n· 수업 시작 후·노쇼 예약금 미환불\n" +
                                "· 강사 취소·수업 미제공 전액 환불",
                            fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 20.sp,
                            color = MuyeonColors.textSub,
                        )
                        Row(
                            Modifier.fillMaxWidth().clickable { policyAgreed = !policyAgreed },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "취소·환불 규정을 확인했습니다.",
                                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                                lineHeight = 17.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                            )
                            Switch(
                                checked = policyAgreed, onCheckedChange = { policyAgreed = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = MuyeonColors.primary),
                            )
                        }
                    }
                }

                errorText?.let {
                    Text(
                        it,
                        fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp, color = Color.Red,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = startAt)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    // 날짜만 갈아끼우고 시각은 유지한다.
                    state.selectedDateMillis?.let { startAt = replaceDate(startAt, it) }
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
    if (showTime) {
        val cal = kstCalendar(startAt)
        val state = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = false,
        )
        DatePickerDialog(
            onDismissRequest = { showTime = false },
            confirmButton = {
                TextButton(onClick = {
                    startAt = replaceTime(startAt, state.hour, state.minute)
                    showTime = false
                }) { Text("완료", fontFamily = customFontFamily, color = MuyeonColors.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showTime = false }) {
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
    conflictText?.let { msg ->
        QuoteDialog(
            "이미 일정이 있어요", "$msg\n같은 시간에 제안을 보낼까요?", "그래도 보내기",
            onConfirm = { conflictText = null; submit(force = true) },
            onDismiss = { conflictText = null },
        )
    }
}

@Composable
private fun ComposerField(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 17.sp, color = MuyeonColors.textHead,
        )
        content()
    }
}

@Composable
private fun BoxButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
        lineHeight = 18.sp, color = MuyeonColors.textHead, textAlign = TextAlign.Center,
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFFF2F2F7))
            .clickable(onClick = onClick).padding(12.dp),
    )
}

@Composable
private fun PriceRow(title: String, amount: Int, accent: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 17.sp,
            color = MuyeonColors.textSub, modifier = Modifier.weight(1f),
        )
        Text(
            "${String.format(Locale.KOREA, "%,d", amount)}원",
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 17.sp, color = if (accent) MuyeonColors.primary else MuyeonColors.textHead,
        )
    }
}

// ── 시간 계산(전부 KST 기준) ──

private val KST: TimeZone = TimeZone.getTimeZone("Asia/Seoul")
private val KST_DATE = SimpleDateFormat("M/d(E)", Locale.KOREA).apply { timeZone = KST }
private val KST_TIME = SimpleDateFormat("a h:mm", Locale.KOREA).apply { timeZone = KST }

private fun kstCalendar(millis: Long): Calendar =
    Calendar.getInstance(KST).apply { timeInMillis = millis }

/** 내일 오후 3시(KST). */
private fun defaultStart(): Long = Calendar.getInstance(KST).apply {
    add(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 15)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** 날짜만 교체(시각 유지) — DatePicker 는 UTC 자정을 돌려준다. */
private fun replaceDate(current: Long, pickedUtcMidnight: Long): Long {
    val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = pickedUtcMidnight }
    return kstCalendar(current).apply {
        set(Calendar.YEAR, picked.get(Calendar.YEAR))
        set(Calendar.MONTH, picked.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, picked.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun replaceTime(current: Long, hour: Int, minute: Int): Long =
    kstCalendar(current).apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
