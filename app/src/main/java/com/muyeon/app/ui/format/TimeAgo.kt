package com.muyeon.app.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R

/** "방금 · N분 전 · N시간 전 · 어제 · N일 전 · 2026.11.07" — 웹 util/date.js timeAgo 와 같은 단계. */
@Composable
fun timeAgo(iso: String?): String {
    val sec = KstDates.secondsAgo(iso) ?: return KstDates.dot(iso)
    val min = sec / 60
    val hr = min / 60
    val day = hr / 24
    return when {
        sec < 60 -> stringResource(R.string.time_just_now)
        min < 60 -> stringResource(R.string.time_minutes_ago, min)
        hr < 24 -> stringResource(R.string.time_hours_ago, hr)
        day == 1L -> stringResource(R.string.time_yesterday)
        day < 7 -> stringResource(R.string.time_days_ago, day)
        else -> KstDates.dot(iso)
    }
}
