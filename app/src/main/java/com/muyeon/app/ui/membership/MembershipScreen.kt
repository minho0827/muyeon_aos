package com.muyeon.app.ui.membership

import com.muyeon.app.result.launchScreen
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.muyeon.app.BuildConfig
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.AutoQuoteHistoryActivity
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.ui.quote.intOrNull
import com.muyeon.app.ui.quote.map
import com.muyeon.app.ui.quote.stringOrNull
import com.muyeon.app.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * 멤버십/이용권 — iOS `Membership/MembershipView.swift` 이식.
 *  featureType 별 플랜 목록 + 보유 이용권(entitlements) + 구매.
 */
/**
 * @param durationDays 서버 필드명은 `durationDays` 다.
 *   예전에 `periodDays` 로 읽어 **기간이 항상 비어 있었다**(가격 옆 기간 표시가 통째로 사라짐).
 */
data class MembershipPlan(
    val code: String,
    val name: String?,
    val priceKrw: Int?,
    val listPriceKrw: Int?,
    val durationDays: Int?,
    val tier: String?,
    val limits: MembershipLimits?,
    // PASS(기간권) | SUBSCRIPTION(월 자동결제) — 안내 문구를 판매 방식에 맞춘다(웹 MembershipPlans 와 같은 판정).
    val planKind: String? = null,
    /** 구글 플레이 구독 상품 id·기본 요금제 id. channel=PLAYSTORE 로 받은 목록에만 있다. */
    val googleProductId: String? = null,
    val googleBasePlanId: String? = null,
) {
    val months: Int get() = maxOf(1, Math.round((durationDays ?: 30) / 30.0).toInt())
    val discountPct: Int
        get() {
            val list = listPriceKrw ?: 0
            val price = priceKrw ?: 0
            return if (list > price && list > 0) Math.round((list - price) * 100.0 / list).toInt() else 0
        }

    companion object {
        fun from(o: JSONObject): MembershipPlan {
            val meta = o.optJSONObject("meta")
            return MembershipPlan(
                o.optString("code"), o.stringOrNull("name"), o.intOrNull("priceKrw"),
                o.intOrNull("listPriceKrw"), o.intOrNull("durationDays"),
                meta?.stringOrNull("tier"),
                meta?.optJSONObject("limits")?.let(MembershipLimits::from),
                o.stringOrNull("planKind"),
                o.textOrNull("googleProductId"), o.textOrNull("googleBasePlanId"),
            )
        }
    }
}

/** 등급이 정하는 한도. -1 = 무제한, 0 = 사용 불가. 혜택 문구를 이 값에서 만든다. */
data class MembershipLimits(
    val postings: Int?, val lessons: Int?, val resumeViews: Int?,
    val autoQuotes: Int?, val boostWeight: Int?, val performanceDays: Int?,
    /** 종류별 공고 한도 {JOB,SUB,CASTING}. 구 서버엔 없다 → postings 로 폴백(MembershipBenefits). */
    val postingsByKind: Map<String, Int>? = null,
) {
    companion object {
        fun from(o: JSONObject) = MembershipLimits(
            o.intOrNull("postings"), o.intOrNull("lessons"), o.intOrNull("resumeViews"),
            o.intOrNull("autoQuotes"), o.intOrNull("boostWeight"), o.intOrNull("performanceDays"),
            o.optJSONObject("postingsByKind")?.let { k ->
                listOf("JOB", "SUB", "CASTING").mapNotNull { key -> k.intOrNull(key)?.let { key to it } }
                    .toMap().ifEmpty { null }
            },
        )
    }
}

/** 내 멤버십 요약(/monetization/membership/me). 판정은 서버가 한 번만 한다. */
data class MyMembership(
    val active: Boolean,
    val tier: String?,
    val endAt: String?,
    val limits: MembershipLimits?,
    val resumeViewsUsed: Int,
    val autoQuotesUsed: Int,
    /** PLAYSTORE / APPSTORE / TOSS / null(자동결제 없음). 구 서버엔 없다 → null. */
    val billingProvider: String? = null,
    /** 구글 구독 중일 때 지금 상품(productId)·기간(basePlanId). */
    val googleProductId: String? = null,
    val googleBasePlanId: String? = null,
    /** 자동 갱신 여부(지금 결제 수단 기준). 모르면 null. */
    val autoRenew: Boolean? = null,
    /** 내 혜택 사용 현황(서버가 회원유형별로 정리). 구 서버엔 없다 → null(예전 자동견적 줄로 폴백). */
    val benefitUsage: List<BenefitUsage>? = null,
    /** 지금 등급·인증 유형으로 열린 기능(DETAIL_IMAGE 등). 구 서버·비활성이면 빈 목록. */
    val features: Set<String> = emptySet(),
) {
    companion object {
        /**
         * 구글 필드는 서버와 동시에 만들어져 이름이 확정 전이다 — 두 모양을 다 받는다.
         *  최상위 `googleProductId`/`googleBasePlanId`/`autoRenew` 또는 iOS `apple` 과 같은 꼴의 `google{productId,basePlanId,autoRenew}`.
         */
        fun from(o: JSONObject): MyMembership {
            val usage = o.optJSONObject("usage")
            val provider = o.textOrNull("billingProvider")
            val google = o.optJSONObject("google")
            val apple = o.optJSONObject("apple")
            val store = if (provider == "APPSTORE") apple else google
            return MyMembership(
                o.optBoolean("active", false), o.stringOrNull("tier"), o.stringOrNull("endAt"),
                o.optJSONObject("limits")?.let(MembershipLimits::from),
                usage?.optInt("resumeViews", 0) ?: 0, usage?.optInt("autoQuotes", 0) ?: 0,
                provider,
                o.textOrNull("googleProductId") ?: google?.textOrNull("productId"),
                o.textOrNull("googleBasePlanId") ?: google?.textOrNull("basePlanId"),
                store?.boolOrNull("autoRenew") ?: o.boolOrNull("autoRenew"),
                BenefitUsage.listFrom(o),
                o.optJSONArray("features").featureKeys(),
            )
        }
    }
}

/**
 * 멤버십 화면 문구(/monetization/public/membership-copy) — 관리자가 정한 등급 이름·한 줄 소개·추가 혜택·안내.
 *
 * 비어 있거나 없는 필드는 null 로 둔다 → TierInfo 가 필드 단위로 앱 기본 문구를 쓴다.
 * 구 서버(404)·네트워크 오류·형식 오류도 같은 방식으로 기본 문구가 된다(화면을 막지 않는다).
 */
data class MembershipTierCopy(
    val label: String?,
    val tagline: String?,
    val extras: List<String>?,
    val footnote: String?,
) {
    companion object {
        fun from(o: JSONObject) = MembershipTierCopy(
            o.textOrNull("label"), o.textOrNull("tagline"),
            o.optJSONArray("extras").textLines(), o.textOrNull("footnote"),
        )
    }
}

data class MembershipCopy(
    val tiers: Map<String, MembershipTierCopy>,
    val notices: List<String>?,
) {
    companion object {
        fun from(o: JSONObject): MembershipCopy {
            val t = o.optJSONObject("tiers")
            val tiers = TierInfo.order.mapNotNull { key ->
                t?.optJSONObject(key)?.let { key to MembershipTierCopy.from(it) }
            }.toMap()
            return MembershipCopy(tiers, o.optJSONArray("notices").textLines())
        }
    }
}

private fun JSONObject.boolOrNull(key: String): Boolean? =
    if (isNull(key)) null else (opt(key) as? Boolean)

/** features — 문자열 배열(서버 기준). 객체 꼴({featureType})로 와도 받는다. */
private fun JSONArray?.featureKeys(): Set<String> {
    if (this == null) return emptySet()
    return (0 until length()).mapNotNull { i ->
        when (val v = opt(i)) {
            is String -> v
            is JSONObject -> v.textOrNull("featureType") ?: v.textOrNull("key")
            else -> null
        }?.takeIf { it.isNotBlank() }
    }.toSet()
}

/** 공백뿐인 문자열·null 은 '값 없음'으로 본다. */
private fun JSONObject.textOrNull(key: String): String? =
    stringOrNull(key)?.takeIf { it.isNotBlank() }

/** 문자열 배열 중 비어 있지 않은 줄만. 남는 줄이 없으면 null(기본 문구 사용). */
private fun JSONArray?.textLines(): List<String>? {
    if (this == null) return null
    return (0 until length()).mapNotNull { i -> (opt(i) as? String)?.takeIf { it.isNotBlank() } }
        .ifEmpty { null }
}

/** 등급 표기 + 혜택 문구. iOS `MembershipTierInfo` 와 같은 규칙. copy 가 있으면 서버 문구가 우선이다. */
object TierInfo {
    val order = listOf("BASIC", "STANDARD", "PRO")

    const val DEFAULT_FOOTNOTE = "회원유형을 바꿔도 멤버십은 그대로 유지돼요. 인증받은 유형의 혜택이 함께 열려요."
    val DEFAULT_NOTICES = listOf(
        "멤버십은 계정에 하나이며, 인증받은 회원유형의 혜택이 함께 열려요.",
        "회원유형을 바꾸거나 추가해도 이용 기간은 그대로 유지돼요.",
    )

    fun label(tier: String?, copy: MembershipCopy? = null) = copy?.tiers?.get(tier)?.label ?: when (tier) {
        "BASIC" -> "베이직"
        "STANDARD" -> "스탠다드"
        "PRO" -> "프로"
        else -> "멤버십"   // 옛 유형별 멤버십을 쓰던 회원
    }

    fun tagline(tier: String?, copy: MembershipCopy? = null) = copy?.tiers?.get(tier)?.tagline ?: when (tier) {
        "BASIC" -> "부담 없이 시작하기"
        "STANDARD" -> "회원이 강사에게 직접 견적을 요청하게"
        "PRO" -> "가장 먼저 눈에 띄기"
        else -> ""
    }

    fun footnote(tier: String?, copy: MembershipCopy? = null) =
        copy?.tiers?.get(tier)?.footnote ?: DEFAULT_FOOTNOTE

    /** '멤버십 안내' 고정 줄. 판매 방식(자동결제/기간권) 줄은 화면이 뒤에 붙인다. */
    fun notices(copy: MembershipCopy? = null) = copy?.notices ?: DEFAULT_NOTICES

    /**
     * 혜택 문구는 **서버가 내려준 한도에서 만든다**(MembershipBenefits — 웹·iOS 와 같은 규칙).
     * 앱에 박아두면 관리자가 숫자를 바꿔도 옛말이 남는다.
     * 한도가 아닌 추가 혜택(extras)은 서버 문구가 있으면 그것을, 없으면 앱 기본 목록을 붙인다.
     */
    fun benefits(l: MembershipLimits?, tier: String?, isDancer: Boolean, copy: MembershipCopy? = null): List<String> {
        if (l == null) return emptyList()
        val extras = copy?.tiers?.get(tier)?.extras ?: MembershipBenefits.extras(tier)
        return MembershipBenefits.lines(l, isDancer) + extras
    }
}

class MembershipApi(private val token: String?) {
    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    // channel=PLAYSTORE — 구글 상품 id 가 연결된 상품만(앱은 구글 플레이 결제로만 판다).
    suspend fun plans(): Result<List<MembershipPlan>> =
        call("/monetization/plans?featureType=MEMBERSHIP&channel=PLAYSTORE")
            .map { JSONArray(it.ifBlank { "[]" }).map(MembershipPlan::from) }

    /** 멤버십 화면 문구 — 비로그인 공개 API. 실패하면 화면은 앱 기본 문구를 쓴다. */
    suspend fun membershipCopy(): Result<MembershipCopy> =
        call("/monetization/public/membership-copy", auth = false)
            .map { MembershipCopy.from(JSONObject(it.ifBlank { "{}" })) }

    suspend fun myMembership(): Result<MyMembership> =
        call("/monetization/membership/me").map { MyMembership.from(JSONObject(it.ifBlank { "{}" })) }

    /** 구글 결제에 실을 obfuscatedAccountId — 서버가 구글 알림에서 구매자를 찾는 열쇠. */
    suspend fun googleAccountToken(): Result<String> =
        call("/monetization/google/account-token").map {
            JSONObject(it).optString("obfuscatedAccountId").ifBlank { throw IllegalStateException("빈 토큰") }
        }

    /** HTTP 상태까지 필요한 호출 결과(구매 전송 — 4xx/5xx 에 따라 재전송 여부가 갈린다). */
    class StatusResponse(val status: Int, val body: String)

    /**
     * 구글 구매 토큰을 서버로(서버가 구글에 확인·acknowledge 하고 멤버십을 연다). 200 본문 = /membership/me 와 같은 꼴.
     *  basePlanId 를 모르면(이 기기에서 시작한 결제가 아님) 빼고 보낸다 — 서버가 구글 조회로 안다.
     *  ★ 통신 오류만 실패(Result.failure)로, HTTP 오류는 상태 코드로 돌려준다.
     */
    suspend fun submitGooglePurchase(productId: String, basePlanId: String?, purchaseToken: String): Result<StatusResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body = JSONObject().put("productId", productId).put("purchaseToken", purchaseToken)
                if (basePlanId != null) body.put("basePlanId", basePlanId)
                val req = Request.Builder().url("$apiBase/monetization/google/purchases")
                    .post(body.toString().toRequestBody(JSON))
                    .addHeader("Content-Type", "application/json")
                    .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                    .build()
                client.newCall(req).execute().use { res -> StatusResponse(res.code, res.body?.string().orEmpty()) }
            }
        }

    private suspend fun call(
        path: String, method: String = "GET", body: JSONObject? = null, auth: Boolean = true,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = when {
                    body != null -> body.toString().toRequestBody(JSON)
                    method != "GET" -> "".toRequestBody(JSON)
                    else -> null
                }
                val req = Request.Builder().url(apiBase + path).method(method, payload)
                    .addHeader("Content-Type", "application/json")
                    .apply { if (auth && !token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                    .build()
                client.newCall(req).execute().use { res ->
                    val text = res.body?.string().orEmpty()
                    if (!res.isSuccessful) {
                        val msg = runCatching { JSONObject(text).optString("message") }.getOrNull()
                        throw IllegalStateException(msg?.ifEmpty { null } ?: "요청에 실패했어요.")
                    }
                    text
                }
            }
        }

    private companion object { val JSON = "application/json".toMediaType() }
}

/**
 * 멤버십 안내 + 구독 — iOS `MembershipView` 와 같은 구성.
 *
 * ★ 앱에서는 **구글 플레이 결제(자동갱신 구독)로만** 판다 — MembershipBilling / MembershipPurchaseSection.
 *   앱 안 디지털 혜택이라 스토어 결제만 쓸 수 있다. 토스 결제는 웹 전용이며 웹 결제로 보내는 링크도 두지 않는다.
 */
@Composable
fun MembershipScreen(api: MembershipApi, onClose: () -> Unit) {
    var plans by remember { mutableStateOf<List<MembershipPlan>>(emptyList()) }
    var mine by remember { mutableStateOf<MyMembership?>(null) }
    var loading by remember { mutableStateOf(true) }
    var selectedTier by remember { mutableStateOf("STANDARD") }
    var toast by remember { mutableStateOf<String?>(null) }
    // 서버 문구 — 받기 전·실패 시 null 이면 TierInfo 기본 문구를 쓴다.
    var copy by remember { mutableStateOf<MembershipCopy?>(null) }

    LaunchedEffect(Unit) {
        // 문구는 상품 목록과 동시에 받는다. 로딩 표시를 기다리게 하지 않고, 실패는 조용히 무시한다.
        launch { api.membershipCopy().onSuccess { copy = it } }
        api.plans().onSuccess { plans = it }.onFailure { toast = it.message }
        api.myMembership().onSuccess { mine = it }
        // 이용 중인 등급이 있으면 그 등급 탭부터 보여준다.
        mine?.takeIf { it.active }?.tier?.let { t -> if (plans.any { it.tier == t }) selectedTier = t }
        loading = false
        // 구글 가격(현지 통화)은 화면을 띄운 뒤 받는다 — 받기 전엔 서버 가격을 보인다.
        MembershipBilling.loadProducts(plans.mapNotNull { it.googleProductId }.distinct())
    }
    // 결제가 서버에 반영됐다(이 화면의 구매·앱 복귀 재전송·Play 콜백) — 다시 불러온다(iOS membershipDidChange).
    LaunchedEffect(Unit) {
        MembershipBilling.changes.collect { m -> mine = m ?: api.myMembership().getOrNull() ?: mine }
    }

    // 판매중인 등급만 노출(관리자가 등급을 내리면 자동으로 빠진다).
    val availableTiers = TierInfo.order.filter { t -> plans.any { it.tier == t } }
    val tierPlans = plans.filter { it.tier == selectedTier }.sortedBy { it.durationDays ?: 0 }
    val tierLimits = tierPlans.firstOrNull()?.limits
    // 무용수 활동 중에는 레슨 활동 불가(2026-09-26) — 레슨·자동견적 혜택 줄은 뺀다.
    val context = androidx.compose.ui.platform.LocalContext.current
    val isDancer = remember { com.muyeon.app.webview.ActiveRole.isDancer(context) }
    val isLoggedIn = remember { !TokenManager.getAccessToken(context).isNullOrEmpty() }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "멤버십", onBack = onClose)

        if (loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            return@Column
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 이용중 배너
            mine?.takeIf { it.active }?.let { m ->
                Column(
                    Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MuyeonColors.primary.copy(alpha = 0.07f)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "${TierInfo.label(m.tier, copy)} 멤버십 이용중",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        lineHeight = 18.sp, color = MuyeonColors.textHead,
                    )
                    m.endAt?.take(10)?.let {
                        Text(
                            "${it}까지 이용 가능",
                            fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp,
                            color = MuyeonColors.textSub,
                        )
                    }
                    // 스토어 구독 자동 갱신을 끈 상태 — 화면에 안 보이면 "해지가 안 된다"고 느낀다(iOS 와 같은 줄).
                    if ((m.billingProvider == "PLAYSTORE" || m.billingProvider == "APPSTORE") && m.autoRenew == false) {
                        Text(
                            "자동 갱신 해지됨 · 이용 기간이 끝나면 멤버십이 종료돼요",
                            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                            lineHeight = 18.sp, color = MuyeonColors.primary,
                        )
                    }
                    // 이번 달 남은 횟수 — 다 쓰기 전에 알아야 등급을 올릴지 판단할 수 있다.
                    //  서버가 benefitUsage 를 주면 아래 '내 혜택 사용 현황' 카드가 대신한다(구 서버 폴백만).
                    val remains = if (m.benefitUsage != null) emptyList() else listOfNotNull(
                        remainText("자동견적", m.limits?.autoQuotes, m.autoQuotesUsed),
                    )
                    if (remains.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                remains.joinToString(" · "),
                                fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp,
                                color = MuyeonColors.textSub, modifier = Modifier.weight(1f, fill = false),
                            )
                            // 어느 요청에 자동견적이 쓰였는지 — 강사·학원만(무용수는 내역 화면이 막혀 있다).
                            if (!isDancer) {
                                Text(
                                    "발송 내역 >",
                                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp, lineHeight = 18.sp, color = MuyeonColors.primary,
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .clickable { AutoQuoteHistoryActivity.start(context) },
                                )
                            }
                        }
                    }
                }
            }

            // 내 혜택 사용 현황 — 공고·레슨·자동견적·노출·성과 등. 무료 회원도 공고·레슨 한도는 보인다(서버가 정리).
            mine?.benefitUsage?.takeIf { it.isNotEmpty() }?.let { items ->
                // '등급 올리기' = 지금 등급 다음 탭(무료면 첫 등급). 최고 등급이면 링크를 숨긴다.
                val current = mine?.takeIf { it.active }?.tier
                val next = if (current == null) availableTiers.firstOrNull()
                else availableTiers.getOrNull(availableTiers.indexOf(current) + 1)
                    ?.takeIf { availableTiers.indexOf(current) >= 0 }
                Box(Modifier.padding(top = if (mine?.active == true) 0.dp else 14.dp)) {
                    BenefitUsageCard(items, onUpgrade = next?.let { t -> { selectedTier = t } })
                }
            }

            if (availableTiers.isEmpty()) {
                Text(
                    "현재 안내할 멤버십이 없어요.",
                    fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.textSub,
                )
            } else {
                // 등급 선택
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    availableTiers.forEach { t ->
                        val on = t == selectedTier
                        Text(
                            TierInfo.label(t, copy),
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            lineHeight = 17.sp, textAlign = TextAlign.Center,
                            color = if (on) Color.White else MuyeonColors.textSub,
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                                .background(if (on) MuyeonColors.primary else MuyeonColors.tileIdle)
                                .clickable { selectedTier = t }
                                .padding(vertical = 11.dp),
                        )
                    }
                }

                // 혜택
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MuyeonColors.tileIdle).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "${TierInfo.label(selectedTier, copy)} · ${TierInfo.tagline(selectedTier, copy)}",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        lineHeight = 19.sp, color = MuyeonColors.textHead,
                    )
                    TierInfo.benefits(tierLimits, selectedTier, isDancer, copy).forEach { b ->
                        Text(
                            "· $b",
                            fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp,
                            color = MuyeonColors.textHead,
                        )
                    }
                    Text(
                        TierInfo.footnote(selectedTier, copy),
                        fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 17.sp,
                        color = MuyeonColors.textSub,
                    )
                }

                // 기간·가격 + 구독 버튼(구글 플레이 결제)
                MembershipPurchaseSection(
                    tier = selectedTier, plans = tierPlans, mine = mine, isLoggedIn = isLoggedIn, copy = copy,
                )
            }
            // 구독 필수 안내 + 구매 복원 + 약관. 결제 링크(웹·토스)는 두지 않는다.
            MembershipLegalFooter(isLoggedIn = isLoggedIn, notices = TierInfo.notices(copy))
            Spacer(Modifier.height(12.dp))
        }
    }

    toast?.let { msg ->
        QuoteDialog("알림", msg, "확인", onConfirm = { toast = null }, onDismiss = { toast = null })
    }
}

/** 남은 횟수 문구. 무제한(-1)은 숫자 대신 "무제한", 미사용(0)은 표시하지 않는다. */
private fun remainText(name: String, limit: Int?, used: Int): String? {
    if (limit == null || limit == 0) return null
    if (limit < 0) return "$name 무제한"
    return "$name ${maxOf(0, limit - used)}회 남음"
}

/** 웹 `openMembership` 브릿지 진입점. 결과: MEMBERSHIP(가입·해지로 이용권이 바뀐다). */
class MembershipActivity : com.muyeon.app.result.ResultActivity() {

    override val defaultResultKeys = setOf(com.muyeon.app.result.ResultKeys.MEMBERSHIP)

    companion object {
        //  featureType 은 더 이상 쓰지 않는다 — 낱개 기능 상품 판매를 접었고,
        //  멤버십은 계정에 하나라 회원유형으로도 갈리지 않는다(2026-08-30 개편).
        fun intent(context: Context): Intent = Intent(context, MembershipActivity::class.java)

        fun start(context: Context) = context.launchScreen(intent(context))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val api = remember { MembershipApi(TokenManager.getAccessToken(this)) }
            MembershipScreen(api, onClose = { finish() })
        }
    }
}
