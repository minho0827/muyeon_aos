package com.muyeon.app.ui.policy.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.policy.PolicyBlock

/** 약관 본문 — 웹 renderMd 결과(.privacy-policy 의 h3 · h4 · p · ul.disc) 그대로. 소제목 위 16, 목록 왼쪽 들여쓰기 16. */
@Composable
fun PolicyBlocks(blocks: List<PolicyBlock>) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spacingM)) {
        blocks.forEach { b ->
            when (b) {
                is PolicyBlock.Heading -> Text(b.text, style = AppTypography.Title18, color = AppColors.TextHead, modifier = Modifier.padding(top = Dimensions.spacingS))
                is PolicyBlock.SubHeading -> Text(b.text, style = AppTypography.Section16, color = AppColors.TextHead, modifier = Modifier.padding(top = Dimensions.spacingL))
                is PolicyBlock.Paragraph -> Text(b.text, style = AppTypography.Body14, color = AppColors.TextBody)
                is PolicyBlock.Bullets -> PolicyBullets(b.items)
            }
        }
    }
}

@Composable
fun PolicyBullets(items: List<String>, content: (@Composable (Int, String) -> Unit)? = null) {
    Column(Modifier.padding(start = BULLET_INDENT), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXS)) {
        items.forEachIndexed { i, t ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
                Text("•", style = AppTypography.Body14, color = AppColors.TextBody)
                if (content != null) content(i, t) else Text(t, style = AppTypography.Body14, color = AppColors.TextBody)
            }
        }
    }
}

private val BULLET_INDENT = 6.dp
