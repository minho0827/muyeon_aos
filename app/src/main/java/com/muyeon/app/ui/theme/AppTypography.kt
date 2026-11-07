package com.muyeon.app.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * 글자 단일 출처 — 웹 mix.text-style(크기, 줄, 색, 굵기)을 옮겼다. 웹 1rem = 10px = 앱 10sp.
 *   이름은 `<크기><굵기>`. 화면은 여기서 고르고, 색은 Text(color=AppColors.X) 로 따로 준다.
 */
object AppTypography {
    private fun s(size: TextUnit, weight: FontWeight, line: Float = 1.4f) = TextStyle(
        fontFamily = AppFonts.Pretendard, fontSize = size, fontWeight = weight,
        lineHeight = size * line,
    )

    /** 화면 제목(헤더) — 18 bold. */
    val Title18 = s(18.sp, FontWeight.Bold)
    /** 큰 안내(홈 '📍 우리 학원 맞춤', MY 이름) — 20 bold. */
    val Title20 = s(20.sp, FontWeight.Bold)
    /** 섹션 제목(웹 .meal-time) — 16 bold. */
    val Section16 = s(16.sp, FontWeight.Bold)
    /** 카드 제목(.meal-menu-card .tit · 목록 제목) — 16 semibold. */
    val CardTitle16 = s(16.sp, FontWeight.SemiBold)
    val Body15 = s(15.sp, FontWeight.Normal)
    val Body15Medium = s(15.sp, FontWeight.Medium)
    /** 칩(.type-chip) — 14 medium, 선택 시 bold. */
    val Chip14 = s(14.sp, FontWeight.Medium)
    val Chip14Selected = s(14.sp, FontWeight.Bold)
    /** '더보기' 버튼 — 14 medium. */
    val More14 = s(14.sp, FontWeight.Medium)
    val Body14 = s(14.sp, FontWeight.Normal)
    val Body14SemiBold = s(14.sp, FontWeight.SemiBold)
    /** 탭바 글자 — 13 semibold. */
    val Tab13 = s(13.sp, FontWeight.SemiBold)
    /** 보조 줄(.sub) — 13 regular. */
    val Sub13 = s(13.sp, FontWeight.Normal)
    val Sub13SemiBold = s(13.sp, FontWeight.SemiBold)
    val Caption12 = s(12.sp, FontWeight.Medium)
    /** 배지(.level) — 12 bold. */
    val Badge12 = s(12.sp, FontWeight.Bold)
    /** 알약(pill) — 11 semibold. */
    val Pill11 = s(11.sp, FontWeight.SemiBold)
    val Tiny10 = s(10.sp, FontWeight.Bold, line = 1.2f)
}
