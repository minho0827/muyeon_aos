package com.muyeon.app.ui.membership

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import java.util.Locale

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
            )
        }
    }
}

/** 등급이 정하는 한도. -1 = 무제한, 0 = 사용 불가. 혜택 문구를 이 값에서 만든다. */
data class MembershipLimits(
    val postings: Int?, val lessons: Int?, val resumeViews: Int?,
    val autoQuotes: Int?, val boostWeight: Int?, val performanceDays: Int?,
) {
    companion object {
        fun from(o: JSONObject) = MembershipLimits(
            o.intOrNull("postings"), o.intOrNull("lessons"), o.intOrNull("resumeViews"),
            o.intOrNull("autoQuotes"), o.intOrNull("boostWeight"), o.intOrNull("performanceDays"),
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
) {
    companion object {
        fun from(o: JSONObject): MyMembership {
            val usage = o.optJSONObject("usage")
            return MyMembership(
                o.optBoolean("active", false), o.stringOrNull("tier"), o.stringOrNull("endAt"),
                o.optJSONObject("limits")?.let(MembershipLimits::from),
                usage?.optInt("resumeViews", 0) ?: 0, usage?.optInt("autoQuotes", 0) ?: 0,
            )
        }
    }
}

/** 등급 표기 + 혜택 문구. iOS `MembershipTierInfo` 와 같은 규칙. */
object TierInfo {
    val order = listOf("BASIC", "STANDARD", "PRO")

    fun label(tier: String?) = when (tier) {
        "BASIC" -> "베이직"
        "STANDARD" -> "스탠다드"
        "PRO" -> "프로"
        else -> "멤버십"   // 옛 유형별 멤버십을 쓰던 회원
    }

    fun tagline(tier: String?) = when (tier) {
        "BASIC" -> "제한 없이 등록부터 시작하기"
        "STANDARD" -> "회원이 먼저 찾아오게 만들기"
        "PRO" -> "가장 앞자리에서 영업하기"
        else -> ""
    }

    /** 혜택 문구는 **서버가 내려준 한도에서 만든다.** 앱에 박아두면 관리자가 숫자를 바꿔도 옛말이 남는다. */
    fun benefits(l: MembershipLimits?): List<String> {
        if (l == null) return emptyList()
        val out = mutableListOf<String>()
        out += "${count("공고", l.postings)} · ${count("레슨", l.lessons)} 등록"
        l.resumeViews?.takeIf { it != 0 }?.let {
            out += if (it < 0) "이력서 열람 무제한 (학원·공연팀)" else "이력서 열람 월 ${it}건 (학원·공연팀)"
        }
        l.autoQuotes?.takeIf { it != 0 }?.let {
            out += if (it < 0) "자동견적 발송 무제한" else "자동견적 발송 월 ${it}건"
        }
        l.boostWeight?.takeIf { it > 0 }?.let {
            out += if (it >= 3) "홈·목록 상단 노출 (가장 자주)" else "목록 상단 노출"
        }
        l.performanceDays?.takeIf { it != 0 }?.let {
            out += if (it < 0) "성과 보기 (전체 기간)" else "성과 보기 (최근 ${it}일)"
        }
        out += "상세페이지 이미지 · 영상 포트폴리오"
        return out
    }

    private fun count(name: String, v: Int?): String =
        when { v == null -> name; v < 0 -> "$name 무제한"; else -> "$name ${v}개" }
}

class MembershipApi(private val token: String?) {
    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    suspend fun plans(): Result<List<MembershipPlan>> =
        call("/monetization/plans?featureType=MEMBERSHIP")
            .map { JSONArray(it.ifBlank { "[]" }).map(MembershipPlan::from) }

    suspend fun myMembership(): Result<MyMembership> =
        call("/monetization/membership/me").map { MyMembership.from(JSONObject(it.ifBlank { "{}" })) }

    // ★ 구매 API 는 부르지 않는다. 앱에서 디지털 상품을 팔면 스토어 결제를 요구받는다.
    //   결제는 웹에서만 받는다(2026-08-30 개편).

    private suspend fun call(path: String, method: String = "GET", body: JSONObject? = null): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = when {
                    body != null -> body.toString().toRequestBody(JSON)
                    method != "GET" -> "".toRequestBody(JSON)
                    else -> null
                }
                val req = Request.Builder().url(apiBase + path).method(method, payload)
                    .addHeader("Content-Type", "application/json")
                    .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
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
 * 멤버십 안내 — iOS `MembershipView` 와 같은 구성.
 *
 * ★ 앱에서는 결제를 받지 않는다. 앱 안에서 디지털 상품을 팔면 스토어가 자체 결제를 요구한다.
 *   그래서 이 화면은 **현재 상태와 등급별 혜택을 알려주는 데까지만** 한다.
 *   결제 버튼도, 웹으로 보내는 링크도 두지 않는다.
 */
@Composable
fun MembershipScreen(api: MembershipApi, onClose: () -> Unit) {
    var plans by remember { mutableStateOf<List<MembershipPlan>>(emptyList()) }
    var mine by remember { mutableStateOf<MyMembership?>(null) }
    var loading by remember { mutableStateOf(true) }
    var selectedTier by remember { mutableStateOf("STANDARD") }
    var toast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        api.plans().onSuccess { plans = it }.onFailure { toast = it.message }
        api.myMembership().onSuccess { mine = it }
        loading = false
    }

    // 판매중인 등급만 노출(관리자가 등급을 내리면 자동으로 빠진다).
    val availableTiers = TierInfo.order.filter { t -> plans.any { it.tier == t } }
    val tierPlans = plans.filter { it.tier == selectedTier }.sortedBy { it.durationDays ?: 0 }
    val tierLimits = tierPlans.firstOrNull()?.limits

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
                        "${TierInfo.label(m.tier)} 멤버십 이용중",
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
                    // 이번 달 남은 횟수 — 다 쓰기 전에 알아야 등급을 올릴지 판단할 수 있다.
                    val remains = listOfNotNull(
                        remainText("이력서 열람", m.limits?.resumeViews, m.resumeViewsUsed),
                        remainText("자동견적", m.limits?.autoQuotes, m.autoQuotesUsed),
                    )
                    if (remains.isNotEmpty()) {
                        Text(
                            remains.joinToString(" · "),
                            fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp,
                            color = MuyeonColors.textSub,
                        )
                    }
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
                            TierInfo.label(t),
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
                        "${TierInfo.label(selectedTier)} · ${TierInfo.tagline(selectedTier)}",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        lineHeight = 19.sp, color = MuyeonColors.textHead,
                    )
                    TierInfo.benefits(tierLimits).forEach { b ->
                        Text(
                            "· $b",
                            fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp,
                            color = MuyeonColors.textHead,
                        )
                    }
                    Text(
                        "회원유형을 바꿔도 멤버십은 그대로 유지돼요. 인증받은 유형의 혜택이 함께 열려요.",
                        fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 17.sp,
                        color = MuyeonColors.textSub,
                    )
                }

                // 기간·가격
                tierPlans.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MuyeonColors.border, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                "${p.months}개월",
                                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                                lineHeight = 18.sp, color = MuyeonColors.textHead,
                            )
                            if (p.months > 1 && p.priceKrw != null) {
                                Text(
                                    String.format(Locale.KOREA, "월 %,d원 꼴", p.priceKrw / p.months),
                                    fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 17.sp,
                                    color = MuyeonColors.textSub,
                                )
                            }
                        }
                        if (p.discountPct > 0) {
                            Text(
                                "${p.discountPct}%",
                                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                                lineHeight = 15.sp, color = Color.White,
                                modifier = Modifier.padding(end = 8.dp).clip(RoundedCornerShape(50))
                                    .background(MuyeonColors.primary)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                        Text(
                            p.priceKrw?.let { String.format(Locale.KOREA, "%,d원", it) } ?: "-",
                            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            lineHeight = 19.sp, color = MuyeonColors.textHead,
                        )
                    }
                }

                // 안내 — 결제 링크는 두지 않는다(스토어 정책).
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "멤버십은 계정에 하나이며, 인증받은 회원유형의 혜택이 함께 열립니다.",
                        "회원유형을 바꾸거나 추가해도 이용 기간은 그대로 유지됩니다.",
                        "선택한 기간만큼 이용하는 상품이며 자동으로 다시 결제되지 않습니다.",
                        "신청과 결제는 무용연 홈페이지에서 진행됩니다.",
                    ).forEach {
                        Text(
                            "- $it",
                            fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 17.sp,
                            color = MuyeonColors.textSub,
                        )
                    }
                }
            }
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

/** 웹 `openMembership` 브릿지 진입점. */
class MembershipActivity : ComponentActivity() {

    companion object {
        //  featureType 은 더 이상 쓰지 않는다 — 낱개 기능 상품 판매를 접었고,
        //  멤버십은 계정에 하나라 회원유형으로도 갈리지 않는다(2026-08-30 개편).
        fun start(context: Context) {
            val i = Intent(context, MembershipActivity::class.java)
            if (context !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val api = remember { MembershipApi(TokenManager.getAccessToken(this)) }
            MembershipScreen(api, onClose = { finish() })
        }
    }
}
