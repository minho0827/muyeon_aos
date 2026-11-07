package com.muyeon.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.DmButton
import com.muyeon.app.ui.components.Pill
import com.muyeon.app.ui.home.HomeLabels
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.domain.model.teacher.TeacherCard

/**
 * 홈 강사 카드 — 웹 Home teacherSlide: 사진(높이 186 · 얼굴 쪽) · 이름 · [추천][장르] 알약 · 오른쪽 DM(비회원·일반 회원).
 *   그 밖 유형은 DM 대신 사진 왼쪽 위 ⋮(추천 안 받기 시트).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeTeacherSlide(
    teacher: TeacherCard,
    showDm: Boolean,
    onClick: () -> Unit,
    onRequest: () -> Unit,
    onBlockMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val g = teacher.genres.firstOrNull()
    HomeCard(
        imageUrl = teacher.image, fallbackRes = R.drawable.img_noimage_teacher, onClick = onClick, modifier = modifier,
        imageAlignment = BiasAlignment(0f, -0.7f),
        overlay = {
            if (!showDm) {
                Box(
                    Modifier.align(Alignment.TopStart).padding(8.dp).size(28.dp).shadow(2.dp, CircleShape).background(AppColors.Surface, CircleShape).clickable(onClick = onBlockMenu),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.MoreVert, stringResource(R.string.home_rec_settings), tint = AppColors.TextSub, modifier = Modifier.size(18.dp)) }
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HomeCardTitle(teacher.name.ifBlank { stringResource(R.string.lesson_teacher_default) })
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (HomeLabels.isBoosted(teacher.promotedUntil)) Pill(stringResource(R.string.home_boosted), CategoryColors.Boosted)
                    g?.let { Pill(it, CategoryColors.genre(it)) }
                }
            }
            if (showDm) DmButton(onClick = onRequest)
        }
    }
}
