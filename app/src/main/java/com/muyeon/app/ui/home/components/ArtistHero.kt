package com.muyeon.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.R
import com.muyeon.app.ui.components.RemoteImage
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.domain.model.teacher.TeacherCard

/**
 * 강사 쪽 맨 위 — COREO 첫 화면처럼 사진 두 장을 이어 붙인 넓은 띠(위쪽 맞춤으로 얼굴이 보이게). 누르면 그 강사 프로필.
 *   두 명이 안 되면 그리지 않는다.
 */
@Composable
fun ArtistHero(pair: List<TeacherCard>, onOpen: (TeacherCard) -> Unit, modifier: Modifier = Modifier) {
    if (pair.size < 2) return
    Row(modifier.fillMaxWidth().aspectRatio(1.7f)) {
        pair.take(2).forEach { t ->
            RemoteImage(t.image, Modifier.weight(1f).fillMaxHeight().clickable { onOpen(t) }, alignment = Alignment.TopCenter)
        }
    }
}

/** "ARTISTS" 제목 + 굵은 밑줄(COREO). */
@Composable
fun ArtistsHeading(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 28.dp, bottom = 10.dp)) {
        Text(stringResource(R.string.home_artists), style = HEADING, color = AppColors.TextHead)
        Box(Modifier.fillMaxWidth().padding(top = 14.dp).height(3.dp).background(AppColors.TextHead))
    }
}

private val HEADING = TextStyle(fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 0.5.sp)
