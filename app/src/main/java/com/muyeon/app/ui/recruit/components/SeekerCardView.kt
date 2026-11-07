package com.muyeon.app.ui.recruit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.CardTags
import com.muyeon.app.ui.components.HeartButton
import com.muyeon.app.ui.components.RemoteImage
import com.muyeon.app.ui.components.SlotText
import com.muyeon.app.ui.lessons.LessonLabels
import com.muyeon.app.ui.recruit.RecruitLabels
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.domain.model.recruit.SeekerCard

/** 구직 카드 — 웹 JobBoard seek 카드: 사진(3:4, 위쪽 맞춤) · '신규' · 찜 · 이름 · "{장르} {강사|무용수}" · 📍지역 · 태그(분야 또는 장르 + 경력). */
@Composable
fun SeekerCardView(c: SeekerCard, liked: Boolean, onClick: () -> Unit, onLike: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(modifier.fillMaxWidth().clip(shape).border(1.dp, AppColors.Divider, shape).clickable(onClick = onClick)) {
        Box {
            RemoteImage(c.image, Modifier.fillMaxWidth().aspectRatio(3f / 4f), fallbackRes = R.drawable.img_noimage_teacher, thumbnail = true, alignment = Alignment.TopCenter)
            if (c.isNew) Text(stringResource(R.string.home_new), style = AppTypography.Caption12, color = Color.White,
                modifier = Modifier.align(Alignment.TopStart).padding(10.dp).background(Color(0xE6F58232), RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
            HeartButton(liked, onLike, size = 20.dp,
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).shadow(4.dp, CircleShape).background(AppColors.OverlayWhite, CircleShape))
        }
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            // 이름 · "장르 강사" · 지역 · 태그 — 값이 없어도 자리를 남긴다(카드 높이 맞춤).
            SlotText(c.name, AppTypography.CardTitle16, AppColors.TextHead)
            val head = c.genres.firstOrNull() ?: c.fields.firstOrNull()?.let { f -> LessonLabels.field(f)?.let { stringResource(it) } ?: f }
            SlotText(listOfNotNull(head, stringResource(RecruitLabels.seekType(c.role))).joinToString(" "), AppTypography.Sub13, AppColors.TextSub)
            SlotText(c.region?.let { "📍 $it" }, AppTypography.Sub13, AppColors.TextSub)
            val tags = (if (c.fields.isNotEmpty()) c.fields.map { f -> LessonLabels.field(f)?.let { stringResource(it) } ?: f } else c.genres) +
                listOfNotNull(LessonLabels.career(c.career)?.let { stringResource(it) })
            CardTags(tags, Modifier.padding(top = 4.dp))
        }
    }
}
