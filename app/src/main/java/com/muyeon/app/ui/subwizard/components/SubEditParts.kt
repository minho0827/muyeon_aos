package com.muyeon.app.ui.subwizard.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** ★ 2026-11-08 묶음 3 — 대타공고 수정: 기존 공고를 읽는 중(돌기) · 못 읽음(웹 '공고를 불러오지 못했어요.'). iOS SubEditParts.swift 1:1. */
@Composable
fun SubEditLoading(failed: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(Dimensions.gutter), Alignment.Center) {
        if (failed) Text(stringResource(R.string.b3_subedit_load_failed), style = AppTypography.Body14, color = AppColors.TextSub, textAlign = TextAlign.Center)
        else CircularProgressIndicator(color = AppColors.Primary)
    }
}

/** 수정 화면 맨 위 안내 — 긴급 발송 중(급여 칸 없음) · 확정된 공고(급여 잠금). 둘 다 아니면 없음. */
@Composable
fun SubEditNotice(dispatchLocked: Boolean, payLocked: Boolean) {
    val res = when {
        payLocked -> R.string.b3_subedit_pay_locked
        dispatchLocked -> R.string.b3_subedit_dispatch
        else -> return
    }
    Text(stringResource(res), style = AppTypography.Body14, color = AppColors.TextSub, modifier = Modifier.padding(top = Dimensions.spacingM))
}
