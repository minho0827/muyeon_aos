package com.muyeon.app.ui.lesson

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.ui.quote.QuoteUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 레슨 개설 위저드 — iOS `Wizard/LessonWizardView.swift` + `+Steps.swift` 1:1.
 *  6단계: 기본 / 소개·사진 / 위치 / 시간·정원·가격 / 예약 안내(선택) / 미리보기.
 *  lessonId 가 있으면 수정 모드(프리필 + PUT).
 *
 * ⚠️ 단계 진행 조건(canProceed)은 iOS 와 동일:
 *   0=제목+장르 / 2=온라인이거나 장소·주소 중 하나 / 3=요일 1개 이상 / 나머지 자유.
 */
private val STEP_TITLES = listOf("기본 정보", "소개·사진", "위치", "시간·정원·가격", "예약 안내", "미리보기")
private val STEP_SUBTITLES = listOf(
    "레슨명과 장르를 정해주세요.",
    "레슨을 소개하고 사진을 올려주세요.",
    "수업 장소와 주소를 등록하세요.",
    "요일·시간·정원·가격을 설정하면 예약 시간이 자동 생성돼요.",
    "예약 시 수강생에게 보일 안내예요. 비워도 됩니다.",
    "내용을 확인하고 게시하세요.",
)

@Composable
fun LessonWizardScreen(
    api: LessonWizardApi,
    lessonId: Int?,            // null = 개설, 값 = 수정
    onClose: () -> Unit,
    onCreated: (Int) -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    var draft by remember { mutableStateOf(LessonWizardDraft()) }
    var hasDetailImageEnt by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showExit by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(lessonId) {
        hasDetailImageEnt = api.hasDetailImage()
        if (lessonId != null) {
            api.getProduct(lessonId).onSuccess { d ->
                draft = d.toDraft().also {
                    // iOS: 장소명이 '온라인 레슨'이거나 주소·좌표가 모두 없으면 온라인으로 판정.
                    it.isOnline = d.place == "온라인 레슨" || (d.address.isNullOrEmpty() && d.lat == null)
                }
            }
        }
    }

    suspend fun upload(uri: android.net.Uri): String? {
        val bytes = withContext(Dispatchers.IO) {
            runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        } ?: return null
        return api.uploadImage(bytes).getOrNull()
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        uploading = true
        scope.launch {
            uris.forEach { u -> upload(u)?.let { draft = draft.copy(images = draft.images + it) } }
            uploading = false
        }
    }
    val detailPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        uploading = true
        scope.launch {
            uris.forEach { u -> upload(u)?.let { draft = draft.copy(detailImages = draft.detailImages + it) } }
            uploading = false
        }
    }

    val canProceed = when (step) {
        0 -> draft.title.trim().isNotEmpty() && draft.genre.isNotEmpty()
        2 -> draft.isOnline || draft.place.trim().isNotEmpty() || draft.address.trim().isNotEmpty()
        3 -> draft.days.isNotEmpty()
        else -> true
    }
    val isLast = step == STEP_TITLES.lastIndex

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = if (lessonId != null) "레슨 수정" else "레슨 개설", onClose = { showExit = true })

        // 진행바
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            STEP_TITLES.indices.forEach { i ->
                Box(
                    Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(50))
                        .background(if (i <= step) MuyeonColors.primary else MuyeonColors.border),
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                STEP_TITLES[step],
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                lineHeight = 24.sp, color = MuyeonColors.textHead,
            )
            Text(
                STEP_SUBTITLES[step],
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                lineHeight = 18.sp, color = MuyeonColors.textSub,
            )
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            when (step) {
                0 -> StepBasic(draft) { draft = it }
                1 -> StepIntro(draft, hasDetailImageEnt, uploading, { draft = it },
                    onPickPhotos = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onPickDetail = { detailPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                2 -> StepLocation(draft) { draft = it }
                3 -> StepSchedule(draft) { draft = it }
                4 -> StepExtra(draft) { draft = it }
                else -> StepPreview(draft)
            }
        }

        // 하단 버튼
        Row(
            Modifier.fillMaxWidth().background(MuyeonColors.surface).padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (step > 0) {
                WizardButton("이전", filled = false, enabled = true, modifier = Modifier.weight(1f)) { step -= 1 }
            }
            val label = if (isLast) {
                if (submitting) "저장 중…" else if (lessonId != null) "수정 완료" else "레슨 게시"
            } else "다음"
            val enabled = if (isLast) !submitting else canProceed
            WizardButton(label, filled = true, enabled = enabled, modifier = Modifier.weight(1f)) {
                if (!isLast) { step += 1; return@WizardButton }
                submitting = true
                scope.launch {
                    val payload = draft.toPayload()
                    val result = if (lessonId != null) api.updateProduct(lessonId, payload) else api.createProduct(payload)
                    result.onSuccess { id -> onCreated(if (id > 0) id else (lessonId ?: 0)) }
                        .onFailure { errorMessage = it.message }
                    submitting = false
                }
            }
        }
    }

    if (showExit) {
        QuoteDialog(
            title = "작성을 그만둘까요?",
            message = "저장하지 않은 내용은 사라져요.",
            confirmText = "나가기",
            onConfirm = { showExit = false; onClose() },
            onDismiss = { showExit = false },
        )
    }
    errorMessage?.let { msg ->
        QuoteDialog("알림", msg, "확인", onConfirm = { errorMessage = null }, onDismiss = { errorMessage = null })
    }
}

// ============================================================
// 단계별 폼
// ============================================================

@Composable
private fun StepBasic(d: LessonWizardDraft, onChange: (LessonWizardDraft) -> Unit) {
    WizardField("레슨명", required = true) {
        WizardTextField(d.title, "예: 성인 취미 발레 (초급)") { onChange(d.copy(title = it)) }
    }
    WizardField("장르", required = true) {
        WizardMenu(d.genre, "장르를 선택하세요", LessonOptions.genres) { onChange(d.copy(genre = it)) }
    }
    WizardField("난이도") {
        WizardMenu(d.level, "난이도를 선택하세요", LessonOptions.levels) { onChange(d.copy(level = it)) }
    }
    WizardToggle("원데이 체험 레슨", d.isExperience) { onChange(d.copy(isExperience = it)) }
}

@Composable
private fun StepIntro(
    d: LessonWizardDraft,
    hasDetailEnt: Boolean,
    uploading: Boolean,
    onChange: (LessonWizardDraft) -> Unit,
    onPickPhotos: () -> Unit,
    onPickDetail: () -> Unit,
) {
    WizardField("레슨 소개") {
        WizardMultiline(d.intro, "어떤 레슨인지, 누구에게 맞는지 알려주세요.") { onChange(d.copy(intro = it)) }
    }
    WizardField("대표 사진") {
        PhotoStrip(d.images, uploading, onPickPhotos) { url -> onChange(d.copy(images = d.images - url)) }
    }
    // 상세페이지 — 이미지형은 멤버십 DETAIL_IMAGE 이용권 보유자만.
    WizardField("상세 페이지") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("TEXT" to "글", "IMAGE" to "이미지").forEach { (v, label) ->
                    val on = d.detailType == v
                    val locked = v == "IMAGE" && !hasDetailEnt
                    Text(
                        if (locked) "$label (이용권)" else label,
                        fontFamily = customFontFamily,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp, lineHeight = 16.sp, textAlign = TextAlign.Center,
                        color = if (on) Color.White else if (locked) MuyeonColors.secondary else MuyeonColors.textSub,
                        modifier = Modifier.weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (on) MuyeonColors.primary else Color(0xFFF2F2F7))
                            .clickable(enabled = !locked) { onChange(d.copy(detailType = v)) }
                            .padding(vertical = 9.dp),
                    )
                }
            }
            if (d.detailType == "IMAGE") {
                PhotoStrip(d.detailImages, uploading, onPickDetail) { url ->
                    onChange(d.copy(detailImages = d.detailImages - url))
                }
            }
        }
    }
}

@Composable
private fun StepLocation(d: LessonWizardDraft, onChange: (LessonWizardDraft) -> Unit) {
    // 온라인↔오프라인 전환 시 위치 입력을 전부 초기화 — 서로 의미가 다른 값이 남아
    //  '유령 좌표'로 게시되는 것을 막는다(iOS setOnline 규칙).
    WizardToggle("온라인(비대면) 레슨", d.isOnline) { on ->
        onChange(d.copy(isOnline = on, place = "", address = "", addressDetail = "", region = "", regionCode = "", lat = null, lng = null))
    }
    if (!d.isOnline) {
        WizardField("장소명") {
            WizardTextField(d.place, "예: 무용연 스튜디오 강남점") { onChange(d.copy(place = it)) }
        }
        WizardField("주소") {
            WizardTextField(d.address, "도로명 또는 지번 주소") { onChange(d.copy(address = it)) }
        }
        WizardField("상세 주소") {
            WizardTextField(d.addressDetail, "건물명·동·호수") { onChange(d.copy(addressDetail = it)) }
        }
        Text(
            "장소명 또는 주소 중 하나는 입력해야 다음으로 넘어갈 수 있어요.",
            fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp, color = MuyeonColors.textSub,
        )
    } else {
        Text(
            "온라인 레슨은 장소·주소가 필요 없어요. 접속 방법은 '예약 안내'에 적어주세요.",
            fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = MuyeonColors.textSub,
        )
    }
}

@Composable
private fun StepSchedule(d: LessonWizardDraft, onChange: (LessonWizardDraft) -> Unit) {
    WizardField("요일", required = true) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            LessonOptions.weekdays.forEach { (value, label) ->
                val on = d.days.contains(value)
                Text(
                    label,
                    fontFamily = customFontFamily,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp, lineHeight = 16.sp, textAlign = TextAlign.Center,
                    color = if (on) Color.White else MuyeonColors.textSub,
                    modifier = Modifier.weight(1f)
                        .clip(RoundedCornerShape(50))
                        .background(if (on) MuyeonColors.primary else Color(0xFFF2F2F7))
                        .clickable { onChange(d.copy(days = if (on) d.days - value else d.days + value)) }
                        .padding(vertical = 9.dp),
                )
            }
        }
    }
    WizardField("시작 시간") {
        WizardMenu(d.startTime, "시작 시간", LessonOptions.startTimes) { onChange(d.copy(startTime = it)) }
    }
    WizardField("수업 시간") {
        WizardIntMenu(d.duration, LessonOptions.durations, { "${it}분" }) { onChange(d.copy(duration = it)) }
    }
    WizardField("정원") {
        WizardIntMenu(d.capacity, LessonOptions.capacities, { "${it}명" }) { onChange(d.copy(capacity = it)) }
    }
    WizardField("가격") {
        WizardIntMenu(d.price, LessonOptions.prices, LessonOptions::priceLabel) { onChange(d.copy(price = it)) }
    }
    WizardField("예약 반복") {
        WizardIntMenu(d.weeksAhead, LessonOptions.weeks, LessonOptions::weeksLabel) { onChange(d.copy(weeksAhead = it)) }
    }
    if (d.days.isNotEmpty()) {
        Text(
            scheduleSummary(d),
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            lineHeight = 18.sp, color = MuyeonColors.primary,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(MuyeonColors.primary.copy(alpha = 0.08f)).padding(12.dp),
        )
    }
}

@Composable
private fun StepExtra(d: LessonWizardDraft, onChange: (LessonWizardDraft) -> Unit) {
    WizardField("연락처") { WizardTextField(d.phone, "예약 문의용", KeyboardType.Phone) { onChange(d.copy(phone = it)) } }
    WizardField("주차 안내") { WizardTextField(d.parkingInfo, "") { onChange(d.copy(parkingInfo = it)) } }
    WizardField("발렛 안내") { WizardTextField(d.valetInfo, "") { onChange(d.copy(valetInfo = it)) } }
    WizardField("홈페이지") { WizardTextField(d.homepage, "https://") { onChange(d.copy(homepage = it)) } }
    WizardField("공지사항") { WizardMultiline(d.notice, "수강생에게 알릴 내용") { onChange(d.copy(notice = it)) } }
    WizardField("취소 규정") { WizardMultiline(d.cancelPolicy, "예: 수업 24시간 전까지 취소 가능") { onChange(d.copy(cancelPolicy = it)) } }
}

@Composable
private fun StepPreview(d: LessonWizardDraft) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (d.images.isNotEmpty()) {
            AsyncImage(
                QuoteUi.imageUrl(d.images.first()), null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)),
            )
        }
        Text(
            d.title.ifEmpty { "(레슨명 없음)" },
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp,
            lineHeight = 24.sp, color = MuyeonColors.textHead,
        )
        Text(
            listOf(d.genre, d.level).filter { it.isNotEmpty() }.joinToString(" · "),
            fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 18.sp, color = MuyeonColors.textSub,
        )
        PreviewRow("일정", scheduleSummary(d))
        PreviewRow("장소", if (d.isOnline) "온라인 레슨" else listOf(d.place, d.address).filter { it.isNotEmpty() }.joinToString(" · "))
        PreviewRow("정원", "${d.capacity}명")
        PreviewRow("가격", LessonOptions.priceLabel(d.price))
        if (d.intro.isNotEmpty()) PreviewRow("소개", d.intro)
    }
}

/** "월·수 19:00~20:00 · 8주" — iOS scheduleText. */
private fun scheduleSummary(d: LessonWizardDraft): String {
    if (d.days.isEmpty()) return ""
    // 월~일 순 정렬(0=일을 맨 뒤로)
    val sorted = d.days.sortedBy { (it + 6) % 7 }
    val end = LessonOptions.addMinutes(d.startTime, d.duration)
    return "${sorted.joinToString("·") { LessonOptions.dayLabel(it) }} ${d.startTime}~$end · ${LessonOptions.weeksLabel(d.weeksAhead)}"
}

// ============================================================
// 공용 폼 요소
// ============================================================

@Composable
private fun WizardField(label: String, required: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(label, fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 17.sp, color = MuyeonColors.textHead)
            if (required) Text("*", fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MuyeonColors.primary)
        }
        content()
    }
}

@Composable
private fun WizardTextField(
    value: String,
    placeholder: String,
    keyboard: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, singleLine = true,
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder, fontFamily = customFontFamily, fontSize = 14.sp) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun WizardMultiline(value: String, placeholder: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        placeholder = { Text(placeholder, fontFamily = customFontFamily, fontSize = 14.sp) },
        modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp),
    )
}

@Composable
private fun WizardMenu(value: String, placeholder: String, options: List<String>, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
                .clickable { open = true }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                value.ifEmpty { placeholder },
                fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 18.sp,
                color = if (value.isEmpty()) MuyeonColors.secondary else MuyeonColors.textHead,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = MuyeonColors.secondary, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(
                    text = { Text(o, fontFamily = customFontFamily, fontSize = 14.sp) },
                    onClick = { open = false; onSelect(o) },
                )
            }
        }
    }
}

@Composable
private fun WizardIntMenu(value: Int, options: List<Int>, label: (Int) -> String, onSelect: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
                .clickable { open = true }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label(value),
                fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 18.sp,
                color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = MuyeonColors.secondary, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { o ->
                DropdownMenuItem(
                    text = { Text(label(o), fontFamily = customFontFamily, fontSize = 14.sp) },
                    onClick = { open = false; onSelect(o) },
                )
            }
        }
    }
}

@Composable
private fun WizardToggle(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 17.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
        )
        Switch(checked = on, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = MuyeonColors.primary))
    }
}

@Composable
private fun PhotoStrip(images: List<String>, uploading: Boolean, onAdd: () -> Unit, onRemove: (String) -> Unit) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.size(78.dp).clip(RoundedCornerShape(10.dp))
                .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
                .clickable(enabled = !uploading, onClick = onAdd),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, "사진 추가", tint = MuyeonColors.secondary, modifier = Modifier.size(22.dp))
        }
        images.forEach { url ->
            Box {
                AsyncImage(
                    QuoteUi.imageUrl(url), null, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(78.dp).clip(RoundedCornerShape(10.dp)),
                )
                Icon(
                    Icons.Filled.Close, "삭제", tint = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(16.dp)
                        .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.5f))
                        .clickable { onRemove(url) },
                )
            }
        }
    }
}

@Composable
private fun PreviewRow(label: String, value: String) {
    if (value.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
            lineHeight = 18.sp, color = MuyeonColors.textSub, modifier = Modifier.width(50.dp),
        )
        Text(
            value,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            lineHeight = 18.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun WizardButton(
    text: String,
    filled: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 19.sp,
        color = if (filled) Color.White else MuyeonColors.textHead, textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    !filled -> Color(0xFFE5E5EA)
                    enabled -> MuyeonColors.primary
                    else -> Color.Gray.copy(alpha = 0.4f)
                },
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
    )
}
