package com.muyeon.app.ui.reservation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.components.LevelBadge
import com.muyeon.app.ui.components.ListRow
import com.muyeon.app.ui.format.KstDates
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.domain.model.reservation.CompletedLesson

/** 레슨 완료 한 줄 — 웹 MyReservations DONE(강사 사진 · '레슨 완료' + 후기 배지 · "레슨 · 날짜" · 강사·장소). */
@Composable
fun CompletedLessonRow(r: CompletedLesson, onClick: () -> Unit, showDivider: Boolean = true) {
    val date = KstDates.dot(r.startAt)
    ListRow(
        title = r.lessonLabel + if (date.isNotBlank()) " · $date" else "",
        onClick = onClick,
        sub = listOfNotNull(r.teacherName.ifBlank { null }, r.place).joinToString(" · "),
        thumbUrl = r.teacherImage,
        thumbFallbackRes = R.drawable.img_noimage_teacher,
        tags = {
            LevelBadge(stringResource(R.string.reservation_done_badge), CategoryColors.Level.High)
            if (r.reviewed) LevelBadge(stringResource(R.string.reservation_my_review, String.format("%.1f", r.myRating ?: 0.0)), CategoryColors.Level.Middle)
            else LevelBadge(stringResource(R.string.reservation_no_review), CategoryColors.Level.VeryHigh)
        },
        showDivider = showDivider,
    )
}
