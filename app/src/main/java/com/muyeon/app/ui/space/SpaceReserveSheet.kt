package com.muyeon.app.ui.space

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

/**
 * 공간 '바로 예약하기' 시트 — iOS `SpaceReserveSheet.swift`(+Fields) 1:1.
 *  Figma 하단 폼(라벨+입력박스 / 알려드립니다 / 고정 CTA) 스타일에 항목만 공간 예약으로 바꿨다.
 *  백엔드는 웹과 동일한 POST /spaces/:id/reservation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceReserveSheet(
    api: SpaceApi,
    space: SpaceDetail,
    initialDate: String = "",
    /** 완료(또는 취소) 문구를 상위에 전달하며 닫힌다. null 이면 문구 없이 닫기. */
    onFinish: (String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val minHours = maxOf(1, space.hourlyMinHours ?: 1).toDouble()
    var bookingType by remember {
        mutableStateOf(
            when {
                space.hourlyEnabled != false -> "HOURLY"
                space.packageEnabled == true -> "PACKAGE"
                else -> "HOURLY"
            },
        )
    }
    var dateMillis by remember {
        mutableLongStateOf(
            initialDate.takeIf { it.isNotEmpty() }?.let { runCatching { YMD.parse(it)?.time }.getOrNull() }
                ?: System.currentTimeMillis(),
        )
    }
    var startTime by remember { mutableStateOf("") }
    var hours by remember { mutableDoubleStateOf(minHours) }
    var optionId by remember { mutableStateOf<Int?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    val currentOptions = if (bookingType == "HOURLY") space.hourlyOptions else space.packageOptions

    /** 예상 금액 — 시간제는 단가 × 이용시간, 패키지는 옵션 금액. */
    val estimate = run {
        val unit = currentOptions.firstOrNull { it.id == optionId }?.price ?: space.pricePerHour ?: 0
        if (bookingType == "HOURLY") (unit * hours).roundToInt() else unit
    }
    val canSubmit = !submitting && (bookingType != "HOURLY" || startTime.isNotEmpty())

    ModalBottomSheet(onDismissRequest = { onFinish(null) }, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth()) {
            // 제목바
            Row(
                Modifier.fillMaxWidth().height(SpaceDesign.headerHeight)
                    .padding(horizontal = SpaceDesign.gutter),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "예약하기",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    lineHeight = 22.sp, color = SpaceDesign.ink900, modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Filled.Close, "닫기", tint = SpaceDesign.ink900,
                    modifier = Modifier.size(16.dp).clickable { onFinish(null) },
                )
            }

            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .padding(horizontal = SpaceDesign.gutter).padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (space.hourlyEnabled != false || space.packageEnabled == true) {
                    FieldGroup("예약 선택") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (space.hourlyEnabled != false) {
                                RadioRow(
                                    "시간 단위 예약" + if (minHours > 1) " (최소 ${minHours.toInt()}시간)" else "",
                                    bookingType == "HOURLY",
                                ) { bookingType = "HOURLY"; optionId = null }
                            }
                            if (space.packageEnabled == true) {
                                RadioRow("패키지 예약", bookingType == "PACKAGE") {
                                    bookingType = "PACKAGE"; optionId = null
                                }
                            }
                        }
                    }
                }

                FieldGroup("예약 날짜") {
                    BoxField(YMD.format(Date(dateMillis)), placeholder = false) { showDatePicker = true }
                }

                if (bookingType == "HOURLY") {
                    FieldGroup("시작 시간") {
                        MenuField(
                            text = startTime.ifEmpty { "시작 시간 선택" },
                            placeholder = startTime.isEmpty(),
                            options = TIME_OPTIONS,
                            label = { it },
                        ) { startTime = it }
                    }
                    FieldGroup("이용 시간") {
                        MenuField(
                            text = hourLabel(hours),
                            placeholder = false,
                            options = hourOptions(minHours),
                            label = { hourLabel(it) },
                        ) { hours = it }
                    }
                }

                if (currentOptions.isNotEmpty()) {
                    FieldGroup("옵션 선택") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            currentOptions.forEach { o ->
                                RadioRow("${o.label ?: "옵션"} — ${o.priceText}", optionId == o.id) {
                                    optionId = o.id
                                }
                            }
                        }
                    }
                }

                // 예상 금액
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(SpaceDesign.ink100)
                        .padding(horizontal = 20.dp, vertical = 12.dp).height(30.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "예상 금액",
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        lineHeight = 18.sp, color = SpaceDesign.ink900,
                    )
                    Text(
                        SpaceDesign.won(estimate),
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp,
                        lineHeight = 29.sp, color = SpaceDesign.primaryText, textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "원",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                        lineHeight = 22.sp, color = SpaceDesign.primaryText,
                    )
                }

                // 알려드립니다
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(SpaceDesign.primaryTint).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Info, null, tint = SpaceDesign.primaryText, modifier = Modifier.size(16.dp))
                        Text(
                            "알려드립니다",
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                            lineHeight = 18.sp, color = SpaceDesign.ink900,
                        )
                    }
                    Text(
                        "예약 요청은 공간 소유자의 확인 후 확정됩니다. 확정·취소 결과는 알림으로 안내드리며, 결제는 확정 이후에 진행됩니다.",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                        lineHeight = 24.sp, color = SpaceDesign.ink700,
                    )
                }

                error?.let {
                    Text(
                        it,
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                        lineHeight = 16.sp, color = Color.Red,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }

            // 하단 고정 CTA
            Column(Modifier.fillMaxWidth().background(Color.White)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(SpaceDesign.ink200))
                Text(
                    if (submitting) "요청 중…" else "예약 요청하기",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                    lineHeight = 20.sp,
                    color = if (canSubmit) Color.White else SpaceDesign.ink100,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = SpaceDesign.gutter)
                        .padding(top = 12.dp, bottom = 20.dp)
                        .fillMaxWidth().height(52.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (canSubmit) SpaceDesign.primary else SpaceDesign.ink300)
                        .clickable(enabled = canSubmit) {
                            submitting = true
                            scope.launch {
                                api.reserve(
                                    space.id,
                                    SpaceReservationRequest(
                                        bookingType = bookingType,
                                        optionId = optionId,
                                        date = YMD.format(Date(dateMillis)),
                                        startTime = if (bookingType == "HOURLY") startTime else null,
                                        hours = if (bookingType == "HOURLY") hours else null,
                                    ),
                                )
                                    .onSuccess { onFinish("예약 요청이 접수되었습니다.") }
                                    .onFailure { error = "예약 요청에 실패했습니다. 잠시 후 다시 시도해 주세요." }
                                submitting = false
                            }
                        }
                        .wrapContentHeight(Alignment.CenterVertically),
                )
            }
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text("확인", fontFamily = customFontFamily, color = SpaceDesign.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("취소", fontFamily = customFontFamily, color = SpaceDesign.ink600)
                }
            },
        ) { DatePicker(state) }
    }
}

/** 06:00 ~ 23:00, 30분 단위 (웹 TIME_OPTIONS 와 동일) */
private val TIME_OPTIONS: List<String> = (0 until 35).map { i ->
    val minutes = 6 * 60 + i * 30
    String.format(Locale.KOREA, "%02d:%02d", minutes / 60, minutes % 60)
}

/** 최소 이용시간 ~ 12시간, 30분 단위 */
private fun hourOptions(minHours: Double): List<Double> {
    val count = maxOf(1, ((12 - minHours) * 2).toInt() + 1)
    return (0 until count).map { minHours + it * 0.5 }
}

private fun hourLabel(value: Double): String {
    val whole = value.toInt()
    return if (value % 1.0 != 0.0) {
        if (whole > 0) "${whole}시간 30분" else "30분"
    } else {
        "${whole}시간"
    }
}

private val YMD = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).apply {
    timeZone = TimeZone.getTimeZone("Asia/Seoul")
}

// MARK: 폼 조각 — 라벨 + 테두리 박스(h48, r8, #EAEAEA)

@Composable
private fun FieldGroup(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 17.sp, color = SpaceDesign.ink900,
        )
        content()
    }
}

@Composable
private fun RadioRow(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp))
            .border(
                1.dp,
                if (selected) SpaceDesign.primary else SpaceDesign.ink200,
                RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(20.dp).clip(CircleShape)
                .border(
                    if (selected) 6.dp else 1.dp,
                    if (selected) SpaceDesign.primary else SpaceDesign.ink300,
                    CircleShape,
                ),
        )
        Text(
            text,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
            lineHeight = 18.sp, color = SpaceDesign.ink900,
        )
    }
}

@Composable
private fun BoxField(text: String, placeholder: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp))
            .border(1.dp, SpaceDesign.ink200, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
            lineHeight = 18.sp,
            color = if (placeholder) SpaceDesign.ink500 else SpaceDesign.ink900,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Filled.ExpandMore, null, tint = SpaceDesign.ink500, modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun <T> MenuField(
    text: String,
    placeholder: Boolean,
    options: List<T>,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        BoxField(text, placeholder) { open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(
                    text = { Text(label(o), fontFamily = customFontFamily, fontSize = 15.sp) },
                    onClick = { open = false; onSelect(o) },
                )
            }
        }
    }
}

/** 오늘 자정(KST) — 날짜 기본값 계산용. */
internal fun todayKstMillis(): Long {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul"))
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}
