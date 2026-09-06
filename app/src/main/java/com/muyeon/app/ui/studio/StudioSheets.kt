package com.muyeon.app.ui.studio

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * 스튜디오 시트 — iOS `Studio/StudioSheets.swift` + `StudioSettingsSheet.swift` 1:1.
 *  수강권 발급 / 스튜디오 설정(노쇼 차감).
 *
 * ⚠️ 종전 AOS 회원 상세는 수강권을 **보기만** 했다. 발급이 없으니 원장은
 *   수강권을 웹으로 끊고 앱으로 확인하는 반쪽 운영을 해야 했다.
 */

private val YMD = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).apply {
    timeZone = TimeZone.getTimeZone("Asia/Seoul")
}

/** 'yyyy-MM-dd' — iOS IssuePassSheet.ymd(KST 고정). */
internal fun studioYmd(millis: Long): String = YMD.format(java.util.Date(millis))

/** 오늘 0시(KST) millis — DatePicker 초기값. */
private fun todayMillis(): Long = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

/**
 * KST 날짜를 그대로 나타내는 UTC 자정 — Material DatePicker 초기값용.
 *  DatePicker 는 값을 UTC 로 읽으므로 KST 자정(전날 15:00 UTC)을 그대로 주면 하루가 밀린다.
 */
private fun toUtcMidnight(millis: Long): Long {
    val kst = Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply { timeInMillis = millis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(kst.get(Calendar.YEAR), kst.get(Calendar.MONTH), kst.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun plusMonths(millis: Long, months: Int): Long =
    Calendar.getInstance(TimeZone.getTimeZone("Asia/Seoul")).apply {
        timeInMillis = millis; add(Calendar.MONTH, months)
    }.timeInMillis

/** 수강권 발급 시트 — iOS `IssuePassSheet`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssuePassSheet(onSubmit: (JSONObject) -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var productName by remember { mutableStateOf("") }
    var passType by remember { mutableStateOf("PERIOD") }   // PERIOD | COUNT
    var category by remember { mutableStateOf("GROUP") }    // GROUP | PRIVATE
    var totalCount by remember { mutableIntStateOf(10) }
    var priceText by remember { mutableStateOf("") }
    var startAt by remember { mutableLongStateOf(todayMillis()) }
    var useExpire by remember { mutableStateOf(true) }
    var expireAt by remember { mutableLongStateOf(plusMonths(todayMillis(), 1)) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showExpirePicker by remember { mutableStateOf(false) }

    val canSubmit = productName.isNotBlank() && (passType == "PERIOD" || totalCount >= 1)

    fun buildBody() = JSONObject().apply {
        put("productName", productName.trim())
        put("passType", passType)
        put("category", category)
        put("startAt", studioYmd(startAt))
        if (passType == "COUNT") put("totalCount", totalCount)
        if (useExpire) put("expireAt", studioYmd(expireAt))
        priceText.filter { it.isDigit() }.toIntOrNull()?.takeIf { it > 0 }?.let { put("price", it) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth()) {
            SheetHeader("수강권 발급", "발급", canSubmit, onDismiss) { onSubmit(buildBody()) }
            HorizontalDivider(color = MuyeonColors.border)

            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                SheetField("수강권명") {
                    OutlinedTextField(
                        value = productName, onValueChange = { productName = it },
                        placeholder = {
                            Text(
                                "예: 프라이빗 6개월",
                                fontFamily = customFontFamily, fontSize = 15.sp, color = MuyeonColors.chevron,
                            )
                        },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                    )
                }
                SheetField("종류") {
                    SegmentedRow(
                        listOf("PERIOD" to "기간권", "COUNT" to "횟수권"), passType,
                    ) { passType = it }
                }
                SheetField("분류") {
                    SegmentedRow(
                        listOf("GROUP" to "그룹", "PRIVATE" to "프라이빗"), category,
                    ) { category = it }
                }
                if (passType == "COUNT") {
                    SheetField("횟수") {
                        // iOS Stepper(1...500) 대응.
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StepperButton("−") { if (totalCount > 1) totalCount -= 1 }
                            Text(
                                "${totalCount}회",
                                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                                lineHeight = 18.sp, color = MuyeonColors.textHead,
                                textAlign = TextAlign.Center, modifier = Modifier.width(64.dp),
                            )
                            StepperButton("+") { if (totalCount < 500) totalCount += 1 }
                        }
                    }
                }
                SheetField("시작일") {
                    SheetBox(studioYmd(startAt)) { showStartPicker = true }
                }
                Row(
                    Modifier.fillMaxWidth().clickable { useExpire = !useExpire },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "만료일 설정",
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        lineHeight = 18.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = useExpire, onCheckedChange = { useExpire = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = MuyeonColors.primary),
                    )
                }
                if (useExpire) {
                    SheetField("만료일") {
                        SheetBox(studioYmd(expireAt)) { showExpirePicker = true }
                    }
                }
                SheetField("결제 금액 (원, 선택)") {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { v -> priceText = v.filter { it.isDigit() } },
                        placeholder = {
                            Text(
                                "예: 300000",
                                fontFamily = customFontFamily, fontSize = 15.sp, color = MuyeonColors.chevron,
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showStartPicker) {
        StudioDatePicker(startAt, onDismiss = { showStartPicker = false }) { picked ->
            startAt = picked
            // iOS DatePicker(in: startAt...) 대응 — 시작일이 만료일을 넘으면 만료일을 민다.
            if (expireAt < picked) expireAt = plusMonths(picked, 1)
        }
    }
    if (showExpirePicker) {
        StudioDatePicker(expireAt, minMillis = startAt, onDismiss = { showExpirePicker = false }) { expireAt = it }
    }
}

/**
 * 스튜디오 설정 시트 — iOS `StudioSettingsSheet`. 지금은 노쇼 수강권 차감 여부.
 *  일정 화면에도 같은 토글이 있지만 서버가 소스라 어긋나지 않는다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioSettingsSheet(api: StudioApi, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var noshowConsumes by remember { mutableStateOf(false) }
    // 최초 로드 전 토글 변경은 저장하지 않는다(iOS loaded 가드) — 기본값 false 가
    //  서버 true 를 덮어쓰는 사고를 막는다.
    var loaded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        api.getSettings().onSuccess { noshowConsumes = it }
        loaded = true
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth()) {
            SheetHeader("스튜디오 설정", "완료", true, onDismiss) { onDismiss() }
            HorizontalDivider(color = MuyeonColors.border)
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "결석(노쇼)도 수강권 차감",
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        lineHeight = 18.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = noshowConsumes,
                        onCheckedChange = { v ->
                            noshowConsumes = v
                            if (!loaded) return@Switch
                            scope.launch {
                                api.updateSettings(v).onFailure {
                                    noshowConsumes = !v
                                    errorMessage = it.message ?: "설정을 저장하지 못했어요."
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(checkedTrackColor = MuyeonColors.primary),
                    )
                }
                Text(
                    "켜면 결석 처리된 회원도 횟수권이 1회 차감됩니다. 끄면 출석한 회원만 차감돼요.",
                    fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 17.sp, color = MuyeonColors.textSub,
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    errorMessage?.let { msg ->
        com.muyeon.app.ui.quote.QuoteDialog(
            "알림", msg, "확인", onConfirm = { errorMessage = null }, onDismiss = { errorMessage = null },
        )
    }
}

// ============================================================
// 공용 조각
// ============================================================

/** iOS NavigationStack 툴바(취소 / 제목 / 확인) 대응. */
@Composable
private fun SheetHeader(
    title: String,
    confirmText: String,
    canConfirm: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "취소",
            fontFamily = customFontFamily, fontSize = 15.sp, color = MuyeonColors.textSub,
            modifier = Modifier.clickable(onClick = onCancel),
        )
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
            lineHeight = 20.sp, color = MuyeonColors.textHead,
            textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
        )
        Text(
            confirmText,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            color = if (canConfirm) MuyeonColors.primary else MuyeonColors.chevron,
            modifier = Modifier.clickable(enabled = canConfirm, onClick = onConfirm),
        )
    }
}

@Composable
private fun SheetField(label: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 17.sp, color = MuyeonColors.textHead,
        )
        content()
    }
}

@Composable
private fun SheetBox(value: String, onClick: () -> Unit) {
    Text(
        value,
        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
        lineHeight = 18.sp, color = MuyeonColors.textHead,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(MuyeonColors.groupedBg).clickable(onClick = onClick).padding(12.dp),
    )
}

/** iOS .pickerStyle(.segmented) 대응. */
@Composable
private fun SegmentedRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(MuyeonColors.groupedBg).padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (value, label) ->
            val on = selected == value
            Text(
                label,
                fontFamily = customFontFamily,
                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 14.sp, lineHeight = 17.sp,
                color = if (on) MuyeonColors.textHead else MuyeonColors.textSub,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(7.dp))
                    .background(if (on) MuyeonColors.surface else Color.Transparent)
                    .clickable { onSelect(value) }
                    .padding(vertical = 7.dp),
            )
        }
    }
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(RoundedCornerShape(10.dp))
            .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
            color = MuyeonColors.textHead,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudioDatePicker(
    initialMillis: Long,
    minMillis: Long? = null,
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit,
) {
    val state = rememberDatePickerState(initialSelectedDateMillis = toUtcMidnight(initialMillis))
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { picked ->
                    // ⚠️ DatePicker 는 UTC 자정을 준다. KST 로 그대로 포맷하면 하루가 밀린다.
                    //   정오로 옮겨 시간대 차이를 흡수한다.
                    val noon = picked + 12 * 60 * 60 * 1000
                    onPick(if (minMillis != null && noon < minMillis) minMillis else noon)
                }
                onDismiss()
            }) { Text("확인", fontFamily = customFontFamily, color = MuyeonColors.primary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소", fontFamily = customFontFamily, color = MuyeonColors.textSub)
            }
        },
    ) { DatePicker(state) }
}
