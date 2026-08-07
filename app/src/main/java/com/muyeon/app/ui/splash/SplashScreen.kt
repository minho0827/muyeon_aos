package com.muyeon.app.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 스플래시 — iOS `SplashView.swift` 1:1 이식.
 *  흰 배경 전체 + 세로 가운데 정렬, VStack spacing 12.
 *   1행 "무용연 (舞踊緣)" 32sp Bold 검정
 *   2행 "무용으로 맺어지는 모든 인연의 시작" 15sp Regular 회색(white 0.4 = #666666) 가운데
 *  (후니드 주황 배경 + 로고 이미지였던 것을 교체)
 */
@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "무용연 (舞踊緣)",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
        )
        Text(
            text = "무용으로 맺어지는 모든 인연의 시작",
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF666666), // iOS Color(white: 0.4)
            textAlign = TextAlign.Center,
        )
    }
}
