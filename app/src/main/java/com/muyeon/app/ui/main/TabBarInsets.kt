package com.muyeon.app.ui.main

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.muyeon.app.ui.theme.Dimensions

/** 탭 화면 목록 맨 아래 여백 — 떠 있는 탭바와 내비게이션 바에 마지막 항목이 가리지 않게(PaceERA tabBarClearance). */
@Composable
fun tabBarBottomPadding(): Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + Dimensions.tabBarClearance
