package com.muyeon.app.ui.dancer.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.components.HeartButton
import com.muyeon.app.ui.components.LevelBadge
import com.muyeon.app.ui.components.ListRow
import com.muyeon.app.ui.resume.ResumeOptions
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.domain.model.dancer.DancerCard

/** 무용수 한 줄 — 웹 DancerList ListRow(전공·성별·영상·수상 배지 · "전공 무용수 · 키" · 지역·경력 · 찜). */
@Composable
fun DancerRow(d: DancerCard, liked: Boolean, onClick: () -> Unit, onLike: () -> Unit, showDivider: Boolean = true) {
    val major = d.major ?: stringResource(R.string.dancer_major_default)
    ListRow(
        title = listOfNotNull(stringResource(R.string.dancer_title, major), d.height).joinToString(" · "),
        onClick = onClick,
        sub = listOfNotNull(d.region, d.career?.let(ResumeOptions::careerLabel)?.takeIf { it.isNotBlank() }).joinToString(" · "),
        thumbUrl = d.image,
        thumbFallbackRes = R.drawable.img_noimage_teacher,
        tags = {
            LevelBadge(major, CategoryColors.Level.High)
            d.gender?.let { LevelBadge(it, CategoryColors.Level.Middle) }
            if (d.hasVideo) LevelBadge(stringResource(R.string.dancer_video), CategoryColors.Level.Row)
            if (d.hasAward) LevelBadge(stringResource(R.string.dancer_award), CategoryColors.Level.Row)
        },
        trailing = { HeartButton(liked, onLike) },
        showDivider = showDivider,
    )
}
