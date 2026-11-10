package com.muyeon.app.ui.lesson.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.R
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.review.ReviewList
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 레슨 상세 '리뷰·평점' 칸 — 웹 LessonDetail.js 의 `TeacherReviews`(개설 강사 후기 재활용, 기본형) 1:1. iOS `LessonDetailReviews.swift`.
 *  제목 '리뷰·평점 (N)' · 별 5개 + '리뷰 N개 · 평균 X' · (쓸 수 있으면) 후기 작성 · 후기(작성자·내 리뷰·날짜 / 별 / 본문) · 없으면 '아직 등록된 리뷰가 없어요.'
 *  앱은 최근 3개만 보이고 더 있으면 [후기 전체 보기](기존 후기 목록). 후기를 누르면 기존 후기 상세.
 *  GET /teachers/:id/reviews(ReviewApi.list) 결과를 그대로 받는다 — 강사 줄의 ★평균(개수)과 같은 값.
 */
@Composable
fun LessonDetailReviews(
    data: ReviewList,
    onSelectReview: (Int) -> Unit,
    onViewAll: () -> Unit,
    onWrite: () -> Unit,
    /** 내 후기 줄에 붙일 [수정]·[삭제](ReviewOwnerActions). */
    mineActions: @Composable () -> Unit = {},
) {
    val count = data.count ?: data.items.size
    HorizontalDivider(color = MuyeonColors.border)
    Column(
        Modifier.fillMaxWidth().padding(20.dp).testTag("lessonDetail.reviews"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            if (count > 0) stringResource(R.string.lesson_review_title_count, count) else stringResource(R.string.lesson_review_title),
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = MuyeonColors.textHead,
        )
        // 평점 요약 — 웹 review-summary(별 18 + '리뷰 N개 · 평균 X').
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ReviewStars(Math.round(data.avg ?: 0.0).toInt(), 18.dp)
            Text(
                if (count > 0) stringResource(R.string.lesson_review_summary, count, lessonReviewAvgText(data.avg))
                else stringResource(R.string.lesson_review_summary_empty),
                fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 16.sp, color = MuyeonColors.textSub,
            )
        }
        // 신규 작성 — 웹과 같이 쓸 수 있고(레슨 이용 회원) 내 리뷰가 없을 때만.
        if (data.canWrite == true && data.myReview == null && data.items.none { it.mine == true }) {
            WriteBox(onWrite)
        }
        if (data.items.isEmpty()) {
            Text(
                stringResource(R.string.lesson_review_empty),
                fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp, color = MuyeonColors.hint,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        } else {
            data.items.take(PREVIEW).forEachIndexed { i, r ->
                if (i > 0) HorizontalDivider(color = MuyeonColors.tileIdle)
                ReviewRow(r, mineActions) { onSelectReview(r.id) }
            }
        }
        if (count > PREVIEW && data.items.size > PREVIEW) {
            Text(
                stringResource(R.string.lesson_review_view_all),
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                lineHeight = 16.sp, color = MuyeonColors.primary,
                modifier = Modifier.clickable(onClick = onViewAll).padding(vertical = 4.dp).testTag("lessonDetail.reviewsAll"),
            )
        }
    }
}

private const val PREVIEW = 3

@Composable
private fun ReviewRow(r: ReviewList.Item, mineActions: @Composable () -> Unit, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                r.reviewerName ?: stringResource(R.string.lesson_review_reviewer_default),
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                lineHeight = 17.sp, color = MuyeonColors.textHead, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (r.mine == true) {
                Text(
                    stringResource(R.string.lesson_review_mine),
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp,
                    lineHeight = 13.sp, color = MuyeonColors.primary,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(MuyeonColors.primary.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                dateText(r.createdAt),
                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 14.sp, color = MuyeonColors.hint,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            ReviewStars(r.rating ?: 0, 14.dp)
            Spacer(Modifier.weight(1f))
            if (r.mine == true) mineActions()
        }
        r.content?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                lineHeight = 20.sp, color = MuyeonColors.body, maxLines = 3, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun WriteBox(onWrite: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MuyeonColors.tileIdle).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            stringResource(R.string.lesson_review_write_prompt),
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
            lineHeight = 20.sp, color = MuyeonColors.textHead,
        )
        Text(
            stringResource(R.string.lesson_review_write),
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
            lineHeight = 44.sp, color = Color.White,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(10.dp))
                .background(MuyeonColors.primary).clickable(onClick = onWrite).testTag("lessonDetail.reviewWrite"),
        )
    }
}

/** 별 5개 — 켜짐 주황(primary) / 꺼짐 회색(ReviewListScreen 카드와 같은 #E5E5EA). */
@Composable
private fun ReviewStars(value: Int, size: Dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        (1..5).forEach { i ->
            Icon(
                Icons.Filled.Star, null,
                tint = if (i <= value) MuyeonColors.primary else Color(0xFFE5E5EA),
                modifier = Modifier.size(size),
            )
        }
    }
}

private val dateFmt = DateTimeFormatter.ofPattern("yyyy.MM.dd")

/** 웹 fmtDate — 'YYYY.MM.DD'(KST). 읽지 못하면 빈칸. */
private fun dateText(iso: String?): String =
    iso?.let { runCatching { OffsetDateTime.parse(it).atZoneSameInstant(ZoneId.of("Asia/Seoul")).format(dateFmt) }.getOrNull() }
        ?: ""
