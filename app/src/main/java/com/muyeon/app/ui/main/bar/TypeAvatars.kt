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
    // ★ 2026-11-08 촘촘하게 — 지금 유형(앞)만 크게, 나머지는 작게 뒤에서 살짝만 보이게 겹친다(사용자: 너무 길게 늘어졌다).
    val front = if (n == 1) 26.dp else 24.dp
    val back = 16.dp
    val peek = when { n <= 3 -> 8.dp; n == 4 -> 6.dp; else -> 5.dp }   // 뒤 동그라미끼리 보이는 폭
    // rank 0 = 앞(오른쪽, 크게). rank 1.. = 왼쪽으로 한 칸씩 뒤. 3개면 삼각형(앞 위, 뒤 둘 아래 좌우).
    fun spot(rank: Int): Pair<androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp> = when {
        n == 3 -> when (rank) { 0 -> 6.dp to 0.dp; 1 -> 22.dp to 14.dp; else -> 0.dp to 14.dp }
        rank == 0 -> peek * (n - 1) + 4.dp to 0.dp
        else -> peek * (n - 1 - rank) to (front - back) / 2
    }
    val w = (0 until n).maxOf { spot(it).first + (if (it == 0) front else back) } + 6.dp
    val h = (0 until n).maxOf { spot(it).second + (if (it == 0) front else back) } + 6.dp
    val activeIdx = list.indexOf(active).coerceAtLeast(0)
    // 바뀌는 게 눈에 보이게 — 자리 이동은 0.55초(천천히 출발해 부드럽게 멈춤), 크기는 살짝 튀게.
    val move = androidx.compose.animation.core.tween<androidx.compose.ui.unit.Dp>(550, easing = androidx.compose.animation.core.FastOutSlowInEasing)
    val pop = androidx.compose.animation.core.spring<androidx.compose.ui.unit.Dp>(dampingRatio = 0.45f, stiffness = 260f)
    Box(Modifier.wrapContentSize(unbounded = true).width(w).height(h)) {
        list.forEachIndexed { i, t ->
            val rank = (i - activeIdx).mod(n)
            val on = rank == 0
            val (tx, ty) = spot(rank)
            val x by animateDpAsState(tx, move, label = "avatarX")
            val y by animateDpAsState(ty, move, label = "avatarY")
            val size by animateDpAsState(if (on) front else back, pop, label = "avatarSize")
            // 뒤 동그라미도 보이게 — 옅은 회색 테두리(흰 바탕에 묻히지 않게).
            val ring = when { on && selected -> AppColors.TextHead; on -> AppColors.TextHint; else -> AppColors.ChipBorder }
            Box(
                Modifier.offset(x = x + 2.dp, y = y + 2.dp).zIndex((n - rank).toFloat())
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
