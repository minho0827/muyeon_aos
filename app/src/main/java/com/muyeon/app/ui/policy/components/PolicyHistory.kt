package com.muyeon.app.ui.policy.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.policy.PolicyVersion

/** 이전 버전 목록 — 웹 Policy.js '이전 버전'(h4 + ul.disc 의 Link). 현재 버전을 볼 때만, 지난 버전이 있을 때만. */
@Composable
fun PolicyHistory(history: List<PolicyVersion>, onOpenVersion: (String?) -> Unit) {
    if (history.isEmpty()) return
    Column {
        Text(stringResource(R.string.policy_history), style = AppTypography.Section16, color = AppColors.TextHead,
            modifier = Modifier.padding(top = Dimensions.spacingL, bottom = Dimensions.spacingM))
        PolicyBullets(history.map { it.version }) { i, _ ->
            val h = history[i]
            Text(stringResource(R.string.policy_history_item, h.effectiveDate.orEmpty()), style = AppTypography.Body14.copy(textDecoration = TextDecoration.Underline),
                color = AppColors.Primary, modifier = Modifier.clickable { onOpenVersion(h.version) })
        }
    }
}
