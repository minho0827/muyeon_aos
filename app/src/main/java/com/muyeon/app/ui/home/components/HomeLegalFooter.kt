package com.muyeon.app.ui.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography

/**
 * 약관·사업자 정보 — 웹 components/muyeon/HomeLegalFooter.js. 정책 링크 · 고객센터/이메일 · 접힌 사업자 정보 · 중개자 안내 · 저작권.
 *   전화·메일은 기기 앱으로([onDial]·[onMail]), 정책은 웹 창([onOpenPath]).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeLegalFooter(
    onOpenPath: (String) -> Unit, onDial: (String) -> Unit, onMail: (String) -> Unit, modifier: Modifier = Modifier,
    /** (b4) 사업자 정보를 펼친 채로 — 웹 About 의 `<HomeLegalFooter defaultOpen />`. */
    defaultOpen: Boolean = false,
) {
    var open by rememberSaveable { mutableStateOf(defaultOpen) }
    val tel = stringResource(R.string.company_tel)
    val email = stringResource(R.string.company_email)
    val links = listOf(
        "/about" to R.string.legal_about, "/policy?doc=terms" to R.string.legal_terms, "/policy?doc=paid" to R.string.legal_paid,
        "/policy?doc=privacy" to R.string.legal_privacy, "/policy?doc=location" to R.string.legal_location,
        "/policy?doc=refund" to R.string.legal_refund, "/policy?doc=company" to R.string.legal_company,
    )
    Column(modifier.fillMaxWidth().background(AppColors.SurfaceMuted).padding(horizontal = 20.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            links.forEach { (path, label) -> Text(stringResource(label), style = AppTypography.Caption12, color = AppColors.TextSub, modifier = Modifier.clickable { onOpenPath(path) }) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.legal_tel, tel), style = AppTypography.Caption12, color = AppColors.TextSub, modifier = Modifier.clickable { onDial(tel) })
            Text(stringResource(R.string.legal_email, email), style = AppTypography.Caption12, color = AppColors.TextSub, modifier = Modifier.clickable { onMail(email) })
            Text(stringResource(R.string.legal_report_sla), style = AppTypography.Caption12, color = AppColors.TextSub)
        }
        Text(stringResource(R.string.legal_company_toggle, stringResource(R.string.company_name)) + if (open) " ▴" else " ▾",
            style = AppTypography.Caption12.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), color = AppColors.TextBody, modifier = Modifier.clickable { open = !open })
        AnimatedVisibility(open) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    R.string.legal_ceo to R.string.company_ceo, R.string.legal_biz_no to R.string.company_biz_no,
                    R.string.legal_address to R.string.company_address, R.string.legal_cs to R.string.company_tel,
                    R.string.legal_cs_email to R.string.company_email, R.string.legal_hosting to R.string.company_hosting,
                ).forEach { (k, v) ->
                    Row { Text(stringResource(k), style = AppTypography.Caption12, color = AppColors.TextSub, modifier = Modifier.padding(end = 8.dp)); Text(stringResource(v), style = AppTypography.Caption12, color = AppColors.TextBody) }
                }
            }
        }
        Text(stringResource(R.string.company_marketplace_notice), style = AppTypography.Caption12, color = AppColors.TextHint)
        Text(stringResource(R.string.legal_copyright), style = AppTypography.Caption12, color = AppColors.TextHint)
    }
}
