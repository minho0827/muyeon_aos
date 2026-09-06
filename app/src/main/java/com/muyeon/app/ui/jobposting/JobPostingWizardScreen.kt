package com.muyeon.app.ui.jobposting

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.muyeon.app.ui.quote.QuoteUi
import com.muyeon.app.ui.resume.ResumeOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 상세 이미지 최대 장수 — iOS JobFormData.imagesMax. */
private const val IMAGES_MAX = 10

private val STEP_TITLES = listOf("기본 정보", "상세 정보", "근무 조건", "등록 완료")

private val YMD = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)

/**
 * 채용 공고 등록 위저드 — iOS `JobPostingWizardView.swift`(+Steps) 1:1.
 *  4단계(기본정보→상세정보→근무조건→등록완료) + 상단 스텝 인디케이터 + [임시저장][이전][다음].
 *
 * ⚠️ 종전 AOS 는 이걸 단일 폼으로 압축해 두어 대표/상세 이미지·상세 설명·모집 인원·
 *   급여 부가설명·필요 경력 직접입력이 통째로 빠져 있었다(같은 공고인데 등록 경로에 따라
 *   항목이 달랐다). 단계 구성과 필드를 iOS 에 맞춘다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobPostingWizardScreen(
    api: JobPostingApi,
    jobId: Int?,
    onClose: () -> Unit,
    onSaved: () -> Unit,
    /** 무료 회원 공고 1개 제한 — 멤버십 화면으로 보낸다. */
    onOpenMembership: () -> Unit = {},
) {
    var form by remember { mutableStateOf(JobForm()) }
    // 미저장 이탈 가드 기준값 — 로드 직후(신규는 빈 폼) 스냅샷과 비교해 dirty 판정.
    var baseline by remember { mutableStateOf(JobForm()) }
    var step by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(jobId != null) }
    var saving by remember { mutableStateOf(false) }
    var uploadingCover by remember { mutableStateOf(false) }
    var uploadingDetail by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var membershipRequired by remember { mutableStateOf(false) }
    var showExit by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var showDeadlinePicker by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    LaunchedEffect(jobId) {
        jobId?.let { api.loadJob(it).onSuccess { f -> form = f; baseline = f } }
        loading = false
    }
    // 단계 전환 시 최상단으로 — 안 하면 중간부터 보인다(iOS scrollTo(topAnchor)).
    LaunchedEffect(step) { scroll.scrollTo(0) }

    val isDirty = form != baseline
    // Step1 필수: 제목·장르·모집분야·지역 (웹 JobCreate submit 검증과 동일 기준)
    val step1Valid = form.title.isNotBlank() &&
        !form.genre.isNullOrEmpty() &&
        !form.fields.isNullOrEmpty() &&
        !form.region.isNullOrEmpty()
    val canNext = step != 0 || step1Valid
    val images = form.images ?: emptyList()

    suspend fun readBytes(uri: android.net.Uri): ByteArray? = withContext(Dispatchers.IO) {
        runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
    }

    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploadingCover = true
        scope.launch {
            readBytes(uri)?.let { bytes ->
                api.uploadImage(bytes)
                    .onSuccess { form = form.copy(imageUrl = it) }
                    .onFailure { errorMessage = "이미지 업로드에 실패했어요." }
            } ?: run { errorMessage = "이미지 업로드에 실패했어요." }
            uploadingCover = false
        }
    }
    val detailPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = IMAGES_MAX),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        uploadingDetail = true
        scope.launch {
            // 최대 장수를 넘는 선택분은 자른다(이력서 포트폴리오와 동일 정책).
            val room = (IMAGES_MAX - (form.images?.size ?: 0)).coerceAtLeast(0)
            var list = form.images ?: emptyList()
            uris.take(room).forEach { u ->
                readBytes(u)?.let { bytes -> api.uploadImage(bytes).onSuccess { list = list + it } }
            }
            form = form.copy(images = list.ifEmpty { null })
            uploadingDetail = false
        }
    }

    fun save(asDraft: Boolean) {
        if (saving) return
        saving = true
        scope.launch {
            // 수정 모드에서 기존 상태(마감·보류)를 임의로 OPEN 으로 되돌리지 않는다.
            //  임시저장(DRAFT)만 '등록하기' 시 OPEN 으로 전환.
            val status = when {
                asDraft -> "DRAFT"
                !form.status.isNullOrEmpty() && form.status != "DRAFT" -> form.status
                else -> "OPEN"
            }
            api.saveJob(jobId, form.copy(status = status))
                .onSuccess { baseline = form; onSaved() }
                .onFailure {
                    val msg = it.message.orEmpty()
                    if (msg.contains("POSTING_MEMBERSHIP_REQUIRED")) membershipRequired = true
                    else errorMessage = msg.ifEmpty { "저장에 실패했어요." }
                }
            saving = false
        }
    }

    // 뒤로가기 공통 — 2단계 이상은 이전 단계로, 1단계는 이탈 확인.
    fun requestBack() {
        if (step > 0) { step -= 1; return }
        if (isDirty) showExit = true else onClose()
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        // 내비 — 좌측 뒤로 / 가운데 제목 / 우측 미리보기
        Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text(
                if (jobId != null) "채용 공고 수정" else "채용 공고 등록",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                lineHeight = 22.sp, color = MuyeonColors.textHead,
                textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center),
            )
            Icon(
                Icons.Filled.Close, "뒤로", tint = MuyeonColors.body,
                modifier = Modifier.align(Alignment.CenterStart).size(18.dp).clickable { requestBack() },
            )
            Icon(
                Icons.Outlined.Info, "공고 미리보기", tint = MuyeonColors.primary,
                modifier = Modifier.align(Alignment.CenterEnd).size(20.dp).clickable { showPreview = true },
            )
        }

        StepIndicator(step)
        Box(Modifier.fillMaxWidth().height(1.dp).background(MuyeonColors.border))

        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            return@Column
        }

        Column(
            Modifier.weight(1f).verticalScroll(scroll)
                .padding(horizontal = 20.dp).padding(top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            when (step) {
                0 -> {
                    SectionHead("기본 정보", "공고의 기본 정보를 입력해주세요.")
                    JobField("공고 제목", form.title, "예) 성인 발레 강사 모집", required = true) {
                        form = form.copy(title = it)
                    }
                    JobChips("장르", JobFormOptions.genres.map { it to it }, setOfNotNull(form.genre)) {
                        form = form.copy(genre = if (form.genre == it) null else it)
                    }
                    JobChips("모집 분야", ResumeOptions.teachingFields, (form.fields ?: emptyList()).toSet()) { v ->
                        val cur = form.fields ?: emptyList()
                        form = form.copy(fields = (if (cur.contains(v)) cur - v else cur + v).ifEmpty { null })
                    }
                    JobChips("수업 대상", ResumeOptions.classTargets, setOfNotNull(form.target)) {
                        form = form.copy(target = if (form.target == it) null else it)
                    }
                    JobField("근무 지역", form.region.orEmpty(), "예: 서울 강남구", required = true) {
                        form = form.copy(region = it)
                    }
                    CoverUpload(form.imageUrl, uploadingCover) {
                        coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    DetailImages(
                        images = images, uploading = uploadingDetail,
                        onAdd = {
                            detailPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        },
                        onRemove = { idx ->
                            form = form.copy(images = images.filterIndexed { i, _ -> i != idx }.ifEmpty { null })
                        },
                    )
                }
                1 -> {
                    SectionHead("상세 정보", "지원자에게 보여줄 상세 내용을 입력해주세요.")
                    JobMultiline("상세 설명", form.description.orEmpty(), "근무 내용, 지원 자격 등 자세히 적어주세요.") {
                        form = form.copy(description = it)
                    }
                    JobField("학원명", form.academy.orEmpty(), "예) 미노발레아카데미") { form = form.copy(academy = it) }
                    JobField("상세 주소", form.address.orEmpty(), "예) 강남구 테헤란로 …") { form = form.copy(address = it) }
                    JobField("가까운 지하철역", form.subway.orEmpty(), "예) 강남역 도보 5분") { form = form.copy(subway = it) }
                    JobChips("고용 형태", JobFormOptions.employments, setOfNotNull(form.employment)) {
                        form = form.copy(employment = if (form.employment == it) null else it)
                    }
                    JobField(
                        "모집 인원", form.headcount?.toString().orEmpty(), "예) 1",
                        keyboard = KeyboardType.Number,
                    ) { form = form.copy(headcount = it.toIntOrNull()) }
                    // 목록 카드의 D-day / 상세의 '지원 마감일' 근거값 — 자유 텍스트는 표기가 제각각이라 휠로 받는다.
                    DeadlineRow(
                        deadline = form.deadline,
                        onPick = { showDeadlinePicker = true },
                        onClear = { form = form.copy(deadline = null) },
                    )
                }
                2 -> {
                    SectionHead("근무 조건", "근무 요일·시간·급여·경력 조건을 입력해주세요.")
                    val days = (form.days ?: "").split("·").filter { it.isNotEmpty() }
                    JobChips("근무 요일", JobFormOptions.weekDays.map { it to it }, days.toSet()) { d ->
                        val next = if (days.contains(d)) days - d else days + d
                        form = form.copy(days = next.joinToString("·").ifEmpty { null })
                    }
                    JobTimeRange("근무 시간", form.time.orEmpty()) { form = form.copy(time = it) }
                    JobChips("급여 (시급)", JobFormOptions.salaryRanges, setOfNotNull(form.salary)) {
                        form = form.copy(salary = if (form.salary == it) null else it)
                    }
                    JobField("급여 부가설명", form.pay.orEmpty(), "예) 경력에 따라 협의") { form = form.copy(pay = it) }
                    JobChips("허용 경력", JobFormOptions.careerLevels, (form.careerLevels ?: emptyList()).toSet()) { v ->
                        val cur = form.careerLevels ?: emptyList()
                        form = form.copy(careerLevels = (if (cur.contains(v)) cur - v else cur + v).ifEmpty { null })
                    }
                    JobField(
                        "필요 경력 직접입력", form.careerText.orEmpty(),
                        "예) 발레 전공 5년 이상, 입시 지도 경험 우대",
                    ) { form = form.copy(careerText = it.ifBlank { null }) }

                    // 원하는 강사 조건 — 웹 JobCreate 와 항목이 같아야 한다.
                    JobChips(
                        "지도 가능 분야(우대)", ResumeOptions.teachingFields,
                        (form.pref.fields ?: emptyList()).toSet(),
                    ) { v ->
                        val cur = form.pref.fields ?: emptyList()
                        val next = if (cur.contains(v)) cur - v else cur + v
                        form = form.copy(pref = form.pref.copy(fields = next.ifEmpty { null }))
                    }
                    JobYesNo("예고 출신 우대", form.pref.artHigh) { form = form.copy(pref = form.pref.copy(artHigh = it)) }
                    JobYesNo("대학 졸업 우대", form.pref.university) { form = form.copy(pref = form.pref.copy(university = it)) }
                    JobField("우대 대학명", form.pref.universityName.orEmpty(), "예: 한예종, 세종대 (선택)") {
                        form = form.copy(pref = form.pref.copy(universityName = it.ifBlank { null }))
                    }
                    JobYesNo("무용단 출신 우대", form.pref.company) { form = form.copy(pref = form.pref.copy(company = it)) }
                    JobYesNo("자격증 필수", form.pref.certRequired) { form = form.copy(pref = form.pref.copy(certRequired = it)) }
                    JobYesNo("영상 포트폴리오 필수", form.pref.videoRequired) {
                        form = form.copy(pref = form.pref.copy(videoRequired = it))
                    }
                    JobField("기타 우대 조건", form.pref.note.orEmpty(), "예: 성인반 경험자 우대") {
                        form = form.copy(pref = form.pref.copy(note = it.ifBlank { null }))
                    }
                }
                else -> {
                    SectionHead("등록 완료", "입력한 내용을 확인하고 등록해주세요.")
                    SummaryRow("공고 제목", form.title)
                    SummaryRow("장르", form.genre.orEmpty())
                    SummaryRow("모집 분야", (form.fields ?: emptyList()).joinToString(", ") { ResumeOptions.fieldLabel(it) })
                    SummaryRow("근무 지역", form.region.orEmpty())
                    SummaryRow("근무 요일", form.days.orEmpty())
                    SummaryRow("급여", JobFormOptions.salaryLabel(form.salary))
                    SummaryRow("지원 마감일", displayDeadline(form.deadline).takeIf { it != "미정" }.orEmpty())
                    SummaryRow("상세 이미지", if (images.isEmpty()) "" else "${images.size}장")
                    Row(
                        Modifier.padding(top = 8.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MuyeonColors.primary, RoundedCornerShape(12.dp))
                            .clickable { showPreview = true }
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Visibility, null, tint = MuyeonColors.primary, modifier = Modifier.size(15.dp))
                        Text(
                            "미리보기",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            lineHeight = 18.sp, color = MuyeonColors.primary,
                        )
                    }
                    Text(
                        "공고는 등록 즉시 강사님들에게 노출됩니다. 임시저장한 공고는 목록에서 언제든 이어서 작성할 수 있어요.",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                        lineHeight = 18.sp, color = MuyeonColors.chevron,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Row(
            Modifier.padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (step == 0) {
                // 수정 모드는 '임시저장'(DRAFT 강등) 대신 현재 상태 그대로 저장.
                JobButton(
                    if (jobId == null) "임시저장" else "저장",
                    filled = false, enabled = !saving, modifier = Modifier.weight(1f),
                ) { save(jobId == null) }
            } else {
                JobButton("이전", filled = false, enabled = true, modifier = Modifier.weight(1f)) { step -= 1 }
            }
            if (step < 3) {
                JobButton("다음", filled = true, enabled = canNext, modifier = Modifier.weight(1f)) { step += 1 }
            } else {
                JobButton(
                    if (saving) "등록 중…" else "등록하기",
                    filled = true, enabled = !saving, modifier = Modifier.weight(1f),
                ) { save(false) }
            }
        }
    }

    if (showPreview) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showPreview = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) { JobPostingPreviewScreen(form) { showPreview = false } }
    }

    if (showDeadlinePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = form.deadline?.take(10)
                ?.let { runCatching { YMD.parse(it)?.time }.getOrNull() } ?: System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDeadlinePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    // [완료] 를 눌렀을 때만 반영 → 취소하면 기존 값(또는 미정) 유지(iOS 와 동일).
                    state.selectedDateMillis?.let { form = form.copy(deadline = YMD.format(Date(it))) }
                    showDeadlinePicker = false
                }) { Text("완료", fontFamily = customFontFamily, color = MuyeonColors.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showDeadlinePicker = false }) {
                    Text("취소", fontFamily = customFontFamily, color = MuyeonColors.textSub)
                }
            },
        ) { DatePicker(state) }
    }

    if (showExit) {
        JobExitPrompt(
            // 수정 모드에서 '임시저장'을 하면 이미 게시된 공고가 DRAFT 로 내려간다 → 신규일 때만 임시저장.
            saveLabel = if (jobId == null) "임시저장 후 나가기" else "저장하고 나가기",
            onSave = { showExit = false; save(jobId == null) },
            onDiscard = { showExit = false; onClose() },
            onCancel = { showExit = false },
        )
    }
    if (membershipRequired) {
        QuoteDialog(
            "공고를 하나 더 등록하시겠어요?",
            "무료 회원은 공개중인 공고를 1개까지 등록할 수 있어요. 멤버십에 가입하면 추가 공고를 등록할 수 있습니다.",
            "멤버십 보기",
            onConfirm = { membershipRequired = false; onOpenMembership() },
            onDismiss = { membershipRequired = false },
        )
    }
    errorMessage?.let { msg ->
        QuoteDialog("오류", msg, "확인", onConfirm = { errorMessage = null }, onDismiss = { errorMessage = null })
    }
}

private fun displayDeadline(raw: String?): String {
    val d = raw?.takeIf { it.isNotEmpty() && it != "-" } ?: return "미정"
    return d.take(10).replace("-", ".")
}

/** 스텝 인디케이터 (1—2—3—4). */
@Composable
private fun StepIndicator(step: Int) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        (0..3).forEach { i ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    Modifier.size(26.dp).clip(CircleShape)
                        .background(if (i <= step) MuyeonColors.primary else MuyeonColors.border),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${i + 1}",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        lineHeight = 16.sp,
                        color = if (i <= step) Color.White else MuyeonColors.chevron,
                    )
                }
                Text(
                    STEP_TITLES[i],
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = if (i == step) MuyeonColors.textHead else MuyeonColors.chevron,
                )
            }
            if (i < 3) {
                Box(
                    Modifier.weight(1f).padding(top = 12.dp).height(2.dp)
                        .background(if (i < step) MuyeonColors.primary else MuyeonColors.border),
                )
            }
        }
    }
}

@Composable
private fun SectionHead(title: String, sub: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp,
            lineHeight = 24.sp, color = MuyeonColors.textHead,
        )
        Text(
            sub,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.textSub,
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.textSub, modifier = Modifier.width(80.dp),
        )
        Text(
            value.ifEmpty { "-" },
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 17.sp, color = MuyeonColors.textHead,
        )
    }
}

@Composable
private fun JobMultiline(label: String, value: String, placeholder: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            lineHeight = 18.sp, color = MuyeonColors.textHead,
        )
        OutlinedTextField(
            value = value, onValueChange = onChange,
            placeholder = {
                Text(placeholder, fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.chevron)
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
        )
    }
}

/** 대표 이미지 — 없으면 점선 업로드 박스, 있으면 160dp 미리보기(iOS coverUpload). */
@Composable
private fun CoverUpload(imageUrl: String?, uploading: Boolean, onPick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "대표 이미지",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            lineHeight = 18.sp, color = MuyeonColors.textHead,
        )
        if (!imageUrl.isNullOrEmpty()) {
            AsyncImage(
                QuoteUi.imageUrl(imageUrl), null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onPick),
            )
        } else {
            Column(
                Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF7F7F7))
                    .border(1.dp, MuyeonColors.border, RoundedCornerShape(12.dp))
                    .clickable(enabled = !uploading, onClick = onPick),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            ) {
                if (uploading) {
                    CircularProgressIndicator(color = MuyeonColors.primary, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Filled.AddPhotoAlternate, null, tint = MuyeonColors.textSub, modifier = Modifier.size(26.dp))
                    Text(
                        "이미지 업로드",
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        lineHeight = 17.sp, color = MuyeonColors.textSub,
                    )
                    Text(
                        "JPG, PNG (최대 5MB)",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                        lineHeight = 15.sp, color = MuyeonColors.chevron,
                    )
                }
            }
        }
    }
}

/**
 * 상세 이미지(최대 10장) — 웹 JobCreate 의 "상세 이미지" 와 같은 images[].
 *  ⚠️ 가변폭 그리드는 이미지가 셀 폭을 밀어 서로 겹쳐 보인다 → 고정 정사각형 가로 스트립(iOS 와 동일).
 */
@Composable
private fun DetailImages(
    images: List<String>,
    uploading: Boolean,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
) {
    val side = 96.dp
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "상세 이미지",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                lineHeight = 18.sp, color = MuyeonColors.textHead,
            )
            Text(
                "${images.size}/$IMAGES_MAX",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                lineHeight = 15.sp, color = MuyeonColors.chevron,
            )
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (images.size < IMAGES_MAX) {
                Column(
                    Modifier.size(side).clip(RoundedCornerShape(10.dp)).background(Color(0xFFF7F7F7))
                        .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
                        .clickable(enabled = !uploading, onClick = onAdd),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                ) {
                    if (uploading) {
                        CircularProgressIndicator(color = MuyeonColors.primary, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Filled.AddPhotoAlternate, null, tint = MuyeonColors.textSub, modifier = Modifier.size(20.dp))
                        Text(
                            "${images.size}/$IMAGES_MAX",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp,
                            lineHeight = 14.sp, color = MuyeonColors.textSub,
                        )
                    }
                }
            }
            images.forEachIndexed { idx, path ->
                Box {
                    AsyncImage(
                        QuoteUi.imageUrl(path), null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(side).clip(RoundedCornerShape(10.dp)),
                    )
                    Icon(
                        Icons.Filled.Close, "삭제", tint = Color.White,
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(16.dp)
                            .clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))
                            .clickable { onRemove(idx) },
                    )
                }
            }
        }
        Text(
            "공고 상세에서 좌우로 넘겨보는 이미지예요.",
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
            lineHeight = 15.sp, color = MuyeonColors.chevron,
        )
    }
}

@Composable
private fun DeadlineRow(deadline: String?, onPick: () -> Unit, onClear: () -> Unit) {
    val text = displayDeadline(deadline)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "지원 마감일",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            lineHeight = 18.sp, color = MuyeonColors.textHead,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
                    .clickable(onClick = onPick)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    lineHeight = 17.sp,
                    color = if (text == "미정") MuyeonColors.chevron else MuyeonColors.textHead,
                    modifier = Modifier.weight(1f),
                )
                Icon(Icons.Filled.CalendarMonth, null, tint = MuyeonColors.chevron, modifier = Modifier.size(13.dp))
            }
            if (!deadline.isNullOrEmpty()) {
                Text(
                    "미정",
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    lineHeight = 16.sp, color = MuyeonColors.textSub,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFFF4F4F4))
                        .clickable(onClick = onClear)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                )
            }
        }
    }
}

/**
 * 작성 중 이탈 확인 — iOS `PostingClosePrompt` / confirmationDialog 대응.
 *  버튼이 3개라 AlertDialog 의 confirm/dismiss 두 슬롯으로는 못 담아 목록형으로 만든다.
 */
@Composable
private fun JobExitPrompt(
    saveLabel: String,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onCancel: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onCancel) {
        Column(
            Modifier.clip(RoundedCornerShape(16.dp)).background(MuyeonColors.surface).padding(vertical = 20.dp),
        ) {
            Text(
                "작성 중인 내용이 사라져요.",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                lineHeight = 19.sp, color = MuyeonColors.textHead,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp),
            )
            ExitAction(saveLabel, MuyeonColors.primary, onSave)
            ExitAction("저장 안 하고 나가기", MuyeonColors.danger, onDiscard)
            ExitAction("계속 작성", MuyeonColors.textSub, onCancel)
        }
    }
}

@Composable
private fun ExitAction(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
        lineHeight = 18.sp, color = color, textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    )
}
