package com.muyeon.app.ui.my.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** MY 메뉴 한 칸(그리기 값). */
data class MyMenuRow(
    val key: String,
    val title: String,
    @DrawableRes val iconRes: Int,
    val desc: String? = null,
    val showDot: Boolean = false,
)

/**
 * MY 메뉴 묶음 — 웹 #my_page `ul.menu-list` > `a.menu-btn`(높이 58 · 아이콘 24 · 16 semibold · 끝 화살표, 줄 사이 1px).
 *   묶음 위아래 여백 24/32(웹). 비회원·회원 MY 가 같이 쓴다.
 */
@Composable
fun MyMenuList(rows: List<MyMenuRow>, onClick: (MyMenuRow) -> Unit, modifier: Modifier = Modifier, topPadding: Boolean = true) {
    // 묶음 사이는 위 여백 없이(웹: 두 번째 묶음부터 paddingTop 0) — 아래 32 가 묶음 간격.
    Column(modifier.fillMaxWidth().padding(start = Dimensions.gutter, end = Dimensions.gutter, top = if (topPadding) Dimensions.spacingXXL else 0.dp, bottom = Dimensions.spacing3XL)) {
        rows.forEachIndexed { i, row ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 58.dp).clickable { onClick(row) }.padding(vertical = if (row.desc != null) 11.dp else 0.dp).testTag("my_menu_${row.key}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingM),
            ) {
                // 웹 MY 아이콘은 원래 색 그대로(필터 없음).
                Icon(painterResource(row.iconRes), contentDescription = null, tint = androidx.compose.ui.graphics.Color.Unspecified, modifier = Modifier.size(Dimensions.iconL))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(row.title, style = AppTypography.CardTitle16, color = AppColors.TextHead)
                        // 웹 ActivityPulseDot — 안 읽은 알림·새 제안.
                        if (row.showDot) androidx.compose.foundation.layout.Box(Modifier.size(7.dp).background(AppColors.Badge, androidx.compose.foundation.shape.CircleShape))
                    }
                    row.desc?.let { Text(it, style = AppTypography.Caption12, color = AppColors.TextSub) }
                }
                Icon(painterResource(R.drawable.ic_web_arrow_right_bold), contentDescription = null, tint = AppColors.TextHint, modifier = Modifier.size(Dimensions.iconL))
            }
            if (i < rows.lastIndex) HorizontalDivider(thickness = 1.dp, color = AppColors.Divider)
        }
    }
}
