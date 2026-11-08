package com.muyeon.app.ui.policy.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import com.muyeon.app.R
import com.muyeon.app.ui.theme.AppColors
import com.muyeon.app.ui.theme.AppTypography
import com.muyeon.app.ui.theme.Dimensions
import com.muyeon.domain.model.policy.PolicyView

/**
 * 약관 맨 위 안내 한 줄 — 웹 Policy.js `.t-13-single.fc-s` + `<Link>`.
 *   다른 버전(지난·시행 예정)을 보는 중이면 '…버전입니다. [현재 버전 보기]', 현재 문서에 시행 예정 개정이 있으면 '…적용됩니다. [개정 내용 보기]'.
 */
@Composable
fun PolicyNotice(view: PolicyView, versionRequested: Boolean, onOpenVersion: (String?) -> Unit) {
    val shown = view.shown
    val upcoming = view.current?.upcoming
    when {
        view.isOther && shown != null -> NoticeLine(
            stringResource(if (shown.status == "SCHEDULED") R.string.policy_version_scheduled else R.string.policy_version_past, shown.effectiveDate.orEmpty()),
            stringResource(R.string.policy_current_link),
        ) { onOpenVersion(null) }
        !versionRequested && upcoming != null -> NoticeLine(
            upcoming.changeSummary?.takeIf { it.isNotBlank() }
                ?.let { stringResource(R.string.policy_upcoming_summary, upcoming.effectiveDate.orEmpty(), it) }
                ?: stringResource(R.string.policy_upcoming, upcoming.effectiveDate.orEmpty()),
            stringResource(R.string.policy_upcoming_link),
        ) { onOpenVersion(upcoming.version) }
    }
}

@Composable
private fun NoticeLine(text: String, link: String, onLink: () -> Unit) {
    val styles = TextLinkStyles(SpanStyle(color = AppColors.Primary, textDecoration = TextDecoration.Underline))
    Text(
        buildAnnotatedString {
            append(text); append(" ")
            withLink(LinkAnnotation.Clickable("link", styles) { onLink() }) { append(link) }
        },
        style = AppTypography.Sub13, color = AppColors.TextSub, modifier = Modifier.padding(bottom = Dimensions.spacingS),
    )
}
