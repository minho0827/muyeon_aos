package com.muyeon.app.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.muyeon.app.R

val customFontFamily = FontFamily(
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_regular, FontWeight.Normal)
)
data class CustomTypography(
    val fontSize: TextUnit,
    val lineHeight: TextUnit
) {
    fun toTextStyle(fontWeight: FontWeight): TextStyle {
        return TextStyle(
            fontSize = fontSize,
            lineHeight = lineHeight,
            fontWeight = fontWeight,
            fontFamily = customFontFamily
        )
    }
}

@Suppress("unused")
object AppTypography {
    val T1 = CustomTypography(
        fontSize = 40.sp,
        lineHeight = 54.sp
    )
    val T2 = CustomTypography(
        fontSize = 38.sp,
        lineHeight = 50.sp
    )
    val T3 = CustomTypography(
        fontSize = 36.sp,
        lineHeight = 48.sp
    )
    val T4 = CustomTypography(
        fontSize = 34.sp,
        lineHeight = 44.sp
    )
    val T5 = CustomTypography(
        fontSize = 32.sp,
        lineHeight = 42.sp
    )
    val T6 = CustomTypography(
        fontSize = 30.sp,
        lineHeight = 40.sp
    )
    val T7 = CustomTypography(
        fontSize = 28.sp,
        lineHeight = 38.sp
    )
    val T8 = CustomTypography(
        fontSize = 26.sp,
        lineHeight = 36.sp
    )
    val T9 = CustomTypography(
        fontSize = 24.sp,
        lineHeight = 34.sp
    )
    val T10 = CustomTypography(
        fontSize = 22.sp,
        lineHeight = 32.sp
    )
    val T11 = CustomTypography(
        fontSize = 20.sp,
        lineHeight = 30.sp
    )
    val T12 = CustomTypography(
        fontSize = 18.sp,
        lineHeight = 26.sp
    )
    val T13 = CustomTypography(
        fontSize = 16.sp,
        lineHeight = 24.sp
    )
    val T14 = CustomTypography(
        fontSize = 14.sp,
        lineHeight = 20.sp
    )
    val T15 = CustomTypography(
        fontSize = 13.sp,
        lineHeight = 19.sp
    )
    val T16 = CustomTypography(
        fontSize = 12.sp,
        lineHeight = 18.sp
    )
    val T17 = CustomTypography(
        fontSize = 11.sp,
        lineHeight = 14.sp
    )

    val T18 = CustomTypography(
        fontSize = 16.sp,
        lineHeight = 22.sp
    )

    val T19 = CustomTypography(
        fontSize = 22.sp,
        lineHeight = 30.sp
    )

    val T20 = CustomTypography(
        fontSize = 14.sp,
        lineHeight = 22.sp
    )
}