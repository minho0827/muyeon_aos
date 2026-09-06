package com.muyeon.app.ui.membership

import com.muyeon.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

/** 날짜별 지표 한 줄. */
data class MembershipDailyMetric(
    val date: String,
    val impressions: Int,
    val clicks: Int,
    val detailViews: Int,
    val leads: Int,
)

data class MembershipFunnel(
    val directQuoteRequests: Int?, val quotesSent: Int?, val quotesAccepted: Int?, val chatRooms: Int?,
    val lessonAppointments: Int?, val lessonsCompleted: Int?, val reviewsCreated: Int?,
    val revisitYes: Int?, val repeatCustomers: Int?, val classInquiries: Int?, val reservations: Int?,
    val attended: Int?, val noShows: Int?, val jobApplications: Int?,
)

/** 멤버십 결제 전/후 비교 지표. 금액은 실결제가 아니라 예약·성사 견적 기준 예상 금액이다. */
data class MembershipImpactMetrics(
    val impressions: Int, val clicks: Int, val detailViews: Int, val leads: Int,
    val reservationAmount: Double, val quoteAmount: Double, val revenue: Double,
)

/** ready=false 는 결제 3일 미만 — 하루치 증감률은 그날 운이라 서버가 비교를 내주지 않는다. */
data class MembershipImpact(
    val startAt: String,
    val daysSince: Int,
    val ready: Boolean,
    val windowDays: Int?,
    val before: MembershipImpactMetrics?,
    val after: MembershipImpactMetrics?,
)

data class MembershipPerformance(
    val discovery: Map<String, Int>,   // 이벤트 코드 → total
    val funnel: MembershipFunnel,
    val daily: List<MembershipDailyMetric>,
    val impact: MembershipImpact?,
)

/** 403 은 고장이 아니라 자격 문제 — 무엇이 없어서 막혔는지 문구로 구분한다(iOS blockedMessage). */
class MembershipBlocked(val reason: String) : Exception(reason)

class MembershipPerformanceApi(private val token: String?) {

    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    suspend fun load(memberType: String): Result<MembershipPerformance> = withContext(Dispatchers.IO) {
        runCatching {
            val q = URLEncoder.encode(memberType, "UTF-8")
            val req = Request.Builder().url("$apiBase/analytics/performance/me?memberType=$q")
                .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                .build()
            client.newCall(req).execute().use { res ->
                val text = res.body?.string().orEmpty()
                if (!res.isSuccessful) {
                    if (res.code == 403) {
                        val code = runCatching { JSONObject(text).optString("code") }.getOrNull()
                        throw MembershipBlocked(
                            if (code == "ROLE_REQUIRED") "이 회원유형 인증을 마치면 성과를 볼 수 있어요."
                            else "성과는 멤버십 회원만 볼 수 있어요.",
                        )
                    }
                    error("성과를 불러오지 못했어요.")
                }
                parse(JSONObject(text.ifBlank { "{}" }))
            }
        }
    }

    private fun parse(o: JSONObject): MembershipPerformance {
        val disc = mutableMapOf<String, Int>()
        o.optJSONObject("discovery")?.let { d ->
            d.keys().forEach { k -> disc[k] = d.optJSONObject(k)?.optInt("total") ?: 0 }
        }
        val f = o.optJSONObject("funnel") ?: JSONObject()
        val daily = o.optJSONArray("daily")?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                arr.optJSONObject(i)?.let {
                    MembershipDailyMetric(
                        it.optString("date"), it.optInt("impressions"), it.optInt("clicks"),
                        it.optInt("detailViews"), it.optInt("leads"),
                    )
                }
            }
        } ?: emptyList()
        return MembershipPerformance(
            discovery = disc,
            funnel = MembershipFunnel(
                f.intOrNull("directQuoteRequests"), f.intOrNull("quotesSent"),
                f.intOrNull("quotesAccepted"), f.intOrNull("chatRooms"),
                f.intOrNull("lessonAppointments"), f.intOrNull("lessonsCompleted"),
                f.intOrNull("reviewsCreated"), f.intOrNull("revisitYes"),
                f.intOrNull("repeatCustomers"), f.intOrNull("classInquiries"),
                f.intOrNull("reservations"), f.intOrNull("attended"),
                f.intOrNull("noShows"), f.intOrNull("jobApplications"),
            ),
            daily = daily,
            impact = o.optJSONObject("impact")?.let { im ->
                MembershipImpact(
                    startAt = im.optString("startAt"),
                    daysSince = im.optInt("daysSince"),
                    ready = im.optBoolean("ready"),
                    windowDays = im.intOrNull("windowDays"),
                    before = im.optJSONObject("before")?.let(::metrics),
                    after = im.optJSONObject("after")?.let(::metrics),
                )
            },
        )
    }

    private fun metrics(o: JSONObject) = MembershipImpactMetrics(
        o.optInt("impressions"), o.optInt("clicks"), o.optInt("detailViews"), o.optInt("leads"),
        o.optDouble("reservationAmount", 0.0), o.optDouble("quoteAmount", 0.0),
        o.optDouble("revenue", 0.0),
    )

    private fun JSONObject.intOrNull(key: String): Int? = if (has(key) && !isNull(key)) optInt(key) else null
}
