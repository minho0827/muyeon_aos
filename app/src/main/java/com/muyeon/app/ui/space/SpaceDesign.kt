package com.muyeon.app.ui.space

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import java.util.Locale

/**
 * 공간 상세 디자인 토큰 — iOS `SpaceDesign.swift` 1:1.
 *  Figma "MO 01_예약_숙박"(node 1:97636) 시안의 색/서체/도형을 옮긴 것으로,
 *  색 이름은 시안 변수명(Grayscale/Ink*, Primary/P*)을 따른다.
 *
 * ⚠️ 도형(점선 구분선·└ 리드·백화살표·꺾쇠)은 시안이 내보낸 SVG 가 전부 단일 path 의
 *   기하도형이라, 만료되는 원격 SVG 대신 같은 좌표를 Canvas 로 1:1 재현했다.
 *   (수정 시 원본 좌표 주석을 함께 고칠 것)
 */
object SpaceDesign {

    // MARK: Color — 회색조·주황은 프론트 팔레트(_colors.scss)를 그대로 따른다.

    /** $gray-900 #101116 — 본문 제목 */
    val ink900 = MuyeonColors.textHead
    /** $gray-800 #37383B — 안내 박스 제목 */
    val ink800 = Color(0xFF37383B)
    /** $gray-700 #4D4F53 — 안내 박스 본문/읽기 값 */
    val ink700 = Color(0xFF4D4F53)
    /** $gray-600 #6D6E71 — 패널 라벨·보조 설명 */
    val ink600 = MuyeonColors.textSub
    /** $gray-500 #8E8E8E — 상세 행 텍스트 */
    val ink500 = Color(0xFF8E8E8E)
    /** $gray-200 #D0D0D0 — └ 리드선, 비활성 버튼 면 */
    val ink300 = Color(0xFFD0D0D0)
    /** $gray-100 #EAEAEA — 구분선·입력 테두리 */
    val ink200 = MuyeonColors.border
    /** $gray-50 #F7F7F7 — 페이지 배경·패널 배경 */
    val ink100 = Color(0xFFF7F7F7)

    /** $primary-500 #F58232 — 브랜드 메인. 채움(CTA·카드 테두리·선택 상태) */
    val primary = MuyeonColors.primary
    /** $primary-600 #E55E00 — 흰 배경 위 강조 텍스트/아이콘(500은 대비가 낮다) */
    val primaryText = Color(0xFFE55E00)
    /** $primary-550 #F4741A — 흰 글씨를 얹는 배지 배경 */
    val primaryFill = Color(0xFFF4741A)
    /** $pr-06 (primary-500 6%) — 안내 박스 배경 */
    val primaryTint = MuyeonColors.primary.copy(alpha = 0.06f)
    /** $primary-50 #FFE4D2 — 안내 박스 테두리 */
    val primaryTintLine = Color(0xFFFFE4D2)

    /** $primary-600 #E55E00 — 배너 배경(시안의 진한 파랑 자리) */
    val bannerBg = Color(0xFFE55E00)
    /** $secondary-yellow-200 #FADFAA — 배너 강조 텍스트 */
    val bannerAccent = Color(0xFFFADFAA)

    // MARK: Metrics
    /** 시안 좌우 여백 (카드/섹션 공통) */
    val gutter = 20.dp
    val cardPadding = 20.dp
    /** card.svg r=13.11 → 연속 곡률 12 */
    val cardRadius = 12.dp
    val headerHeight = 56.dp

    /** 12345 → "12,345" */
    fun won(value: Int): String = String.format(Locale.KOREA, "%,d", value)
}

/** ic_x12_level — 상세 행 앞 └ 리드선. 원본: 12x12 viewBox, `M0.5 0 V6 H5.5`, stroke #DADADA 1. */
@Composable
fun SpaceLevelLead() {
    Canvas(Modifier.size(12.dp)) {
        val u = size.width / 12f
        val path = Path().apply {
            moveTo(0.5f * u, 0f)
            lineTo(0.5f * u, 6f * u)
            lineTo(5.5f * u, 6f * u)
        }
        drawPath(path, SpaceDesign.ink300, style = Stroke(width = 1f * u))
    }
}

/** 패스 2603 — 배너 우측 꺾쇠. 원본: 5.5x9.5, `M0.75 0.75 L4.75 4.75 L0.75 8.75`, stroke 1.5 round. */
@Composable
fun SpaceChevronRight(color: Color = Color.White) {
    val density = LocalDensity.current
    Canvas(Modifier.size(5.5.dp, 9.5.dp)) {
        val u = with(density) { 1.dp.toPx() }
        val path = Path().apply {
            moveTo(0.75f * u, 0.75f * u)
            lineTo(4.75f * u, 4.75f * u)
            lineTo(0.75f * u, 8.75f * u)
        }
        drawPath(
            path, color,
            style = Stroke(width = 1.5f * u, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/** ic_x24_back — 헤더 뒤로가기. 원본: 24x24, `M22 12 H4` + `M11 4 L3 12 L11 20`, stroke 2 round. */
@Composable
fun SpaceBackArrow() {
    Canvas(Modifier.size(24.dp)) {
        val u = size.width / 24f
        val stroke = Stroke(width = 2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        drawLine(SpaceDesign.ink900, Offset(22f * u, 12f * u), Offset(4f * u, 12f * u), 2f * u, StrokeCap.Round)
        val path = Path().apply {
            moveTo(11f * u, 4f * u)
            lineTo(3f * u, 12f * u)
            lineTo(11f * u, 20f * u)
        }
        drawPath(path, SpaceDesign.ink900, style = stroke)
    }
}

/** devide_line — 점선 구분선. 원본: stroke #EAEAEA, dash 2 2. */
@Composable
fun SpaceDashedDivider() {
    Canvas(Modifier.fillMaxWidth().height(1.dp)) {
        drawLine(
            SpaceDesign.ink200, Offset(0f, 0.5f), Offset(size.width, 0.5f),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 2f), 0f),
        )
    }
}

/** label_x14 — 값 앞에 붙는 아웃라인 배지. 원본: h14, border, r2, p3, 10px. */
@Composable
fun SpaceMiniLabel(text: String, color: Color = SpaceDesign.primaryText) {
    Box(
        Modifier.height(14.dp).clip(RoundedCornerShape(2.dp))
            .background(Color.White)
            .border(1.dp, color, RoundedCornerShape(2.dp))
            .padding(horizontal = 3.dp),
    ) {
        Text(
            text,
            fontFamily = customFontFamily, fontSize = 10.sp, lineHeight = 14.sp,
            color = color, textAlign = TextAlign.Center,
        )
    }
}
