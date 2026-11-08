package com.muyeon.app.ui.support.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.components.LevelBadge
import com.muyeon.app.ui.components.ListRow
import com.muyeon.app.ui.format.KstDates
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.domain.model.support.Notice

/** 공지 한 줄 — 웹 #notice .notice-list .list-item(고정이면 '중요' 배지 · 제목 · 날짜 · 화살표). */
@Composable
fun NoticeRow(notice: Notice, onClick: () -> Unit, showDivider: Boolean = true) {
    val important = stringResource(R.string.support_notice_important)
    ListRow(
        title = notice.title,
        onClick = onClick,
        sub = KstDates.dot(notice.createdAt),
        tags = if (notice.pinned) ({ LevelBadge(important, CategoryColors.Level.High) }) else null,
        showDivider = showDivider,
    )
}
