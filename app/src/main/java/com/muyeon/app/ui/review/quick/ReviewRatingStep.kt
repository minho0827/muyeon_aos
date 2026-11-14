package com.muyeon.app.ui.review.quick

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteNavBar

/**
 * ★ 2026-11-14 빠른 후기 1단계 — 레슨 요약 + "○○님과 레슨이 어떠셨나요?" + 얼굴 3개(iOS ReviewRatingStep 1:1).
 *  얼굴을 고르면 바로 아래에 사유(ReviewReasonStep)가 펼쳐지고, 하나 이상 고르면 [다음].
 */
@Composable
fun ReviewRatingStep(c: QuickReviewCoordinator, onClose: () -> Unit, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "레슨 후기 보내기", onClose = onClose)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            LessonSummaryLine(c.lessonLine, c.teacherName)
            HorizontalDivider(color = MuyeonColors.border, modifier = Modifier.padding(horizontal = 20.dp))
            Text(
                "${c.teacherName}님과\n레슨이 어떠셨나요?",
                color = MuyeonColors.textHead, fontFamily = customFontFamily, fontWeight = FontWeight.Bold,
                fontSize = 22.sp, lineHeight = 30.sp, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp),
            )
            Text(
                "남겨 주신 후기는 강사님 프로필에 공개돼요. 아쉬운 점은 공개되지 않고 운영팀만 참고해요.",
                color = MuyeonColors.textSub, fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 19.sp,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 6.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ReviewFace.entries.forEach { f -> FaceOption(f, selected = c.face == f) { c.selectFace(f) } }
            }
            c.face?.let { ReviewReasonStep(c, it) }
            Spacer(Modifier.height(12.dp))
        }
        QuickReviewBottomButton("다음", c.canGoNext, onNext, "quickReview.next")
    }
}

@Composable
private fun FaceOption(f: ReviewFace, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clickable(onClick = onClick).testTag("quickReview.face.${f.name}"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(painterResource(if (selected) f.onRes else f.offRes), f.label, Modifier.size(88.dp))
        Text(
            f.label, color = if (selected) MuyeonColors.primary else MuyeonColors.textSub,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

/** 위쪽 레슨 요약 — "발레 레슨 11/26(목) 오후 3:00 · 레슨한 강사 ○○". */
@Composable
private fun LessonSummaryLine(lessonLine: String, teacherName: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
        Text(
            lessonLine, color = MuyeonColors.textHead, fontFamily = customFontFamily,
            fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp, maxLines = 1,
        )
        Text(
            "레슨한 강사 $teacherName", color = MuyeonColors.textSub, fontFamily = customFontFamily,
            fontSize = 14.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 3.dp),
        )
    }
}

