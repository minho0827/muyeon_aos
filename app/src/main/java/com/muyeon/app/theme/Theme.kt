package com.muyeon.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * ★ 2026-11-14 무용연 색 — 예전엔 안드로이드 기본 틀(보라 Purple40)이 그대로라, 색을 안 준 시스템 부품
 *   (아래 시트 바탕·입력칸 테두리·날짜·시간 선택)이 연보라·보라로 보였다. 주황 + 흰 바탕 + 회색 계단으로 바꾼다.
 *   앱은 밝은 화면만 쓴다(다크·배경화면 색 따라가기 없음).
 */
private val LightColorScheme = lightColorScheme(
    primary = AppColor.Primary,
    onPrimary = Color.White,
    primaryContainer = AppColor.Orange50,
    onPrimaryContainer = AppColor.Orange900,
    secondary = AppColor.Gray600,
    onSecondary = Color.White,
    secondaryContainer = AppColor.Gray50,
    onSecondaryContainer = AppColor.Gray900,
    tertiary = AppColor.Orange600,
    background = Color.White,
    onBackground = AppColor.Gray900,
    surface = Color.White,
    onSurface = AppColor.Gray900,
    surfaceVariant = AppColor.Gray50,
    onSurfaceVariant = AppColor.Gray600,
    surfaceTint = Color.White,
    surfaceBright = Color.White,
    surfaceDim = AppColor.Gray50,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = AppColor.Gray50,
    outline = AppColor.Gray150,
    outlineVariant = AppColor.Gray100,
    error = AppColor.SemanticNegative,
)

/** 무용연은 밝은 화면만 — 기본값도 밝게·배경화면 색 끔(호출부 3곳이 이미 이렇게 넘긴다). */
@Composable
fun MuyeonTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
