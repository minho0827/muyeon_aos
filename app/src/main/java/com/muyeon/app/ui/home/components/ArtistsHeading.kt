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

/** "ARTISTS" 제목 + 굵은 밑줄(COREO). */
@Composable
fun ArtistsHeading(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 28.dp, bottom = 10.dp)) {
        Text(stringResource(R.string.home_artists), style = HEADING, color = AppColors.TextHead)
        Box(Modifier.fillMaxWidth().padding(top = 14.dp).height(3.dp).background(AppColors.TextHead))
    }
}

private val HEADING = TextStyle(fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 0.5.sp)
