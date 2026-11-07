package com.muyeon.app.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.Pill
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.domain.model.home.HomeMyJob

/** 우리 학원 강사 모집 공고 카드 — 웹 jobSlide: 사진 3:2 · [모집중][장르] · 제목 · "지역 · 요일 · ~마감" · "지원 N · 조회 N". */
@Composable
fun HomeMyJobSlide(j: HomeMyJob, onClick: () -> Unit, modifier: Modifier = Modifier) {
    HomeCard(imageUrl = j.imageUrl, fallbackRes = R.drawable.img_noimage, onClick = onClick, modifier = modifier, image = HomeImageSize.Ratio(3f / 2f)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Pill(stringResource(R.string.home_job_open), CategoryColors.Recruit)
            j.genre?.let { Pill(it, CategoryColors.genre(it)) }
        }
        HomeCardTitle(j.title ?: stringResource(R.string.home_recruit_default))
        listOfNotNull(j.region, j.days, j.deadline?.takeIf { it != "-" }?.let { "~" + it.take(10) }).joinToString(" · ").takeIf { it.isNotEmpty() }?.let { HomeCardSub(it) }
        HomeCardSub(stringResource(R.string.home_job_stats, j.applicants, j.views))
    }
}

/** 빈 자리 안내 카드 — 웹 emptyPostingSlide(기본 그림 3:2 · 제목 · 설명). 누르지 않는다. */
@Composable
fun HomeEmptyCard(title: String, desc: String, modifier: Modifier = Modifier) {
    HomeCard(imageUrl = null, fallbackRes = R.drawable.img_noimage, onClick = {}, modifier = modifier, image = HomeImageSize.Ratio(3f / 2f)) {
        HomeCardTitle(title)
        HomeCardSub(desc)
    }
}
