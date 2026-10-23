package com.muyeon.app.ui.resume

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.ui.quote.QuoteUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 이력서 편집 폼 상태 — NavHost 항목(ViewModel 소유자) 수명.
 *  공개 범위·미리보기 화면에 다녀와도 입력이 유지된다. [loaded] = 서버 값으로 한 번 채웠음.
 */
class ResumeEditForm(defaultTitle: String, loading: Boolean) : androidx.lifecycle.ViewModel() {
    var loaded = false
    var title by mutableStateOf(defaultTitle)
    var basic by mutableStateOf(ResumeBasic())
    var oneLiner by mutableStateOf("")
    var intro by mutableStateOf("")
    var image by mutableStateOf<String?>(null)
    var images by mutableStateOf(listOf<String>())
    var genres by mutableStateOf(listOf<String>())
    // 가르칠 수 있는 수업 대상(코드). 강사 이력서에만 쓴다.
    var targets by mutableStateOf(listOf<String>())
    // 이름+코드 쌍으로 들고 있는다 — iOS 와 동일. 이름만 두면 첫 지역을 지웠을 때
    //  대표 코드(activeRegionCode)가 사라진 지역을 계속 가리킨다.
    var activeRegions by mutableStateOf(listOf<Pair<String, String?>>())   // 최대 3
    var days by mutableStateOf(listOf<String>())
    var slots by mutableStateOf(listOf<String>())
    var careerBucket by mutableStateOf("")
    var educations by mutableStateOf(listOf<EduItem>())
    var careers by mutableStateOf(listOf<CareerItem>())
    var performances by mutableStateOf(listOf<PerfItem>())
    var certificates by mutableStateOf("")
    var awards by mutableStateOf("")
    var desired by mutableStateOf(DesiredCond())
    var desiredRegionCode by mutableStateOf<String?>(null)
    // 무용수 전용
    var gender by mutableStateOf("")
    var height by mutableStateOf("")
    var weight by mutableStateOf("")
    var companyCareer by mutableStateOf("")
    var videoUrl by mutableStateOf("")

    var loadedData by mutableStateOf(ResumeData())   // 미지 키 보존용 원본
    var loading by mutableStateOf(loading)

    // 긴급 대타 알림 — null = 아직 못 불러옴(서버 미배포·실패). 그땐 섹션을 그리지도, 저장하지도 않는다.
    var dispatch by mutableStateOf<SubDispatchPrefs?>(null)
}

/**
 * 이력서 작성/수정 — iOS `ResumeEditView.swift` 1:1.
 *  기본 정보 + 전공/활동지역/레슨 가능 시간 + 학력·경력·공연·자격증·수상(반복) +
 *  희망 근무 조건 + 자기소개. 무용수 모드는 희망조건/경력/레슨시간을 빼고 신체정보·포트폴리오를 넣는다.
 *  섹션 순서·라벨·플레이스홀더·컨트롤은 iOS 와 동일하게 맞춘다. iOS 화면에 없는 값(연락처·이메일·
 *  학력 전공·경력 담당 수업·강사 포트폴리오)은 화면에서 빼되, 폼/원본에 그대로 들고 있다가 저장 시 보존한다.
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
    // 구직 프로필 등록 진입 — 상단 제목/하단 설정 섹션이 바뀌고 저장 시 기본 이력서로 지정한다.
    isSeekProfile: Boolean = false,
    onVisibility: () -> Unit = {},
    /** 공개 범위 화면에서 돌아옴(RESUME 결과) — 부모가 올린다. 폼은 그대로 두고 설정 값만 다시 읽는다. */
    settingsReload: Int = 0,
    /** AI 도우미 초안(3단계) — 불러온 뒤 자기소개를 채운다. iOS ResumeEditView.aiDraftId 와 같다. */
    aiDraftId: String? = null,
) {
    // ★ 폼 상태는 NavHost 항목 수명의 ViewModel 에 둔다(ResumeEditForm).
    //   예전엔 remember 라 '공개 범위'(visibility) 화면에 다녀오면 이 목적지가 composition 에서 빠지며
    //   입력 중이던 내용이 통째로 사라지고, 돌아와서 LaunchedEffect(resumeId) 가 서버 값으로 다시 덮었다.
    val form: ResumeEditForm = androidx.lifecycle.viewmodel.compose.viewModel(
        key = "resumeEdit:${resumeId ?: 0}:$isSeekProfile",
    ) { ResumeEditForm(mode.defaultResumeTitle, loading = resumeId != null) }
    var title by form::title
    var basic by form::basic
    var oneLiner by form::oneLiner
    var intro by form::intro
    var image by form::image
    var images by form::images
    var genres by form::genres
    // 가르칠 수 있는 수업 대상(코드). 강사 이력서에만 쓴다.
    var targets by form::targets
    // 이름+코드 쌍으로 들고 있는다 — iOS 와 동일. 이름만 두면 첫 지역을 지웠을 때
    //  대표 코드(activeRegionCode)가 사라진 지역을 계속 가리킨다.
    var activeRegions by form::activeRegions   // 최대 3
    var days by form::days
    var slots by form::slots
    var careerBucket by form::careerBucket
    var educations by form::educations
    var careers by form::careers
    var performances by form::performances
    var certificates by form::certificates
    var awards by form::awards
    var desired by form::desired
    var desiredRegionCode by form::desiredRegionCode
    // 무용수 전용
    var gender by form::gender
    var height by form::height
    var weight by form::weight
    var companyCareer by form::companyCareer
    var videoUrl by form::videoUrl

    var loadedData by form::loadedData   // 미지 키 보존용 원본
    var loading by form::loading
    var saving by remember { mutableStateOf(false) }
    var uploadingPhoto by remember { mutableStateOf(false) }
    var uploadingPortfolio by remember { mutableStateOf(false) }
    var previewOpen by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var viewAlert by remember { mutableStateOf(true) }   // 구직 프로필 — 내 프로필 열람 알림
    var awaitingNotificationSettings by remember { mutableStateOf(false) }
    // 설정 앱에서 알림을 허용하고 돌아왔을 때 켜 줄 토글(true = 긴급 대타, false = 열람 알림)
    var pendingDispatchAlert by remember { mutableStateOf(false) }
    var dispatch by form::dispatch
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val portfolioMax = if (mode.isDancer) 20 else 10

    // 구직 프로필 설정(열람 알림) — 최초 + 공개 범위 화면에서 돌아올 때(RESUME 결과 → settingsReload).
    //  폼 입력은 건드리지 않고 이 서버 파생 값만 다시 읽는다.
    // 이미 구직 목록에 등록된 프로필이면 제목을 '수정'으로(값이 채워진 채 '등록'으로 보여 헷갈리던 문제).
    var seekPublished by remember { mutableStateOf(false) }
    LaunchedEffect(isSeekProfile, settingsReload) {
        if (isSeekProfile) {
            viewAlert = api.getProfileViewAlert() && context.notificationsEnabled()
            seekPublished = api.getSeekPublished()
        }
    }

    // 긴급 대타 알림 설정 — 한 번만 읽는다(공개 범위 화면에 다녀와도 편집 중인 값 유지).
    LaunchedEffect(isSeekProfile) {
        if (!isSeekProfile || dispatch != null) return@LaunchedEffect
        val prefs = api.getSubDispatchPrefs() ?: return@LaunchedEffect
        dispatch = prefs.copy(enabled = prefs.enabled && context.notificationsEnabled())
    }

    // AI 초안: 서버 값을 채운 뒤 한 번 — 자기소개(+경력 요약), 한줄 소개는 비어 있을 때 강점 2개.
    //  구조화된 경력 행은 회원이 직접(지어낸 경력이 행으로 들어가지 않게).
    var aiApplied by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(aiDraftId, form.loaded) {
        if (aiDraftId == null || aiApplied || (resumeId != null && !form.loaded)) return@LaunchedEffect
        aiApplied = true
        val p = com.muyeon.app.ui.assistant.AssistantDraft.payload(context, aiDraftId) ?: return@LaunchedEffect
        val ai = p.optString("intro").takeIf { it.isNotEmpty() && it != "null" } ?: return@LaunchedEffect
        val career = p.optString("careerSummary").takeIf { it.isNotEmpty() && it != "null" }
        intro = listOfNotNull(ai, career).joinToString("\n\n")
        val strengths = p.optJSONArray("strengths")
        if (oneLiner.isEmpty() && strengths != null && strengths.length() > 0) {
            oneLiner = (0 until minOf(2, strengths.length())).joinToString(" · ") { strengths.optString(it) }
        }
    }

    // 서버 값으로 폼을 채우는 건 한 번뿐 — 재진입마다 다시 채우면 입력 중이던 내용이 날아간다.
    LaunchedEffect(resumeId) {
        if (form.loaded) return@LaunchedEffect
        val id = resumeId
        if (id != null) api.getOne(id).onSuccess { dto ->
            form.loaded = true
            title = dto.title
            val d = dto.data
            loadedData = d
            basic = d.basic ?: ResumeBasic()
            oneLiner = d.oneLiner.orEmpty(); intro = d.intro.orEmpty()
            image = d.image; images = d.images ?: emptyList()
            gender = d.gender.orEmpty(); height = d.height.orEmpty(); weight = d.weight.orEmpty()
            companyCareer = d.companyCareer.orEmpty(); videoUrl = d.videoUrl.orEmpty()
            genres = d.genres ?: emptyList()
            targets = d.targets ?: emptyList()
            // 다중 지역: activeRegions[] 우선, 없으면 activeRegion 을 콤마 분해(최대 3).
            //  서버가 주는 코드는 단일이라 iOS 처럼 첫 지역에만 붙인다.
            val names = d.activeRegions?.takeIf { it.isNotEmpty() }
                ?: d.activeRegion?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.take(3)
                ?: emptyList()
            activeRegions = names.mapIndexed { i, n ->
                n to if (i == 0) d.activeRegionCode?.ifEmpty { null } else null
            }
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
        if (id == null) form.loaded = true
        api.getEditorIdentity()?.takeIf { it.phoneVerified }?.let { identity ->
            basic = basic.copy(
                name = identity.name.ifEmpty { basic.name },
                birth = identity.birth.ifEmpty { basic.birth },
            )
        }
        loading = false
    }

    DisposableEffect(lifecycleOwner, awaitingNotificationSettings) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && awaitingNotificationSettings) {
                awaitingNotificationSettings = false
                if (context.notificationsEnabled()) {
                    if (pendingDispatchAlert) dispatch = dispatch?.copy(enabled = true) else viewAlert = true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    suspend fun upload(uri: android.net.Uri): String? {
        val bytes = withContext(Dispatchers.IO) {
            runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        } ?: return null
        return api.uploadImage(bytes).getOrNull()
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploadingPhoto = true
        scope.launch {
            upload(uri)?.let { image = it; basic = basic.copy(photo = it) } ?: run { errorMessage = "사진 업로드에 실패했어요." }
            uploadingPhoto = false
        }
    }
    val portfolioPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        uploadingPortfolio = true
        scope.launch {
            val room = (portfolioMax - images.size).coerceAtLeast(0)
            uris.take(room).forEach { u -> upload(u)?.let { images = images + it } }
            uploadingPortfolio = false
        }
    }

    fun save() {
        if (saving || uploadingPhoto) return
        saving = true
        scope.launch {
            // ⚠️ 빈 값도 항상 전송 — 서버 spread 병합이라 키를 빼면 '지운' 항목이 안 지워진다.
            // 전 기간 파싱 성공 시에만 버킷 자동 산출 — 비정형 섞이면 수동 값 유지(과소 저장 방지)
            ResumePeriod.mergedTotalMonths(careers.map { it.period })?.let { careerBucket = ResumePeriod.careerBucket(it) }
            // basic 은 화면에 없는 phone/email 까지 그대로 들고 있다(원본 보존).
            val d = loadedData.copy(
                basic = basic.copy(photo = image),
                oneLiner = oneLiner, intro = intro,
                image = image ?: "", images = images, genres = genres,
                targets = if (mode.isDancer) loadedData.targets else targets,
                gender = if (mode.isDancer) gender else loadedData.gender,
                height = if (mode.isDancer) height else loadedData.height,
                weight = if (mode.isDancer) weight else loadedData.weight,
                companyCareer = if (mode.isDancer) companyCareer else loadedData.companyCareer,
                videoUrl = if (mode.isDancer) videoUrl else loadedData.videoUrl,
                activeRegions = activeRegions.map { it.first },
                activeRegion = activeRegions.joinToString(", ") { it.first },
                // 서버 컬럼은 단일 — iOS 와 같이 첫 지역 코드를 대표로 보낸다.
                activeRegionCode = activeRegions.firstOrNull()?.second ?: "",
                availableDays = days, availableTimeSlots = slots,
                career = careerBucket,
                // EduItem.major / CareerItem.classes 는 화면에 없지만 항목 안에 그대로 남아 함께 저장된다.
                educations = educations.filter { it.school.isNotEmpty() },
                careers = careers.filter { it.academy.isNotEmpty() || it.position.isNotEmpty() },
                performances = performances.filter { it.title.isNotEmpty() },
                certificates = certificates, awards = awards,
                desired = desired,
                desiredRegion = desired.region ?: "",
                desiredRegionCode = desiredRegionCode ?: "",
            )
            api.save(resumeId, title, d, mode)
                .onSuccess { newId ->
                    if (isSeekProfile) {
                        // 구직 프로필은 기본 이력서가 원본이다(iOS ResumeEditVM.save).
                        if (resumeId == null) api.setDefault(newId)
                        api.setProfileViewAlert(viewAlert)
                        dispatch?.let { api.setSubDispatchPrefs(it) }
                    }
                    if (mode.isDancer) {
                        // 유형은 인증 절차에서만 부여한다 — 이력서 저장이 유형 인증을 우회하면 안 된다.
                        //  (iOS `ResumeEditVM.save` 와 동일. 신규는 기본이력서 지정만 한다)
                        if (resumeId == null) api.setDefault(newId)
                    }
                    com.muyeon.app.ui.assistant.AssistantDraft.saved(context, aiDraftId, "RESUME", newId, intro) // AI 초안 채택 기록
                    onSaved()
                }
                .onFailure { errorMessage = it.message }
            saving = false
        }
    }

    val mergedCareerMonths = ResumePeriod.mergedTotalMonths(careers.map { it.period })

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(
            title = if (isSeekProfile) (if (seekPublished) "구직 프로필 수정" else "구직 프로필 등록") else mode.navTitle,
            onBack = onClose,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clickable { previewOpen = true }
                            .semantics { contentDescription = "이력서 미리보기" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Info, null, tint = MuyeonColors.primary, modifier = Modifier.size(20.dp))
                    }
                    Box(
                        Modifier.heightIn(min = 40.dp).clickable(enabled = !saving && !uploadingPhoto) { save() }
                            .padding(start = 4.dp, end = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                color = MuyeonColors.textSub, strokeWidth = 2.dp, modifier = Modifier.size(18.dp),
                            )
                        } else {
                            Text(
                                "저장",
                                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                                lineHeight = 19.sp,
                                color = if (uploadingPhoto) MuyeonColors.primary.copy(alpha = 0.4f) else MuyeonColors.primary,
                            )
                        }
                    }
                }
            },
        )

        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
        } else {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp).padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                // 이력서 이름 — 다중 이력서 구분용(목록·지원 첨부 표시명)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("이력서 이름")
                    PlainInput(title, "예) 유아 발레 이력서") { title = it }
                }

                // 기본 정보 — 좌측 사진(카메라 뱃지) + 우측 라벨/값 스택
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle("기본 정보")
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier.size(102.dp).clickable(enabled = !uploadingPhoto) {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        ) {
                            Box(
                                Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)).background(MuyeonColors.tileIdle),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (image != null) {
                                    AsyncImage(
                                        QuoteUi.imageUrl(image), null, contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                } else {
                                    Icon(Icons.Filled.Person, null, tint = MuyeonColors.hint, modifier = Modifier.size(40.dp))
                                }
                            }
                            // 카메라 뱃지 — iOS offset(6,6) 처럼 사진 우하단 밖으로 살짝 걸친다
                            Box(
                                Modifier.align(Alignment.BottomEnd).size(28.dp).clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (uploadingPhoto) {
                                    CircularProgressIndicator(
                                        color = Color.White, strokeWidth = 1.5.dp, modifier = Modifier.size(14.dp),
                                    )
                                } else {
                                    Icon(Icons.Filled.PhotoCamera, "사진 등록", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // 이름은 계정의 본인확인 실명이 정본이다(서버 ResumesService.applyOwnerName 이 저장 시 덮어쓴다).
                            //  이력서는 학원·공연팀이 지원자를 볼 때 여는 문서라, 여기만 활동명을 허용하면
                            //  목록의 지원자명(실명)과 문서 안 이름이 갈린다. (iOS ResumeEditView 와 동일)
                            ReadOnlyRow("이름", basic.name.orEmpty(), hint = "본인인증으로 등록된 이름이에요.")
                            ReadOnlyRow("생년월일", basic.birth.orEmpty(), hint = "본인인증으로 등록된 생년월일이에요.")
                        }
                    }
                    LabeledInput("한줄 소개", oneLiner, "예) 성인발레 전문, 맞춤형 지도") { oneLiner = it }
                    if (mode.isDancer) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "성별",
                                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                                lineHeight = 14.sp, color = MuyeonColors.textSub,
                            )
                            SingleChips(listOf("여성" to "여성", "남성" to "남성"), gender) { gender = it }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            LabeledInput("키 (cm)", height, "168", KeyboardType.Number, Modifier.weight(1f)) { height = it }
                            LabeledInput("몸무게 (kg)", weight, "50", KeyboardType.Number, Modifier.weight(1f)) { weight = it }
                        }
                    }
                }

                // 포트폴리오 — 다중 사진 3열 그리드(무용수 최대 20). 강사 포트폴리오는 iOS 처럼 화면에서 뺐다(images 값은 보존).
                if (mode.isDancer) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            SectionTitle("포트폴리오")
                            Text(
                                "${images.size}/$portfolioMax",
                                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                                lineHeight = 14.sp, color = MuyeonColors.hint,
                            )
                        }
                        val tiles: List<String?> = images + if (images.size < portfolioMax) listOf(null) else emptyList()
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            tiles.chunked(3).forEachIndexed { rowIdx, row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    row.forEachIndexed { colIdx, url ->
                                        val idx = rowIdx * 3 + colIdx
                                        Box(Modifier.weight(1f).height(104.dp)) {
                                            if (url != null) {
                                                AsyncImage(
                                                    QuoteUi.imageUrl(url), null, contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                                                        .background(MuyeonColors.tileIdle),
                                                )
                                                Box(
                                                    Modifier.align(Alignment.TopEnd).padding(4.dp).size(18.dp)
                                                        .clip(CircleShape).background(Color.Black.copy(alpha = 0.5f))
                                                        .clickable {
                                                            images = images.filterIndexed { i, _ -> i != idx }
                                                        },
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    Icon(Icons.Filled.Close, "삭제", tint = Color.White, modifier = Modifier.size(12.dp))
                                                }
                                            } else {
                                                Box(
                                                    Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                                                        .background(MuyeonColors.tileIdle)
                                                        .clickable(enabled = !uploadingPortfolio) {
                                                            portfolioPicker.launch(
                                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                                            )
                                                        },
                                                    contentAlignment = Alignment.Center,
                                                ) {
                                                    if (uploadingPortfolio) {
                                                        CircularProgressIndicator(
                                                            color = MuyeonColors.textSub, strokeWidth = 2.dp,
                                                            modifier = Modifier.size(20.dp),
                                                        )
                                                    } else {
                                                        Icon(Icons.Filled.Add, "추가", tint = MuyeonColors.hint, modifier = Modifier.size(22.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }

                // 전공 / 수업 대상 / 경력 구간 / 활동 지역 (무용수는 수업 대상·경력 구간 제외)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle(if (mode.isDancer) "전공 · 장르" else "전문 분야")
                    MultiChips(ResumeOptions.genres.map { it to it }, genres) { v ->
                        genres = if (genres.contains(v)) genres - v else genres + v
                    }
                    if (!mode.isDancer) {
                        // 대타는 "유아를 볼 수 있는가"가 지원 여부를 가른다. 웹 이력서엔 진작 있던 항목인데
                        //  앱에만 없어, 앱으로 쓴 이력서는 '내 조건에 맞는 공고만'이 걸러 주질 못했다.
                        SectionTitle("수업 대상", Modifier.padding(top = 8.dp))
                        MultiChips(ResumeOptions.classTargets, targets) { v ->
                            targets = if (targets.contains(v)) targets - v else targets + v
                        }
                        SectionTitle("경력 구간", Modifier.padding(top = 8.dp))
                        if (mergedCareerMonths != null) {
                            // 자동 모드 — 기간 입력이 완전하면 수동 선택 무의미(저장 시 자동값 적용)라 안내만
                            Row(
                                Modifier.padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.AutoFixHigh, null, tint = MuyeonColors.primary, modifier = Modifier.size(12.dp))
                                Text(
                                    "${ResumeOptions.careerLabel(ResumePeriod.careerBucket(mergedCareerMonths))} · 경력 기간으로 자동 계산돼요",
                                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                                    lineHeight = 16.sp, color = MuyeonColors.textSub,
                                )
                            }
                        } else {
                            SingleChips(ResumeOptions.careerLevels, careerBucket) { careerBucket = it }
                        }
                    }
                    SectionTitle("활동 지역", Modifier.padding(top = 8.dp))
                    //  ★ 자유 텍스트가 아니라 /regions 코드에서 고른다 — activeRegion 이
                    //    regions.name("서울 강남구") 포맷이어야 지역 강사수(prefix 매칭)·목록 필터·
                    //    견적 지역 매칭이 싱크된다. 최대 3개(시안).
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        activeRegions.forEachIndexed { idx, (name, _) ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                    .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    name,
                                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                                    lineHeight = 17.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
                                )
                                Icon(
                                    Icons.Filled.Cancel, "삭제", tint = MuyeonColors.hint,
                                    modifier = Modifier.size(16.dp).clickable {
                                        activeRegions = activeRegions.filterIndexed { i, _ -> i != idx }
                                    },
                                )
                            }
                        }
                        if (activeRegions.size < 3) {
                            com.muyeon.app.ui.quote.RegionAddRow(token = api.token, dashed = true) { name, code ->
                                if (activeRegions.none { it.first == name }) {
                                    activeRegions = (activeRegions + (name to code)).take(3)
                                }
                            }
                        }
                    }
                }

                if (!mode.isDancer) {
                    // 레슨 가능 시간
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionTitle("레슨 가능 시간")
                        MultiChips(ResumeOptions.weekDays.map { it to it }, days) { v ->
                            days = if (days.contains(v)) days - v else days + v
                        }
                        MultiChips(ResumeOptions.timeSlots, slots) { v ->
                            // 저장 순서는 iOS 처럼 timeSlots 정의 순서
                            val next = if (slots.contains(v)) slots - v else slots + v
                            slots = ResumeOptions.timeSlots.map { it.first }.filter { it in next }
                        }
                    }
                }

                // 학력(반복) — iOS 와 같이 학교·전공 한 칸 + 기간. (레거시 major 값은 항목 안에 보존)
                RepeatSection(
                    title = "학력",
                    items = educations,
                    onAdd = { educations = educations + EduItem() },
                    onRemove = { i -> educations = educations.filterIndexed { idx, _ -> idx != i } },
                ) { i, item ->
                    FieldRow("학교·전공", item.school, "예) 한국예술종합학교 무용원 발레과") { v ->
                        educations = educations.replaced(i) { it.copy(school = v) }
                    }
                    PeriodPickerRow("기간", item.period) { v -> educations = educations.replaced(i) { it.copy(period = v) } }
                }

                if (!mode.isDancer) {
                    // 경력(반복) — 기간 병합 총계로 버킷 자동 산출. (레거시 classes 값은 항목 안에 보존)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        RepeatSection(
                            title = "경력",
                            items = careers,
                            onAdd = { careers = careers + CareerItem() },
                            onRemove = { i -> careers = careers.filterIndexed { idx, _ -> idx != i } },
                        ) { i, item ->
                            FieldRow("기관", item.academy, "예) ABC발레학원") { v -> careers = careers.replaced(i) { it.copy(academy = v) } }
                            FieldRow("직책", item.position, "예) 발레 강사") { v -> careers = careers.replaced(i) { it.copy(position = v) } }
                            PeriodPickerRow("기간", item.period) { v -> careers = careers.replaced(i) { it.copy(period = v) } }
                        }
                        if (mergedCareerMonths != null) CareerTotalRow(mergedCareerMonths)
                    }
                }

                // 공연 이력(반복)
                RepeatSection(
                    title = "공연 이력",
                    items = performances,
                    onAdd = { performances = performances + PerfItem() },
                    onRemove = { i -> performances = performances.filterIndexed { idx, _ -> idx != i } },
                ) { i, item ->
                    FieldRow("연도", item.year, "예) 2024") { v -> performances = performances.replaced(i) { it.copy(year = v) } }
                    FieldRow("공연명", item.title, "예) K-Ballet Gala | 백조의 호수") { v ->
                        performances = performances.replaced(i) { it.copy(title = v) }
                    }
                    FieldRow("역할", item.role, "예) 주역") { v -> performances = performances.replaced(i) { it.copy(role = v) } }
                }

                if (mode.isDancer) {
                    LinesSection("무용단 경력", companyCareer, "한 줄에 하나씩 입력\n예) 국립발레단 준단원 2019~2022") { companyCareer = it }
                }
                if (!mode.isDancer) {
                    LinesSection("자격증", certificates, "한 줄에 하나씩 입력\n예) ABT Ballet Teaching Certificate 2021.06") {
                        certificates = it
                    }
                }
                LinesSection("수상 이력", awards, "한 줄에 하나씩 입력\n예) 2018 서울국제무용콩쿠르 발레 시니어 1위") { awards = it }

                if (mode.isDancer) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionTitle("영상 포트폴리오")
                        PlainInput(videoUrl, "https://youtu.be/…", KeyboardType.Uri) { videoUrl = it }
                    }
                }

                if (!mode.isDancer) {
                    // 희망 근무 조건 — 시안 표 형식
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionTitle("희망 근무 조건")
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp)),
                        ) {
                            TableRow("희망 직종") {
                                BareField(
                                    desired.job.orEmpty(), "예) 발레 강사",
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                ) { desired = desired.copy(job = it) }
                            }
                            HorizontalDivider(color = MuyeonColors.border)
                            TableRow("희망 지역") {
                                var open by remember { mutableStateOf(false) }
                                DropdownValue(desired.region.orEmpty(), "선택") { open = true }
                                if (open) {
                                    com.muyeon.app.ui.quote.RegionPickerSheet(
                                        token = api.token, title = "희망 지역 선택", allowSidoAll = true,
                                        onPick = { name, code ->
                                            desired = desired.copy(region = name)
                                            desiredRegionCode = code
                                        },
                                        onDismiss = { open = false },
                                    )
                                }
                            }
                            HorizontalDivider(color = MuyeonColors.border)
                            TableRow("희망 급여") {
                                // 버킷 메뉴(웹 SALARY_RANGES 계약)
                                var open by remember { mutableStateOf(false) }
                                Box {
                                    DropdownValue(desired.salary?.let { ResumeOptions.salaryLabel(it) }.orEmpty(), "선택") { open = true }
                                    DropdownMenu(
                                        expanded = open, onDismissRequest = { open = false },
                                        containerColor = MuyeonColors.surface,
                                    ) {
                                        ResumeOptions.salaryRanges.forEach { (v, label) ->
                                            DropdownMenuItem(
                                                text = { Text(label, fontFamily = customFontFamily, fontSize = 14.sp) },
                                                onClick = { desired = desired.copy(salary = v); open = false },
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("선택 안 함", fontFamily = customFontFamily, fontSize = 14.sp) },
                                            onClick = { desired = desired.copy(salary = null); open = false },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle("자기소개")
                    BareField(
                        intro, "", singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                            .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        minHeight = 120,
                    ) { intro = it }
                }

                if (isSeekProfile) {
                    // 켤 때 시스템 알림 권한 확인 — 꺼져 있으면 켜도 푸시가 안 오므로 알림 설정으로 보낸다.
                    fun enableAlert(isDispatch: Boolean, apply: () -> Unit) {
                        if (context.notificationsEnabled()) {
                            apply()
                        } else {
                            pendingDispatchAlert = isDispatch
                            awaitingNotificationSettings = true
                            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            })
                        }
                    }
                    SeekProfileSettings(
                        isDancer = mode.isDancer,
                        viewAlert = viewAlert,
                        onViewAlert = { enabled ->
                            if (!enabled) viewAlert = false else enableAlert(false) { viewAlert = true }
                        },
                        onVisibility = onVisibility,
                    )
                    // ★ 2026-10-11 긴급 발송 종료 — 설정 섹션을 숨긴다(다시 열면 SUB_DISPATCH_ENABLED=true).
                    if (SUB_DISPATCH_ENABLED) dispatch?.let { prefs ->
                        SubDispatchSettings(
                            prefs = prefs,
                            onEnabled = { enabled ->
                                if (!enabled) dispatch = prefs.copy(enabled = false)
                                else enableAlert(true) { dispatch = dispatch?.copy(enabled = true) }
                            },
                            onChange = { dispatch = it },
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (previewOpen) {
        Dialog(
            onDismissRequest = { previewOpen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            ResumeDraftPreview(form = form, mode = mode, onClose = { previewOpen = false })
        }
    }

    errorMessage?.let { msg ->
        QuoteDialog("오류", msg, "확인", onConfirm = { errorMessage = null }, onDismiss = { errorMessage = null })
    }
}


/** 구직 프로필 설정 — iOS `ResumeEditView.seekProfileSettingsSection` 1:1. */
@Composable
private fun SeekProfileSettings(isDancer: Boolean, viewAlert: Boolean, onViewAlert: (Boolean) -> Unit, onVisibility: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MuyeonColors.border, RoundedCornerShape(12.dp))
                .clickable(onClick = onVisibility)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(36.dp).clip(CircleShape).background(MuyeonColors.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Tune, null, tint = MuyeonColors.primary, modifier = Modifier.size(16.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "공개 범위 설정",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                )
                Text(
                    if (isDancer) "공연팀·기획자에게 공개할 프로필 항목을 설정하세요."
                    else "원장님에게 공개할 프로필 항목을 설정하세요.",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                    lineHeight = 14.sp, color = MuyeonColors.textSub,
                )
            }
            Icon(Icons.Filled.KeyboardArrowRight, null, tint = MuyeonColors.chevron, modifier = Modifier.size(16.dp))
        }
        Row(
            Modifier.fillMaxWidth().clickable { onViewAlert(!viewAlert) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "내 프로필을 열람하면 알림 받기",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                lineHeight = 17.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
            )
            Switch(
                checked = viewAlert, onCheckedChange = onViewAlert,
                colors = SwitchDefaults.colors(checkedTrackColor = MuyeonColors.primary),
            )
        }
    }
}

/**
 * 긴급 대타 알림 — iOS `ResumeEditView.subDispatchSection` 1:1.
 *  지역·장르·대상·시간대는 위 프로필 본문으로 매칭(요일 무관)하므로 타임 수·타임당 최소 금액만 고른다.
 */
private const val SUB_DISPATCH_ENABLED = false

@Composable
private fun SubDispatchSettings(
    prefs: SubDispatchPrefs,
    onEnabled: (Boolean) -> Unit,
    onChange: (SubDispatchPrefs) -> Unit,
) {
    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable { onEnabled(!prefs.enabled) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "긴급 대타 알림 받기",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                    lineHeight = 17.sp, color = MuyeonColors.textHead,
                )
                Text(
                    "위에 적은 활동 지역·장르·지도 대상·수업 가능 시간대에 맞는 긴급 대타 공고가 올라오면 " +
                        "요일 상관없이 바로 알려 드려요. 원하는 타임 수와 타임당 최소 금액만 골라 주세요.",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
                    lineHeight = 16.sp, color = MuyeonColors.textSub,
                )
            }
            Switch(
                checked = prefs.enabled, onCheckedChange = onEnabled,
                colors = SwitchDefaults.colors(checkedTrackColor = MuyeonColors.primary),
            )
        }
        if (prefs.enabled) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF7F7F7)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                DispatchKeyword("타임 수") {
                    // 단일 선택 — "" = 상관없음(min/max 둘 다 null)
                    val current = ResumeOptions.dispatchClassCounts.firstOrNull {
                        it.third.first == prefs.minClassCount && it.third.second == prefs.maxClassCount
                    }?.first ?: ""
                    MultiChips(
                        listOf("" to "상관없음") + ResumeOptions.dispatchClassCounts.map { it.first to it.second },
                        listOf(current),
                    ) { v ->
                        val range = ResumeOptions.dispatchClassCounts.firstOrNull { it.first == v && v != current }?.third
                        onChange(prefs.copy(minClassCount = range?.first, maxClassCount = range?.second))
                    }
                }
                DispatchKeyword("타임당 최소 금액") {
                    MultiChips(
                        listOf("" to "상관없음") + ResumeOptions.dispatchMinPays.map { it.first.toString() to it.second },
                        listOf(prefs.minPayPerSession?.toString() ?: ""),
                    ) { v ->
                        val pay = v.toIntOrNull()
                        onChange(prefs.copy(minPayPerSession = if (pay == prefs.minPayPerSession) null else pay))
                    }
                }
            }
        }
    }
}

@Composable
private fun DispatchKeyword(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.textHead,
        )
        content()
    }
}

private fun Context.notificationsEnabled(): Boolean =
    NotificationManagerCompat.from(this).areNotificationsEnabled()

private fun <T> List<T>.replaced(index: Int, transform: (T) -> T): List<T> =
    mapIndexed { i, v -> if (i == index) transform(v) else v }

/**
 * 작성값을 저장하지 않고도 실제 노출 구성을 확인하는 읽기 전용 화면 — iOS `ResumeDraftPreviewView` 1:1.
 *  (PublicProfileScreen 은 서버 공개 프로필(userId)을 읽어 그리는 구조라 미저장 입력값을 넣을 수 없어 별도 이식)
 */
@Composable
private fun ResumeDraftPreview(form: ResumeEditForm, mode: ResumeMode, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val merged = ResumePeriod.mergedTotalMonths(form.careers.map { it.period })
    Column(
        Modifier.fillMaxSize().background(MuyeonColors.surface)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        QuoteNavBar(title = "미리보기", onBack = onClose)
        Row(
            Modifier.fillMaxWidth().background(MuyeonColors.primary.copy(alpha = 0.08f)).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Info, null, tint = MuyeonColors.primary, modifier = Modifier.size(16.dp))
            Text(
                "다른 회원에게 보이는 화면이에요. 실제 기능 버튼은 보이지 않아요.",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.primary,
            )
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            PreviewSection("이력서 이름", listOf(form.title))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(88.dp).clip(RoundedCornerShape(12.dp)).background(MuyeonColors.tileIdle),
                    contentAlignment = Alignment.Center,
                ) {
                    val img = form.image
                    if (img != null) {
                        AsyncImage(QuoteUi.imageUrl(img), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Filled.Person, null, tint = MuyeonColors.hint, modifier = Modifier.size(24.dp))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    // 활동명은 더 이상 허용하지 않는다 — 이력서 이름은 계정 실명 고정.
                    Text(
                        form.basic.name?.ifEmpty { null } ?: "이름",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                        lineHeight = 27.sp, color = MuyeonColors.textHead,
                    )
                    Text(
                        form.oneLiner.ifEmpty { "한 줄 소개를 입력해 주세요." },
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                        lineHeight = 18.sp, color = MuyeonColors.textSub,
                    )
                }
            }
            val basicInfo = buildList {
                form.basic.birth?.takeIf { it.isNotEmpty() }?.let { add("생년월일  $it") }
                if (mode.isDancer) {
                    if (form.gender.isNotEmpty()) add("성별  ${form.gender}")
                    if (form.height.isNotEmpty()) add("키  ${form.height}cm")
                    if (form.weight.isNotEmpty()) add("몸무게  ${form.weight}kg")
                }
            }
            PreviewSection("기본 정보", basicInfo)
            if (mode.isDancer && form.images.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PreviewTitle("포트폴리오")
                    form.images.chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { url ->
                                AsyncImage(
                                    QuoteUi.imageUrl(url), null, contentScale = ContentScale.Crop,
                                    modifier = Modifier.weight(1f).height(104.dp).clip(RoundedCornerShape(10.dp))
                                        .background(MuyeonColors.tileIdle),
                                )
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    HorizontalDivider(color = MuyeonColors.border)
                }
            }
            PreviewSection("전공", listOf(form.genres.joinToString(", ")))
            PreviewSection("활동 지역", listOf(form.activeRegions.joinToString(", ") { it.first }))
            if (!mode.isDancer) {
                PreviewSection("수업 대상", listOf(form.targets.joinToString(", ") { ResumeOptions.targetLabel(it) }))
                PreviewSection(
                    "레슨 가능 시간",
                    listOf(form.days.joinToString("·"), form.slots.joinToString(", ") { ResumeOptions.timeSlotLabel(it) }),
                )
            }
            PreviewSection("학력", form.educations.map { e -> listOf(e.school, e.major, e.period).filter { it.isNotEmpty() }.joinToString(" · ") })
            if (!mode.isDancer) {
                val summary = buildList {
                    merged?.let { add("총 경력  ${ResumePeriod.label(it)}") }
                    val bucket = merged?.let { ResumePeriod.careerBucket(it) } ?: form.careerBucket
                    if (bucket.isNotEmpty()) add("경력 구간  ${ResumeOptions.careerLabel(bucket)}")
                }
                PreviewSection("경력 요약", summary)
                PreviewSection(
                    "경력",
                    form.careers.map { c -> listOf(c.academy, c.position, c.classes, c.period).filter { it.isNotEmpty() }.joinToString(" · ") },
                )
            }
            PreviewSection("공연 이력", form.performances.map { p -> listOf(p.year, p.title, p.role).filter { it.isNotEmpty() }.joinToString(" · ") })
            if (mode.isDancer) {
                PreviewSection("무용단 경력", listOf(form.companyCareer))
                PreviewSection("영상 포트폴리오", listOf(form.videoUrl))
            } else {
                PreviewSection("자격증", form.certificates.lines())
            }
            PreviewSection("수상 경력", form.awards.lines())
            if (!mode.isDancer) {
                PreviewSection(
                    "희망 조건",
                    listOf(
                        form.desired.job?.let { "희망 직종  $it" } ?: "",
                        form.desired.region?.let { "희망 지역  $it" } ?: "",
                        form.desired.salary?.let { "희망 급여  ${ResumeOptions.salaryLabel(it)}" } ?: "",
                    ),
                )
            }
            PreviewSection("자기소개", listOf(form.intro))
        }
    }
}

@Composable
private fun PreviewTitle(title: String) {
    Text(
        title,
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
        lineHeight = 20.sp, color = MuyeonColors.textHead,
    )
}

@Composable
private fun PreviewSection(title: String, values: List<String>) {
    val visible = values.filter { it.isNotBlank() }
    if (visible.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PreviewTitle(title)
        visible.forEach {
            Text(
                it,
                fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp,
                lineHeight = 21.sp, color = MuyeonColors.body,
            )
        }
        HorizontalDivider(color = MuyeonColors.border, modifier = Modifier.padding(top = 10.dp))
    }
}

/** 섹션 타이틀 — iOS sectionTitle (Bold 16, 101116). */
@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
        lineHeight = 19.sp, color = MuyeonColors.textHead, modifier = modifier,
    )
}

/** 테두리 없는 입력 — iOS 기본 TextField(Medium 14). [modifier] 로 테두리/패딩을 감싼다. */
@Composable
private fun BareField(
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minHeight: Int = 0,
    placeholderSize: Int = 14,
    onChange: (String) -> Unit,
) {
    BasicTextField(
        value = value, onValueChange = onChange, singleLine = singleLine,
        textStyle = TextStyle(
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
            lineHeight = 18.sp, color = MuyeonColors.textHead,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        cursorBrush = SolidColor(MuyeonColors.primary),
        modifier = modifier,
        decorationBox = { inner ->
            Box(Modifier.heightIn(min = minHeight.dp)) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        placeholder,
                        fontFamily = customFontFamily, fontWeight = FontWeight.Medium,
                        fontSize = placeholderSize.sp, lineHeight = (placeholderSize + 4).sp, color = MuyeonColors.hint,
                    )
                }
                inner()
            }
        },
    )
}

/** iOS plainInput — EAEAEA 테두리 r10, 12x11 패딩. */
@Composable
private fun PlainInput(value: String, placeholder: String, keyboard: KeyboardType = KeyboardType.Text, onChange: (String) -> Unit) {
    BareField(
        value, placeholder, keyboard = keyboard,
        modifier = Modifier.fillMaxWidth().border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        onChange = onChange,
    )
}

/** iOS labeledInput — 라벨(Medium 12, 6D6E71) + 테두리 없는 값 입력, 간격 3. */
@Composable
private fun LabeledInput(
    label: String,
    value: String,
    placeholder: String,
    keyboard: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
    onChange: (String) -> Unit,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 14.sp, color = MuyeonColors.textSub)
        BareField(value, placeholder, Modifier.fillMaxWidth(), keyboard = keyboard, onChange = onChange)
    }
}

/**
 * 수정할 수 없는 값 행. 계정에서 정해지는 값(본인확인 실명 등)에 쓴다.
 *  비활성 입력칸으로 두지 않고 값 표시로 그린다 — 비활성 입력칸은 "왜 안 눌리지"로 읽히지만,
 *  값 표시는 애초에 입력칸이 아니라는 게 보인다. (iOS readOnlyRow 와 동일)
 */
@Composable
private fun ReadOnlyRow(label: String, value: String, hint: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 14.sp, color = MuyeonColors.textSub)
        Text(
            value.ifEmpty { "휴대폰 본인인증 후 채워져요" },
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 18.sp,
            color = if (value.isEmpty()) MuyeonColors.textSub else MuyeonColors.textHead,
            modifier = Modifier.fillMaxWidth(),
        )
        if (hint != null) {
            Text(hint, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 13.sp, color = MuyeonColors.textSub)
        }
    }
}

/** 반복 항목 카드 안 필드 행 — iOS fieldRow(라벨 64dp 회색 13 + 입력 14). */
@Composable
private fun FieldRow(label: String, value: String, placeholder: String, onChange: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.textSub, modifier = Modifier.width(64.dp),
        )
        BareField(value, placeholder, Modifier.weight(1f), onChange = onChange)
    }
}

/** 여러 줄(줄당 1건) 섹션 — 자격증/수상/무용단 경력 (iOS linesSection). */
@Composable
private fun LinesSection(title: String, value: String, placeholder: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(title)
        BareField(
            value, placeholder, singleLine = false, minHeight = 76, placeholderSize = 13,
            modifier = Modifier.fillMaxWidth().border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
                .padding(14.dp),
            onChange = onChange,
        )
    }
}

/** 총 경력 자동 계산 줄 — 기간 합산(개월) → "총 경력 N년 M개월" + 자동 반영되는 경력 구간. */
@Composable
private fun CareerTotalRow(total: Int) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .background(MuyeonColors.primary.copy(alpha = 0.08f)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Functions, null, tint = MuyeonColors.primary, modifier = Modifier.size(12.dp))
        Text(
            "총 경력 ${ResumePeriod.label(total)}",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            lineHeight = 17.sp, color = MuyeonColors.textHead,
        )
        Text(
            "· 경력 구간 ${ResumeOptions.careerLabel(ResumePeriod.careerBucket(total))} 자동 적용",
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp,
            lineHeight = 14.sp, color = MuyeonColors.textSub,
        )
    }
}

/** 희망 근무 조건 표 행 — 회색 라벨 칸(92dp + 좌우 12) + 값 칸. */
@Composable
private fun TableRow(label: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.fillMaxHeight().background(MuyeonColors.tileIdle).padding(horizontal = 12.dp, vertical = 12.dp).width(92.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(label, fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp, color = MuyeonColors.textSub)
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) { content() }
    }
}

@Composable
private fun DropdownValue(value: String, placeholder: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            value.ifEmpty { placeholder },
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 17.sp,
            color = if (value.isEmpty()) MuyeonColors.hint else MuyeonColors.textHead,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Filled.KeyboardArrowDown, null, tint = MuyeonColors.hint, modifier = Modifier.size(14.dp))
    }
}

/** iOS ResumeChips — 줄바꿈 칩(SemiBold 13, 12x8). 선택 = 주황 글자 + 주황 10% 채움 + 주황 테두리. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResumeChips(options: List<Pair<String, String>>, isSelected: (String) -> Boolean, onTap: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (v, label) ->
            val on = isSelected(v)
            Text(
                label,
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                lineHeight = 16.sp, maxLines = 1,
                color = if (on) MuyeonColors.primary else MuyeonColors.textSub,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (on) MuyeonColors.primary.copy(alpha = 0.10f) else MuyeonColors.tileIdle)
                    .border(1.dp, if (on) MuyeonColors.primary else Color.Transparent, CircleShape)
                    .clickable { onTap(v) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

/** 다중 선택 칩 (value, label) — 화면엔 라벨, 저장은 값. */
@Composable
private fun MultiChips(options: List<Pair<String, String>>, selected: List<String>, onToggle: (String) -> Unit) {
    ResumeChips(options, { selected.contains(it) }, onToggle)
}

/** 단일 선택 칩 — 같은 칩을 다시 누르면 해제(""). iOS chipGroupSingle. */
@Composable
private fun SingleChips(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    ResumeChips(options, { it == selected }) { v -> onSelect(if (selected == v) "" else v) }
}

/** 반복 섹션 — 타이틀 + "+ 추가", 항목 = 테두리 카드 + 우상단(걸침) 삭제. iOS repeatSection. */
@Composable
private fun <T> RepeatSection(
    title: String,
    items: List<T>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    itemContent: @Composable ColumnScope.(Int, T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(title)
            Spacer(Modifier.weight(1f))
            Text(
                "+ 추가",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                lineHeight = 17.sp, color = MuyeonColors.primary,
                modifier = Modifier.clickable(onClick = onAdd).padding(vertical = 2.dp),
            )
        }
        items.forEachIndexed { i, item ->
            Box(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth()
                        .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemContent(i, item)
                }
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-6).dp).size(16.dp)
                        .clip(CircleShape).background(MuyeonColors.surface)
                        .clickable { onRemove(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Cancel, "삭제", tint = MuyeonColors.hint, modifier = Modifier.size(16.dp))
                }
            }
        }
        if (items.isEmpty()) {
            Text(
                "+ 추가를 눌러 입력해 주세요.",
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.hint,
            )
        }
    }
}
