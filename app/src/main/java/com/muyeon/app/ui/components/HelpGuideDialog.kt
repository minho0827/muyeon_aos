package com.muyeon.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.CategoryColors
import com.muyeon.app.ui.theme.Dimensions

/** 이용 안내 한 줄 — 아이콘(이모지) · 제목 · 설명. */
data class HelpItem(val icon: String, val title: String, val desc: String)

/** 이용 안내 창 — 웹 components/muyeon/HelpGuideModal.js(연주황 원 아이콘 40 · 제목 15 bold · 설명 13 · [확인]). */
@Composable
fun HelpGuideDialog(title: String, items: List<HelpItem>, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.fillMaxWidth().background(AppColors.Surface, RoundedCornerShape(Dimensions.cardRadius)).padding(Dimensions.spacingXXL),
            verticalArrangement = Arrangement.spacedBy(Dimensions.spacingL),
        ) {
            Text(title, style = AppTypography.Title18, color = AppColors.TextHead)
            items.forEach { it ->
                Row(horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingM), verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(40.dp).background(CategoryColors.Dm.bg, CircleShape), contentAlignment = Alignment.Center) {
                        Text(it.icon, style = AppTypography.Title18)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(it.title, style = AppTypography.Body15.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = AppColors.TextHead)
                        Text(it.desc, style = AppTypography.Sub13, color = AppColors.TextSub, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
            SheetButton(SheetAction(stringResource(R.string.common_confirm), true, onClose), Modifier.fillMaxWidth())
        }
    }
}
