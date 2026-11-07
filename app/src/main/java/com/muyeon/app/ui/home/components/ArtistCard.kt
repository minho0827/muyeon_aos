package com.muyeon.app.ui.home.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.RemoteImage
import com.muyeon.app.ui.components.SlotText
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.domain.model.teacher.TeacherCard

/**
 * 강사 카드 — COREO 아티스트 카드처럼: 큰 세로 사진(약 3:4.2) · 얇은 테두리 · 아래 이름만 · 모서리 각지게.
 *   이름 옆 장르는 회색 작은 글자(한 줄). 누르면 공개 프로필.
 */
@Composable
fun ArtistCard(t: TeacherCard, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().border(1.dp, AppColors.Divider).clickable(onClick = onClick)) {
        RemoteImage(t.image, Modifier.fillMaxWidth().aspectRatio(PHOTO_RATIO), fallbackRes = R.drawable.img_noimage_teacher, thumbnail = true)
        Column(Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            // 이름·장르 줄은 값이 없어도 자리를 남긴다(카드 높이 맞춤).
            SlotText(t.name, AppTypography.Body15Medium, AppColors.TextHead)
            SlotText(t.genres.joinToString(" · "), AppTypography.Caption12, AppColors.TextHint, Modifier.padding(top = 2.dp))
        }
    }
}

/** 사진 비율(가로/세로) — COREO 세로 사진. */
const val PHOTO_RATIO = 0.72f
