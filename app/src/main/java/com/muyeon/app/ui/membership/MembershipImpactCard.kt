package com.muyeon.app.ui.membership

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * "멤버십 결제하길 잘했다" 를 한 화면 위에서 보여주는 히어로 카드 — iOS `MembershipImpactCard.swift` 1:1.
 *  큰 금액 1개 + 노출·열람·문의 3개 증감률(근거)로 구성한다.
 *  숫자는 0 에서 카운트업한다 — 정지된 숫자보다 '늘었다'가 읽힌다.
 */
@Composable
fun MembershipImpactCard(impact: MembershipImpact, isAcademy: Boolean) {
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            // 원색 주황을 카드 전체에 깔면 눈이 아프다 — 배경은 아주 옅게 두고 강조는 숫자에만 준다.
            .background(Brush.verticalGradient(listOf(MembershipPalette.heroTop, MembershipPalette.heroBottom)))
            .border(1.dp, MembershipPalette.impressionDeep.copy(alpha = 0.16f), RoundedCornerShape(24.dp))
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            if (impact.ready) "멤버십 결제 후 ${impact.windowDays ?: impact.daysSince}일"
            else "멤버십 결제 ${impact.daysSince}일째",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp,
            lineHeight = 14.sp, color = MembershipPalette.impressionDeep,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(MuyeonColors.surface)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )

        val before = impact.before
        val after = impact.after
        if (impact.ready && before != null && after != null) {
            Headline(before, after, isAcademy)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RatePill("노출", before.impressions.toDouble(), after.impressions.toDouble())
                RatePill("프로필 열람", before.detailViews.toDouble(), after.detailViews.toDouble())
                RatePill(
                    if (isAcademy) "상담문의·예약" else "견적요청·채팅",
                    before.leads.toDouble(), after.leads.toDouble(),
                )
            }
            Text(
                "결제 직전 같은 길이 기간과 비교했어요. 금액은 실결제가 아니라 예약·성사된 견적 기준 예상 금액이에요.",
                fontFamily = customFontFamily, fontSize = 11.sp, lineHeight = 15.sp,
                color = MuyeonColors.textSub,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "성과를 모으는 중이에요",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                    lineHeight = 26.sp, color = MuyeonColors.textHead,
                )
                Text(
                    "결제 3일 뒤부터 결제 전과 비교한 성과를 보여드려요.\n하루치로 낸 증감은 그날 운에 좌우돼서요.",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                    lineHeight = 18.sp, color = MuyeonColors.textSub,
                )
            }
        }
    }
}

/** 헤드라인 — 금액이 늘었으면 금액, 아니면 가장 많이 오른 지표로 문장을 만든다. */
@Composable
private fun Headline(
    before: MembershipImpactMetrics,
    after: MembershipImpactMetrics,
    isAcademy: Boolean,
) {
    val money = after.revenue - before.revenue
    val title = when {
        money > 0 -> "결제 전 같은 기간보다 예상 금액이 이만큼 늘었어요"
        else -> topGrowth(before, after, isAcademy)
            ?.let { "결제 전 같은 기간보다 ${it.first}이(가) ${it.second} 늘었어요" }
            ?: "결제 후 ${if (isAcademy) "학원" else "강사님"}을 이만큼 찾아봤어요"
    }
    val target = if (money > 0) money
    else max(after.leads, max(after.detailViews, after.impressions)).toDouble()
    val shown by animateFloatAsState(target.toFloat(), tween(1100), label = "impactHeadline")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            title,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 15.sp,
            lineHeight = 21.sp, color = MuyeonColors.textSub,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            if (money > 0) {
                Text(
                    "+",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 30.sp,
                    lineHeight = 36.sp, color = MembershipPalette.impressionDeep,
                )
            }
            Text(
                "${comma(shown.toDouble())}${if (money > 0) "원" else "회"}",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 40.sp,
                lineHeight = 46.sp, color = MembershipPalette.impressionDeep,
            )
        }
    }
}

/** pill 에 띄울 값 — 이전 구간이 0 이면 증가 '횟수', 아니면 증감 '%'. */
@Composable
private fun RowScope.RatePill(label: String, before: Double, after: Double) {
    val diff = after - before
    val target = if (before <= 0) after else (after - before) / before * 100
    val shown by animateFloatAsState(target.toFloat(), tween(1100), label = "impactRate$label")

    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(MuyeonColors.surface)
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            label,
            fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp,
            lineHeight = 13.sp, color = MuyeonColors.textSub, maxLines = 1,
        )
        Text(
            // 이전 구간이 0 이면 몇 % 늘었다고 말할 수 없다 — 늘어난 횟수를 그대로 적는다.
            if (before <= 0) "${shown.roundToInt()}회"
            else "${if (diff >= 0) "+" else ""}${shown.roundToInt()}%",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp,
            lineHeight = 20.sp, color = MembershipPalette.delta(diff),
        )
    }
}

/** 증가율이 가장 큰 지표(이전 구간이 0 이 아닌 것 중에서) → (이름, 표기). */
private fun topGrowth(
    before: MembershipImpactMetrics,
    after: MembershipImpactMetrics,
    isAcademy: Boolean,
): Pair<String, String>? {
    val candidates = listOf(
        Triple(if (isAcademy) "상담문의" else "견적요청·채팅", before.leads.toDouble(), after.leads.toDouble()),
        Triple("프로필 열람", before.detailViews.toDouble(), after.detailViews.toDouble()),
        Triple("노출", before.impressions.toDouble(), after.impressions.toDouble()),
    )
    return candidates
        .filter { (_, b, a) -> b > 0 && a > b }
        .maxByOrNull { (_, b, a) -> a / b }
        ?.let { (name, b, a) ->
            val times = a / b
            // 2배가 넘으면 "2.4배"가 "140%"보다 훨씬 빨리 읽힌다.
            val text = if (times >= 2) String.format(Locale.KOREA, "%.1f배", times)
            else "${((times - 1) * 100).roundToInt()}%"
            name to text
        }
}

internal fun comma(v: Double): String = String.format(Locale.KOREA, "%,d", v.roundToInt())
