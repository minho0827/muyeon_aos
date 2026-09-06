package com.muyeon.app.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteNavBar

/**
 * 강사 리뷰 수정 — iOS `ReviewEditSheet.swift` 1:1.
 *
 * ⚠️ 네이티브는 **UI 만** 담당한다. 저장 API 는 웹이 `window.__onReviewEdited(rating, content)`
 *   콜백을 받아서 수행한다. 콜백을 안 쏘면 수정이 조용히 사라진다.
 */
@Composable
fun ReviewEditSheet(
    initialRating: Int,
    initialContent: String,
    onSave: (Int, String) -> Unit,
    onClose: () -> Unit,
) {
    var rating by remember { mutableIntStateOf(initialRating) }
    var content by remember { mutableStateOf(initialContent) }
    val msgs = listOf("아쉬워요", "별로에요", "보통이에요", "만족해요", "최고예요")

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(
            title = "리뷰 수정",
            trailing = {
                Icon(
                    Icons.Filled.Close, "닫기", tint = MuyeonColors.textSub,
                    modifier = Modifier.size(40.dp).clickable(onClick = onClose).padding(12.dp),
                )
            },
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            (1..5).forEach { i ->
                Icon(
                    if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    "$i 점",
                    tint = if (i <= rating) MuyeonColors.primary else MuyeonColors.tileLocked,
                    modifier = Modifier.size(38.dp).clickable { rating = i },
                )
            }
        }
        Text(
            if (rating > 0) msgs[rating - 1] else "평점을 선택해 주세요",
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
            lineHeight = 16.sp,
            color = if (rating > 0) MuyeonColors.primary else MuyeonColors.textSub,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp),
        )

        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            placeholder = {
                Text(
                    "레슨 후기를 남겨주세요. (선택)",
                    fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.tileLocked,
                )
            },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(140.dp),
        )

        Text(
            "저장",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 12.dp)
                .fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(if (rating == 0) MuyeonColors.tileLocked else MuyeonColors.primary)
                .clickable(enabled = rating > 0) { onSave(rating, content.trim()) }
                .padding(vertical = 16.dp),
        )
    }
}
