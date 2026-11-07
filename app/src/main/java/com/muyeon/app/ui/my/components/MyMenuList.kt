package com.muyeon.app.ui.my.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
fun MyMenuList(rows: List<MyMenuRow>, onClick: (MyMenuRow) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(start = Dimensions.gutter, end = Dimensions.gutter, top = Dimensions.spacingXXL, bottom = Dimensions.spacing3XL)) {
        rows.forEachIndexed { i, row ->
            Row(
                Modifier.fillMaxWidth().height(58.dp).clickable { onClick(row) }.testTag("my_menu_${row.key}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingM),
            ) {
                Icon(painterResource(row.iconRes), contentDescription = null, tint = AppColors.TextHead, modifier = Modifier.size(Dimensions.iconL))
                Column(Modifier.weight(1f)) {
                    Text(row.title, style = AppTypography.CardTitle16, color = AppColors.TextHead)
                    row.desc?.let { Text(it, style = AppTypography.Caption12, color = AppColors.TextSub) }
                }
                Icon(painterResource(R.drawable.ic_web_arrow_right_bold), contentDescription = null, tint = AppColors.TextHint, modifier = Modifier.size(Dimensions.iconL))
            }
            if (i < rows.lastIndex) HorizontalDivider(thickness = 1.dp, color = AppColors.Divider)
        }
    }
}
