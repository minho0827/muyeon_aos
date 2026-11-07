package com.muyeon.app.ui.home.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.home.HomeLabels
import com.muyeon.domain.model.home.HomeSpace
import com.muyeon.domain.model.home.HomeTransfer

/** 공간 대관 카드 — 웹 spaceCard: 사진(186) · 이름 · "지역 · 시간당 N원~". */
@Composable
fun HomeSpaceCard(s: HomeSpace, onClick: () -> Unit, modifier: Modifier = Modifier) {
    HomeCard(imageUrl = s.image, fallbackRes = R.drawable.img_noimage, onClick = onClick, modifier = modifier) {
        HomeCardTitle(s.name)
        listOfNotNull(s.region, s.price?.let { stringResource(R.string.home_space_price, HomeLabels.won(it)) }).joinToString(" · ").takeIf { it.isNotEmpty() }?.let { HomeCardSub(it) }
    }
}

/** 무용센터 양도양수 카드 — 웹 transferCard: 사진 · 제목 · "지역 · N평" · "보증금 N만 · 월세 N만"(없으면 협의). */
@Composable
fun HomeTransferCard(t: HomeTransfer, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val man: @Composable (Int?) -> String = { n -> n?.let { stringResource(R.string.home_man, HomeLabels.won(it)) } ?: stringResource(R.string.home_negotiable) }
    HomeCard(imageUrl = t.image, fallbackRes = R.drawable.transfer_noimage, onClick = onClick, modifier = modifier) {
        HomeCardTitle(t.title)
        listOfNotNull(t.region, t.areaPyeong?.let { stringResource(R.string.home_pyeong, formatArea(it)) }).joinToString(" · ").takeIf { it.isNotEmpty() }?.let { HomeCardSub(it) }
        HomeCardSub(stringResource(R.string.home_transfer_money, man(t.deposit), man(t.monthlyRent)))
    }
}

private fun formatArea(a: Double): String = if (a % 1.0 == 0.0) a.toInt().toString() else a.toString()
