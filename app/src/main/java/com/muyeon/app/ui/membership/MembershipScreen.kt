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
data class MembershipPlan(
    val code: String,
    val name: String?,
    val priceKrw: Int?,
    val featureType: String?,
    val periodDays: Int?,
    val description: String?,
) {
    companion object {
        fun from(o: JSONObject) = MembershipPlan(
            o.optString("code"), o.stringOrNull("name"), o.intOrNull("priceKrw"),
            o.stringOrNull("featureType"), o.intOrNull("periodDays"), o.stringOrNull("description"),
        )
    }
}

data class MonEntitlement(val featureType: String, val status: String?, val expiresAt: String?) {
    val isActive: Boolean get() = status == null || status == "ACTIVE"

    companion object {
        fun from(o: JSONObject) = MonEntitlement(
            o.optString("featureType"), o.stringOrNull("status"), o.stringOrNull("expiresAt"),
        )
    }
}

class MembershipApi(private val token: String?) {
    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    suspend fun plans(featureType: String?): Result<List<MembershipPlan>> {
        val path = "/monetization/plans" + if (featureType.isNullOrEmpty()) "" else "?featureType=$featureType"
        return call(path).map { JSONArray(it.ifBlank { "[]" }).map(MembershipPlan::from) }
    }

    suspend fun myEntitlements(): Result<List<MonEntitlement>> =
        call("/monetization/entitlements/me").map { JSONArray(it.ifBlank { "[]" }).map(MonEntitlement::from) }

    suspend fun purchase(planCode: String): Result<Unit> =
        call("/monetization/purchase", "POST", JSONObject().put("planCode", planCode)).map { }

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

@Composable
fun MembershipScreen(api: MembershipApi, featureType: String?, onClose: () -> Unit) {
    var plans by remember { mutableStateOf<List<MembershipPlan>>(emptyList()) }
    var entitlements by remember { mutableStateOf<List<MonEntitlement>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var confirm by remember { mutableStateOf<MembershipPlan?>(null) }
    var toast by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        api.plans(featureType).onSuccess { plans = it }
        api.myEntitlements().onSuccess { entitlements = it }
        loading = false
    }

    LaunchedEffect(featureType) { load() }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "이용권", onBack = onClose)

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
            val active = entitlements.filter { it.isActive }
            if (active.isNotEmpty()) {
                Column(
                    Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MuyeonColors.primary.copy(alpha = 0.07f)).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        "보유 이용권",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        lineHeight = 17.sp, color = MuyeonColors.textHead,
                    )
                    active.forEach { e ->
                        Text(
                            listOfNotNull(e.featureType, e.expiresAt?.take(10)?.let { "~$it" }).joinToString(" · "),
                            fontFamily = customFontFamily, fontSize = 13.sp, lineHeight = 18.sp, color = MuyeonColors.textSub,
                        )
                    }
                }
            }

            Text(
                "이용권 상품",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                lineHeight = 19.sp, color = MuyeonColors.textHead, modifier = Modifier.padding(top = 4.dp),
            )
            if (plans.isEmpty()) {
                Text("이용 가능한 상품이 없어요.", fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.textSub)
            }
            plans.forEach { p ->
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MuyeonColors.border, RoundedCornerShape(12.dp))
                        .clickable { confirm = p }.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        p.name ?: p.code,
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        lineHeight = 18.sp, color = MuyeonColors.textHead,
                    )
                    p.description?.takeIf { it.isNotEmpty() }?.let {
                        Text(it, fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 17.sp, color = MuyeonColors.textSub)
                    }
                    Text(
                        listOfNotNull(
                            p.priceKrw?.let { String.format(Locale.KOREA, "%,d원", it) },
                            p.periodDays?.let { "${it}일" },
                        ).joinToString(" · "),
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        lineHeight = 17.sp, color = MuyeonColors.primary,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    confirm?.let { p ->
        QuoteDialog(
            title = "${p.name ?: p.code} 구매",
            message = listOfNotNull(
                p.priceKrw?.let { String.format(Locale.KOREA, "%,d원", it) },
                p.periodDays?.let { "${it}일간 이용" },
            ).joinToString(" · "),
            confirmText = "구매하기",
            onConfirm = {
                confirm = null
                scope.launch {
                    api.purchase(p.code).onSuccess { toast = "구매를 신청했어요." }.onFailure { toast = it.message }
                    load()
                }
            },
            onDismiss = { confirm = null },
        )
    }
    toast?.let { msg ->
        QuoteDialog("알림", msg, "확인", onConfirm = { toast = null }, onDismiss = { toast = null })
    }
}

/** 웹 `openMembership` 브릿지 진입점. */
class MembershipActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_FEATURE = "featureType"

        fun start(context: Context, featureType: String?) {
            val i = Intent(context, MembershipActivity::class.java).putExtra(EXTRA_FEATURE, featureType ?: "")
            if (context !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val feature = intent.getStringExtra(EXTRA_FEATURE)?.ifEmpty { null }
        setContent {
            val api = remember { MembershipApi(TokenManager.getAccessToken(this)) }
            MembershipScreen(api, feature, onClose = { finish() })
        }
    }
}
