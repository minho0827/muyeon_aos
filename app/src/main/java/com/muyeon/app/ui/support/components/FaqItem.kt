package com.muyeon.app.ui.support.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.support.Faq

/** FAQ 한 개 — 웹 faq.js details(.toggle-trigger "[분류] 질문" · 펼치면 회색 상자에 답). */
@Composable
fun FaqItem(faq: Faq, open: Boolean, onToggle: () -> Unit) {
    val turn by animateFloatAsState(if (open) 180f else 0f, label = "faqArrow")
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = Dimensions.gutter, vertical = Dimensions.spacingL),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingS),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXXS)) {
                if (faq.category.isNotBlank()) Text("[${faq.category}]", style = AppTypography.Sub13SemiBold, color = AppColors.Primary)
                Text(faq.question, style = AppTypography.CardTitle16, color = AppColors.TextHead)
            }
            Icon(painterResource(R.drawable.ic_web_arrow), null, tint = AppColors.TextHead, modifier = Modifier.size(18.dp).rotate(turn))
        }
        AnimatedVisibility(open) {
            Text(
                faq.answer,
                style = AppTypography.Body14,
                color = AppColors.TextBody,
                modifier = Modifier.padding(start = Dimensions.gutter, end = Dimensions.gutter, bottom = Dimensions.spacingL)
                    .fillMaxWidth().background(AppColors.SurfaceMuted, RoundedCornerShape(Dimensions.spacingS)).padding(Dimensions.spacingL),
            )
        }
        HorizontalDivider(Modifier.padding(horizontal = Dimensions.gutter), thickness = 1.dp, color = AppColors.Divider)
    }
}
