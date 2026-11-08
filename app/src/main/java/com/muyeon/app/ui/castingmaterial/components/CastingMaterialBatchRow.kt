package com.muyeon.app.ui.castingmaterial.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.muyeon.app.R
import com.muyeon.app.ui.format.KstDates
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.castingmaterial.CastingMaterialBatch
import java.time.format.DateTimeFormatter

/**
 * 올린 자료 한 묶음 — 웹 CastingMaterials 목록 한 칸 그대로:
 *   '상태 · 사진 N장 · M.d HH:mm' / 링크 / (완료) 등록·건너뜀 수 / 등록된 글 ›(누르면 그 공고·공연) / 건너뜀 사유 / (실패) 오류.
 */
@Composable
fun CastingMaterialBatchRow(b: CastingMaterialBatch, onOpenPath: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = Dimensions.spacingM), verticalArrangement = Arrangement.spacedBy(Dimensions.spacingXS)) {
        val status = statusRes(b.status)?.let { stringResource(it) } ?: b.status
        // 웹 fmt — 기기 시간대 기준 'M.d HH:mm'.
        val time = KstDates.parse(b.createdAt)?.atZone(java.time.ZoneId.systemDefault())?.format(TIME).orEmpty()
        Text(
            buildAnnotatedString {
                append(stringResource(R.string.casting_mat_row_head, status, b.imageCount))
                if (time.isNotEmpty()) withStyle(SpanStyle(color = AppColors.TextSub, fontWeight = FontWeight.Normal)) { append(" · $time") }
            },
            style = AppTypography.Body14SemiBold, color = AppColors.TextHead,
        )
        listOfNotNull(b.instagramUrl, b.linkUrl).takeIf { it.isNotEmpty() }?.let {
            Text(it.joinToString(" · "), style = AppTypography.Sub13, color = AppColors.TextSub)
        }
        if (b.status == "DONE") Text(stringResource(R.string.casting_mat_row_counts, b.created.size, b.skipped.size), style = AppTypography.Body14, color = AppColors.TextSub)
        b.created.forEach { c ->
            Text(
                "${c.title} ›", style = AppTypography.Body14SemiBold, color = AppColors.TextBody,
                modifier = Modifier.clickable { c.path?.let(onOpenPath) }.padding(vertical = Dimensions.spacingXXS),
            )
        }
        b.skipped.forEach { s ->
            val what = s.title ?: s.image?.let { stringResource(R.string.casting_mat_row_skipped_image, it + 1) }.orEmpty()
            Text(stringResource(R.string.casting_mat_row_skipped, what, s.reason.orEmpty()), style = AppTypography.Sub13, color = AppColors.TextSub)
        }
        if (b.status == "FAILED") b.errorMessage?.let { Text(it, style = AppTypography.Sub13, color = AppColors.Danger) }
    }
    HorizontalDivider(color = AppColors.Divider)
}

private fun statusRes(status: String): Int? = when (status) {
    "PENDING" -> R.string.casting_mat_status_pending
    "PROCESSING" -> R.string.casting_mat_status_processing
    "DONE" -> R.string.casting_mat_status_done
    "FAILED" -> R.string.casting_mat_status_failed
    else -> null
}

private val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("M.d HH:mm")
