package com.muyeon.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.painter.Painter
import com.muyeon.app.ui.theme.AppColors

/**
 * ★ 2026-11-10 사진 불러오는 중 자리 — 인스타그램처럼 옅은 회색 상자만(그림·스피너 없음, 사용자 결정).
 *  AsyncImage 의 placeholder 로 넘긴다. 이미지 모양(clip)을 그대로 따라서 아바타는 회색 원이 된다.
 *  '주소 없음·받지 못함' 그림(fallback/error)은 각자 그대로 둔다.
 */
@Composable
fun imageLoadingPainter(): Painter = remember { ColorPainter(AppColors.ImagePlaceholder) }
