package com.muyeon.app.ui.castingmaterial

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.ui.castingmaterial.components.CastingMaterialBatchRow
import com.muyeon.app.ui.common.FormField
import com.muyeon.app.ui.common.FormTextField
import com.muyeon.app.ui.common.LocalRequiredForm
import com.muyeon.app.ui.common.RequiredHint
import com.muyeon.app.ui.common.rememberMultiImageUpload
import com.muyeon.app.ui.common.rememberRequiredForm
import com.muyeon.app.ui.common.requiredField
import com.muyeon.app.ui.components.EmptyView
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.MdButton
import com.muyeon.app.ui.components.ReadSectionTitle
import com.muyeon.app.ui.jobposting.DetailImages
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.castingmaterial.CastingMaterialKind
import com.muyeon.domain.usecase.castingmaterial.CastingMaterialUseCase

/**
 * ★ 2026-11-08 묶음 2 — 캐스팅·공연 자료 업로드(웹 /my/castingMaterials · /my/performanceMaterials, 무용연 운영 계정 전용).
 *   안내 · 캡처 사진(최대 20, 필수 — RequiredForm) · 인스타그램 링크(선택) · 그 밖의 링크(선택) · [AI로 … 등록하기] · 올린 자료 목록.
 *   웹처럼 수정·삭제는 없다(올리면 AI 가 바로 공고·공연으로 공개). 등록된 글은 누르면 그 상세(앱 경로표).
 */
@Composable
fun CastingMaterialsScreen(viewModel: CastingMaterialsViewModel, onBack: () -> Unit, onOpenPath: (String) -> Unit) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val form = rememberRequiredForm()
    val casting = viewModel.kind == CastingMaterialKind.CASTING
    val doneText = stringResource(if (casting) R.string.casting_mat_done_casting else R.string.casting_mat_done_performance)
    val failText = stringResource(R.string.casting_mat_failed)
    val uploadFail = stringResource(R.string.casting_mat_upload_failed)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { if (it == CastingMaterialsEvent.Submitted) Toast.makeText(context, doneText, Toast.LENGTH_LONG).show() }
    }
    val pick = rememberMultiImageUpload(
        max = CastingMaterialUseCase.MAX_IMAGES,
        room = { CastingMaterialUseCase.MAX_IMAGES - viewModel.state.value.images.size },
        onUploading = viewModel::onUploading,
        onUploaded = viewModel::addImages,
        onError = { viewModel.showMessage(it ?: uploadFail) },
    )

    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding().imePadding()) {
        MainTopBar(
            title = stringResource(if (casting) R.string.casting_mat_title_casting else R.string.casting_mat_title_performance),
            primary = false, onBack = onBack,
        )
        when (s.allowed) {
            null -> Unit
            false -> EmptyView(stringResource(R.string.casting_mat_denied))
            true -> CompositionLocalProvider(LocalRequiredForm provides form) {
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(Dimensions.gutter),
                    verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXL),
                ) {
                    Text(
                        stringResource(if (casting) R.string.casting_mat_intro_casting else R.string.casting_mat_intro_performance),
                        style = AppTypography.Body14, color = AppColors.TextSub,
                    )
                    val label = stringResource(R.string.casting_mat_images)
                    Column(Modifier.requiredField(form, KEY_IMAGES, label, s.images.isNotEmpty()), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXS)) {
                        DetailImages(
                            images = s.images, uploading = s.uploading, onAdd = pick, onRemove = viewModel::removeImage,
                            title = label, hint = stringResource(R.string.casting_mat_images_hint), max = CastingMaterialUseCase.MAX_IMAGES,
                        )
                        RequiredHint(form, KEY_IMAGES)
                    }
                    FormField(stringResource(R.string.casting_mat_insta)) {
                        FormTextField(s.instagram, viewModel::onInstagram, stringResource(R.string.casting_mat_insta_ph), max = 500, tag = "castingMat.insta")
                    }
                    FormField(stringResource(R.string.casting_mat_link)) {
                        FormTextField(s.link, viewModel::onLink, stringResource(R.string.casting_mat_link_ph), max = 500, tag = "castingMat.link")
                    }
                    s.message?.let { Text(it, style = AppTypography.Body14, color = AppColors.Danger) }
                    MdButton(
                        stringResource(
                            when {
                                s.uploading -> R.string.casting_mat_uploading
                                s.saving -> R.string.casting_mat_saving
                                casting -> R.string.casting_mat_submit_casting
                                else -> R.string.casting_mat_submit_performance
                            },
                        ),
                        onClick = { if (form.validate(context)) viewModel.submit(failText) },
                        enabled = !s.uploading && !s.saving, tag = "castingMat.submit",
                    )
                    ReadSectionTitle(stringResource(R.string.casting_mat_list_title))
                    if (s.items.isEmpty()) Text(stringResource(R.string.casting_mat_list_empty), style = AppTypography.Body14, color = AppColors.TextSub)
                    else Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spacingM)) {
                        s.items.forEach { CastingMaterialBatchRow(it, onOpenPath) }
                    }
                }
            }
        }
    }
}

private const val KEY_IMAGES = "castingMat.images"
