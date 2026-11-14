package com.muyeon.app.ui.quote.respond.decline

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.common.ApiMessageError
import com.muyeon.app.ui.quote.QuoteFull
import com.muyeon.app.ui.quote.respond.QuoteRespondApi
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import kotlinx.coroutines.launch

/**
 * ★ 2026-11-14 견적 보내기 시트 아래 작은 링크 "죄송해요, 다음에 뵐게요" → 사유 시트 → 서버 거절(iOS QuoteDeclineAction 1:1).
 *  모아보기 덱·수강생 상담처럼 상세를 거치지 않고 바로 작성 시트를 여는 곳도 같은 부품 하나로 거절한다.
 *  성공하면 [onDeclined](부르는 쪽이 시트를 닫고 다시 읽는다). 다이렉트 요청에만 보인다(부르는 쪽 판단).
 */
@Composable
fun QuoteDeclineAction(quote: QuoteFull, onDeclined: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val api = remember { QuoteRespondApi(context) }
    val scope = rememberCoroutineScope()
    var open by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val failed = stringResource(R.string.quote_decline_failed)
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(R.string.quote_decline_button), style = AppTypography.Body14, color = AppColors.TextSub,
            textDecoration = TextDecoration.Underline, textAlign = TextAlign.Center,
            modifier = Modifier.clickable { open = true }.padding(8.dp).testTag("quoteDecline.open"),
        )
        error?.let { Text(it, style = AppTypography.Caption12, color = AppColors.Danger) }
    }
    if (open) {
        QuoteDeclineSheet(
            academy = quote.isAcademyRequest, busy = busy,
            onSubmit = { r, detail ->
                if (!busy) scope.launch {
                    busy = true
                    api.decline(quote.id, r, detail)
                        .onSuccess { open = false; error = null; onDeclined() }
                        .onFailure { e -> open = false; error = (e as? ApiMessageError)?.message?.ifBlank { null } ?: failed }
                    busy = false
                }
            },
            onDismiss = { open = false },
        )
    }
}
