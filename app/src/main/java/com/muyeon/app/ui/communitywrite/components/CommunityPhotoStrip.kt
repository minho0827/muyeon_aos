package com.muyeon.app.ui.communitywrite.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.components.RemoteImage
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/**
 * 사진 여러 장 — [+ n/max] 칸 · 정사각형 미리보기 가로 줄 · 각 사진 오른쪽 위 ×(웹 ImageUploader multi · 앱 상세 이미지 칸과 같은 모양).
 *   iOS Shell/CommunityWrite/Components/CommunityPhotoStrip.swift 1:1.
 */
@Composable
fun CommunityPhotoStrip(images: List<String>, max: Int, uploading: Boolean, onAdd: () -> Unit, onRemove: (Int) -> Unit) {
    val side = 96.dp
    val shape = RoundedCornerShape(10.dp)
    Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = Dimensions.spacingXXS), horizontalArrangement = Arrangement.spacedBy(Dimensions.spacingS)) {
        if (images.size < max) {
            val add = stringResource(R.string.b3_cwrite_photo_add)
            Column(
                Modifier.size(side).clip(shape).background(AppColors.SurfaceMuted).border(1.dp, AppColors.ChipBorder, shape)
                    .clickable(enabled = !uploading, onClick = onAdd).testTag("cwrite.photo.add"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXS, Alignment.CenterVertically),
            ) {
                if (uploading) CircularProgressIndicator(color = AppColors.Primary, modifier = Modifier.size(20.dp))
                else {
                    Icon(Icons.Filled.AddPhotoAlternate, add, tint = AppColors.TextSub, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.b3_cwrite_count, images.size, max), style = AppTypography.Caption12, color = AppColors.TextSub)
                }
            }
        }
        images.forEachIndexed { i, url ->
            Box {
                RemoteImage(url, Modifier.size(side).clip(shape))
                Icon(
                    Icons.Filled.Close, stringResource(R.string.b3_cwrite_photo_remove), tint = AppColors.TextOnPrimary,
                    modifier = Modifier.align(Alignment.TopEnd).padding(Dimensions.spacingXS).size(16.dp)
                        .clip(CircleShape).background(AppColors.Scrim).clickable { onRemove(i) },
                )
            }
        }
    }
}
