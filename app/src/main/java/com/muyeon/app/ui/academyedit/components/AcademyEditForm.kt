package com.muyeon.app.ui.academyedit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.academyedit.AcademyEditOptions
import com.muyeon.app.ui.academyedit.AcademyEditUiState
import com.muyeon.app.ui.academyedit.AcademyEditViewModel
import com.muyeon.app.ui.common.FormField
import com.muyeon.app.ui.common.FormReadOnlyBox
import com.muyeon.app.ui.common.FormTextField
import com.muyeon.app.ui.common.RequiredForm
import com.muyeon.app.ui.common.RequiredHint
import com.muyeon.app.ui.common.requiredField
import com.muyeon.app.ui.jobposting.DetailImages
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.usecase.academy.AcademyProfileUseCase

/** 학원 정보 관리 양식 — 웹 섹션 3개(공개 프로필 · 위치와 운영 안내 · 맞춤 강사 추천 조건), 칸 순서·문구·글자 수 그대로. */
@Composable
fun AcademyEditForm(s: AcademyEditUiState, form: RequiredForm, vm: AcademyEditViewModel, onAddImage: () -> Unit) {
    val f = s.form
    // ① 공개 프로필
    Section {
        AcademyEditSectionHead(stringResource(R.string.academy_edit_public_title), stringResource(R.string.academy_edit_public_desc))
        val imagesLabel = stringResource(R.string.academy_edit_images_label)
        Column(Modifier.requiredField(form, KEY_IMAGES, imagesLabel, f.academyImages.isNotEmpty()), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXS)) {
            DetailImages(
                images = f.academyImages, uploading = s.uploading, onAdd = onAddImage, onRemove = vm::removeImage,
                title = stringResource(R.string.academy_edit_images), hint = stringResource(R.string.academy_edit_images_hint),
                max = AcademyProfileUseCase.MAX_IMAGES,
            )
            RequiredHint(form, KEY_IMAGES)
        }
        FormField(stringResource(R.string.academy_edit_name), required = true, filled = f.academyName.isNotBlank()) {
            FormTextField(f.academyName, { v -> vm.edit { copy(academyName = v) } }, stringResource(R.string.academy_edit_name_ph), max = 40, tag = "academyEdit.name")
        }
        FormField(stringResource(R.string.academy_edit_owner)) {
            FormReadOnlyBox(s.ownerName.ifBlank { stringResource(R.string.academy_edit_owner_ph) })
            AcademyFieldGuide(stringResource(R.string.academy_edit_owner_guide))
        }
        AcademyCheckGroup(
            stringResource(R.string.academy_edit_genres), AcademyEditOptions.genres, f.academyGenres,
            onToggle = { v -> vm.edit { copy(academyGenres = vm.toggle(academyGenres, v)) } },
            hint = stringResource(R.string.academy_edit_genres_guide),
        )
        FormField(stringResource(R.string.academy_edit_intro), required = true, filled = f.academyIntro.isNotBlank()) {
            FormTextField(f.academyIntro, { v -> vm.edit { copy(academyIntro = v) } }, stringResource(R.string.academy_edit_intro_ph), max = AcademyProfileUseCase.MAX_INTRO, tag = "academyEdit.intro", minLines = 6)
        }
        FormField(stringResource(R.string.academy_edit_keywords)) {
            FormTextField(s.keywordText, vm::onKeywords, stringResource(R.string.academy_edit_keywords_ph), max = 160, tag = "academyEdit.keywords")
            AcademyFieldGuide(stringResource(R.string.academy_edit_keywords_guide))
        }
    }
    // ② 위치와 운영 안내
    Section {
        AcademyEditSectionHead(stringResource(R.string.academy_edit_location_title))
        FormField(stringResource(R.string.academy_edit_region)) {
            FormTextField(f.academyRegion, { v -> vm.edit { copy(academyRegion = v) } }, stringResource(R.string.academy_edit_region_ph), max = 50, tag = "academyEdit.region")
        }
        FormField(stringResource(R.string.academy_edit_address)) {
            FormTextField(f.academyAddress, { v -> vm.edit { copy(academyAddress = v) } }, stringResource(R.string.academy_edit_address_ph), max = 120, tag = "academyEdit.address")
        }
        FormField(stringResource(R.string.academy_edit_hours)) {
            FormTextField(f.academyBusinessHours, { v -> vm.edit { copy(academyBusinessHours = v) } }, stringResource(R.string.academy_edit_hours_ph), max = 500, tag = "academyEdit.hours", minLines = 4)
        }
        AcademyCheckGroup(
            stringResource(R.string.academy_edit_facilities), AcademyEditOptions.facilities, f.academyFacilities,
            onToggle = { v -> vm.edit { copy(academyFacilities = vm.toggle(academyFacilities, v)) } },
        )
        FormField(stringResource(R.string.academy_edit_parking)) {
            FormTextField(f.academyParkingInfo, { v -> vm.edit { copy(academyParkingInfo = v) } }, stringResource(R.string.academy_edit_parking_ph), max = 300, tag = "academyEdit.parking", minLines = 3)
        }
        FormField(stringResource(R.string.academy_edit_homepage)) {
            FormTextField(f.academyHomepage, { v -> vm.edit { copy(academyHomepage = v) } }, stringResource(R.string.academy_edit_homepage_ph), max = 200, tag = "academyEdit.homepage")
            AcademyFieldGuide(stringResource(R.string.academy_edit_homepage_guide))
        }
    }
    // ③ 맞춤 강사 추천 조건 — 장르·분야 중 하나 이상(웹 save 확인). 두 묶음을 한 필수 칸으로 본다.
    Section {
        AcademyEditSectionHead(stringResource(R.string.academy_edit_match_title), stringResource(R.string.academy_edit_match_desc))
        val wantLabel = stringResource(R.string.academy_edit_want_label)
        Column(
            Modifier.requiredField(form, KEY_WANT, wantLabel, f.wantGenres.isNotEmpty() || f.wantFields.isNotEmpty()),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXL),
        ) {
            AcademyCheckGroup(
                stringResource(R.string.academy_edit_want_genres), AcademyEditOptions.genres, f.wantGenres,
                onToggle = { v -> vm.edit { copy(wantGenres = vm.toggle(wantGenres, v)) } },
            )
            AcademyCheckGroup(
                stringResource(R.string.academy_edit_want_fields), AcademyEditOptions.fields, f.wantFields,
                onToggle = { v -> vm.edit { copy(wantFields = vm.toggle(wantFields, v)) } },
            )
            RequiredHint(form, KEY_WANT)
        }
    }
}

@Composable
private fun Section(content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXL)) { content() }
}

private const val KEY_IMAGES = "academyEdit.images"
private const val KEY_WANT = "academyEdit.want"
