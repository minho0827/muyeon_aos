package com.muyeon.app.ui.support.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.components.LevelBadge
import com.muyeon.app.ui.components.ListRow
import com.muyeon.app.ui.format.KstDates
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.domain.model.support.SupportInquiry

/** 1:1 문의 한 줄 — 웹 SupportInquiries ListRow(상태 배지 · 첫 줄 제목 · 문의일). */
@Composable
fun InquiryRow(item: SupportInquiry, onClick: () -> Unit, showDivider: Boolean = true) {
    val status = stringResource(if (item.resolved) R.string.support_inquiry_status_done else R.string.support_inquiry_status_waiting)
    ListRow(
        title = item.titleLine ?: stringResource(R.string.support_inquiry_title),
        onClick = onClick,
        sub = stringResource(R.string.support_inquiry_date, KstDates.dot(item.createdAt)),
        tags = { LevelBadge(status, if (item.resolved) CategoryColors.Level.High else CategoryColors.Level.End) },
        showDivider = showDivider,
    )
}
