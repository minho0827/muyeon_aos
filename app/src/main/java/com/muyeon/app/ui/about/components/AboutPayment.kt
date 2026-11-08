package com.muyeon.app.ui.about.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions

/** 결제 안내 — 웹 .about-payment(머리 + 설명 + 링크 3줄 '… →'). */
@Composable
fun AboutPayment(onOpenPath: (String) -> Unit, pricingPath: String) {
    val links = listOf(
        pricingPath to R.string.about_pay_pricing, "/policy?doc=refund" to R.string.about_pay_refund, "/policy?doc=terms" to R.string.about_pay_terms,
    )
    Column(Modifier.fillMaxWidth().padding(horizontal = Dimensions.gutter, vertical = 72.dp)) {
        AboutSectionHead(stringResource(R.string.about_pay_eyebrow), stringResource(R.string.about_pay_title), stringResource(R.string.about_pay_desc))
        Column(Modifier.padding(top = 32.dp)) {
            links.forEach { (path, label) ->
                HorizontalDivider(thickness = 1.dp, color = AppColors.Divider)
                Row(Modifier.fillMaxWidth().clickable { onOpenPath(path) }.padding(vertical = 20.dp)) {
                    Text(stringResource(label), style = AppTypography.Body15.copy(fontWeight = FontWeight.SemiBold), color = AppColors.TextHead, modifier = Modifier.weight(1f))
                    Text(ARROW, style = AppTypography.Body15, color = AppColors.Primary)
                }
            }
            HorizontalDivider(thickness = 1.dp, color = AppColors.Divider)
        }
    }
}

private const val ARROW = "→"
