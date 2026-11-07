package com.muyeon.app.ui.home.components

import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/** 장르 → 레슨 요청(견적) 장르 고르기 화면과 같은 그림(QuoteCategoryScreen categoryImages). */
private val GENRE_IMAGES: Map<String, Int> = mapOf(
    "발레" to R.drawable.img_genre_ballet,
    "한국무용" to R.drawable.img_genre_korean,
    "현대무용" to R.drawable.img_genre_modern,
    "실용무용" to R.drawable.img_genre_practical,
    "뮤지컬" to R.drawable.img_genre_musical,
    "바레" to R.drawable.img_genre_barre,
    "발레핏" to R.drawable.img_genre_balletfit,
)

/**
 * 장르 아이콘 줄 — 뷰티컬리 카테고리 줄(둥근 네모 그림 + 아래 이름, 가로로 넘김).
 *   그림은 레슨 요청 때 장르를 고르는 그림을 그대로 쓴다(사용자 요청 2026-11-08). 그림이 없는 장르는 회색 칸.
 */
@Composable
fun LessonCategoryIcons(genres: List<String>, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(modifier, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(genres, key = { it }) { g ->
            Column(Modifier.width(64.dp).clickable { onSelect(g) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(AppColors.SurfaceMuted), Alignment.Center) {
                    GENRE_IMAGES[g]?.let { Image(painterResource(it), null, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp)) }
                }
                Text(g, style = AppTypography.Caption12, color = AppColors.TextBody, textAlign = TextAlign.Center, maxLines = 1,
                    modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}
