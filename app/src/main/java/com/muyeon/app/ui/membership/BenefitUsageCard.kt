package com.muyeon.app.ui.membership

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.intOrNull
import com.muyeon.app.ui.quote.stringOrNull
import com.muyeon.app.webview.NativeActionRouter
import org.json.JSONArray
import org.json.JSONObject

/**
 * 내 혜택 사용 현황 한 줄(/monetization/membership/me `benefitUsage[]`).
 *
 * ★ 계산·문구·회원유형 필터는 **서버가 한 번만** 한다 — 앱은 그리기만(웹·iOS 와 같은 원칙).
 *   서버와 동시에 만들어지는 필드라 모르는 값·빠진 값은 조용히 넘긴다(opt*).
 *
 * @param kind COUNT(지금 올려둔 개수) | MONTHLY(이번 달 사용) | FLAG(이용 중) | PERIOD(기간)
 * @param limit -1 = 무제한. COUNT/MONTHLY 만.
 * @param parts POSTINGS 의 종류별(채용·대타·캐스팅) 줄.
 * @param action 행을 누르면 열 화면 — 웹 브릿지와 같은 액션명.
 */
data class BenefitUsage(
    val key: String,
    val label: String,
    val kind: String,
    val used: Int?,
    val limit: Int?,
    val parts: List<BenefitUsagePart>,
    val text: String?,
    val sub: String?,
    val warn: Boolean,
    val action: String?,
) {
    /** 진행 막대를 그릴 수 있는가 — 개수형이고 한도가 유한(>0)할 때만. */
    val hasBar: Boolean get() = (kind == "COUNT" || kind == "MONTHLY") && (limit ?: 0) > 0

    companion object {
        fun from(o: JSONObject): BenefitUsage? {
            val key = o.stringOrNull("key")?.takeIf { it.isNotBlank() } ?: return null
            return BenefitUsage(
                key = key,
                label = o.stringOrNull("label")?.takeIf { it.isNotBlank() } ?: key,
                kind = o.stringOrNull("kind").orEmpty(),
                used = o.intOrNull("used"),
                limit = o.intOrNull("limit"),
                parts = o.optJSONArray("parts").objects().mapNotNull(BenefitUsagePart::from),
                text = o.stringOrNull("text")?.takeIf { it.isNotBlank() },
                sub = o.stringOrNull("sub")?.takeIf { it.isNotBlank() },
                warn = o.optBoolean("warn", false),
                action = o.stringOrNull("action")?.takeIf { it.isNotBlank() },
            )
        }

        /** 필드가 없으면(구 서버) null — 화면은 예전 '자동견적 N회 남음' 줄로 폴백한다. */
        fun listFrom(o: JSONObject): List<BenefitUsage>? {
            if (!o.has("benefitUsage") || o.isNull("benefitUsage")) return null
            return o.optJSONArray("benefitUsage").objects().mapNotNull(BenefitUsage::from)
        }
    }
}

/** POSTINGS 종류별 줄 — {label, used, limit, text}. */
data class BenefitUsagePart(val label: String, val used: Int?, val limit: Int?, val text: String?) {
    companion object {
        fun from(o: JSONObject): BenefitUsagePart? {
            val label = o.stringOrNull("label")?.takeIf { it.isNotBlank() } ?: return null
            return BenefitUsagePart(
                label, o.intOrNull("used"), o.intOrNull("limit"),
                o.stringOrNull("text")?.takeIf { it.isNotBlank() },
            )
        }
    }
}

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

/**
 * 행 액션 → 여는 화면. 웹 브릿지와 **같은 표**(NativeActionRouter)를 쓴다.
 *  계획 문서 초안의 액션명(openMyPostings 등)으로 와도 열리게 기존 액션으로 맞춘다.
 */
private fun openBenefitAction(activity: Activity, action: String) {
    val normalized = when (action) {
        "openMyPostings" -> "openMyJobPostings"
        "openStudio" -> "openStudioOps"
        // 레슨 관리 — openLessonBooking 에 레슨 id 가 없으면 레슨 관리 화면을 연다.
        "openLessonManage" -> "openLessonBooking"
        else -> action
    }
    NativeActionRouter.openOrNotice(activity, normalized)
}

/**
 * 멤버십 화면 상단 '내 혜택 사용 현황' 카드 — 웹 `BenefitUsageCard`·iOS `BenefitUsageCard` 와 같은 구성.
 *
 * @param onUpgrade warn 행의 '등급 올리기' — 화면이 다음 등급 탭을 고른다. null 이면 링크를 숨긴다(이미 최고 등급).
 */
@Composable
fun BenefitUsageCard(items: List<BenefitUsage>, onUpgrade: (() -> Unit)?) {
    if (items.isEmpty()) return
    val activity = LocalContext.current as? Activity
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(MuyeonColors.tileIdle).padding(vertical = 6.dp),
    ) {
        Text(
            "내 혜택 사용 현황",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
            lineHeight = 18.sp, color = MuyeonColors.textHead,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
        items.forEachIndexed { i, item ->
            if (i > 0) {
                Box(Modifier.padding(horizontal = 14.dp).fillMaxWidth().height(1.dp).background(MuyeonColors.border))
            }
            BenefitUsageRow(
                item,
                onOpen = item.action?.let { a -> activity?.let { act -> { openBenefitAction(act, a) } } },
                onUpgrade = onUpgrade,
            )
        }
    }
}

@Composable
private fun BenefitUsageRow(item: BenefitUsage, onOpen: (() -> Unit)?, onUpgrade: (() -> Unit)?) {
    val accent = if (item.warn) MuyeonColors.orange else MuyeonColors.primary
    Column(
        Modifier.fillMaxWidth()
            .then(if (onOpen != null) Modifier.clickable(onClick = onOpen) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                item.label,
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                lineHeight = 18.sp, color = MuyeonColors.textHead, modifier = Modifier.weight(1f),
            )
            when {
                // FLAG — 숫자 대신 '이용 중' 초록 배지.
                item.kind == "FLAG" -> Text(
                    item.text ?: "이용 중",
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    lineHeight = 15.sp, color = MuyeonColors.green,
                    modifier = Modifier.clip(RoundedCornerShape(50))
                        .background(MuyeonColors.green.copy(alpha = 0.12f))
                        .padding(horizontal = 9.dp, vertical = 3.dp),
                )
                item.text != null -> Text(
                    item.text,
                    fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    lineHeight = 18.sp, color = if (item.warn) MuyeonColors.orange else MuyeonColors.textHead,
                )
            }
            if (onOpen != null) {
                Text(
                    ">",
                    fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 18.sp,
                    color = MuyeonColors.chevron, modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        if (item.parts.isNotEmpty()) {
            // 공고 — 채용·대타·캐스팅 종류별 미니 줄.
            item.parts.forEach { p -> UsagePartRow(p, accent) }
        } else if (item.hasBar) {
            UsageBar(item.used ?: 0, item.limit ?: 0, accent, Modifier.fillMaxWidth())
        }
        item.sub?.let {
            Text(
                it,
                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp, color = MuyeonColors.textSub,
            )
        }
        if (item.warn && onUpgrade != null) {
            Text(
                "등급 올리기",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                lineHeight = 17.sp, color = MuyeonColors.orange,
                modifier = Modifier.clickable(onClick = onUpgrade).padding(vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun UsagePartRow(p: BenefitUsagePart, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            p.label,
            fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp, color = MuyeonColors.textSub,
            modifier = Modifier.width(44.dp),
        )
        val limit = p.limit ?: 0
        if (limit > 0) {
            UsageBar(p.used ?: 0, limit, accent, Modifier.weight(1f))
        } else {
            Spacer(Modifier.weight(1f))
        }
        Text(
            p.text ?: when {
                limit < 0 -> "무제한"
                else -> "${p.used ?: 0} / ${limit}개"
            },
            fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 16.sp, color = MuyeonColors.textHead,
        )
    }
}

/** 사용량 막대. 한도를 넘겨도(서버 한도 하향 등) 꽉 찬 막대로 멈춘다. */
@Composable
private fun UsageBar(used: Int, limit: Int, color: Color, modifier: Modifier) {
    val ratio = if (limit > 0) (used.toFloat() / limit).coerceIn(0f, 1f) else 0f
    Box(modifier.height(6.dp).clip(RoundedCornerShape(50)).background(MuyeonColors.border)) {
        if (ratio > 0f) {
            Box(Modifier.fillMaxWidth(ratio).fillMaxHeight().clip(RoundedCornerShape(50)).background(color))
        }
    }
}
