package com.muyeon.app.ui.application.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.muyeon.app.R
import com.muyeon.app.ui.application.ApplicationLabels
import com.muyeon.app.ui.common.muyeonPanel
import com.muyeon.app.ui.components.LevelBadge
import com.muyeon.app.ui.format.KstDates
import com.muyeon.app.ui.home.HomeLabels
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.application.ApplicationKind
import com.muyeon.domain.model.application.ApplicationProgress
import com.muyeon.domain.model.application.MyApplication

/**
 * 내 지원 카드 — 웹 MyApplications .my-posting-card(상태 배지 · 종류 · … / 제목 / 지원일·공고 마감·대타 금액 / 미선정 안내).
 */
@Composable
fun MyApplicationCard(a: MyApplication, onClick: (() -> Unit)?, onMore: () -> Unit, modifier: Modifier = Modifier) {
    val (label, level) = ApplicationLabels.progress(a)
    Column(
        modifier.muyeonPanel().let { if (onClick != null) it.clickable(onClick = onClick) else it },
        verticalArrangement = Arrangement.spacedBy(Dimensions.spacingS),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingXS)) {
            LevelBadge(stringResource(label), level)
            if (a.kind != ApplicationKind.JOB) LevelBadge(stringResource(ApplicationLabels.kind(a.kind)), CategoryColors.Level.End)
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(Dimensions.iconL).clickable(onClick = onMore), Alignment.Center) {
                Icon(Icons.Filled.MoreHoriz, stringResource(R.string.common_more_menu), tint = AppColors.TextSub)
            }
        }
        Text(a.title ?: stringResource(R.string.application_posting), style = AppTypography.CardTitle16, color = AppColors.TextHead, maxLines = 2, overflow = TextOverflow.Ellipsis)
        val meta = buildList {
            add(stringResource(R.string.application_applied_at, KstDates.dot(a.appliedAt)))
            when (a.postingStatus) {
                "CLOSED" -> add(stringResource(R.string.application_posting_closed))
                "ARCHIVED" -> add(stringResource(R.string.application_posting_deleted))
                else -> a.postingDeadline?.let { d -> KstDates.localDate(d)?.let { add(stringResource(R.string.application_deadline, KstDates.dot(d))) } }
            }
            if (a.kind == ApplicationKind.SUB && (a.acceptedPay ?: 0) > 0 && a.status in setOf("APPLIED", "REVIEWING", "OFFERED")) {
                val eff = maxOf(a.acceptedPay ?: 0, a.postingCurrentPay ?: 0)
                add(if (eff > (a.acceptedPay ?: 0)) stringResource(R.string.application_pay_raised, HomeLabels.won(a.acceptedPay!!), HomeLabels.won(eff))
                else stringResource(R.string.application_pay, HomeLabels.won(a.acceptedPay!!)))
            }
        }
        Text(meta.joinToString(" · "), style = AppTypography.Sub13, color = AppColors.TextSub)
        if (a.progress == ApplicationProgress.REJECTED) {
            Text(stringResource(ApplicationLabels.rejectedMessage(a)), style = AppTypography.Sub13, color = AppColors.TextBody,
                modifier = Modifier.fillMaxWidth().padding(top = Dimensions.spacingXXS))
        }
    }
}
