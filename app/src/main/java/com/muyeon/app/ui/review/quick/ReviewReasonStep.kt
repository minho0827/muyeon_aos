package com.muyeon.app.ui.review.quick

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors

/**
 * ★ 2026-11-14 빠른 후기 사유 체크(당근 '어떤 점이 최고였나요?', iOS ReviewReasonStep 1:1).
 *  좋아요·최고예요 = 좋았던 점(공개 태그), 별로예요 = 아쉬운 점(비공개 태그).
 */
@Composable
fun ReviewReasonStep(c: QuickReviewCoordinator, face: ReviewFace) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(
            QuickReviewOptions.reasonQuestion(face),
            color = MuyeonColors.textHead, fontFamily = customFontFamily, fontWeight = FontWeight.Bold,
            fontSize = 19.sp, lineHeight = 25.sp, modifier = Modifier.padding(bottom = 6.dp),
        )
        QuickReviewOptions.tagsFor(face).forEach { (code, label) ->
            ReasonRow(label, checked = code in c.tags, tag = "quickReview.tag.$code") { c.toggleTag(code) }
        }
    }
}

@Composable
private fun ReasonRow(label: String, checked: Boolean, tag: String, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 11.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val shape = RoundedCornerShape(6.dp)
        Box(
            Modifier
                .size(24.dp)
                .clip(shape)
                .background(if (checked) MuyeonColors.primary else Color.White)
                .then(if (checked) Modifier else Modifier.border(1.5.dp, MuyeonColors.tileLocked, shape)),
            Alignment.Center,
        ) {
            if (checked) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Text(
            label, color = MuyeonColors.textHead, fontFamily = customFontFamily,
            fontSize = 16.sp, lineHeight = 20.sp, modifier = Modifier.padding(start = 12.dp),
        )
    }
}
