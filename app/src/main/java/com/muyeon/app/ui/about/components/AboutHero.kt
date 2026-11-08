package com.muyeon.app.ui.about.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.R
import com.muyeon.app.ui.components.MdButton
import com.muyeon.app.ui.components.RemoteImage
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 첫 화면 — 웹 .about-hero(모바일: 48/20/64 · 제목 36 · 버튼 세로 · 사진 420 높이 24 모서리 + 아래 어둡게 + 반투명 카드). */
@Composable
fun AboutHero(onBrowse: () -> Unit, onPricing: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(start = Dimensions.gutter, end = Dimensions.gutter, top = 48.dp, bottom = 64.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Column {
            AboutEyebrow(stringResource(R.string.about_eyebrow), Modifier.padding(bottom = 16.dp))
            Text(stringResource(R.string.about_hero_title), style = AppTypography.Title22.copy(fontSize = HERO_TITLE, lineHeight = HERO_TITLE * 1.22f, fontWeight = FontWeight.ExtraBold), color = AppColors.TextHead)
            Text(stringResource(R.string.about_hero_desc), style = AppTypography.Body15.copy(lineHeight = 24.sp), color = AppColors.TextSub, modifier = Modifier.padding(top = 24.dp))
            Column(Modifier.padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                MdButton(stringResource(R.string.about_browse), onBrowse, tag = "about.browse")
                MdButton(stringResource(R.string.about_pricing), onPricing, filled = false, tag = "about.pricing")
            }
        }
        Box(Modifier.fillMaxWidth().height(420.dp).clip(RoundedCornerShape(24.dp)).background(AppColors.SurfaceMuted)) {
            RemoteImage(IMAGE, Modifier.fillMaxSize(), contentDescription = stringResource(R.string.about_image_desc))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.42f to Color.Transparent, 1f to SHADE)))
            Column(
                Modifier.align(Alignment.BottomStart).padding(20.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)).background(Color.White.copy(alpha = 0.16f)).padding(22.dp),
            ) {
                Text(stringResource(R.string.about_phone_title), style = AppTypography.Title22.copy(lineHeight = 30.sp), color = AppColors.TextOnPrimary)
                Text(stringResource(R.string.about_phone_sub), style = AppTypography.Sub13, color = AppColors.TextOnPrimary.copy(alpha = 0.85f), modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

/** 웹 public/images/muyeon/ballet-studio.jpg(같은 서버에서 받는다). */
private const val IMAGE = "/images/muyeon/ballet-studio.jpg"
private val HERO_TITLE = 32.sp
/** 웹 linear-gradient(… rgba(20,17,15,.55)). */
private val SHADE = Color(0x8C14110F)
