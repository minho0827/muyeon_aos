package com.muyeon.app.ui.reservation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.components.LevelBadge
import com.muyeon.app.ui.components.ListRow
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.domain.model.reservation.ReservationCard

/**
 * 예약 한 줄 — 웹 MyReservations ListRow(상태 배지 · 학원/개인레슨 배지 · "레슨 · 날짜 시간" · 인원·장소).
 *   취소건은 누를 수 없다([onClick] null — 웹도 상세 진입 없음).
 */
@Composable
fun ReservationRow(r: ReservationCard, onClick: (() -> Unit)?, showDivider: Boolean = true) {
    val (label, level) = when (r.status) {
        "CONFIRMED" -> R.string.reservation_status_confirmed to CategoryColors.Level.High
        "CANCELED" -> R.string.reservation_status_canceled to CategoryColors.Level.Row
        "ATTENDED" -> R.string.reservation_status_attended to CategoryColors.Level.High
        "NOSHOW" -> R.string.reservation_status_noshow to CategoryColors.Level.Row
        else -> R.string.reservation_status_requested to CategoryColors.Level.Middle
    }
    val kind = r.academyName ?: stringResource(if (r.ownerAcademyId != null) R.string.reservation_tab_academy else R.string.reservation_tab_personal)
    val time = if (r.startTime != null && r.endTime != null) "${r.startTime}~${r.endTime}" else r.startTime
    val whenText = listOfNotNull(r.date, time).joinToString(" ")
    val title = (r.title ?: stringResource(R.string.reservation_lesson)) + if (whenText.isNotBlank()) " · $whenText" else ""
    val statusText = stringResource(label)
    ListRow(
        title = if (r.canceled) "$title (${statusText})" else title,
        onClick = onClick ?: {},
        sub = listOfNotNull(r.headcount?.let { stringResource(R.string.reservation_headcount, it) }, r.place).joinToString(" · "),
        tags = {
            LevelBadge(statusText, level)
            LevelBadge(kind, CategoryColors.Level.Middle)
        },
        trailing = if (onClick == null) ({}) else null,
        showDivider = showDivider,
    )
}
