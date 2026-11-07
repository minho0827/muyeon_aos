package com.muyeon.app.ui.my.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/**
 * MY 주황 머리 — 웹 #my_page .top-content: "{이름}님 / {유형}(으)로 활동 중"(24 bold 흰색) + 오른쪽 흰 화살표.
 *   누르면 회원유형 화면. 아래 여백 80 은 바로가기 카드가 걸쳐 올라앉을 자리(웹 padding-bottom 8rem).
 */
@Composable
fun MyProfileHeader(title: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().background(AppColors.Primary).clickable(onClick = onClick)
            .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 80.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = AppTypography.Title20.copy(fontSize = 24.sp, lineHeight = 34.sp), color = AppColors.TextOnPrimary, modifier = Modifier.weight(1f))
        Icon(painterResource(R.drawable.ic_web_arrow), contentDescription = null, tint = AppColors.TextOnPrimary, modifier = Modifier.size(24.dp).rotate(-90f))
    }
}
