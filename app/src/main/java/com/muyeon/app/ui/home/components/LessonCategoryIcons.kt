package com.muyeon.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.CategoryColors

/**
 * 장르 아이콘 줄 — 뷰티컬리 카테고리 줄(둥근 네모 그림 + 아래 이름, 가로로 넘김).
 *   장르 그림이 아직 없어 장르 색 네모에 장르 첫 글자를 넣었다(그림이 생기면 이 안쪽만 바꾼다).
 */
@Composable
fun LessonCategoryIcons(genres: List<String>, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(modifier, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(genres, key = { it }) { g ->
            val c = CategoryColors.genre(g)
            Column(Modifier.width(64.dp).clickable { onSelect(g) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(56.dp).background(c.bg, RoundedCornerShape(18.dp)), Alignment.Center) {
                    Text(g.take(1), color = c.fg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Text(g, style = AppTypography.Caption12, color = AppColors.TextBody, textAlign = TextAlign.Center, maxLines = 1,
                    modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}
