package com.muyeon.app.ui.quote.respond.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.quote.QuoteFull
import com.muyeon.app.ui.quote.QuoteQuestions
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/**
 * 요청 내용(문진 답변 2열) + 첨부 사진 — 웹 QuoteRespond requestDetail.
 *  지역은 위 요약 카드에 이미 나오므로 뺀다. 답 라벨·값은 모아보기 카드와 같은 [QuoteQuestions.describe].
 * @param photoNote 공개 상세 — 사진 대신 '첨부 사진 N장은 강사·학원 회원에게만 보여요.'
 */
@Composable
fun QuoteRequestDetailSection(
    quote: QuoteFull,
    modifier: Modifier = Modifier,
    showPhotos: Boolean = true,
    photoNote: String? = null,
    onOpenPhoto: (List<String>, Int) -> Unit = { _, _ -> },
) {
    val answers = remember(quote) { (quote.answers ?: emptyList()).filter { it.questionId != "region" } }
    val cells = remember(answers) {
        val details = answers.filter { it.questionId == "detail" }.mapNotNull { it.text }.toSet()
        QuoteQuestions.describe(quote.categoryId, answers).map { (q, v) -> QuoteAnswerCell(q, v, v in details || v.length > WIDE_CHARS) }
    }
    val photos = remember(answers) { answers.flatMap { it.images ?: emptyList() } }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimensions.sectionGap - Dimensions.spacingS)) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spacingM)) {
            Text(stringResource(R.string.quote_respond_section_request), style = AppTypography.Section16, color = AppColors.TextHead)
            QuoteAnswerGrid(cells)
            if (photoNote != null) Text(photoNote, style = AppTypography.Sub13, color = AppColors.TextSub)
        }
        if (showPhotos && photos.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimensions.spacingM)) {
                Text(stringResource(R.string.quote_respond_section_photos, photos.size), style = AppTypography.Section16, color = AppColors.TextHead)
                QuotePhotoList(photos, onOpen = { onOpenPhoto(photos, it) })
            }
        }
    }
}

/** 웹 — `it.v.length > 14` 면 한 줄을 다 쓴다. */
private const val WIDE_CHARS = 14
