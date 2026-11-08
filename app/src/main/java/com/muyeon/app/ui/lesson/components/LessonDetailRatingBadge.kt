package com.muyeon.app.ui.lesson.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.R
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import java.util.Locale

/**
 * 레슨 상세 강사 줄 이름 옆 ★평균(개수) — 웹 LessonDetail.js 강사 줄(`c.reviewCount > 0` 일 때 ★ avg (count))과 같다.
 *  iOS `LessonDetailRatingBadge.swift`. 후기가 없으면 그리지 않는다.
 */
@Composable
fun LessonDetailRatingBadge(avg: Double?, count: Int?) {
    val n = count ?: 0
    if (n <= 0) return
    Row(
        Modifier.testTag("lessonDetail.rating"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(Icons.Filled.Star, null, tint = MuyeonColors.primary, modifier = Modifier.size(13.dp))
        Text(
            lessonReviewAvgText(avg),
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            lineHeight = 16.sp, color = MuyeonColors.textHead,
        )
        Text(
            stringResource(R.string.lesson_review_count_paren, n),
            fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp, color = MuyeonColors.textSub,
        )
    }
}

/** 평균 별점 한 자리 소수 — '4.3'. 리뷰·평점 칸 요약과 같은 표기. */
internal fun lessonReviewAvgText(avg: Double?): String = String.format(Locale.KOREA, "%.1f", avg ?: 0.0)
