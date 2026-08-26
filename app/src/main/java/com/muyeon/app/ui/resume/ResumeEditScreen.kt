package com.muyeon.app.ui.resume

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
 * 이력서 작성/수정 — iOS `ResumeEditView.swift` 1:1.
 *  기본 정보 + 전공/활동지역/레슨 가능 시간 + 학력·경력·공연·자격증·수상(반복) +
 *  희망 근무 조건 + 자기소개. 무용수 모드는 희망조건/경력/레슨시간을 빼고 신체정보·포트폴리오를 넣는다.
 *
 * ⚠️ 저장 시 **빈 값도 항상 전송**한다 — 서버가 data 를 부분 병합(spread)하므로
 *   키를 생략하면 사용자가 지운 항목이 서버에 그대로 남는다(iOS 주석과 동일 규약).
 */
@Composable
fun ResumeEditScreen(
    api: ResumeApi,
    resumeId: Int?,            // null = 신규
    mode: ResumeMode,
    onClose: () -> Unit,
    onSaved: () -> Unit,
) {
    var title by remember { mutableStateOf(mode.defaultResumeTitle) }
    var basic by remember { mutableStateOf(ResumeBasic()) }
    var oneLiner by remember { mutableStateOf("") }
    var intro by remember { mutableStateOf("") }
    var image by remember { mutableStateOf<String?>(null) }
    var images by remember { mutableStateOf(listOf<String>()) }
    var genres by remember { mutableStateOf(listOf<String>()) }
    var activeRegions by remember { mutableStateOf(listOf<String>()) }   // 최대 3
    var activeRegionCode by remember { mutableStateOf<String?>(null) }
    var days by remember { mutableStateOf(listOf<String>()) }
    var slots by remember { mutableStateOf(listOf<String>()) }
    var careerBucket by remember { mutableStateOf("") }
    var educations by remember { mutableStateOf(listOf<EduItem>()) }
    var careers by remember { mutableStateOf(listOf<CareerItem>()) }
    var performances by remember { mutableStateOf(listOf<PerfItem>()) }
    var certificates by remember { mutableStateOf("") }
    var awards by remember { mutableStateOf("") }
    var desired by remember { mutableStateOf(DesiredCond()) }
    var desiredRegionCode by remember { mutableStateOf<String?>(null) }
    // 무용수 전용
    var gender by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var companyCareer by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }

    var loadedData by remember { mutableStateOf(ResumeData()) }   // 미지 키 보존용 원본
    var loading by remember { mutableStateOf(resumeId != null) }
    var saving by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val portfolioMax = if (mode.isDancer) 20 else 10

    LaunchedEffect(resumeId) {
        val id = resumeId ?: return@LaunchedEffect
        api.getOne(id).onSuccess { dto ->
            title = dto.title
            val d = dto.data
            loadedData = d
            basic = d.basic ?: ResumeBasic()
            oneLiner = d.oneLiner.orEmpty(); intro = d.intro.orEmpty()
            image = d.image; images = d.images ?: emptyList()
            gender = d.gender.orEmpty(); height = d.height.orEmpty()
            companyCareer = d.companyCareer.orEmpty(); videoUrl = d.videoUrl.orEmpty()
            genres = d.genres ?: emptyList()
            // 다중 지역: activeRegions[] 우선, 없으면 activeRegion 을 콤마 분해(최대 3)
            activeRegions = d.activeRegions?.takeIf { it.isNotEmpty() }
                ?: d.activeRegion?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.take(3)
                ?: emptyList()
            activeRegionCode = d.activeRegionCode
            days = d.availableDays ?: emptyList()
            slots = d.availableTimeSlots ?: emptyList()
            careerBucket = d.career.orEmpty()
            educations = d.seededEducations()
            careers = d.careers ?: emptyList()
            performances = d.performances ?: emptyList()
            certificates = d.certificates.orEmpty(); awards = d.awards.orEmpty()
            desired = d.desired ?: DesiredCond()
            if (desired.region == null) desired = desired.copy(region = d.desiredRegion)
            desiredRegionCode = d.desiredRegionCode
        }
        loading = false
    }

    suspend fun upload(uri: android.net.Uri): String? {
        val bytes = withContext(Dispatchers.IO) {
            runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        } ?: return null
        return api.uploadImage(bytes).getOrNull()
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploading = true
        scope.launch {
            upload(uri)?.let { image = it; basic = basic.copy(photo = it) } ?: run { errorMessage = "사진 업로드에 실패했어요." }
            uploading = false
        }
    }
    val portfolioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        uploading = true
        scope.launch {
            val room = (portfolioMax - images.size).coerceAtLeast(0)
            uris.take(room).forEach { u -> upload(u)?.let { images = images + it } }
            uploading = false
        }
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = mode.navTitle, onBack = onClose)

        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
        } else {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Spacer(Modifier.height(0.dp))

                // 대표 사진 + 기본 정보
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ResumeSection("기본 정보")
                    Box(
                        Modifier.size(96.dp).clip(CircleShape).background(Color(0xFFF2F2F7))
                            .clickable(enabled = !uploading) {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (image != null) {
                            AsyncImage(
                                QuoteUi.imageUrl(image), null, contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(Icons.Filled.PhotoCamera, "사진 등록", tint = MuyeonColors.secondary, modifier = Modifier.size(26.dp))
                        }
                    }
                    LabeledField("이름", basic.name.orEmpty()) { basic = basic.copy(name = it) }
                    LabeledField("생년월일", basic.birth.orEmpty(), "예: 1995.03.21") { basic = basic.copy(birth = it) }
                    LabeledField("연락처", basic.phone.orEmpty(), keyboard = KeyboardType.Phone) { basic = basic.copy(phone = it) }
                    LabeledField("이메일", basic.email.orEmpty(), keyboard = KeyboardType.Email) { basic = basic.copy(email = it) }
                    LabeledField("이력서 제목", title) { title = it }
                    LabeledField("한 줄 소개", oneLiner, "예: 10년차 발레 전문 강사입니다") { oneLiner = it }
                }

                // 전공/장르
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ResumeSection("전공·장르")
                    MultiChips(ResumeOptions.genres.map { it to it }, genres.toSet()) { v ->
                        genres = if (genres.contains(v)) genres - v else genres + v
                    }
                }

                // 활동 지역(최대 3)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ResumeSection("활동 지역", "최대 3개")
                    LabeledField("지역", activeRegions.joinToString(", "), "예: 서울 강남구, 서울 서초구") { text ->
                        activeRegions = text.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(3)
                    }
                }

                if (!mode.isDancer) {
                    // 레슨 가능 시간
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ResumeSection("레슨 가능 시간")
                        MultiChips(ResumeOptions.weekDays.map { it to it }, days.toSet()) { v ->
                            days = if (days.contains(v)) days - v else days + v
                        }
                        MultiChips(ResumeOptions.timeSlots, slots.toSet()) { v ->
                            slots = if (slots.contains(v)) slots - v else slots + v
                        }
                    }
                } else {
                    // 무용수 — 신체 정보 + 무용단 경력 + 영상
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ResumeSection("신체 정보")
                        MultiChips(listOf("여성" to "여성", "남성" to "남성"), setOfNotNull(gender.ifEmpty { null })) {
                            gender = if (gender == it) "" else it
                        }
                        LabeledField("키 (cm)", height, "예: 168", keyboard = KeyboardType.Number) { height = it }
                        LabeledField("무용단 경력", companyCareer, "예: 국립발레단 2020~2023") { companyCareer = it }
                        LabeledField("영상 링크", videoUrl, "YouTube/Vimeo 링크") { videoUrl = it }
                    }
                }

                // 포트폴리오
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ResumeSection("포트폴리오", "${images.size}/$portfolioMax")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier.size(78.dp).clip(RoundedCornerShape(10.dp))
                                .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
                                .clickable(enabled = !uploading && images.size < portfolioMax) {
                                    portfolioPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Add, "추가", tint = MuyeonColors.secondary, modifier = Modifier.size(22.dp))
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
                                        .clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))
                                        .clickable { images = images - url },
                                )
                            }
                        }
                    }
                }

                // 학력(반복)
                RepeatSection(
                    title = "학력",
                    items = educations,
                    onAdd = { educations = educations + EduItem() },
                    onRemove = { i -> educations = educations.filterIndexed { idx, _ -> idx != i } },
                ) { i, item ->
                    LabeledField("학교·과정", item.school) { v -> educations = educations.replaced(i) { it.copy(school = v) } }
                    LabeledField("전공", item.major) { v -> educations = educations.replaced(i) { it.copy(major = v) } }
                    PeriodField(item.period) { v -> educations = educations.replaced(i) { it.copy(period = v) } }
                }

                if (!mode.isDancer) {
                    // 경력(반복) — 기간 병합 총계로 버킷 자동 산출
                    RepeatSection(
                        title = "경력",
                        subtitle = ResumePeriod.mergedTotalMonths(careers.map { it.period })
                            ?.let { "총 ${ResumePeriod.label(it)}" },
                        items = careers,
                        onAdd = { careers = careers + CareerItem() },
                        onRemove = { i -> careers = careers.filterIndexed { idx, _ -> idx != i } },
                    ) { i, item ->
                        LabeledField("기관", item.academy) { v -> careers = careers.replaced(i) { it.copy(academy = v) } }
                        LabeledField("직책", item.position) { v -> careers = careers.replaced(i) { it.copy(position = v) } }
                        LabeledField("담당 수업", item.classes) { v -> careers = careers.replaced(i) { it.copy(classes = v) } }
                        PeriodField(item.period) { v -> careers = careers.replaced(i) { it.copy(period = v) } }
                    }
                }

                // 공연 이력(반복)
                RepeatSection(
                    title = "공연 이력",
                    items = performances,
                    onAdd = { performances = performances + PerfItem() },
                    onRemove = { i -> performances = performances.filterIndexed { idx, _ -> idx != i } },
                ) { i, item ->
                    LabeledField("연도", item.year, "예: 2024", KeyboardType.Number) { v -> performances = performances.replaced(i) { it.copy(year = v) } }
                    LabeledField("공연명", item.title) { v -> performances = performances.replaced(i) { it.copy(title = v) } }
                    LabeledField("역할", item.role, "예: 주역") { v -> performances = performances.replaced(i) { it.copy(role = v) } }
                }

                if (!mode.isDancer) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ResumeSection("자격증", "줄당 1건")
                        MultilineField(certificates) { certificates = it }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ResumeSection("수상 이력", "줄당 1건")
                    MultilineField(awards) { awards = it }
                }

                if (!mode.isDancer) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ResumeSection("희망 근무 조건")
                        LabeledField("희망 직종", desired.job.orEmpty(), "예: 발레 강사") { desired = desired.copy(job = it) }
                        LabeledField("희망 지역", desired.region.orEmpty(), "예: 서울 전체") { desired = desired.copy(region = it) }
                        Text("희망 급여", fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MuyeonColors.textSub)
                        MultiChips(ResumeOptions.salaryRanges, setOfNotNull(desired.salary)) {
                            desired = desired.copy(salary = if (desired.salary == it) null else it)
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ResumeSection("자기소개")
                    MultilineField(intro, minHeight = 140)
                    { intro = it }
                }

                Spacer(Modifier.height(8.dp))
            }

            Text(
                if (saving) "저장 중…" else "저장",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (saving) Color.Gray.copy(alpha = 0.4f) else MuyeonColors.primary)
                    .clickable(enabled = !saving) {
                        saving = true
                        scope.launch {
                            // ⚠️ 빈 값도 항상 전송 — 서버 spread 병합이라 키를 빼면 '지운' 항목이 안 지워진다.
                            val bucket = ResumePeriod.mergedTotalMonths(careers.map { it.period })
                                ?.let { ResumePeriod.careerBucket(it) } ?: careerBucket
                            val d = loadedData.copy(
                                basic = basic.copy(photo = image),
                                oneLiner = oneLiner, intro = intro,
                                image = image ?: "", images = images, genres = genres,
                                gender = if (mode.isDancer) gender else loadedData.gender,
                                height = if (mode.isDancer) height else loadedData.height,
                                companyCareer = if (mode.isDancer) companyCareer else loadedData.companyCareer,
                                videoUrl = if (mode.isDancer) videoUrl else loadedData.videoUrl,
                                activeRegions = activeRegions,
                                activeRegion = activeRegions.joinToString(", "),
                                activeRegionCode = activeRegionCode ?: "",
                                availableDays = days, availableTimeSlots = slots,
                                career = bucket,
                                educations = educations.filter { it.school.isNotEmpty() },
                                careers = careers.filter { it.academy.isNotEmpty() || it.position.isNotEmpty() },
                                performances = performances.filter { it.title.isNotEmpty() },
                                certificates = certificates, awards = awards,
                                desired = desired,
                                desiredRegion = desired.region ?: "",
                                desiredRegionCode = desiredRegionCode ?: "",
                            )
                            api.save(resumeId, title, d)
                                .onSuccess { newId ->
                                    if (mode.isDancer) {
                                        // 유형은 인증 절차에서만 부여한다 — 이력서 저장이 유형 인증을 우회하면 안 된다.
                                        //  (iOS `ResumeEditVM.save` 와 동일. 신규는 기본이력서 지정만 한다)
                                        if (resumeId == null) api.setDefault(newId)
                                    }
                                    onSaved()
                                }
                                .onFailure { errorMessage = it.message }
                            saving = false
                        }
                    }
                    .padding(vertical = 16.dp),
            )
        }
    }

    errorMessage?.let { msg ->
        QuoteDialog("오류", msg, "확인", onConfirm = { errorMessage = null }, onDismiss = { errorMessage = null })
    }
}

private fun <T> List<T>.replaced(index: Int, transform: (T) -> T): List<T> =
    mapIndexed { i, v -> if (i == index) transform(v) else v }

@Composable
private fun ResumeSection(title: String, sub: String? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = MuyeonColors.textHead,
        )
        sub?.let {
            Text(it, fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 14.sp, color = MuyeonColors.textSub)
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    placeholder: String = "",
    keyboard: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MuyeonColors.textSub)
        OutlinedTextField(
            value = value, onValueChange = onChange, singleLine = true,
            placeholder = { if (placeholder.isNotEmpty()) Text(placeholder, fontFamily = customFontFamily, fontSize = 14.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 기간 — iOS 는 휠 피커, Android 는 정형 힌트를 준 텍스트 입력 + 파싱 결과 표시. */
@Composable
private fun PeriodField(value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("기간", fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = MuyeonColors.textSub)
        OutlinedTextField(
            value = value, onValueChange = onChange, singleLine = true,
            placeholder = { Text("2022.03 ~ 현재", fontFamily = customFontFamily, fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
        )
        val label = ResumePeriod.durationLabel(value)
        if (value.isNotBlank()) {
            Text(
                label ?: "형식을 확인해 주세요 (예: 2022.03 ~ 현재)",
                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 14.sp,
                color = if (label != null) MuyeonColors.textSub else MuyeonColors.orange,
            )
        }
    }
}

@Composable
private fun MultilineField(value: String, minHeight: Int = 96, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onChange,
        modifier = Modifier.fillMaxWidth().heightIn(min = minHeight.dp),
    )
}

@Composable
private fun MultiChips(options: List<Pair<String, String>>, selected: Set<String>, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { (v, label) ->
                    val on = selected.contains(v)
                    Text(
                        label,
                        fontFamily = customFontFamily,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp, lineHeight = 15.sp, maxLines = 1,
                        color = if (on) Color.White else MuyeonColors.textSub, textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                            .clip(RoundedCornerShape(50))
                            .background(if (on) MuyeonColors.primary else Color(0xFFF2F2F7))
                            .clickable { onToggle(v) }
                            .padding(vertical = 8.dp, horizontal = 2.dp),
                    )
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun <T> RepeatSection(
    title: String,
    subtitle: String? = null,
    items: List<T>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    itemContent: @Composable ColumnScope.(Int, T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ResumeSection(title, subtitle)
            Spacer(Modifier.weight(1f))
            Text(
                "+ 추가",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.primary,
                modifier = Modifier.clickable(onClick = onAdd).padding(4.dp),
            )
        }
        items.forEachIndexed { i, item ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MuyeonColors.border, RoundedCornerShape(12.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${i + 1}",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp,
                        color = MuyeonColors.textSub, modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Filled.Close, "삭제", tint = MuyeonColors.secondary,
                        modifier = Modifier.size(16.dp).clickable { onRemove(i) },
                    )
                }
                itemContent(i, item)
            }
        }
    }
}
