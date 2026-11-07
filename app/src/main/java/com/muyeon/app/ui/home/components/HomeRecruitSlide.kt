package com.muyeon.app.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.Pill
import com.muyeon.app.ui.home.HomeLabels
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.domain.model.home.HomeRecruit

/** 장르별 강사 구인 카드 — 웹 recruitSlide: 사진 · 제목 · [채용/대타][장르] · "지역 · 급여". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeRecruitSlide(r: HomeRecruit, onClick: () -> Unit, modifier: Modifier = Modifier) {
    HomeCard(imageUrl = r.image, fallbackRes = R.drawable.img_noimage, onClick = onClick, modifier = modifier) {
        HomeCardTitle(r.title ?: stringResource(R.string.home_recruit_default))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Pill(stringResource(if (r.kind == HomeRecruit.Kind.JOB) R.string.home_kind_job else R.string.home_kind_sub), CategoryColors.Recruit)
            r.genre?.let { Pill(it, CategoryColors.genre(it)) }
        }
        val pay = HomeLabels.salary(r.salary)?.let { stringResource(it) } ?: r.pay
        listOfNotNull(r.region, pay).joinToString(" · ").takeIf { it.isNotEmpty() }?.let { HomeCardSub(it) }
    }
}
