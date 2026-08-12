package com.muyeon.app.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 네이티브 화면 공용 디자인 토큰 — iOS `Color+Muyeon.swift` / `MuyeonLayout.swift` 1:1.
 *  화면마다 여백/서체가 제각각이던 문제를 한 곳에서 통일한다.
 *
 * ⚠️ iOS 와 값이 어긋나면 두 앱 화면이 달라진다. 한쪽만 고치지 말 것.
 */

object MuyeonColors {
    /** 메인 컬러(주황) — front $primary-500 #F58232 */
    val primary = Color(0xFFF58232)
    /** 테두리/구분선 — front $gray-100 #EAEAEA */
    val border = Color(0xFFEAEAEA)
    /** 보조 텍스트 — front $gray-600 #6D6E71 */
    val textSub = Color(0xFF6D6E71)
    /** 헤드라인 텍스트 — front $gray-900 #101116 */
    val textHead = Color(0xFF101116)

    // iOS 가 SwiftUI 시스템 색을 쓰는 자리 — Android 는 명시값으로 고정(다크모드 미지원 화면)
    val surface = Color(0xFFFFFFFF)          // Color(.systemBackground)
    val groupedBg = Color(0xFFF2F2F7)        // Color(.systemGroupedBackground)
    val secondary = Color(0xFF8E8E93)        // Color.secondary
    val green = Color(0xFF34C759)            // Color.green (채택/매칭)
    val orange = Color(0xFFFF9500)           // Color.orange (견적 받는 중)
    val yellow = Color(0xFFFFCC00)           // Color.yellow (별점)
    val danger = Color(0xFFE64747)           // 철회 버튼 — iOS Color(red:0.9 green:0.28 blue:0.28)
    val placeholder = Color(0xFFEBEBEB)      // Color(white: 0.92) 아바타 플레이스홀더
    val chevron = Color(0xFFBFBFBF)          // Color(white: 0.75)
    val info = Color(0xFF007AFF)             // Color(.systemBlue) — 미확인 표시 점 등
    val body = Color(0xFF37383B)             // Asset.Colors.color37383B (견적 메시지 본문)
}

object MuyeonLayout {
    val gutter = 20.dp          // 좌우 페이지 여백
    val sectionGap = 20.dp      // 섹션 사이 상단 간격
    val headerSize = 15.sp      // 섹션 제목
    val captionSize = 13.sp     // 보조 설명
}
