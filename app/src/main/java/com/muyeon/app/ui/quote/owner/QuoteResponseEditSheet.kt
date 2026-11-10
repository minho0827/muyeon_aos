package com.muyeon.app.ui.quote.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.R
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.lesson.LessonRefundPolicyRepo
import com.muyeon.app.ui.quote.QuoteAmountField
import com.muyeon.app.ui.quote.QuoteDepositField
import com.muyeon.app.ui.quote.QuoteMessageField
import kotlinx.coroutines.launch

/**
 * 보낸 제안 고치기 — 보내기 시트와 같은 칸(금액 · 예약금 · 메시지)을 보낸 값으로 채워 연다. 프로필 첨부는 고칠 수 없다(서버도 안 받는다).
 * @param onSave 고친 칸 → 성공 여부. 실패면 시트를 열어 둔 채 버튼을 되살린다(문구는 부르는 쪽이 띄운다).
 */
@Composable
fun QuoteResponseEditSheet(
    categoryLabel: String,
    sent: QuoteResponseDraft,
    onDismiss: () -> Unit,
    onSave: suspend (QuoteResponseDraft) -> Boolean,
) {
    val scope = rememberCoroutineScope()
    var amountText by remember { mutableStateOf(sent.priceAmount?.toString().orEmpty()) }
    var takesDeposit by remember { mutableStateOf(sent.depositAmount != null) }
    var depositAmount by remember { mutableIntStateOf(sent.depositAmount ?: 10_000) }
    var message by remember { mutableStateOf(sent.message) }
    var policy by remember { mutableStateOf(LessonRefundPolicyRepo.current()) }
    LaunchedEffect(Unit) { policy = LessonRefundPolicyRepo.get() }
    var saving by remember { mutableStateOf(false) }
    val problem = if (takesDeposit) amountText.trim().toIntOrNull()?.let { policy.depositError(depositAmount, it) } else null
    val canSave = message.trim().isNotEmpty() && !saving && problem == null

    androidx.compose.ui.window.Dialog(onDismissRequest = { if (!saving) onDismiss() }) {
        Column(Modifier.clip(RoundedCornerShape(16.dp)).background(MuyeonColors.surface).testTag("quoteResponse.editSheet")) {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    stringResource(R.string.quote_owner_edit_title, categoryLabel),
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 20.sp, color = MuyeonColors.textHead,
                )
                Text(stringResource(R.string.quote_owner_edit_note), fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp, color = MuyeonColors.textSub)
                QuoteAmountField(amountText) { amountText = it }
                QuoteDepositField(takesDeposit, { takesDeposit = it }, depositAmount, { depositAmount = it }, policy, problem)
                QuoteMessageField(message) { message = it }
            }
            Text(
                stringResource(if (saving) R.string.quote_owner_saving else R.string.quote_owner_edit_save),
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 19.sp,
                color = Color.White, textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (canSave) MuyeonColors.primary else Color.Gray.copy(alpha = 0.4f))
                    .clickable(enabled = canSave) {
                        saving = true
                        val draft = QuoteResponseDraft(
                            priceAmount = amountText.toIntOrNull(), priceUnit = sent.priceUnit,
                            depositAmount = if (takesDeposit) depositAmount else null, message = message.trim(),
                        )
                        scope.launch { if (!onSave(draft)) saving = false }
                    }
                    .padding(vertical = 15.dp)
                    .testTag("quoteResponse.editSave"),
            )
        }
    }
}
