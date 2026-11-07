package com.muyeon.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 원시 색 — 웹 muyeon-front `assets/styles/variables/_colors.scss` 를 그대로 옮겼다.
 *   화면은 이 객체를 직접 쓰지 않는다. 역할 이름이 붙은 [AppColors] 를 쓴다(PaceERA Palette 와 같은 규칙).
 */
internal object Palette {
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
    val Gray50 = Color(0xFFF7F7F7)
    val Gray100 = Color(0xFFEAEAEA)
    val Gray150 = Color(0xFFDFDFDF)
    val Gray200 = Color(0xFFD0D0D0)
    val Gray300 = Color(0xFFC5C4C4)
    val Gray400 = Color(0xFFAFAFAF)
    val Gray500 = Color(0xFF8E8E8E)
    val Gray600 = Color(0xFF6D6E71)
    val Gray700 = Color(0xFF4D4F53)
    val Gray800 = Color(0xFF37383B)
    val Gray900 = Color(0xFF101116)

    val Primary50 = Color(0xFFFFE4D2)
    val Primary500 = Color(0xFFF58232)
    val Primary550 = Color(0xFFF4741A)

    val Yellow500 = Color(0xFFFDB933)
    val Green = Color(0xFF9AA208)
    val Red = Color(0xFFE32502)
    val BadgeRed = Color(0xFFE2362D)
}
