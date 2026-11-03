package com.muyeon.app.ui.transfer

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.*
import com.muyeon.app.ui.common.market.*
import com.muyeon.app.ui.lesson.LocationSearchSheet
import com.muyeon.app.ui.transfer.wizard.*
import kotlinx.coroutines.launch

/**
 * 양도 글 등록·수정 위저드 — 채용 공고 위저드와 같은 단계형(상단 진행 + 하단 [이전][다음]).
 *  각 단계는 wizard/ 의 단계 컴포넌트, 이 화면은 상태 연결(불러오기·사진 올리기·저장)과 단계 고르기만 한다.
 */
@Composable
fun TransferWizardScreen(
    api: TransferApi,
    editId: Int?,
    onClose: () -> Unit,
    onSaved: (Int) -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val st = remember(editId) { TransferWizardState(editId) }
    var showExit by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    // 양도양수 이용약관 동의 — 새 글만. null = 동의 단계 없음(이미 동의·수정·확인 실패), 값 = 동의 단계를 보인다.
    var consentDocs by remember { mutableStateOf<List<ConsentDocItem>?>(null) }
    var consentChecking by remember { mutableStateOf(editId == null) }
    var consentSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(editId) {
        if (editId == null) {
            api.consent().onSuccess { c -> consentDocs = if (c.agreed) null else c.docs }
            consentChecking = false
        }
        if (editId != null) {
            api.detail(editId).onSuccess { st.form = TransferForm.from(it); st.baseline = st.form }.onFailure { st.error = it.message }
            st.loading = false
        }
    }
    LaunchedEffect(st.step) { scroll.scrollTo(0) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = TRANSFER_MAX_IMAGES)) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        st.uploading = true
        scope.launch {
            var list = st.form.images
            uris.take((TRANSFER_MAX_IMAGES - list.size).coerceAtLeast(0)).forEach { u ->
                UploadImagePreparer.prepare(ctx, u)?.let { bytes ->
                    api.uploadImage(bytes).onSuccess { list = list + it }.onFailure { st.error = it.message }
                }
            }
            st.update { copy(images = list) }
            st.uploading = false
        }
    }

    fun close() { if (st.dirty) showExit = true else onClose() }
    BackHandler { if (st.step > 0) st.step -= 1 else close() }

    fun agreeConsent() {
        consentSubmitting = true
        scope.launch {
            api.agreeConsent("POST").onSuccess { consentDocs = null }.onFailure { st.error = it.message ?: "동의를 저장하지 못했어요." }
            consentSubmitting = false
        }
    }

    fun save() {
        if (st.form.title.isBlank()) { st.step = 2; st.error = "제목을 입력해 주세요."; return }
        st.saving = true
        scope.launch {
            api.save(editId, st.form).onSuccess { st.baseline = st.form; onSaved(it) }.onFailure { e ->
                // 동의 전 등록(403 TRANSFER_CONSENT_REQUIRED) — 작성 내용은 그대로 두고 동의 단계로.
                if (e.isTransferConsentRequired()) {
                    api.consent().onSuccess { c -> consentDocs = c.docs }.onFailure { consentDocs = emptyList() }
                } else st.error = e.message ?: "저장에 실패했어요."
            }
            st.saving = false
        }
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        // 단계 고르기 한 번 — 불러오는 중 / 이용약관 동의 / 작성 단계. 머리 높이는 셋 다 같다(진행 줄은 자리만 두고 숨김).
        val docs = consentDocs
        if (st.loading || consentChecking) {
            WizardHeader(st.title, TRANSFER_STEPS, st.step, onClose = { close() }, showProgress = editId != null)
            WizardLoading()
            return@Column
        }
        if (docs != null) {
            WizardHeader(st.title, TRANSFER_STEPS, 0, onClose = { close() }, showProgress = false)
            MarketConsentStep(
                "양도 글을 올리기 전에 확인해 주세요", "아래 내용에 모두 동의해야 양도 글을 올릴 수 있어요.",
                docs, consentSubmitting, onAgree = { agreeConsent() },
            )
            return@Column
        }
        WizardHeader(st.title, TRANSFER_STEPS, st.step, onClose = { close() })

        Column(
            Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 20.dp).padding(top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            val f = st.form
            val set: (TransferForm) -> Unit = { st.form = it }
            when (st.step) {
                0 -> TransferStepPhotos(
                    f.images, st.uploading,
                    onAdd = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onRemove = { i -> st.update { copy(images = images.filterIndexed { idx, _ -> idx != i }) } },
                )
                1 -> TransferStepLocation(f, api.http.token(), onSearch = { showSearch = true }, onChange = set)
                2 -> TransferStepFacility(f, set)
                3 -> TransferStepPrice(f, set)
                4 -> TransferStepOptions(f.options) { v -> st.update { copy(options = if (v in options) options - v else options + v) } }
                5 -> TransferStepIntro(f, onPickDate = { showDate = true }, onChange = set)
                6 -> TransferStepContact(f, set)
                else -> TransferStepPreview(f)
            }
        }

        WizardNavBar(
            showPrev = st.step > 0, nextLabel = st.nextLabel, nextEnabled = st.canNext, busy = st.saving,
            onPrev = { st.step -= 1 }, onNext = { if (st.isLast) save() else st.step += 1 },
        )
    }

    if (showSearch) {
        LocationSearchSheet(
            initialQuery = st.form.roadAddress,
            placeholder = "주소·건물명 검색",
            onSelect = { p ->
                st.applyPlace(p)
                scope.launch {
                    val addr = p.address ?: p.name
                    MarketRegion.match(addr, api.http.token())?.let { r -> st.update { copy(region = r.name, regionCode = r.code) } }
                }
            },
            onDismiss = { showSearch = false },
        )
    }
    if (showDate) MarketDatePickerDialog(st.form.availableFrom, onPick = { d -> st.update { copy(availableFrom = d) } }, onDismiss = { showDate = false })
    if (showExit) WizardExitDialog(onLeave = onClose, onDismiss = { showExit = false })
    st.error?.let { msg -> MarketMessageDialog(msg) { st.error = null } }
}
