package com.muyeon.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily

/**
 * 기능 묶음 허브(2열 아이콘 카드) — iOS `Hub/HubGridView.swift` 1:1.
 *  '레슨·견적 관리'와 '스튜디오 운영' 두 허브가 items 만 달리해 재사용한다.
 *
 * ⚠️ `key` 는 라우팅 키(네이티브 브릿지 액션명) 또는 웹 경로("/...") — iOS 와 같은 문자열을 쓴다.
 */
data class HubItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val key: String,
    val badge: String? = null,
    val badgeOn: Boolean = false,
)

@Composable
fun HubGridScreen(
    title: String,
    items: List<HubItem>,
    onClose: () -> Unit,
    onSelect: (HubItem) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(MuyeonColors.groupedBg)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Box(Modifier.align(Alignment.CenterStart).size(24.dp).clickable { onClose() }, Alignment.Center) {
                Icon(Icons.Default.Close, "닫기", Modifier.size(16.dp), tint = MuyeonColors.textHead)
            }
            Text(
                title,
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
                lineHeight = 20.sp, color = MuyeonColors.textHead,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = MuyeonLayout.gutter, end = MuyeonLayout.gutter, top = 12.dp, bottom = 32.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.key }) { HubCard(it, onSelect) }
        }
    }
}

@Composable
private fun HubCard(item: HubItem, onSelect: (HubItem) -> Unit) {
    Column(
        Modifier.fillMaxWidth().heightIn(min = 128.dp)
            .clip(RoundedCornerShape(16.dp)).background(MuyeonColors.surface)
            .clickable { onSelect(item) }.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(MuyeonColors.primary),
                Alignment.Center,
            ) {
                Icon(item.icon, null, Modifier.size(20.dp), tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            // 상태 뱃지 — 진입 전에 켜짐/꺼짐을 한눈에(자동응답 등).
            item.badge?.let { badge ->
                Text(
                    badge,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                    lineHeight = 13.sp,
                    color = if (item.badgeOn) Color.White else MuyeonColors.secondary,
                    modifier = Modifier.clip(RoundedCornerShape(50))
                        .background(if (item.badgeOn) MuyeonColors.primary else Color(0xFFE5E5EA))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                item.title,
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                lineHeight = 18.sp, color = MuyeonColors.textHead,
            )
            Text(
                item.subtitle,
                fontFamily = customFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp,
                lineHeight = 16.sp, color = MuyeonColors.secondary,
            )
        }
    }
}
