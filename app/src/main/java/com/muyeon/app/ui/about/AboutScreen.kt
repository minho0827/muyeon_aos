package com.muyeon.app.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.muyeon.app.R
import com.muyeon.app.ui.about.components.AboutAudience
import com.muyeon.app.ui.about.components.AboutCta
import com.muyeon.app.ui.about.components.AboutFeatures
import com.muyeon.app.ui.about.components.AboutHero
import com.muyeon.app.ui.about.components.AboutPayment
import com.muyeon.app.ui.components.MainTopBar
import com.muyeon.app.ui.home.components.HomeLegalFooter
import com.muyeon.app.ui.theme.AppColors

/**
 * ★ 2026-11-08 (b4) 서비스 소개 — 웹 /about(pages/muyeon/About.js) 그대로: 첫 화면 · 서비스 4가지 · 대상 · 결제 안내 링크 · 둘러보기 · 법적 고지(펼친 채).
 *
 * @param onHome '무용연 둘러보기'·'지금 둘러보기'(웹 /home)
 * @param onOpenPath 멤버십(/pricing)·약관(/policy?doc=) 주소
 */
@Composable
fun AboutScreen(onBack: () -> Unit, onHome: () -> Unit, onOpenPath: (String) -> Unit) {
    val context = LocalContext.current
    val open: (Intent) -> Unit = { runCatching { context.startActivity(it) } }
    Column(Modifier.fillMaxSize().background(AppColors.Surface).navigationBarsPadding()) {
        MainTopBar(title = stringResource(R.string.about_title), primary = false, onBack = onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            AboutHero(onBrowse = onHome, onPricing = { onOpenPath(PRICING) })
            AboutFeatures()
            AboutAudience()
            AboutPayment(onOpenPath = onOpenPath, pricingPath = PRICING)
            AboutCta(onBrowse = onHome)
            HomeLegalFooter(
                onOpenPath = onOpenPath,
                onDial = { tel -> open(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + tel.replace("-", "")))) },
                onMail = { mail -> open(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$mail"))) },
                defaultOpen = true,
            )
        }
    }
}

private const val PRICING = "/pricing"
