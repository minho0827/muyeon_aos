package com.muyeon.app.ui.review.quick

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors

/**
 * ★ 2026-11-14 빠른 후기 완료(당근 '따뜻한 후기를 보냈어요', iOS ReviewDoneStep 1:1).
 *  고른 얼굴·사유를 요약해 보여 주고 [확인]이면 채팅방으로 돌아간다.
 */
@Composable
fun ReviewDoneStep(c: QuickReviewCoordinator, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        Column(Modifier.weight(1f).padding(horizontal = 20.dp)) {
            Text(
                "${c.teacherName}님에게\n따뜻한 후기를 보냈어요.", color = MuyeonColors.textHead, fontFamily = customFontFamily,
                fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 33.sp, modifier = Modifier.padding(top = 56.dp),
            )
            Text(
                "${c.teacherName}님과 '${c.lessonLine}' 레슨을 했어요.", color = MuyeonColors.textSub,
                fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 8.dp),
            )
            Column(
                Modifier.fillMaxWidth().padding(top = 28.dp).clip(RoundedCornerShape(16.dp))
                    .background(MuyeonColors.groupedBg).padding(20.dp),
            ) {
                c.face?.let { f ->
                    Image(painterResource(f.onRes), f.label, Modifier.size(64.dp))
                    Text(
                        f.label, color = MuyeonColors.primary, fontFamily = customFontFamily,
                        fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp),
                    )
                }
                c.chosenLabels.forEach {
                    Text("• $it", color = MuyeonColors.textHead, fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 24.sp)
                }
                c.content.trim().takeIf { it.isNotEmpty() }?.let {
                    Text(
                        "“$it”", color = MuyeonColors.body, fontFamily = customFontFamily,
                        fontSize = 14.sp, lineHeight = 20.sp, maxLines = 3, modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }
        QuickReviewBottomButton("확인", true, onDone, "quickReview.done")
    }
}
