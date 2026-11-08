package com.muyeon.app.ui.communitywrite

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.ui.common.FormField
import com.muyeon.app.ui.common.FormTextField
import com.muyeon.app.ui.common.LocalRequiredForm
import com.muyeon.app.ui.common.MuyeonBottomButton
import com.muyeon.app.ui.common.UploadImagePreparer
import com.muyeon.app.ui.common.rememberRequiredForm
import com.muyeon.app.ui.community.CommunityLabels
import com.muyeon.app.ui.communitywrite.components.CommunityPhotoStrip
import com.muyeon.app.ui.components.GuideBanner
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.subwizard.components.SubChipGroup
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.app.utils.TokenManager
import com.muyeon.domain.usecase.write.WriteCommunityPostUseCase
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * ★ 2026-11-08 묶음 3 — 커뮤니티 글쓰기/수정(웹 CommunityWrite 1:1 항목·순서·문구).
 *   안내 배너 → 카테고리(필수) → 제목(필수 · 50자) → 내용(필수 · 1000자) → 사진 첨부(선택 · 최대 10장) → [등록하기].
 *   빠진 칸은 RequiredForm(스크롤·빨강·토스트). 저장되면 완료 팝업 → [onDone](웹: /community 로 바꿔 이동).
 *   사진은 기존 앱 업로더(UploadImagePreparer + /uploads/image) — iOS Shell/CommunityWrite/CommunityWriteScreen.swift 1:1.
 */
@Composable
fun CommunityWriteScreen(
    editId: Long?,
    onBack: () -> Unit,
    onDone: () -> Unit,
    vm: CommunityWriteViewModel = koinViewModel { parametersOf(editId) },
) {
    val s by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val form = rememberRequiredForm()
    BackHandler(onBack = onBack)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(maxItems = WriteCommunityPostUseCase.IMAGES_MAX)) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        vm.onUploading(true)
        scope.launch {
            val api = com.muyeon.app.ui.jobposting.JobPostingApi(TokenManager.getAccessToken(context))
            uris.take((WriteCommunityPostUseCase.IMAGES_MAX - vm.state.value.images.size).coerceAtLeast(0)).forEach { u ->
                val bytes = UploadImagePreparer.prepare(context, u)
                if (bytes == null) vm.showUploadError(null)
                else api.uploadImage(bytes).onSuccess(vm::addImage).onFailure { vm.showUploadError(it.message) }
            }
            vm.onUploading(false)
        }
    }

    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding().imePadding()) {
        MainTopBar(stringResource(if (vm.isEdit) R.string.b3_cwrite_title_edit else R.string.b3_cwrite_title_new), primary = false, onBack = onBack)
        if (s.loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(color = AppColors.Primary) }
            return@Column
        }
        CompositionLocalProvider(LocalRequiredForm provides form) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Dimensions.gutter).padding(bottom = Dimensions.spacingXXL),
                verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXXL),
            ) {
                GuideBanner(stringResource(R.string.b3_cwrite_guide), icon = "♥", modifier = Modifier.padding(top = Dimensions.spacingL))
                FormField(stringResource(R.string.b3_cwrite_category), required = true, filled = s.category.isNotEmpty()) {
                    SubChipGroup(WriteCommunityPostUseCase.CATEGORIES, listOfNotNull(s.category.ifEmpty { null }),
                        { stringResource(CommunityLabels.category(it)) }) { vm.onCategory(it) }
                }
                FormField(stringResource(R.string.b3_cwrite_f_title), required = true, filled = s.title.isNotBlank()) {
                    FormTextField(s.title, vm::onTitle, stringResource(R.string.b3_cwrite_title_hint), max = WriteCommunityPostUseCase.TITLE_MAX, tag = "cwrite.title")
                    Counter(s.title.length, WriteCommunityPostUseCase.TITLE_MAX)
                    Hint(stringResource(R.string.b3_cwrite_title_example))
                }
                FormField(stringResource(R.string.b3_cwrite_f_body), required = true, filled = s.body.isNotBlank()) {
                    FormTextField(s.body, vm::onBody, stringResource(R.string.b3_cwrite_body_hint), max = WriteCommunityPostUseCase.BODY_MAX, tag = "cwrite.body", minLines = 8)
                    Counter(s.body.length, WriteCommunityPostUseCase.BODY_MAX)
                    Hint(stringResource(R.string.b3_cwrite_body_example))
                }
                FormField(stringResource(R.string.b3_cwrite_photos)) {
                    CommunityPhotoStrip(s.images, WriteCommunityPostUseCase.IMAGES_MAX, s.uploading,
                        onAdd = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        onRemove = vm::removeImage)
                    Hint(stringResource(R.string.b3_cwrite_photo_note))
                }
                (s.error ?: s.errorRes?.let { stringResource(it) })?.let { Text(it, style = AppTypography.Body14, color = AppColors.Danger) }
            }
        }
        MuyeonBottomButton(
            stringResource(if (s.uploading) R.string.b3_cwrite_uploading else R.string.b3_cwrite_submit),
            "cwrite.submit", enabled = !s.uploading, loading = s.submitting,
        ) { if (form.validate(context)) vm.submit() }
    }
    CustomDialog(
        title = stringResource(if (vm.isEdit) R.string.b3_cwrite_done_edit else R.string.b3_cwrite_done_new),
        content = null, leftButtonText = null, rightButtonText = stringResource(R.string.common_confirm),
        buttonCount = 1, alignment = ContentAlignment.Middle,
        onDismiss = onDone, onRightButtonClick = onDone, showPopup = s.done,
    )
}

@Composable
private fun Counter(n: Int, max: Int) =
    Text(stringResource(R.string.b3_cwrite_count, n, max), style = AppTypography.Caption12, color = AppColors.TextHint, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())

@Composable
private fun Hint(text: String) = Text(text, style = AppTypography.Caption12, color = AppColors.TextSub)
