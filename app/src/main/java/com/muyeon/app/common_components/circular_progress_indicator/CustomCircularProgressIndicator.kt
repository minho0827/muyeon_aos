package com.muyeon.app.common_components.circular_progress_indicator

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muyeon.app.theme.AppColor

@Composable
fun CustomCircularProgressIndicator() {
    CircularProgressIndicator(
        modifier = Modifier.size(28.dp),
        strokeWidth = 3.dp,
        color = AppColor.RealWhite
    )
}