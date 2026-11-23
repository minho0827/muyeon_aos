package com.muyeon.app.ui.onboarding

import com.muyeon.app.util.optStringOrNull

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.BuildConfig
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.ui.quote.QuoteUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * 회원유형 인증 서류 제출 — iOS `Onboarding/Role/RoleVerificationView.swift` 이식.
 *  사진 첨부 → /uploads/image 업로드 → 업로드 URL 배열을 웹 콜백으로 전달(제출 API 는 웹이 수행).
 *
 * ⚠️ 서류는 화면이 크게 보여야 심사가 가능하므로 업로드 전 최대 1600px / JPEG 70 으로만 줄인다
 *   (iOS 와 동일 수치). 원본을 그대로 올리면 최근 단말 사진은 10MB 를 넘어 업로드가 자주 실패한다.
 */
private const val MAX_DOCS = 5
private const val MAX_DIMENSION = 1600
private const val JPEG_QUALITY = 70
/** ★ 2026-11-23 학원 인증 임시 저장 묶음 간격 — 입력이 멈추고 이만큼 지나면 저장한다. */
private const val DRAFT_SAVE_DEBOUNCE_MS = 800L

/** 역할 코드 → 화면 표기(유형 정보 표의 긴 이름, iOS `roleLabel` 과 같은 문구). 일반은 "회원". */
private fun roleLabel(code: String): String =
    com.muyeon.domain.model.member.ActiveTypeInfos.find(code)?.takeIf { it.type != com.muyeon.domain.model.member.ActiveType.GENERAL }?.long ?: "회원"

/** 역할별 제출 서류 안내(유형 정보 표, iOS `documentGuide` 와 같은 문구). */
private fun documentGuide(code: String): String = (com.muyeon.domain.model.member.ActiveTypeInfos.find(code) ?: com.muyeon.domain.model.member.ActiveTypeInfos.GENERAL).docGuide

class RoleVerificationApi(private val token: String?) {
    private val client = com.muyeon.app.ui.common.AppHttp.client

    suspend fun uploadImage(bytes: ByteArray): String? = withContext(Dispatchers.IO) {
        runCatching {
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", "doc.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
                .build()
            val req = Request.Builder().url(BuildConfig.API_BASE_URL + "/api/uploads/image").post(body)
                .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                .build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@use null
                JSONObject(res.body?.string().orEmpty()).optStringOrNull("url")
            }
        }.getOrNull()
    }

    /** ★ 2026-11-23 학원 인증 이어하기 정보 — verification-status 의 resume(없으면 null, 옛 서버도 null). */
    /** 상태 조회에 실패하면(네트워크 등) 예외 — 호출부가 '불러오기 실패'와 '임시 저장 없음'을 가른다. */
    suspend fun loadResume(): com.muyeon.app.ui.floating.VerifyResume? {
        val st = com.muyeon.app.ui.floating.FloatingApi { token }.verificationStatus()
            ?: throw IllegalStateException("verification-status 조회 실패")
        return st.resume
    }

    /**
     * ★ 2026-11-23 학원 인증 임시 저장 — PUT /me/roles/academy/verification/draft.
     *  사업자등록번호는 번호 확인 API 가 이미 서버에 저장하므로 보내지 않는다. 개업일자는 8자리일 때만 보낸다.
     */
    suspend fun saveAcademyDraft(academyName: String?, startDate: String?, documents: List<String>): Boolean =
        withContext(Dispatchers.IO) {
            if (token.isNullOrEmpty()) return@withContext false
            runCatching {
                val docs = org.json.JSONArray().apply {
                    documents.forEach { put(JSONObject().put("type", "사업자등록증").put("url", it)) }
                }
                val json = JSONObject()
                    .put("academyName", academyName?.trim()?.takeIf { it.isNotEmpty() } ?: JSONObject.NULL)
                    .put("businessStartDate", startDate?.takeIf { it.length == 8 } ?: JSONObject.NULL)
                    .put("documents", docs)
                    .toString()
                val req = Request.Builder()
                    .url(BuildConfig.API_BASE_URL + "/api/me/roles/academy/verification/draft")
                    .put(json.toRequestBody("application/json".toMediaType()))
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                client.newCall(req).execute().use { it.isSuccessful }
            }.getOrDefault(false)
        }

    /** 화면을 닫는 순간의 임시 저장 — 화면 수명과 무관하게 끝까지 보낸다(닫히면 화면 코루틴은 취소된다). */
    fun saveAcademyDraftDetached(academyName: String?, startDate: String?, documents: List<String>) {
        detachedScope.launch {
            if (saveAcademyDraft(academyName, startDate, documents)) {
                withContext(Dispatchers.Main) { com.muyeon.app.ui.floating.FloatingState.requestRefresh() }
            }
        }
    }

    private companion object {
        val detachedScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
    }

    /** ownerMatched: ★ 2026-10-22 개업일자를 함께 보냈을 때만 — 대표자(본인인증 실명)·개업일자가 국세청 정보와 맞는지. */
    data class BusinessNumberCheck(val verified: Boolean, val number: String, val message: String, val ownerMatched: Boolean? = null)

    suspend fun checkAcademyBusinessNumber(number: String, startDate: String? = null): BusinessNumberCheck = withContext(Dispatchers.IO) {
        val json = JSONObject().put("businessRegistrationNumber", number)
            .apply { if (startDate?.length == 8) put("businessStartDate", startDate) }.toString()
        val req = Request.Builder()
            .url(BuildConfig.API_BASE_URL + "/api/me/roles/academy/business-number/check")
            .post(json.toRequestBody("application/json".toMediaType()))
            .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
            .build()
        client.newCall(req).execute().use { res ->
            val payload = JSONObject(res.body?.string().orEmpty().ifEmpty { "{}" })
            if (!res.isSuccessful) throw IllegalStateException(payload.optString("message"))
            BusinessNumberCheck(
                verified = payload.optBoolean("verified"),
                number = payload.optString("businessRegistrationNumber"),
                message = payload.optString("message"),
                ownerMatched = if (payload.has("ownerMatched")) payload.optBoolean("ownerMatched") else null,
            )
        }
    }
}

/**
 * @param initialImages 재제출 시 기존 제출 서류(미리보기). 삭제·추가 후 다시 제출할 수 있다.
 * @param onDone (역할코드, 서류 URL, 학원명) — 학원명은 ACADEMY 에서만 채워진다.
 * @param ownerName 본인확인(NICE)으로 등록된 실명. 학원 인증에서 '대표원장'으로 읽기전용 표시한다.
 *   ★ 입력받지 않는다 — 대표원장 이름은 계정의 실명 인증 결과가 정본이고, 여기서 고쳐 쓸 수 있으면
 *     서류상 대표자와 다른 이름이 공개 프로필·공고에 노출된다. (iOS RoleVerificationView 와 동일)
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RoleVerificationScreen(
    api: RoleVerificationApi,
    role: String,
    onClose: () -> Unit,
    onDone: (String, List<String>, String?, String?, String?) -> Unit,   // (역할, 서류 URL, 학원명, 사업자번호, 개업일자)
    ownerName: String = "",
    initialImages: List<String> = emptyList(),
    allowSkip: Boolean = false,
    /** ★ 2026-11-23 '학원 인증 마무리하기' 플로팅에서 열었다 — 채운 뒤 첫 빈 칸으로 스크롤·포커스한다. */
    resume: Boolean = false,
) {
    // 기존 제출 서류를 그대로 이어받는다 — 재제출인데 빈 화면이면 처음부터 다시 찍어 올려야 한다.
    var images by remember(initialImages) { mutableStateOf(initialImages) }
    var academyName by remember { mutableStateOf("") }
    var businessNumber by remember { mutableStateOf("") }
    var verifyLater by remember { mutableStateOf(false) }
    var checkingBusinessNumber by remember { mutableStateOf(false) }
    var verifiedBusinessNumber by remember { mutableStateOf<String?>(null) }
    var businessNumberMessage by remember { mutableStateOf<String?>(null) }
    // ★ 2026-10-22 개업일자 — 대표자 이름·개업일자가 국세청 정보와 맞으면(ownerMatched) 제출 즉시 자동 승인, 사진 선택.
    var startDate by remember { mutableStateOf("") }
    var ownerMatched by remember { mutableStateOf<Boolean?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    // ★ 2026-11-23 키보드 정리 — 앨범으로 갈 때·앨범에서 돌아올 때 닫는다.
    //  (화면 전체가 키보드 높이만큼 밀리는 구조라, 키보드가 남으면 [제출하기]가 화면 가운데에 걸린다 — 실기기 제보)
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val closeKeyboard = { focusManager.clearFocus(); keyboard?.hide() }
    // 앨범 등 다른 화면에서 돌아오면 키보드를 정리해 [제출하기]가 맨 아래로 돌아오게.
    //  처음 열 때는 건너뛴다 — 이어하기가 첫 빈 칸에 커서·키보드를 올리는 것을 막지 않게.
    var resumedOnce by remember { mutableStateOf(false) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        if (resumedOnce) closeKeyboard() else resumedOnce = true
        onPauseOrDispose { }
    }
    // ★ 2026-11-07 자동 확인 받기 — 국세청 확인이 다 맞으면(바로 승인 대상) 제출 버튼을 누르지 않아도 보낸다(번호 확인만 하고 나가 '미제출'로 남던 사례)
    //  한 화면에서 한 번만 보낸다(자동·버튼 공통 가드 — 두 번 눌림·자동 직후 버튼 탭 중복 제출 방지).
    var submitted by remember { mutableStateOf(false) }
    var academyNameHadFocus by remember { mutableStateOf(false) }

    // ★ 2026-11-23 학원 인증 이어하기 · 임시 저장
    //  prefillDone: 서버 임시 저장값을 채우기 전엔 저장하지 않는다(빈 값으로 덮어쓰기 방지).
    //  dirty: 사용자가 직접 고친 게 있어야 저장한다(열어 보기만 해도 저장되면 '마무리하기'가 생겨 버린다).
    //  prefilled: 임시 저장값으로 채웠다 — 이때는 사용자가 고치기 전까지 자동 제출을 하지 않는다(누르자마자 제출되는 일 방지).
    var prefillDone by remember { mutableStateOf(role != "ACADEMY") }
    var dirty by remember { mutableStateOf(false) }
    var prefilled by remember { mutableStateOf(false) }
    var lastSavedDraft by remember { mutableStateOf<Triple<String, String, List<String>>?>(null) }
    val scrollState = rememberScrollState()
    val nameFocus = remember { FocusRequester() }
    val numberFocus = remember { FocusRequester() }
    val startDateFocus = remember { FocusRequester() }
    val nameBring = remember { BringIntoViewRequester() }
    val numberBring = remember { BringIntoViewRequester() }
    val startDateBring = remember { BringIntoViewRequester() }

    LaunchedEffect(Unit) {
        if (role != "ACADEMY") return@LaunchedEffect
        // ★ 11-23 불러오기에 실패하면 저장을 켜지 않는다 — 빈 칸으로 저장해 서버의 학원명을 지운 일이 있었다(실기기).
        val loaded = runCatching { api.loadResume() }
        if (loaded.isFailure) return@LaunchedEffect
        val draft = loaded.getOrNull()?.draft
        if (draft != null) {
            // 이미 화면에 있는 값(재제출 서류 등)은 지우지 않고, 빈 칸만 채운다.
            if (academyName.isBlank()) draft.academyName?.let { academyName = it; prefilled = true }
            if (businessNumber.isEmpty()) draft.businessRegistrationNumber?.let { businessNumber = it; prefilled = true }
            if (startDate.isEmpty()) draft.businessStartDate?.let { startDate = it; prefilled = true }
            if (images.isEmpty() && draft.documentUrls.isNotEmpty()) { images = draft.documentUrls.take(MAX_DOCS); prefilled = true }
        }
        lastSavedDraft = Triple(academyName.trim(), startDate, images)
        prefillDone = true
        if (!resume) return@LaunchedEffect
        // 첫 빈 칸으로 — 칸이 그려진 뒤에 옮겨야 포커스가 붙는다.
        delay(250)
        val target = when {
            academyName.isBlank() -> nameFocus to nameBring
            businessNumber.filter(Char::isDigit).length != 10 -> numberFocus to numberBring
            startDate.length != 8 -> startDateFocus to startDateBring
            else -> null
        }
        if (target == null) {
            // 제출만 남았어요 — 아래 제출 버튼이 보이도록 끝까지 내린다.
            scrollState.animateScrollTo(scrollState.maxValue)
        } else {
            runCatching { target.second.bringIntoView() }
            runCatching { target.first.requestFocus() }
        }
    }

    /** 지금 값을 임시 저장 — 마지막 저장과 같으면 보내지 않는다. */
    suspend fun saveDraftNow() {
        val snap = Triple(academyName.trim(), startDate, images)
        if (snap == lastSavedDraft) return
        if (api.saveAcademyDraft(snap.first, snap.second, snap.third)) {
            lastSavedDraft = snap
            com.muyeon.app.ui.floating.FloatingState.requestRefresh()
        }
    }

    // 학원명·개업일자·서류가 바뀌면 800ms 동안 더 안 바뀔 때 저장한다.
    LaunchedEffect(academyName, startDate, images, prefillDone) {
        if (role != "ACADEMY" || !prefillDone || !dirty || submitted || verifyLater) return@LaunchedEffect
        delay(DRAFT_SAVE_DEBOUNCE_MS)
        saveDraftNow()
    }
    // 닫기·뒤로가기 등으로 화면을 떠날 때 — 아직 못 보낸 변경이 있으면 마저 보낸다.
    DisposableEffect(Unit) {
        onDispose {
            if (role != "ACADEMY" || !prefillDone || !dirty || submitted || verifyLater) return@onDispose
            val snap = Triple(academyName.trim(), startDate, images)
            if (snap != lastSavedDraft) api.saveAcademyDraftDetached(snap.first, snap.second, snap.third)
        }
    }

    LaunchedEffect(businessNumber, startDate, verifyLater) {
        verifiedBusinessNumber = null
        businessNumberMessage = null
        ownerMatched = null
        val normalized = businessNumber.filter(Char::isDigit).take(10)
        if (normalized.length != 10 || verifyLater) return@LaunchedEffect
        delay(450)
        checkingBusinessNumber = true
        runCatching { api.checkAcademyBusinessNumber(normalized, startDate.takeIf { it.length == 8 }) }
            .onSuccess { result ->
                if (result.verified) verifiedBusinessNumber = result.number
                ownerMatched = if (result.verified) result.ownerMatched else null
                businessNumberMessage = when {
                    !result.verified -> result.message
                    result.ownerMatched == true -> "국세청 정보와 일치해요. 제출하면 바로 승인돼요."
                    // ★ 2026-11-18 불일치 안내는 띄우지 않는다(사용자 결정) — 11-17 원장님이 이 문구를 보고 제출 없이 나갔다.
                    //  불일치여도 그대로 제출되고 관리자가 확인한다. 일치할 때만 '바로 승인' 안내.
                    result.ownerMatched == false -> null
                    else -> "사업자등록번호 확인 완료 — 개업일자를 넣으면 바로 승인돼요."
                }
            }
            .onFailure {
                businessNumberMessage = "자동 확인이 지연되고 있어요. 잠시 후 다시 시도해 주세요."
            }
        checkingBusinessNumber = false
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = MAX_DOCS),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        uploading = true
        scope.launch {
            var failed = 0
            uris.forEach { uri ->
                if (images.size >= MAX_DOCS) return@forEach
                val bytes = withContext(Dispatchers.IO) { compressedJpeg(context, uri) }
                val url = bytes?.let { api.uploadImage(it) }
                if (url != null) { images = images + url; dirty = true } else failed++
            }
            uploading = false
            // 업로드 실패를 삼키면 사용자는 첨부한 줄 알고 빈 서류를 제출한다.
            if (failed > 0) errorMessage = "사진 ${failed}장을 올리지 못했어요. 잠시 후 다시 시도해 주세요."
        }
    }

    // 학원 인증은 학원명이 있어야 제출된다 — 비면 프로필·공고·홈 추천에 이름 없이 노출된다.
    //  ★ 2026-10-22 국세청 확인이 끝난 번호면 사진 없이도 제출된다(서버 자동 승인, 웹 submitDocs 가 번호만 와도 제출).
    val academy = academyName.trim().takeIf { role == "ACADEMY" && it.isNotEmpty() }
    val normalizedNumber = businessNumber.filter(Char::isDigit).take(10)
    val canSubmit = !uploading && !submitted && if (role == "ACADEMY") {
        academy != null && (verifyLater && allowSkip ||
            // ★ 2026-11-18 번호가 국세청에서 확인되면 제출 가능 — 대표자·개업일자가 맞으면 자동 승인, 아니면 사진 없이도
            //  관리자 심사(서버는 서류 0장도 받음). 종전엔 불일치면 사진 필수라 11-17 원장님이 제출 못 하고 나갔다. 다시 묶지 말 것.
            verifiedBusinessNumber == normalizedNumber)
    } else images.isNotEmpty()

    fun submit() {
        if (!canSubmit) return
        submitted = true
        onDone(role, if (verifyLater) emptyList() else images, academy,
            normalizedNumber.takeIf { role == "ACADEMY" && !verifyLater },
            startDate.takeIf { role == "ACADEMY" && !verifyLater && it.length == 8 })
    }

    // ★ 2026-11-07 자동 확인 받기 — 국세청 확인이 다 맞으면(바로 승인 대상) 제출 버튼을 누르지 않아도 보낸다(번호 확인만 하고 나가 '미제출'로 남던 사례)
    //  조건: 학원 + 번호 진위 확인(verified) + 대표자·개업일자 일치(ownerMatched == true) + 학원명 2자 이상.
    //  ownerMatched false/null(사진·관리자 확인 필요)·'다음에 인증할게요'(verifyLater)는 보내지 않는다.
    fun autoSubmitIfReady() {
        if (role != "ACADEMY" || verifyLater || submitted || uploading || checkingBusinessNumber) return
        if (verifiedBusinessNumber != normalizedNumber || ownerMatched != true) return
        if (academyName.trim().length < 2) return
        if (prefilled && !dirty) return
        submit()
    }
    // 확인 결과가 바뀌는 순간 — 학원명이 아직 짧으면 보내지 않고, 학원명 칸을 벗어날 때(포커스 해제·완료 키) 다시 본다.
    LaunchedEffect(verifiedBusinessNumber, ownerMatched, checkingBusinessNumber) { autoSubmitIfReady() }

    // ★ 11-23 '빈 곳 누르면 닫기'·'스크롤하면 닫기'는 넣지 않는다 — 입력칸 누름·키보드가 열릴 때의 자동 스크롤까지 잡아
    //  키보드가 열리자마자 닫혔다(실기기). 키보드는 [다음]/[완료] 키·뒤로가기로 닫는다.
    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "${roleLabel(role)} 인증", onBack = onClose)

        Column(
            Modifier.weight(1f).verticalScroll(scrollState).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                if (role == "ACADEMY") "학원 정보를 입력해 주세요" else "인증 서류를 올려주세요",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                lineHeight = 24.sp, color = MuyeonColors.textHead, modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                // ★ 2026-10-22 학원은 사업자등록번호가 국세청에서 확인되면 서버가 바로 승인한다 — 사진은 선택.
                if (role == "ACADEMY") "학원명과 사업자등록번호를 넣어 주세요. 개업일자까지 넣으면 국세청 확인 후 바로 승인되고, 비워 두면 관리자가 확인해요."
                else "${documentGuide(role)}\n최대 ${MAX_DOCS}장까지 올릴 수 있고, 관리자 확인 후 승인되면 푸시로 알려드려요.",
                fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp, color = MuyeonColors.textSub,
            )

            // 학원 인증 전용 — 학원명(사업자등록증 상호). 공고·홈 추천·공개 프로필이 이 값을 함께 쓴다.
            if (role == "ACADEMY") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "학원명 (필수)",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        lineHeight = 18.sp, color = MuyeonColors.textHead,
                    )
                    OutlinedTextField(
                        value = academyName, onValueChange = { academyName = it; dirty = true }, singleLine = true,
                        // ★ 2026-11-07 자동 확인 받기 — 학원명 입력을 마치면(포커스 해제·완료 키) 조건이 맞을 때 바로 보낸다.
                        // ★ 11-23 [다음] → 사업자등록번호('나중에 인증'이면 키보드 닫기). 칸을 벗어나면 자동 확인 받기를 본다(onFocusChanged).
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = {
                            if (verifyLater) closeKeyboard() else runCatching { numberFocus.requestFocus() }
                        }),
                        placeholder = {
                            Text(
                                "예: 루체 발레 학원 (사업자등록증 상호)",
                                fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.secondary,
                            )
                        },
                        modifier = Modifier.fillMaxWidth().bringIntoViewRequester(nameBring).focusRequester(nameFocus).onFocusChanged { f ->
                            if (academyNameHadFocus && !f.isFocused) autoSubmitIfReady()
                            academyNameHadFocus = f.isFocused
                        },
                    )

                    // 대표원장 — 본인확인으로 등록된 실명. 학원명과 함께 공개된다.
                    //  입력칸이 아니라 값 표시로 그린다 — 비활성 입력칸은 "왜 안 눌리지"로 읽힌다.
                    if (ownerName.isNotEmpty()) {
                        Text(
                            "대표원장",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            lineHeight = 18.sp, color = MuyeonColors.textHead,
                        )
                        Text(
                            ownerName,
                            fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 18.sp,
                            color = MuyeonColors.textHead,
                        )
                        Text(
                            "휴대폰 본인인증으로 등록된 이름이에요. 학원명과 함께 공개돼요.",
                            fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp,
                            color = MuyeonColors.textSub,
                        )
                    }
                }

                AcademyBusinessVerificationSection(
                    businessNumber = businessNumber,
                    onBusinessNumberChange = { if (it != businessNumber) dirty = true; businessNumber = it },
                    verifyLater = verifyLater,
                    onVerifyLaterChange = { verifyLater = it },
                    allowsVerifyLater = allowSkip,
                    onLater = onClose,
                    isChecking = checkingBusinessNumber,
                    isVerified = verifiedBusinessNumber == businessNumber.filter(Char::isDigit).take(10) /* ★ 11-18 대표자 불일치여도 번호 확인은 완료로 */,
                    statusMessage = businessNumberMessage,
                    fieldModifier = Modifier.bringIntoViewRequester(numberBring).focusRequester(numberFocus),
                    onNext = { runCatching { startDateFocus.requestFocus() } },
                )
                // 개업일자 — 대표자 이름·개업일자가 국세청 정보와 맞으면 제출 즉시 자동 승인(사진 선택).
                if (!verifyLater) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "개업일자 (선택)",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        lineHeight = 18.sp, color = MuyeonColors.textHead,
                    )
                    OutlinedTextField(
                        value = startDate, singleLine = true,
                        onValueChange = { v -> v.filter(Char::isDigit).take(8).let { if (it != startDate) dirty = true; startDate = it } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { closeKeyboard(); autoSubmitIfReady() }),
                        placeholder = {
                            Text(
                                "예: 20200115 (사업자등록증의 개업연월일)",
                                fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.secondary,
                            )
                        },
                        modifier = Modifier.fillMaxWidth().bringIntoViewRequester(startDateBring).focusRequester(startDateFocus),
                    )
                    Text(
                        "넣으면 국세청 확인 후 바로 승인돼요. 비워 두면 관리자가 확인해요(보통 1영업일).",
                        fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = MuyeonColors.textSub,
                    )
                }
            }

            if (role == "TEAM") TeamBusinessNumberHelp()

            // ★ 11-23 무엇을 올리는지 분명하게 — 사업자등록증 사진(선택).
            if (role == "ACADEMY" && !verifyLater) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "사업자등록증 사진 (선택)",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                )
                Text(
                    "사업자등록증을 찍어 올려 주세요. 개업일자를 넣었다면 없어도 돼요.",
                    fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = MuyeonColors.textSub,
                )
            }

            if (role != "ACADEMY" || !verifyLater) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.size(96.dp).clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
                        .clickable(enabled = !uploading && images.size < MAX_DOCS) {
                            // ★ 11-23 앨범으로 가기 전에 키보드를 닫는다 — 남아 있으면 돌아왔을 때 [제출하기]가 화면 가운데에 걸린다.
                            closeKeyboard()
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Add, "서류 추가", tint = MuyeonColors.secondary, modifier = Modifier.size(24.dp))
                }
                images.forEach { url ->
                    Box {
                        AsyncImage(
                            QuoteUi.imageUrl(url), null, contentScale = ContentScale.Crop,
                            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(10.dp)),
                            placeholder = com.muyeon.app.ui.components.imageLoadingPainter(),
                        )
                        Icon(
                            Icons.Filled.Close, "삭제", tint = Color.White,
                            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(18.dp)
                                .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.5f))
                                .clickable { images = images - url; dirty = true },
                        )
                    }
                }
            }
            if (role != "ACADEMY" || !verifyLater) {
                Text(
                    "${images.size}/$MAX_DOCS",
                    fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 14.sp, color = MuyeonColors.secondary,
                )
            }
        }

        Text(
            if (uploading) "업로드 중…" else if (role == "ACADEMY" && verifyLater) "가입 계속" else "제출하기",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp)
                .fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(if (canSubmit) MuyeonColors.primary else Color.Gray.copy(alpha = 0.4f))
                .clickable(enabled = canSubmit) { submit() }
                .padding(vertical = 16.dp),
        )
    }

    errorMessage?.let { msg ->
        QuoteDialog("알림", msg, "확인", onConfirm = { errorMessage = null }, onDismiss = { errorMessage = null })
    }
}

/**
 * 업로드용 축소본(최대 1600px, JPEG 70). 실패하면 null — 원본을 그대로 올리지 않는다.
 *  ① 크기만 먼저 읽어 inSampleSize 로 대략 줄이고(메모리 폭발 방지) ② 정확한 배율로 맞춘다.
 */
private fun compressedJpeg(context: Context, uri: Uri): ByteArray? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val longest = maxOf(bounds.outWidth, bounds.outHeight)
    if (longest <= 0) return null
    var sample = 1
    while (longest / sample > MAX_DIMENSION * 2) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opts)
    } ?: return null

    val scale = minOf(1f, MAX_DIMENSION.toFloat() / maxOf(decoded.width, decoded.height))
    val scaled = if (scale >= 1f) decoded else Bitmap.createScaledBitmap(
        decoded, (decoded.width * scale).toInt().coerceAtLeast(1),
        (decoded.height * scale).toInt().coerceAtLeast(1), true,
    )
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
    if (scaled !== decoded) scaled.recycle()
    decoded.recycle()
    out.toByteArray()
}.getOrNull()
