package com.muyeon.app.ui.quote

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.R
import com.muyeon.app.theme.customFontFamily
import kotlinx.coroutines.delay

/**
 * 레슨 요청 종류 선택 — iOS `QuoteCategoryView.swift` 1:1 이식.
 *  원형 썸네일 3열 그리드(가로 3 × 세로 3행). 행 간격은 남는 높이를 나눠 스크롤 없이 화면을 채운다.
 *
 *  ⚠️ iOS `QuoteCategoryView.Metric` 과 수치를 1:1로 맞춘다(아래 값 = iOS 값).
 *   한쪽만 고치면 두 앱 화면이 어긋난다.
 */

// MARK: - 공통 수치(iOS 동일)
private val SIDE = 20.dp            // 좌우 여백
private val H_GAP = 12.dp           // 열 간격
private val NAV_HEIGHT = 44.dp      // 내비바 높이
private val NAV_ICON = 18.dp        // X 아이콘
private val NAV_TOUCH = 44.dp       // X 터치 영역
private val NAV_TRAILING = 7.dp     // 터치영역 우측 여백(아이콘 중심 = 7+22 = 29)
private val TITLE_TOP = 8.dp
private val SUBTITLE_TOP = 8.dp
private val GRID_TOP = 20.dp
private val GRID_BOTTOM = 24.dp
private val ROW_GAP_MIN = 18.dp
private val RING = 2.dp             // 겉 띠 두께
private val RING_INSET = 5.dp       // 띠 ~ 이미지 간격(이미지 지름 = D - 10)
private val NAME_TOP = 10.dp
private val CAPTION_TOP = 3.dp
private const val OVERLAY_ALPHA = 0.55f   // 선택 시 원 안쪽 오렌지 막 투명도
private const val COLUMNS = 3
private const val SELECT_DELAY_MS = 180L

/** 카테고리 표시용 이미지·키워드 — 문진 계약(QuoteCategory)과 분리(서버 매핑 영향 없음). */
private data class CategoryVisual(val imageRes: Int, val caption: String)

private val categoryVisuals: Map<String, CategoryVisual> = mapOf(
    "ballet" to CategoryVisual(R.drawable.img_genre_ballet, "취미·전공"),
    "barre" to CategoryVisual(R.drawable.img_genre_barre, "체형교정·재활"),
    "korean" to CategoryVisual(R.drawable.img_genre_korean, "취미·입시"),
    "modern" to CategoryVisual(R.drawable.img_genre_modern, "컨템포러리"),
    "practical" to CategoryVisual(R.drawable.img_genre_practical, "K-pop·힙합"),
    "balletfit" to CategoryVisual(R.drawable.img_genre_balletfit, "다이어트·체형"),
    "musical" to CategoryVisual(R.drawable.img_genre_musical, "입시·오디션"),
)

@Composable
fun QuoteCategoryScreen(
    onSelect: (QuoteCategory) -> Unit,
    onClose: () -> Unit,
) {
    val rows = remember { QuoteCategory.all.chunked(COLUMNS) }
    // 선택 표시용 — 탭 순간 메인 컬러로 바뀐 뒤(SELECT_DELAY_MS) 위저드로 넘어간다.
    var selectedId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedId) {
        val id = selectedId ?: return@LaunchedEffect
        delay(SELECT_DELAY_MS)
        QuoteCategory.find(id)?.let(onSelect)
    }

    Column(Modifier.fillMaxSize().background(QuoteColors.white)) {
        // navBar — 제목 18sp bold 가운데, X 우측(터치영역 44)
        Box(Modifier.fillMaxWidth().height(NAV_HEIGHT), contentAlignment = Alignment.Center) {
            Text(
                "레슨 요청",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp,
                color = QuoteColors.c101116,
            )
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = NAV_TRAILING)
                    .size(NAV_TOUCH)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, "닫기", tint = QuoteColors.c37383B, modifier = Modifier.size(NAV_ICON))
            }
        }

        Text(
            "어떤 레슨의 견적을 받아볼까요?",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp,
            // lineHeight = 폰트 크기 × 1.1934 (Pretendard hhea 메트릭, iOS 기본 행높이와 동일)
            lineHeight = 26.sp, color = QuoteColors.c101116,
            modifier = Modifier.padding(horizontal = SIDE).padding(top = TITLE_TOP),
        )
        Text(
            "종류를 고르면 몇 가지만 여쭤보고 끝나요.",
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
            lineHeight = 17.sp, color = QuoteColors.c6D6E71,
            modifier = Modifier.padding(horizontal = SIDE).padding(top = SUBTITLE_TOP),
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = SIDE)
                .padding(top = GRID_TOP, bottom = GRID_BOTTOM),
        ) {
            rows.forEachIndexed { index, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(H_GAP)) {
                    // 원 지름 = 3열 균등 분할 폭(weight) — 인셋/폴더블에서도 마지막 열이 넘치지 않는다.
                    row.forEach { category ->
                        CategoryCell(
                            category = category,
                            isSelected = selectedId == category.id,
                            modifier = Modifier.weight(1f),
                            onSelect = { if (selectedId == null) selectedId = category.id },
                        )
                    }
                    // 마지막 행 빈 칸 — 열 정렬 유지용
                    repeat(COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
                }
                // 행 간격 = 최소 18dp + 남는 높이 균등 분배
                if (index < rows.lastIndex) Spacer(Modifier.height(ROW_GAP_MIN))
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CategoryCell(
    category: QuoteCategory,
    isSelected: Boolean,
    modifier: Modifier,
    onSelect: () -> Unit,
) {
    val visual = categoryVisuals[category.id] ?: return
    // 겉 띠·이름은 선택 순간 메인 오렌지로(두께 고정이라 레이아웃 흔들림 없음)
    val ringColor by animateColorAsState(
        if (isSelected) QuoteColors.f58232 else QuoteColors.cEAEAEA,
        tween(120), label = "ring",
    )
    val nameColor by animateColorAsState(
        if (isSelected) QuoteColors.f58232 else QuoteColors.c101116,
        tween(120), label = "name",
    )
    val circleScale by animateFloatAsState(if (isSelected) 0.96f else 1f, tween(120), label = "scale")
    val overlayAlpha by animateFloatAsState(if (isSelected) 1f else 0f, tween(120), label = "overlay")

    Column(
        modifier.clickable(indication = null, interactionSource = null, onClick = onSelect),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .scale(circleScale)
                .border(RING, ringColor, CircleShape)
                .padding(RING_INSET),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(visual.imageRes),
                contentDescription = category.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
            )
            // 선택 오버레이 — 메인 오렌지 반투명 막. 사진 배경(흑백/흰색/어두움)이 제각각이라
            //  띠 색만으로는 안 보이는 경우가 있어 원 안쪽에도 표시한다.
            if (overlayAlpha > 0f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(QuoteColors.f58232.copy(alpha = OVERLAY_ALPHA * overlayAlpha)),
                )
            }
        }
        Text(
            category.name,
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            lineHeight = 17.sp, color = nameColor, textAlign = TextAlign.Center,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = NAME_TOP),
        )
        Text(
            visual.caption,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp,
            lineHeight = 13.sp, color = QuoteColors.c8E8E8E, textAlign = TextAlign.Center,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = CAPTION_TOP),
        )
    }
}
