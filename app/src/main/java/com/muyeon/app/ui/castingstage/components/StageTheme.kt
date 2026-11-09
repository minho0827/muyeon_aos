package com.muyeon.app.ui.castingstage.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.castingstage.model.*
import com.muyeon.app.ui.castingstage.data.*
import com.muyeon.app.ui.castingstage.screens.*

/** 캐스팅·무대 글꼴·색·치수 — 화면 파일들이 함께 쓴다(전엔 파일마다 따로 적었다). */

internal val Bold = TextStyle(fontFamily = customFontFamily, fontWeight = FontWeight.Bold)

internal val Semi = TextStyle(fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold)

internal val Medium = TextStyle(fontFamily = customFontFamily, fontWeight = FontWeight.Medium)

internal val Regular = TextStyle(fontFamily = customFontFamily, fontWeight = FontWeight.Normal)

internal val Brand = Color(0xFFF58232) // 무용연 주황 $primary-500

internal val Blue = Color(0xFFE55E00)

internal val Ink100 = Color(0xFFF8F8F8)

internal val Ink200 = Color(0xFFEAEAEA)

internal val Ink300 = Color(0xFFDADADA)

/** 소식 그림 자리(사진 없을 때) — 분류마다 아이콘을 두지 않는다(11-09 글자만). 무용연 주황 톤온톤. */
internal val StageTint = Color(0xFFC95300)

internal val StageTintBg = Color(0xFFFFF3EA)

internal val StageNewsIcon: androidx.compose.ui.graphics.vector.ImageVector get() = androidx.compose.material.icons.Icons.Filled.Newspaper

internal const val TAB_HEIGHT_DP = 46

internal const val SECTION_TOP_DP = 56

internal val GENRES = listOf("전체", "발레", "한국무용", "현대무용", "실용무용", "뮤지컬", "행사")
