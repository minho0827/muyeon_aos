package com.muyeon.app.ui.castingstage.create

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.castingstage.data.CastingStageApi
import com.muyeon.app.ui.castingstage.model.stageImageUrl
import com.muyeon.app.ui.common.FormCard
import com.muyeon.app.ui.common.FormChipWrap
import com.muyeon.app.ui.common.FormDateRow
import com.muyeon.app.ui.common.FormField
import com.muyeon.app.ui.common.FormReadOnlyBox
import com.muyeon.app.ui.common.FormTextField
import com.muyeon.app.ui.common.FormToggle
import com.muyeon.app.ui.common.MuyeonBottomButton
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.ui.common.MuyeonDialogKind
import com.muyeon.app.ui.jobposting.PostingLimitException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/*
 * 공연 공고 등록(네이티브, 공연팀 기획자 전용) — iOS CastingCreateFlowView 와 1:1.
 *  4단계: 공연 정보 → 모집 조건 → 비용·안내 → 확인. 웹 /casting/create 와 같은 항목·같은 값을 보낸다.
 *  모양은 PACERA 만들기 흐름: 카드 묶음 + 아래 고정 [다음](ui/common/MuyeonFormKit).
 *  캐스팅은 임시저장이 없다(웹도 없음) — 첫 단계에서 나갈 때 적은 게 있으면 확인한다.
 */

private val TitleStyle = TextStyle(fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp)
private val SubStyle = TextStyle(fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp)
private val CaptionStyle = TextStyle(fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp)
private val PageBg = Color(0xFFF8F8F8)

private val STEP_TITLES = listOf("공연 정보", "모집 조건", "비용·안내", "확인")
private val STEP_SUBS = listOf(
    "어떤 공연인지 알려 주세요.",
    "어떤 무용수를 찾는지 적어 주세요.",
    "출연료와 지원 안내를 적어 주세요.",
    "입력한 내용을 확인하고 등록해 주세요.",
)

private enum class DateField(val title: String) {
    SCHEDULE("공연 날짜"), REHEARSAL_START("리허설 시작일"), REHEARSAL_END("리허설 종료일"), DEADLINE("지원 마감일")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CastingCreateScreen(
    api: CastingStageApi,
    onClose: () -> Unit,
    /** 등록이 끝나면 새 공고 id(못 받으면 null). */
    onCreated: (Int?) -> Unit,
    onOpenMembership: () -> Unit,
    /** 한도 안내의 [내 공고 관리]. null 이면 버튼 숨김. */
    onOpenMyPostings: (() -> Unit)? = null,
) {
    var form by remember { mutableStateOf(CastingCreateForm()) }
    var activityName by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var postingLimit by remember { mutableStateOf<PostingLimitException?>(null) }
    var showExit by remember { mutableStateOf(false) }
    var created by remember { mutableStateOf<Int?>(null) }
    var createdDone by remember { mutableStateOf(false) }
    var dateField by remember { mutableStateOf<DateField?>(null) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    LaunchedEffect(Unit) { activityName = api.activityName() }
    LaunchedEffect(step) { scroll.scrollTo(0) }

    fun requestBack() {
        if (step > 0) { step -= 1; return }
        if (form.isDirty) showExit = true else onClose()
    }
    BackHandler { requestBack() }

    val poster = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploading = true
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            }
            val url = bytes?.let { api.uploadPoster(it).getOrNull() }
            if (url != null) form = form.copy(imageUrl = url) else errorMessage = "이미지 업로드에 실패했어요."
            uploading = false
        }
    }

    fun submit() {
        if (saving || !form.basicValid || !form.recruitValid) return
        saving = true
        scope.launch {
            runCatching { api.createCasting(form.toJson(activityName)) }
                .onSuccess { created = it; createdDone = true }
                .onFailure { e ->
                    if (e is PostingLimitException) postingLimit = e
                    else errorMessage = e.message?.takeIf { it.isNotBlank() } ?: "등록에 실패했어요. 잠시 후 다시 시도해 주세요."
                }
            saving = false
        }
    }

    Column(Modifier.fillMaxSize().background(PageBg).statusBarsPadding().imePadding()) {
        // 위 막대 — ‹ · 제목 · n / 4
        Box(Modifier.fillMaxWidth().height(52.dp).background(Color.White)) {
            Box(
                Modifier.align(Alignment.CenterStart).size(48.dp).clickable { requestBack() }
                    .semantics { contentDescription = "뒤로" }.testTag("castingCreate.back"),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MuyeonColors.textHead) }
            Text("공연 공고 등록", style = TitleStyle.copy(fontSize = 18.sp), color = MuyeonColors.textHead, modifier = Modifier.align(Alignment.Center))
            Text("${step + 1} / 4", style = SubStyle, color = MuyeonColors.textSub,
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp))
        }
        LinearProgressIndicator(
            progress = { (step + 1) / 4f }, modifier = Modifier.fillMaxWidth(),
            color = MuyeonColors.primary, trackColor = Color(0xFFEAEAEA),
            gapSize = 0.dp, drawStopIndicator = {},
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(STEP_TITLES[step], style = TitleStyle, color = MuyeonColors.textHead)
                Text(STEP_SUBS[step], style = SubStyle, color = MuyeonColors.textSub)
            }
            when (step) {
                0 -> {
                    BasicCard(form, activityName, api.token) { form = it }
                    ScheduleCard(form, onPick = { dateField = it }) { form = it }
                    PosterCard(form.imageUrl, uploading,
                        onPick = { poster.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onRemove = { form = form.copy(imageUrl = null) })
                }
                1 -> RecruitCard(form) { form = it }
                2 -> {
                    FeeCard(form) { form = it }
                    RequirementCard(form) { form = it }
                    GuideCard(form) { form = it }
                }
                else -> {
                    SummaryCard(form)
                    Text("등록하면 바로 무용수에게 보여요. 공고는 내 공고 관리에서 마감할 수 있어요.",
                        style = CaptionStyle, color = MuyeonColors.textSub, modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
            Box(Modifier.height(24.dp))
        }
        when (step) {
            0 -> MuyeonBottomButton("다음", "castingCreate.next", enabled = form.basicValid && !uploading) { step = 1 }
            1 -> MuyeonBottomButton("다음", "castingCreate.next", enabled = form.recruitValid) { step = 2 }
            2 -> MuyeonBottomButton("다음", "castingCreate.next") { step = 3 }
            else -> MuyeonBottomButton("공고 등록", "castingCreate.submit",
                enabled = form.basicValid && form.recruitValid && !uploading, loading = saving) { submit() }
        }
    }

    dateField?.let { f ->
        val current = when (f) {
            DateField.SCHEDULE -> form.schedule
            DateField.REHEARSAL_START -> form.rehearsalStart ?: form.schedule
            DateField.REHEARSAL_END -> form.rehearsalEnd ?: form.rehearsalStart
            DateField.DEADLINE -> form.deadline
        } ?: LocalDate.now()
        val state = rememberDatePickerState(initialSelectedDateMillis = current.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { dateField = null },
            confirmButton = {
                TextButton(onClick = {
                    // [완료] 를 눌렀을 때만 반영(iOS 휠 시트와 같다).
                    state.selectedDateMillis?.let { ms ->
                        val d = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        form = when (f) {
                            DateField.SCHEDULE -> form.copy(schedule = d)
                            DateField.REHEARSAL_START -> form.copy(rehearsalStart = d,
                                rehearsalEnd = form.rehearsalEnd?.let { if (it.isBefore(d)) d else it })
                            DateField.REHEARSAL_END -> form.copy(rehearsalEnd = form.rehearsalStart?.takeIf { d.isBefore(it) } ?: d)
                            DateField.DEADLINE -> form.copy(deadline = d, deadlineUndecided = false)
                        }
                    }
                    dateField = null
                }) { Text("완료", fontFamily = customFontFamily, color = MuyeonColors.primary) }
            },
            dismissButton = {
                TextButton(onClick = { dateField = null }) { Text("취소", fontFamily = customFontFamily, color = MuyeonColors.textSub) }
            },
        ) { DatePicker(state, title = { Text(f.title, Modifier.padding(start = 24.dp, top = 16.dp), fontFamily = customFontFamily) }) }
    }

    if (showExit) {
        MuyeonDialog(
            title = "작성 중인 내용이 사라져요.",
            actions = listOf(
                MuyeonDialogAction("나가기", MuyeonDialogKind.DESTRUCTIVE) { showExit = false; onClose() },
                MuyeonDialogAction("계속 작성", MuyeonDialogKind.CANCEL) { showExit = false },
            ),
            onDismiss = { showExit = false },
        )
    }
    if (createdDone) {
        MuyeonDialog(
            title = "공연 공고가 등록됐어요.",
            actions = listOf(MuyeonDialogAction("확인", MuyeonDialogKind.PRIMARY) { createdDone = false; onCreated(created) }),
            onDismiss = { createdDone = false; onCreated(created) },
        )
    }
    errorMessage?.let { msg ->
        MuyeonDialog(
            title = "알림", message = msg,
            actions = listOf(MuyeonDialogAction("확인", MuyeonDialogKind.PRIMARY) { errorMessage = null }),
            onDismiss = { errorMessage = null },
        )
    }
    // 공고 한도 초과 — 작성 내용은 그대로 둔다(웹 keepForm 과 같음). 멤버면 [내 공고 관리]/[등급 올리기].
    postingLimit?.let { e ->
        val member = e.hasData && e.tier != null
        val message = e.serverMessage ?: if (member)
            "${e.tier} 멤버십은 공개 중인 ${e.kindLabel} 공고를 ${e.limit ?: 0}개까지 등록할 수 있어요(현재 ${e.used ?: 0}개). 마감할 공고를 정리하거나 멤버십 등급을 올려 주세요."
        else "무료 회원은 공개중인 공고를 1개까지 등록할 수 있어요. 멤버십에 가입하면 공고를 더 등록할 수 있어요."
        MuyeonDialog(
            title = if (member) "공고 등록 한도에 도달했어요" else "공고를 하나 더 등록할까요?",
            message = message,
            actions = if (member) listOfNotNull(
                onOpenMyPostings?.let { go -> MuyeonDialogAction("내 공고 관리", MuyeonDialogKind.CANCEL) { postingLimit = null; go() } },
                MuyeonDialogAction("등급 올리기", MuyeonDialogKind.PRIMARY) { postingLimit = null; onOpenMembership() },
                MuyeonDialogAction("닫기", MuyeonDialogKind.CANCEL) { postingLimit = null },
            ) else listOf(
                MuyeonDialogAction("멤버십 보기", MuyeonDialogKind.PRIMARY) { postingLimit = null; onOpenMembership() },
                MuyeonDialogAction("다음에", MuyeonDialogKind.CANCEL) { postingLimit = null },
            ),
            onDismiss = { postingLimit = null },
        )
    }
}

// MARK: 카드들 — 칸 이름·안내문·글자 수는 웹 CastingCreate.js 와 같다.

@Composable
private fun BasicCard(form: CastingCreateForm, activityName: String, token: String?, update: (CastingCreateForm) -> Unit) {
    FormCard("공연 정보", Icons.Filled.TheaterComedy) {
        FormField("공연명", required = true) {
            FormTextField(form.title, { update(form.copy(title = it)) }, "예: 정기공연 호두까기인형", CastingCreateOptions.TITLE_MAX, tag = "castingCreate.title")
        }
        FormField("활동명") { FormReadOnlyBox(activityName.ifEmpty { "계정 활동명" }) }
        FormField("공연 장르", required = true) {
            FormChipWrap(CastingCreateOptions.genres.map { it to it }, { form.genre == it }) { v ->
                update(form.copy(genre = if (form.genre == v) null else v))
            }
        }
        FormField("공연 장소") {
            FormTextField(form.place, { update(form.copy(place = it)) }, "예: 예술의전당", CastingCreateOptions.PLACE_MAX)
        }
        FormField("지역") {
            com.muyeon.app.ui.quote.RegionPickRow(token = token, placeholder = "지역 선택", name = form.region, allowSidoAll = true) { name, code ->
                update(form.copy(region = name, regionCode = code))
            }
        }
    }
}

@Composable
private fun ScheduleCard(form: CastingCreateForm, onPick: (DateField) -> Unit, update: (CastingCreateForm) -> Unit) {
    FormCard("일정", Icons.Filled.CalendarToday) {
        FormField("공연 날짜") { FormDateRow(form.schedule.dotted(), "날짜 선택", Modifier.fillMaxWidth()) { onPick(DateField.SCHEDULE) } }
        FormField("리허설 기간") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormDateRow(form.rehearsalStart.dotted(), "시작일", Modifier.weight(1f)) { onPick(DateField.REHEARSAL_START) }
                Text("~", color = MuyeonColors.textSub)
                FormDateRow(form.rehearsalEnd.dotted(), "종료일", Modifier.weight(1f)) { onPick(DateField.REHEARSAL_END) }
            }
        }
        FormField("지원 마감일") {
            FormDateRow(if (form.deadlineUndecided) "미정" else form.deadline.dotted(), "날짜 선택", Modifier.fillMaxWidth(),
                enabled = !form.deadlineUndecided) { onPick(DateField.DEADLINE) }
            FormToggle("미정", form.deadlineUndecided) { on ->
                update(form.copy(deadlineUndecided = on, deadline = if (on) null else form.deadline))
            }
        }
    }
}

@Composable
private fun PosterCard(imageUrl: String?, uploading: Boolean, onPick: () -> Unit, onRemove: () -> Unit) {
    FormCard("공연 포스터/대표 이미지", Icons.Filled.AddPhotoAlternate) {
        stageImageUrl(imageUrl)?.let { url ->
            Box {
                AsyncImage(url, null, Modifier.width(120.dp).height(160.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
                Icon(Icons.Filled.Cancel, "이미지 지우기", tint = Color.White,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(22.dp).clickable(onClick = onRemove))
            }
        }
        Row(
            Modifier.clickable(enabled = !uploading, onClick = onPick).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (uploading) CircularProgressIndicator(Modifier.size(16.dp), color = MuyeonColors.primary, strokeWidth = 2.dp)
            else Icon(Icons.Filled.AddPhotoAlternate, null, tint = MuyeonColors.primary, modifier = Modifier.size(18.dp))
            Text(if (uploading) "이미지 업로드 중…" else if (imageUrl == null) "이미지 올리기" else "이미지 바꾸기",
                style = SubStyle, color = MuyeonColors.primary)
        }
        Text("목록과 상세 맨 위에 보여요.", style = CaptionStyle, color = MuyeonColors.textSub)
    }
}

@Composable
private fun RecruitCard(form: CastingCreateForm, update: (CastingCreateForm) -> Unit) {
    FormCard("모집 조건", Icons.Filled.Groups) {
        FormField("모집 역할", required = true, hint = "(복수 선택)") {
            FormChipWrap(CastingCreateOptions.roles.map { it to it }, { it in form.roles }) { update(form.toggleRole(it)) }
        }
        FormField("모집 인원") {
            FormTextField(form.headcount, { update(form.copy(headcount = it.filter(Char::isDigit))) }, "예: 4", 3,
                keyboardType = KeyboardType.Number)
        }
        FormField("성별 조건") {
            FormChipWrap(CastingCreateOptions.genders.map { it to it }, { form.gender == it }) { v ->
                update(form.copy(gender = if (form.gender == v) null else v))
            }
        }
        FormField("나이 조건") { FormTextField(form.age, { update(form.copy(age = it)) }, "예: 20~30세", CastingCreateOptions.SHORT_MAX) }
        FormField("키 조건") { FormTextField(form.height, { update(form.copy(height = it)) }, "예: 165cm 이상", CastingCreateOptions.SHORT_MAX) }
        FormField("전공 조건") {
            FormChipWrap(CastingCreateOptions.genres.map { it to it }, { form.major == it }) { v ->
                update(form.copy(major = if (form.major == v) null else v))
            }
        }
    }
}

@Composable
private fun FeeCard(form: CastingCreateForm, update: (CastingCreateForm) -> Unit) {
    FormCard("비용", Icons.Filled.Payments) {
        FormField("출연료") {
            FormChipWrap(CastingCreateOptions.feeRanges, { form.feeRange == it }) { v ->
                update(form.copy(feeRange = if (form.feeRange == v) null else v))
            }
        }
        FormField("출연료 부가설명") {
            FormTextField(form.fee, { update(form.copy(fee = it)) }, "예: 회당 200,000원 / 리허설 별도", CastingCreateOptions.FEE_MAX)
        }
        HorizontalDivider(color = Color(0xFFEAEAEA))
        FormToggle("리허설비 지급", form.rehearsalFee) { update(form.copy(rehearsalFee = it)) }
        FormToggle("교통비 지급", form.transportFee) { update(form.copy(transportFee = it)) }
    }
}

@Composable
private fun RequirementCard(form: CastingCreateForm, update: (CastingCreateForm) -> Unit) {
    FormCard("제출물 필수 여부", Icons.Filled.Description) {
        FormToggle("영상 제출 필수", form.videoRequired) { update(form.copy(videoRequired = it)) }
        FormToggle("프로필 사진 필수", form.photoRequired) { update(form.copy(photoRequired = it)) }
        FormToggle("수상경력 필수", form.awardRequired) { update(form.copy(awardRequired = it)) }
        FormToggle("공연경력 필수", form.careerRequired) { update(form.copy(careerRequired = it)) }
        FormToggle("자기소개 필수", form.introRequired) { update(form.copy(introRequired = it)) }
    }
}

@Composable
private fun GuideCard(form: CastingCreateForm, update: (CastingCreateForm) -> Unit) {
    FormCard("안내", Icons.AutoMirrored.Filled.FormatAlignLeft) {
        FormField("공연 상세 설명") {
            FormTextField(form.desc, { update(form.copy(desc = it)) }, "공연 및 모집 상세 설명", CastingCreateOptions.DESC_MAX, minLines = 5)
            Text("${form.desc.length} / ${CastingCreateOptions.DESC_MAX}", style = CaptionStyle, color = MuyeonColors.textSub,
                textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
        }
        FormField("지원 방법") {
            FormTextField(form.applyMethod, { update(form.copy(applyMethod = it)) }, "예: 앱 내 지원 / 이메일 제출", CastingCreateOptions.LINE_MAX)
        }
        FormField("문의 방법") {
            FormTextField(form.contact, { update(form.copy(contact = it)) }, "예: 010-0000-0000 / email@team.com", CastingCreateOptions.LINE_MAX)
        }
    }
}

@Composable
private fun SummaryCard(form: CastingCreateForm) {
    fun yes(b: Boolean) = if (b) "예" else "아니오"
    val required = listOf("영상" to form.videoRequired, "프로필 사진" to form.photoRequired, "수상경력" to form.awardRequired,
        "공연경력" to form.careerRequired, "자기소개" to form.introRequired).filter { it.second }.joinToString(", ") { it.first }
    val rows = listOf(
        "공연명" to form.trimmedTitle,
        "장르" to form.genre.orEmpty(),
        "공연 장소" to form.place,
        "지역" to form.region,
        "공연 날짜" to form.schedule.dotted().orEmpty(),
        "리허설" to form.rehearsalText?.replace("-", ".").orEmpty(),
        "지원 마감" to (if (form.deadlineUndecided) "미정" else form.deadline.dotted().orEmpty()),
        "모집 역할" to form.roles.joinToString(", "),
        "모집 인원" to "${form.headcount.ifEmpty { "1" }}명",
        "성별" to form.gender.orEmpty(),
        "출연료" to listOf(CastingCreateOptions.feeLabel(form.feeRange), form.fee).filter { it.isNotBlank() }.joinToString(" · "),
        "리허설비" to yes(form.rehearsalFee),
        "교통비" to yes(form.transportFee),
        "필수 제출물" to required,
        "포스터" to (if (form.imageUrl == null) "" else "1장"),
    )
    FormCard("입력한 내용", Icons.Filled.Checklist) {
        Column {
            rows.forEach { (k, v) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Text(k, style = SubStyle, color = MuyeonColors.textSub, modifier = Modifier.width(84.dp))
                    Text(v.ifEmpty { "-" }, style = SubStyle.copy(fontWeight = FontWeight.Medium), color = MuyeonColors.textHead)
                }
                HorizontalDivider(color = Color(0xFFEAEAEA))
            }
        }
    }
}
