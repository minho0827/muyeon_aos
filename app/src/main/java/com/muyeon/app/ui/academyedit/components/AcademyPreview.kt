package com.muyeon.app.ui.academyedit.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.components.ImageCarousel
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.components.Pill
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.academy.AcademyLinkedLesson
import com.muyeon.domain.model.academy.AcademyProfileForm

/**
 * 저장 전 학원 정보 미리보기 — 웹 AcademyProfilePreviewModal(실제 공개 프로필 구성, 기능 버튼 없음).
 *   사진 넘김 · 학원명 · 장르·지역 · #키워드 · 학원 소개 · 운영시간 · 수업 안내 · 시설(+주차) · 위치 · 홈페이지. 빈 칸은 웹 안내 문구.
 */
@Composable
fun AcademyPreview(form: AcademyProfileForm, keywords: List<String>, lessons: List<AcademyLinkedLesson>, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(R.string.academy_preview_title), primary = false, onBack = onClose)
        Row(
            Modifier.fillMaxWidth().background(AppColors.PrimarySoft).padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingM),
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingS),
        ) {
            Icon(Icons.Outlined.Info, null, tint = AppColors.Primary, modifier = Modifier.size(Dimensions.iconS))
            Text(stringResource(R.string.academy_preview_banner), style = AppTypography.Sub13, color = AppColors.TextBody)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            ImageCarousel(form.academyImages, onOpen = {}, fallbackRes = R.drawable.img_noimage)
            Column(Modifier.padding(Dimensions.gutter), verticalArrangement = Arrangement.spacedBy(Dimensions.sectionGap)) {
                PreviewHead(form, keywords)
                PreviewSection(R.string.academy_preview_intro) { PreviewCopy(form.academyIntro, R.string.academy_preview_intro_ph) }
                PreviewSection(R.string.academy_preview_hours) { PreviewCopy(form.academyBusinessHours, R.string.academy_preview_hours_ph) }
                PreviewSection(R.string.academy_preview_lessons) { AcademyLessonStrip(lessons) }
                PreviewSection(R.string.academy_preview_facilities) {
                    if (form.academyFacilities.isNotEmpty()) AcademyFacilityGrid(form.academyFacilities)
                    else PreviewCopy("", R.string.academy_preview_facilities_ph)
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimensions.buttonRadius)).background(AppColors.SurfaceMuted).padding(Dimensions.spacingL),
                        verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXS),
                    ) {
                        Text(stringResource(R.string.academy_preview_parking), style = AppTypography.Body14SemiBold, color = AppColors.TextHead)
                        PreviewCopy(form.academyParkingInfo, R.string.academy_preview_parking_ph)
                    }
                }
                PreviewSection(R.string.academy_preview_location) {
                    PreviewCopy(form.academyAddress.ifBlank { form.academyRegion }, R.string.academy_preview_location_ph)
                }
                PreviewSection(R.string.academy_preview_link) { PreviewCopy(form.academyHomepage, R.string.academy_preview_link_ph) }
                Spacer(Modifier.height(Dimensions.spacingXL))
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PreviewHead(form: AcademyProfileForm, keywords: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
        Text(form.academyName.ifBlank { stringResource(R.string.academy_preview_name_ph) }, style = AppTypography.Title22, color = AppColors.TextHead)
        Text(
            (form.academyGenres + form.academyRegion).filter { it.isNotBlank() }.joinToString(" · ").ifEmpty { stringResource(R.string.academy_preview_sub_ph) },
            style = AppTypography.Body14, color = AppColors.TextSub,
        )
        val tags = keywords.ifEmpty { listOf(stringResource(R.string.academy_preview_keyword_ph)) }
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingXS),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXS),
        ) { tags.forEach { Pill("#$it", com.muyeon.app.ui.theme.CategoryColors.CardTag) } }
    }
}

@Composable
private fun PreviewSection(title: Int, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spacingM)) {
        Text(stringResource(title), style = AppTypography.Section16, color = AppColors.TextHead)
        content()
    }
}

@Composable
private fun PreviewCopy(text: String, placeholder: Int) {
    Text(text.ifBlank { stringResource(placeholder) }, style = AppTypography.Body14, color = if (text.isBlank()) AppColors.TextHint else AppColors.TextBody)
}
