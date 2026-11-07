package com.muyeon.app.ui.main.bar

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.muyeon.app.ui.quote.QuoteUi
import com.muyeon.app.ui.theme.AppColors

/**
 * ★ 2026-11-08 탭바 MY 칸 — 인스타그램식 유형 동그라미(사용자 요청).
 *   가진 유형을 겹쳐 나란히 놓고, 지금 유형은 맨 위·검정 테두리·조금 크게. 사진이 있으면 사진, 없으면 유형 아이콘.
 *   유형이 여럿인 회원에게 '바꿀 수 있다'를 눈으로 보여 준다(길게 누르면 유형 시트 — 기존 그대로).
 */
@Composable
fun TypeAvatars(types: List<String>, active: String, images: Map<String, String>, selected: Boolean) {
    val list = types.ifEmpty { listOf(active) }
    val n = list.size
    val base = when (n) { 1 -> 26.dp; 3 -> 17.dp; else -> 20.dp }
    // 자리 — 1개: 가운데 / 3개: 삼각형(위가 지금 유형) / 2개·4개 이상: 가로로 겹쳐(오른쪽 끝이 지금 유형).
    //  두 번 누르면 다음 유형이 앞자리로 오고 나머지가 한 칸씩 밀려난다(자리 이동을 스프링으로 — 인스타그램 전환 느낌).
    val step = when { n == 2 -> 18.dp; n == 4 -> 13.dp; else -> 11.dp }
    val spots: List<Pair<androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp>> = when (n) {
        1 -> listOf(0.dp to 0.dp)
        3 -> listOf(9.dp to 0.dp, 18.dp to 11.dp, 0.dp to 11.dp)   // 위 → 오른쪽 아래 → 왼쪽 아래(시계 방향으로 돈다)
        else -> List(n) { i -> step * (n - 1 - i) to 0.dp }        // 0 = 맨 오른쪽(앞)
    }
    val w = spots.maxOf { it.first } + base + 8.dp
    val h = spots.maxOf { it.second } + base + 8.dp
    val activeIdx = list.indexOf(active).coerceAtLeast(0)
    Box(Modifier.wrapContentSize(unbounded = true).width(w).height(h)) {
        list.forEachIndexed { i, t ->
            val rank = (i - activeIdx).mod(n)            // 0 = 지금 유형(앞자리)
            val on = rank == 0
            val (tx, ty) = spots[rank]
            val spring = androidx.compose.animation.core.spring<androidx.compose.ui.unit.Dp>(dampingRatio = 0.7f, stiffness = 500f)
            val x by animateDpAsState(tx, spring, label = "avatarX")
            val y by animateDpAsState(ty, spring, label = "avatarY")
            val size by animateDpAsState(if (on && n > 1) base + 4.dp else base, spring, label = "avatarSize")
            val ring = when { on && selected -> AppColors.TextHead; on -> AppColors.TextHint; else -> AppColors.Surface }
            val shift = (size - base) / 2
            Box(
                Modifier.offset(x = x - shift + 2.dp, y = y - shift + 2.dp).zIndex((n - rank).toFloat())
                    .size(size + 4.dp).clip(CircleShape).background(AppColors.Surface).border(1.5.dp, ring, CircleShape).padding(2.dp)
                    .clip(CircleShape).background(AppColors.SurfaceMuted),
                contentAlignment = Alignment.Center,
            ) {
                val url = images[t]
                if (url != null) AsyncImage(QuoteUi.imageUrl(url), null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
                else Icon(typeIcon(t), null, tint = if (on && selected) AppColors.TextHead else AppColors.TextHint, modifier = Modifier.size(size * 0.62f))
            }
        }
    }
}

private fun typeIcon(t: String): ImageVector = when (t) {
    "TEACHER" -> Icons.Filled.School
    "DANCER" -> Icons.Filled.DirectionsRun
    "ACADEMY" -> Icons.Filled.Apartment
    "TEAM" -> Icons.Filled.TheaterComedy
    else -> Icons.Filled.Person
}
