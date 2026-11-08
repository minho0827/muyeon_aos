package com.muyeon.app.ui.application.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.application.ApplicationLabels
import com.muyeon.app.ui.components.Avatar
import com.muyeon.app.ui.components.LevelBadge
import com.muyeon.app.ui.format.timeAgo
import com.muyeon.app.ui.home.HomeLabels
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.application.ApplicationKind
import com.muyeon.domain.model.application.ReceivedApplication

/**
 * 받은 지원 한 줄 — 웹 ReceivedApplications ListRow(지원자 사진 · 종류·긴급·상태 배지 · 이름 · 공고·시각·대타 금액).
 *   끝: 휴지통이면 [복원], 지원 취소된 건 '열람 불가', 아니면 (새 지원 파란 점) + 화살표.
 */
@Composable
fun ReceivedApplicationRow(a: ReceivedApplication, trash: Boolean, onClick: (() -> Unit)?, onRestore: () -> Unit, showDivider: Boolean = true) {
    val (status, level) = ApplicationLabels.received(a)
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it }
                .padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingL),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingM),
        ) {
            Avatar(a.applicantImage, 48.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXS)) {
                Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingXS)) {
                    LevelBadge(stringResource(ApplicationLabels.kind(a.kind)), when (a.kind) {
                        ApplicationKind.SUB -> CategoryColors.Level.Middle
                        ApplicationKind.CASTING -> CategoryColors.Level.End
                        else -> CategoryColors.Level.High
                    })
                    if (a.kind == ApplicationKind.SUB && a.viaDispatch) LevelBadge(stringResource(R.string.received_dispatch), CategoryColors.Level.High)
                    LevelBadge(stringResource(status), level)
                }
                Text(a.applicantName ?: stringResource(R.string.received_applicant), style = AppTypography.CardTitle16, color = AppColors.TextHead, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val sub = buildList {
                    add(a.postingTitle ?: stringResource(R.string.application_posting))
                    add(timeAgo(a.createdAt))
                    if (a.kind == ApplicationKind.SUB && (a.acceptedPay ?: 0) > 0) {
                        add(stringResource(R.string.application_pay, HomeLabels.won(a.acceptedPay!!)))
                        if ((a.effectivePay ?: 0) > a.acceptedPay!!) add(stringResource(R.string.received_effective_pay, HomeLabels.won(a.effectivePay!!)))
                    }
                    if (a.kind == ApplicationKind.SUB && a.applicantCancels90d != null) add(stringResource(R.string.received_cancels_90d, a.applicantCancels90d!!))
                }.filter { it.isNotBlank() }.joinToString(" · ")
                Text(sub, style = AppTypography.Sub13, color = AppColors.TextHint, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            when {
                trash -> OutlinedButton(onClick = onRestore, shape = CircleShape) { Text(stringResource(R.string.received_restore), style = AppTypography.Body14SemiBold, color = AppColors.TextBody) }
                a.withdrawn -> Text(stringResource(R.string.received_unavailable), style = AppTypography.Sub13, color = AppColors.TextSub)
                else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
                    if (a.isNew) Box(Modifier.size(8.dp).background(AppColors.NewDot, CircleShape))
                    Icon(painterResource(R.drawable.ic_web_arrow), null, tint = AppColors.TextHead, modifier = Modifier.size(18.dp).rotate(-90f))
                }
            }
        }
        if (showDivider) HorizontalDivider(Modifier.padding(horizontal = Dimensions.gutter), thickness = 1.dp, color = AppColors.Divider)
    }
}
