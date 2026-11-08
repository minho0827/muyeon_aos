package com.muyeon.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 여백·크기·모서리 단일 출처 — 웹 rem 값을 그대로(1rem = 10dp). 화면 코드에 `.dp` 직접 대신 여기 이름을 쓴다.
 *   (PaceERA Dimensions 와 같은 규칙. 1회성 미세값은 컴포넌트 파일 안 private 상수로 둔다.)
 */
object Dimensions {
    val spacingXXS = 2.dp
    val spacingXS = 4.dp
    val spacingS = 8.dp
    val spacingM = 12.dp
    val spacingL = 16.dp
    val spacingXL = 20.dp
    val spacingXXL = 24.dp
    val spacing3XL = 32.dp

    /** 화면 좌우 기본 여백(웹 .section-head padding 0 2rem). */
    val gutter = 20.dp
    /** 홈 섹션 사이(웹 3.2rem). */
    val sectionGap = 32.dp

    // === 탭바(웹 _bottom-tab-bar.scss) ===
    val tabBarHeight = 82.dp
    /** 떠 있는 탭바 아래 목록 끝 여백 — 마지막 항목이 탭바에 가리지 않게(PaceERA tabBarClearance). 내비게이션 바 높이는 따로 더한다. */
    val tabBarClearance = 100.dp
    val tabBarPadding = 16.dp
    val tabButtonWidth = 52.dp
    val tabIconSize = 28.dp
    val tabIconGap = 4.dp

    // === 헤더(웹 $top-bar 5.6rem) ===
    val topBarHeight = 56.dp

    // === 칩(웹 _tab.scss type-chip) ===
    val chipHeight = 36.dp
    val chipPaddingH = 16.dp
    val chipGap = 8.dp

    // === 카드 ===
    val cardRadius = 16.dp
    val imageRadius = 14.dp
    val pillRadius = 6.dp
    val badgeRadius = 6.dp
    /** 홈 가로 카드 사진 높이(웹 18.6rem). */
    val slideImageHeight = 186.dp
    val slideGap = 8.dp

    // === 아이콘 ===
    val iconXS = 14.dp
    val iconS = 16.dp
    val iconM = 20.dp
    val iconL = 24.dp
    val dmButton = 36.dp
    val heartCircle = 32.dp

    // === 빈 목록 안내(EmptyView — 웹 .list-none) ===
    /** 목록 영역(칩·탭 줄 아래) 맨 위에서 안내 그림까지. 모든 화면이 같은 값 — 세로 가운데 정렬하지 않는다. */
    val emptyTop = 64.dp
    /** 안내 아래 여백(다음 쪽 표시·탭바와 떨어지게). */
    val emptyBottom = 64.dp
    /** 안내 그림(문서 그림) 크기. */
    val emptyIcon = 48.dp

    // === 플로팅(웹 floating-wrap) ===
    val floatingSize = 48.dp
    val floatingMargin = 20.dp
}
