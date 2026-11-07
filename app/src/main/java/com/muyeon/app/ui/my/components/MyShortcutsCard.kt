package com.muyeon.app.ui.my.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/** 바로가기 한 칸. */
data class MyShortcut(val label: String, val iconRes: Int, val onClick: () -> Unit)

/**
 * 아이콘 바로가기 카드 — 웹 components/muyeon/HomeShortcuts.js(.home-shortcuts): 주황 머리 경계에 60 걸쳐 올라온 흰 카드(모서리 18 · 그림자),
 *   칸마다 회색 둥근 상자(56 · 모서리 20) 안 아이콘 32 + 13 semibold.
 */
@Composable
fun MyShortcutsCard(items: List<MyShortcut>, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .offset(y = (-60).dp)
            .padding(horizontal = 20.dp)
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .background(AppColors.Surface, RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        items.forEach { s ->
            Column(
                Modifier.weight(1f).clickable(onClick = s.onClick).padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.size(56.dp).background(AppColors.SurfaceMuted, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                    Icon(painterResource(s.iconRes), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(32.dp))
                }
                Text(s.label, style = AppTypography.Sub13SemiBold, color = AppColors.TextHead, maxLines = 1)
            }
        }
    }
}
