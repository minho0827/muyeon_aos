package com.muyeon.app.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors

/**
 * 그리드 제목 + 굵은 밑줄(COREO ARTISTS 자리를 한글로 — 사용자 요청 2026-11-08).
 *   [genre] 전체면 "강사 · 무용수", 장르 탭이면 그 장르 이름(발레·한국무용 …).
 */
@Composable
fun ArtistsHeading(genre: String?, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 28.dp, bottom = 10.dp)) {
        Text(genre ?: stringResource(R.string.home_artists), style = HEADING, color = AppColors.TextHead)
        Box(Modifier.fillMaxWidth().padding(top = 14.dp).height(3.dp).background(AppColors.TextHead))
    }
}

/** 앱 글꼴(Pretendard) 굵게 — 처음엔 글꼴을 안 줘서 기기 기본 글꼴(Black)이 나왔다. */
private val HEADING = TextStyle(fontFamily = com.muyeon.app.ui.theme.AppFonts.Pretendard, fontWeight = FontWeight.Bold, fontSize = 22.sp, letterSpacing = (-0.3).sp)
