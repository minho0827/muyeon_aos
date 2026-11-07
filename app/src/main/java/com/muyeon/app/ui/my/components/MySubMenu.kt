package com.muyeon.app.ui.my.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/** MY 아래 작은 회색 알약 버튼 줄(웹 .sub-menu-box — 앱 설정 · 로그아웃). */
data class MySubMenuItem(val key: String, val title: String, val iconRes: Int?)

@Composable
fun MySubMenu(items: List<MySubMenuItem>, onClick: (MySubMenuItem) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.background(AppColors.SurfaceMuted, CircleShape).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { item ->
            Row(
                Modifier.clickable { onClick(item) }.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item.iconRes?.let { Icon(painterResource(it), contentDescription = null, tint = AppColors.TextSub, modifier = Modifier.size(18.dp)) }
                Text(item.title, style = AppTypography.Sub13SemiBold, color = AppColors.TextSub)
            }
        }
    }
}
