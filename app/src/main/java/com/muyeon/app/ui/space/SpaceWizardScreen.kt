package com.muyeon.app.ui.space

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
import com.muyeon.app.ui.space.wizard.*
import kotlinx.coroutines.launch

/**
 * 공간 등록·수정 위저드(호스트) — 사진 → 위치(카카오 검색) → 기본(이름·카테고리·인원·추가 요금) → 가격(시간당 + 세부공간)
 *  → 운영(영업시간·요일·최소 시간·예약 마감) → 시설·주의사항·환불 규정(기본값 미리 채움) → 소개.
 *  각 단계는 wizard/ 의 단계 컴포넌트, 이 화면은 상태 연결(불러오기·사진 올리기·저장)과 단계 고르기만 한다.
 */
@Composable
fun SpaceWizardScreen(
    api: SpaceApi,
    editId: Int?,
    onClose: () -> Unit,
    onSaved: (Int) -> Unit,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val st = remember(editId) { SpaceWizardState(editId) }
    var showExit by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var categories by remember { mutableStateOf<List<SpaceCategory>>(emptyList()) }
    // 환불 단계(서버). null = 불러오는 중(같은 크기 자리), 실패하면 단계 없이 직접 설정만.
    var refundPresets by remember { mutableStateOf<SpaceRefundPresets?>(null) }
    // 호스트 이용 동의 — 새 공간만. null = 동의 단계 없음(이미 동의·수정·확인 실패), 값 = 동의 단계를 보인다.
    var consentDocs by remember { mutableStateOf<List<SpaceConsentDoc>?>(null) }
    var consentChecking by remember { mutableStateOf(editId == null) }
    var consentSubmitting by remember { mutableStateOf(false) }

    suspend fun loadConsent() {
        api.hostConsent().onSuccess { c -> consentDocs = if (c.agreed) null else c.docs }
        consentChecking = false
    }

    LaunchedEffect(editId) {
        if (editId == null) loadConsent()
        api.categories().onSuccess { categories = it }
        refundPresets = api.refundPresets().getOrElse {
            // 단계를 못 읽으면 직접 설정만(새 공간은 기본 보통 숫자로 시작).
            if (st.form.refundPreset != null) st.update { copy(refundPreset = null) }
            SpaceRefundPresets("STANDARD", emptyList(), SPACE_REFUND_POLICY)
        }
        if (editId != null) {
            api.detail(editId).onSuccess { st.form = SpaceForm.from(it); st.baseline = st.form }.onFailure { st.error = it.message }
            st.loading = false
        }
    }
    LaunchedEffect(st.step) { scroll.scrollTo(0) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = SPACE_MAX_IMAGES)) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        st.uploading = true
        scope.launch {
            var list = st.form.images
            uris.take((SPACE_MAX_IMAGES - list.size).coerceAtLeast(0)).forEach { u ->
                UploadImagePreparer.prepare(ctx, u)?.let { bytes -> api.uploadImage(bytes).onSuccess { list = list + it }.onFailure { st.error = it.message } }
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
            api.agreeHostConsent().onSuccess { consentDocs = null }.onFailure { st.error = it.message ?: "동의를 저장하지 못했어요." }
            consentSubmitting = false
        }
    }

    fun save() {
        if (st.form.name.isBlank()) { st.step = 2; st.error = "공간 이름을 입력해 주세요."; return }
        if (st.form.region.isBlank()) { st.step = 1; st.error = "지역을 선택해 주세요."; return }
        st.saving = true
        scope.launch {
            api.save(editId, st.formForSave().toJson()).onSuccess { st.baseline = st.form; onSaved(it) }.onFailure { e ->
                // 동의 전 등록(403 HOST_CONSENT_REQUIRED) — 동의 단계로 되돌린다(작성한 내용은 그대로).
                if ((e as? ApiMessageError)?.code == HOST_CONSENT_REQUIRED) {
                    api.hostConsent().onSuccess { c -> consentDocs = c.docs }.onFailure { consentDocs = emptyList() }
                } else st.error = e.message ?: "저장에 실패했어요."
            }
            st.saving = false
        }
    }

    Column(Modifier.fillMaxSize().background(Color.White)) {
        // 단계 고르기 한 번 — 불러오는 중 / 호스트 이용 동의 / 등록 단계.
        val docs = consentDocs
        if (st.loading || consentChecking) {
            // 새 공간은 동의 확인 중 — 진행 줄은 자리만(동의 단계가 뜰 수도 있어서).
            WizardHeader(st.title, SPACE_STEPS, st.step, onClose = { close() }, showProgress = editId != null)
            WizardLoading()
            return@Column
        }
        if (docs != null) {
            WizardHeader(st.title, SPACE_STEPS, 0, onClose = { close() }, showProgress = false)
            SpaceStepHostConsent(docs, consentSubmitting, onAgree = { agreeConsent() })
            return@Column
        }
        WizardHeader(st.title, SPACE_STEPS, st.step, onClose = { close() })

        Column(
            Modifier.weight(1f).verticalScroll(scroll).padding(horizontal = 20.dp).padding(top = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            val f = st.form
            val set: (SpaceForm) -> Unit = { st.form = it }
            when (st.step) {
                0 -> SpaceStepPhotos(
                    f.images, st.uploading,
                    onAdd = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    onRemove = { i -> st.update { copy(images = images.filterIndexed { idx, _ -> idx != i }) } },
                )
                1 -> SpaceStepLocation(f, api.http.token(), onSearch = { showSearch = true }, onChange = set)
                2 -> SpaceStepBasic(f, categories, set)
                3 -> SpaceStepPrice(f, set)
                4 -> SpaceStepOperation(f, set)
                5 -> SpaceStepRules(f, refundPresets, set)
                else -> SpaceStepIntro(f.intro) { v -> st.update { copy(intro = v) } }
            }
        }

        WizardNavBar(
            showPrev = st.step > 0, nextLabel = st.nextLabel, nextEnabled = st.canNext, busy = st.saving,
            onPrev = { st.step -= 1 }, onNext = { if (st.isLast) save() else st.step += 1 },
        )
    }

    if (showSearch) {
        LocationSearchSheet(
            initialQuery = st.form.roadAddress, placeholder = "주소·건물명 검색",
            onSelect = { p ->
                st.applyPlace(p)
                val addr = p.address ?: p.name
                scope.launch { MarketRegion.match(addr, api.http.token())?.let { r -> st.update { copy(region = r.name, regionCode = r.code) } } }
            },
            onDismiss = { showSearch = false },
        )
    }
    if (showExit) WizardExitDialog(onLeave = onClose, onDismiss = { showExit = false })
    st.error?.let { msg -> MarketMessageDialog(msg) { st.error = null } }
}
