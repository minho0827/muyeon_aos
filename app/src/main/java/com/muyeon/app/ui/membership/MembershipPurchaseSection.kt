package com.muyeon.app.ui.membership

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.ProductDetails
import com.muyeon.app.result.findActivity
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.DialogAction
import com.muyeon.app.ui.quote.DialogMessage
import com.muyeon.app.ui.quote.DialogTitle
import com.muyeon.app.utils.Constants
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * 멤버십 화면의 기간 선택 + 구글 플레이 구독 버튼 — iOS `MembershipPurchaseSection.swift` 이식.
 *
 * 결제 수단은 계정에 하나:
 *  - billingProvider == TOSS / APPSTORE → 웹·아이폰에서 결제 중. 구매 버튼을 숨기고 안내만(이중 결제 방지).
 *                                         ★ 웹 결제로 보내는 링크는 두지 않는다(외부 결제 유도 금지).
 *  - billingProvider == PLAYSTORE       → 구글 구독 중. 다른 상품을 고르면 [변경하기] — 등급 변경은 결제 금액이 오르면 즉시,
 *                                         내려가면 다음 갱신일부터. 같은 등급의 기간 변경은 즉시(새 기간 요금 결제, 남은 기간 이어짐).
 *  - null                               → [구독하기].
 *
 * 가격은 서버 priceKrw 가 아니라 **구글이 내려준 가격**을 우선 보여준다(실제 청구 금액과 같아야 한다).
 *  구글 상품을 못 받았을 때만 서버 가격으로 대신한다(그때는 구매 버튼이 꺼진다).
 */
@Composable
internal fun MembershipPurchaseSection(
    tier: String,
    plans: List<MembershipPlan>,
    mine: MyMembership?,
    isLoggedIn: Boolean,
    copy: MembershipCopy?,
) {
    val context = LocalContext.current
    val products by MembershipBilling.products.collectAsState()
    val purchasingId by MembershipBilling.purchasingId.collectAsState()
    val isRestoring by MembershipBilling.isRestoring.collectAsState()
    var selectedCode by remember { mutableStateOf<String?>(null) }

    val provider = mine?.billingProvider
    val currentProductId = mine?.googleProductId?.takeIf { provider == "PLAYSTORE" }
    // 서버가 기간(basePlanId)을 안 내려주면 이 기기에서 마지막으로 결제한 기간으로 대신한다.
    val currentBasePlanId = currentProductId?.let { mine.googleBasePlanId ?: MembershipBilling.lastBasePlan(it) }
    fun isCurrent(p: MembershipPlan) =
        currentProductId != null && p.googleProductId == currentProductId && p.googleBasePlanId == currentBasePlanId

    // 이용 중인 상품이 이 등급에 있으면 그것, 아니면 첫 기간(1개월).
    LaunchedEffect(tier, plans, currentProductId, currentBasePlanId) {
        selectedCode = (plans.firstOrNull(::isCurrent) ?: plans.firstOrNull())?.code
    }

    val monthlyPhase = plans.firstOrNull { monthsOf(it, products) == 1 }
        ?.let { MembershipBilling.recurringPhase(products[it.googleProductId], it.googleBasePlanId) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        plans.forEach { p ->
            val phase = MembershipBilling.recurringPhase(products[p.googleProductId], p.googleBasePlanId)
            DurationRow(
                plan = p, months = monthsOf(p, products), phase = phase, monthlyPhase = monthlyPhase,
                selected = selectedCode == p.code, current = isCurrent(p),
                onClick = { if (phase != null) selectedCode = p.code },
            )
        }
        if (plans.isNotEmpty() && plans.all { MembershipBilling.offerFor(products[it.googleProductId], it.googleBasePlanId) == null }) {
            Note("Google Play 상품 정보를 불러오지 못했어요. 잠시 후 다시 열어 주세요.")
        }

        when {
            provider == "APPSTORE" ->
                Note("아이폰(App Store)에서 결제한 멤버십을 이용 중이에요. 결제 변경·해지는 아이폰의 App Store 구독 관리에서 할 수 있어요.")
            // TOSS 등 다른 결제 수단 — 웹으로 보내는 링크는 두지 않는다.
            provider != null && provider != "PLAYSTORE" ->
                Note("웹에서 결제한 멤버십을 이용 중이에요. 결제 변경·해지는 결제하신 곳에서 할 수 있어요.")
            !isLoggedIn -> Note("로그인한 뒤 구독할 수 있어요.")
            else -> {
                val selected = plans.firstOrNull { it.code == selectedCode }
                val purchasable = selected != null &&
                    MembershipBilling.offerFor(products[selected.googleProductId], selected.googleBasePlanId) != null
                val busy = purchasingId != null || isRestoring
                val current = selected != null && isCurrent(selected)
                val enabled = purchasable && !busy && !current
                Row(
                    Modifier.padding(top = 6.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(if (enabled) MuyeonColors.primary else MuyeonColors.tileLocked)
                        .clickable(enabled = enabled) {
                            val plan = selected ?: return@clickable
                            val activity = context.findActivity() ?: return@clickable
                            // 결제 창을 다녀오면 이 화면이 다시 만들어질 수 있다 — 결과는 앱 범위(MembershipBilling.notice)로 받는다.
                            MembershipBilling.launchPurchase(activity, plan, currentBasePlanId) { o ->
                                when (o) {
                                    is MembershipBilling.Outcome.Success ->
                                        if (o.deferred) {
                                            "변경 예약 완료" to "다음 갱신일부터 ${TierInfo.label(plan.tier, copy)} ${plan.months}개월 상품으로 바뀌어요."
                                        } else {
                                            "구독 완료" to "${TierInfo.label(o.mine?.tier ?: plan.tier, copy)} 멤버십 혜택이 열렸어요."
                                        }
                                    MembershipBilling.Outcome.Pending -> "결제 대기 중" to "결제가 완료되면 자동으로 반영돼요."
                                    MembershipBilling.Outcome.Cancelled -> null
                                    is MembershipBilling.Outcome.Failed -> "구독하지 못했어요" to o.message
                                }
                            }
                        }
                        .padding(vertical = 15.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (purchasingId != null) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        when {
                            current -> "이용 중인 상품이에요"
                            provider == "PLAYSTORE" -> "이 상품으로 변경하기"
                            else -> "구독하기"
                        },
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        lineHeight = 19.sp, color = Color.White,
                    )
                }

                if (provider == "PLAYSTORE") {
                    Note("등급을 바꾸면 결제 금액이 오를 때는 바로, 내려갈 때는 다음 갱신일부터 적용돼요. 같은 등급의 기간을 바꾸면 바로 적용되고 새 기간 요금이 결제돼요(남은 기간은 이어져요).")
                    // 해지·결제 수단 변경·환불은 구글이 처리한다 — 앱은 Play 구독 관리 화면만 연다.
                    LinkText("구독 관리 · 해지 (Google Play)", bold = true) {
                        openUrl(context, MembershipBilling.manageUrl(context.packageName, currentProductId))
                    }
                }
            }
        }
    }

    // 화면 반영은 MembershipBilling.changes 를 듣는 MembershipScreen 이 한다.
    val notice by MembershipBilling.notice.collectAsState()
    notice?.let { (title, message) -> InfoDialog(title, message) { MembershipBilling.consumeNotice() } }
}

/** 기간 행 — 개월 수 · 월 평균 · 할인율 · 가격. */
@Composable
private fun DurationRow(
    plan: MembershipPlan,
    months: Int,
    phase: ProductDetails.PricingPhase?,
    monthlyPhase: ProductDetails.PricingPhase?,
    selected: Boolean,
    current: Boolean,
    onClick: () -> Unit,
) {
    val priceText = phase?.formattedPrice ?: plan.priceKrw?.let { String.format(Locale.KOREA, "%,d원", it) } ?: "-"
    val perMonth = when {
        months <= 1 -> null
        phase != null -> formatMicros(phase.priceAmountMicros / months, phase.priceCurrencyCode)
        plan.priceKrw != null -> String.format(Locale.KOREA, "%,d원", plan.priceKrw / months)
        else -> null
    }
    // 같은 등급 1개월 상품을 n번 낸 값 대비 할인율(구글 가격 기준, 없으면 서버 정가 기준).
    val discount = if (phase != null && monthlyPhase != null && months > 1) {
        val full = monthlyPhase.priceAmountMicros * months
        if (full > 0 && phase.priceAmountMicros < full) Math.round((full - phase.priceAmountMicros) * 100.0 / full).toInt() else 0
    } else plan.discountPct

    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .border(
                if (selected) 2.dp else 1.dp,
                if (selected) MuyeonColors.primary else MuyeonColors.border,
                RoundedCornerShape(12.dp),
            )
            .clickable(enabled = phase != null, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 선택 표시(라디오)
        Spacer(
            Modifier.size(18.dp).clip(CircleShape)
                .border(if (selected) 5.dp else 1.5.dp, if (selected) MuyeonColors.primary else MuyeonColors.tileLocked, CircleShape),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "${months}개월",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textHead,
                )
                if (current) {
                    Text(
                        "이용 중",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp,
                        lineHeight = 14.sp, color = MuyeonColors.primary,
                        modifier = Modifier.clip(RoundedCornerShape(50))
                            .background(MuyeonColors.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            perMonth?.let {
                Text(
                    "월 평균 $it",
                    fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 17.sp, color = MuyeonColors.textSub,
                )
            }
        }
        if (discount > 0) {
            Text(
                "$discount%",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                lineHeight = 15.sp, color = Color.White,
                modifier = Modifier.padding(end = 8.dp).clip(RoundedCornerShape(50))
                    .background(MuyeonColors.primary)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Text(
            priceText,
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = MuyeonColors.textHead,
        )
    }
}

/**
 * 구독 필수 안내 + 구매 복원 + 약관 — iOS `MembershipLegalFooter` 의 구글 판.
 *  자동 갱신·해지 방법(Google Play)·환불 주체를 구매 버튼 가까이에 밝힌다(Play 구독 정책).
 */
@Composable
internal fun MembershipLegalFooter(isLoggedIn: Boolean, notices: List<String>) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isRestoring by MembershipBilling.isRestoring.collectAsState()
    var restoreMessage by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MuyeonColors.tileIdle).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "구독 안내",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            lineHeight = 18.sp, color = MuyeonColors.textHead,
        )
        (listOf(
            "결제는 구매 확인 시 Google Play 계정으로 청구됩니다.",
            "구독은 선택한 기간(1·3·6개월)마다 같은 기간·가격으로 자동 갱신되며, 갱신 요금은 다음 결제일에 청구됩니다.",
            "자동 갱신은 언제든 Google Play > 결제 및 구독 > 구독에서 해지할 수 있습니다. 해지해도 남은 기간까지 혜택이 유지됩니다.",
            "환불은 Google Play 환불 정책에 따라 Google Play 가 처리합니다.",
        ) + notices).forEach {
            Text(
                "- $it",
                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 17.sp, color = MuyeonColors.textSub,
            )
        }

        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf("이용약관" to "terms", "유료서비스 이용약관" to "paid", "개인정보처리방침" to "privacy").forEach { (title, doc) ->
                LinkText(title) { openPolicy(context, doc) }
            }
        }
        LinkText("취소·환불 정책") { openPolicy(context, "refund") }

        if (isLoggedIn) {
            Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isRestoring) {
                    CircularProgressIndicator(Modifier.size(12.dp), color = MuyeonColors.textSub, strokeWidth = 1.5.dp)
                    Spacer(Modifier.width(6.dp))
                }
                LinkText("구매 복원", bold = true, enabled = !isRestoring) {
                    scope.launch {
                        when (val o = MembershipBilling.restore()) {
                            is MembershipBilling.Outcome.Success -> restoreMessage = "구독을 복원했어요."
                            MembershipBilling.Outcome.Pending -> restoreMessage = "결제가 완료되면 자동으로 반영돼요."
                            is MembershipBilling.Outcome.Failed -> restoreMessage = o.message
                            MembershipBilling.Outcome.Cancelled -> Unit
                        }
                    }
                }
            }
        }
    }

    restoreMessage?.let { InfoDialog("구매 복원", it) { restoreMessage = null } }
}

@Composable
private fun Note(text: String) {
    Text(
        text, fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = MuyeonColors.textSub,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun LinkText(text: String, bold: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        text,
        fontFamily = customFontFamily, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        fontSize = if (bold) 13.sp else 12.sp, lineHeight = 17.sp, color = MuyeonColors.body,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
    )
}

@Composable
private fun InfoDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitle(title) },
        text = { DialogMessage(message) },
        confirmButton = { TextButton(onClick = onDismiss) { DialogAction("확인") } },
    )
}

/** 구글 상품의 결제 주기(P1M·P3M·P6M·P1Y)로 개월 수. 모르면 서버 durationDays 기준. */
private fun monthsOf(plan: MembershipPlan, products: Map<String, ProductDetails>): Int {
    val period = MembershipBilling.recurringPhase(products[plan.googleProductId], plan.googleBasePlanId)?.billingPeriod
    val m = period?.let { Regex("""P(\d+)([MY])""").matchEntire(it) }
    return m?.destructured?.let { (n, unit) -> n.toInt() * (if (unit == "Y") 12 else 1) } ?: plan.months
}

private fun formatMicros(micros: Long, currencyCode: String): String {
    if (currencyCode == "KRW") return String.format(Locale.KOREA, "%,d원", Math.round(micros / 1_000_000.0))
    return runCatching {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply { currency = Currency.getInstance(currencyCode) }
            .format(micros / 1_000_000.0)
    }.getOrDefault("")
}

/** 약관 — embed=1: 웹이 자체 뒤로가기를 그리지 않게(iOS MembershipLegalFooter 와 같은 규칙). */
private fun openPolicy(context: Context, doc: String) {
    openUrl(context, "${Constants.getBaseUrl(context).trimEnd('/')}/policy?doc=$doc&embed=1")
}

private fun openUrl(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}
