package com.muyeon.app.ui.academyedit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.RemoteImage
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.academy.AcademyLinkedLesson

/** 미리보기 '수업 안내' — 가로로 넘기는 수업 카드(웹 .academy-lesson-card), 없으면 '운영 중인 수업을 올려보세요'. */
@Composable
fun AcademyLessonStrip(lessons: List<AcademyLinkedLesson>) {
    if (lessons.isEmpty()) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(Dimensions.buttonRadius)).background(AppColors.SurfaceMuted).padding(Dimensions.spacingXL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXS),
        ) {
            Text(stringResource(R.string.academy_preview_lessons_empty), style = AppTypography.Body14SemiBold, color = AppColors.TextHead)
            Text(stringResource(R.string.academy_preview_lessons_empty_desc), style = AppTypography.Sub13, color = AppColors.TextSub, textAlign = TextAlign.Center)
        }
        return
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingM)) {
        lessons.forEach { l ->
            Column(
                Modifier.width(CARD_WIDTH).clip(RoundedCornerShape(Dimensions.buttonRadius))
                    .border(1.dp, AppColors.Divider, RoundedCornerShape(Dimensions.buttonRadius)),
            ) {
                RemoteImage(l.image, Modifier.fillMaxWidth().aspectRatio(1f), fallbackRes = R.drawable.img_noimage)
                Column(Modifier.padding(Dimensions.spacingM), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXXS)) {
                    Text(l.title ?: stringResource(R.string.academy_preview_lesson_default), style = AppTypography.Body14SemiBold, color = AppColors.TextHead, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        listOfNotNull(l.genre, l.scheduleText).filter { it.isNotBlank() }.joinToString(" · ").ifEmpty { stringResource(R.string.academy_preview_lesson_schedule_ph) },
                        style = AppTypography.Caption12, color = AppColors.TextSub, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private val CARD_WIDTH = 160.dp
