package com.muyeon.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/**
 * 밑줄 글자 탭 줄 — 컬리 위쪽 탭(추천·베스트…)과 같은 모양: 고른 탭은 굵은 글자 + 아래 막대, 나머지는 회색. 아래 얇은 구분선.
 *   고른 탭은 검정 글자 + 검정 막대(2026-11-08 사용자 요청). 캐스팅(캐스팅·공연) · 공간(대관·양도양수) 탭 안 나눔과 홈 위쪽 탭이 같이 쓴다.
 */
@Composable
fun UnderlineTabs(options: List<ChipOption>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(AppColors.Surface)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
            options.forEach { o ->
                val on = o.value == selected
                val color by animateColorAsState(if (on) AppColors.TextHead else AppColors.TextSub, label = "underlineTab")
                Column(Modifier.width(IntrinsicSize.Max).clickable { onSelect(o.value) }.padding(horizontal = 12.dp)) {
                    Text(o.text, style = AppTypography.Body15Medium.copy(fontWeight = if (on) FontWeight.Bold else FontWeight.Medium), color = color,
                        modifier = Modifier.padding(top = 12.dp, bottom = 10.dp))
                    Box(Modifier.fillMaxWidth().height(3.dp).background(if (on) AppColors.TextHead else AppColors.Surface))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppColors.Divider))
    }
}
