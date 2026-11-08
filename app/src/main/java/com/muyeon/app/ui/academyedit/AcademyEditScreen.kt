package com.muyeon.app.ui.academyedit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.ui.academyedit.components.AcademyEditForm
import com.muyeon.app.ui.academyedit.components.AcademyPreview
import com.muyeon.app.ui.assistant.AssistantDraft
import com.muyeon.app.ui.assistant.str
import com.muyeon.app.ui.common.LocalRequiredForm
import com.muyeon.app.ui.common.rememberMultiImageUpload
import com.muyeon.app.ui.common.rememberRequiredForm
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.MdButton
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.usecase.academy.AcademyProfileUseCase
import org.koin.androidx.compose.koinViewModel

/**
 * ★ 2026-11-08 묶음 2 — 학원 정보 관리(웹 /academyProfile · pages/muyeon/my/AcademyProfile.js).
 *   위 바 오른쪽 (i) = 학원 정보 미리보기(웹 헤더 preview-info). 공개 프로필 · 위치와 운영 안내 · 맞춤 강사 추천 조건 →
 *   [취소][저장]. 필수(사진·학원명·학원 소개·원하는 장르/분야)는 RequiredForm(버튼은 늘 눌리고 빠진 칸으로 스크롤·빨강·토스트).
 *   저장 결과 팝업 → 성공이면 MY(웹 navigate('/mypage')). ?aiDraft= 면 불러온 뒤 학원 소개를 AI 초안으로(웹 useAiDraft).
 */
@Composable
fun AcademyEditScreen(
    aiDraftId: String?,
    onBack: () -> Unit,
    onMy: () -> Unit,
    viewModel: AcademyEditViewModel = koinViewModel(),
) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val form = rememberRequiredForm()
    val savedText = stringResource(R.string.academy_edit_saved)
    val failText = stringResource(R.string.academy_edit_save_failed)
    val uploadFail = stringResource(R.string.academy_edit_upload_failed)
    var aiApplied by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { e ->
            when (e) {
                AcademyEditEvent.GoMy -> onMy()
                is AcademyEditEvent.Saved -> AssistantDraft.saved(context, aiDraftId, "ACADEMY_PROFILE", null, e.intro)
            }
        }
    }
    // AI 도우미 초안 — 기존 정보를 다 불러온 뒤 한 번만(웹 useAiDraft(…, loaded)).
    LaunchedEffect(aiDraftId, s.loaded) {
        if (aiDraftId == null || aiApplied || !s.loaded) return@LaunchedEffect
        aiApplied = true
        val p = AssistantDraft.payload(context, aiDraftId) ?: return@LaunchedEffect
        viewModel.applyAiDraft(p.str("academyIntro"), p.str("facilitiesText"))
    }
    val pick = rememberMultiImageUpload(
        max = AcademyProfileUseCase.MAX_IMAGES,
        room = { AcademyProfileUseCase.MAX_IMAGES - viewModel.state.value.form.academyImages.size },
        onUploading = viewModel::onUploading,
        onUploaded = viewModel::addImages,
        onError = { viewModel.showMessage(it ?: uploadFail) },
    )

    if (s.previewOpen) {
        AcademyPreview(s.form, AcademyProfileUseCase.toKeywords(s.keywordText), s.lessons, onClose = { viewModel.setPreview(false) })
        return
    }
    BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding().imePadding()) {
        MainTopBar(title = stringResource(R.string.academy_edit_title), primary = false, onBack = onBack) {
            IconButton(onClick = { viewModel.setPreview(true) }) {
                Icon(Icons.Outlined.Info, stringResource(R.string.academy_edit_preview), tint = AppColors.Primary, modifier = Modifier.size(Dimensions.iconM))
            }
        }
        if (!s.loaded) return@Column
        CompositionLocalProvider(LocalRequiredForm provides form) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Dimensions.gutter),
                verticalArrangement = Arrangement.spacedBy(Dimensions.sectionGap),
            ) {
                AcademyEditForm(s, form, viewModel, onAddImage = pick)
                Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
                    MdButton(stringResource(R.string.common_cancel), onClick = onBack, modifier = Modifier.weight(1f), filled = false, tag = "academyEdit.cancel")
                    MdButton(
                        stringResource(if (s.uploading) R.string.academy_edit_uploading else R.string.common_save),
                        onClick = { if (form.validate(context)) viewModel.save(savedText, failText) },
                        modifier = Modifier.weight(1f), enabled = !s.uploading, loading = s.saving, tag = "academyEdit.save",
                    )
                }
            }
        }
    }
    s.resultMessage?.let { m ->
        CustomDialog(
            title = m, content = null, leftButtonText = null, rightButtonText = stringResource(R.string.common_confirm),
            buttonCount = 1, alignment = ContentAlignment.Middle, onDismiss = viewModel::dismissResult, onRightButtonClick = viewModel::dismissResult, showPopup = true,
        )
    }
}
