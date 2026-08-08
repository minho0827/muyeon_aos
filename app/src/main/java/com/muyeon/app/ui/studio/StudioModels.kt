package com.muyeon.app.ui.studio

import com.muyeon.app.BuildConfig
import com.muyeon.app.ui.quote.QuoteUi
import com.muyeon.app.ui.quote.intOrNull
import com.muyeon.app.ui.quote.map
import com.muyeon.app.ui.quote.stringOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.ceil

/**
 * 스튜디오 운영(회원·수강권·매출·일정) — iOS `Studio/StudioModels.swift` 1:1.
 */

/** 수강권 — 기간권(PERIOD) / 횟수권(COUNT). */
data class StudioPass(
    val id: Int,
    val productName: String,
    val passType: String,
    val category: String?,
    val totalCount: Int?,
    val remainingCount: Int?,
    val startAt: String?,
    val expireAt: String?,
    val lastAttendedAt: String?,
    val price: Int,
    val status: String,     // ACTIVE | EXPIRED | CANCELED
) {
    val isCount: Boolean get() = passType == "COUNT"

    /** "잔여 28/60회"(횟수권) / "기간권". */
    val remainText: String
        get() = if (isCount) "잔여 ${remainingCount ?: 0}/${totalCount ?: 0}회" else "기간권"

    /** 만료 D-day — "71일 남음" / "오늘 만료" / "만료됨". */
    val expireText: String?
        get() {
            val d = StudioDateFmt.daysUntil(expireAt ?: return null) ?: return null
            return when {
                d < 0 -> "만료됨"
                d == 0 -> "오늘 만료"
                else -> "${d}일 남음"
            }
        }

    companion object {
        fun from(o: JSONObject) = StudioPass(
            o.optInt("id"), o.optString("productName"), o.optString("passType"),
            o.stringOrNull("category"), o.intOrNull("totalCount"), o.intOrNull("remainingCount"),
            o.stringOrNull("startAt"), o.stringOrNull("expireAt"), o.stringOrNull("lastAttendedAt"),
            o.optInt("price"), o.optString("status"),
        )
    }
}

data class StudioMemberSummary(
    val memberId: Int,
    val name: String,
    val phone: String?,
    val grade: String?,
    val memo: String?,
    val leadStatus: String,   // LEAD | ACTIVE
    val passCount: Int,
    val pass: StudioPass?,
    val reservationCount: Int,
    val lastReservedAt: String?,
) {
    val isLead: Boolean get() = leadStatus == "LEAD"

    companion object {
        fun from(o: JSONObject) = StudioMemberSummary(
            o.optInt("memberId"), o.optString("name"), o.stringOrNull("phone"),
            o.stringOrNull("grade"), o.stringOrNull("memo"), o.optString("leadStatus").ifEmpty { "ACTIVE" },
            o.optInt("passCount"), o.optJSONObject("pass")?.let { StudioPass.from(it) },
            o.optInt("reservationCount"), o.stringOrNull("lastReservedAt"),
        )
    }
}

data class StudioRecentReservation(
    val id: Int,
    val status: String,
    val headcount: Int,
    val date: String?,
    val startTime: String?,
    val endTime: String?,
    val title: String?,
) {
    companion object {
        fun from(o: JSONObject) = StudioRecentReservation(
            o.optInt("id"), o.optString("status"), o.optInt("headcount", 1),
            o.stringOrNull("date"), o.stringOrNull("startTime"), o.stringOrNull("endTime"),
            o.stringOrNull("title"),
        )
    }
}

data class StudioMemberDetail(
    val memberId: Int,
    val name: String,
    val phone: String?,
    val grade: String?,
    val memo: String?,
    val leadStatus: String,
    val passes: List<StudioPass>,
    val recentReservations: List<StudioRecentReservation>,
) {
    companion object {
        fun from(o: JSONObject) = StudioMemberDetail(
            o.optInt("memberId"), o.optString("name"), o.stringOrNull("phone"),
            o.stringOrNull("grade"), o.stringOrNull("memo"), o.optString("leadStatus"),
            o.optJSONArray("passes")?.map(StudioPass::from) ?: emptyList(),
            o.optJSONArray("recentReservations")?.map(StudioRecentReservation::from) ?: emptyList(),
        )
    }
}

// ── 매출 ──

data class SaleCategoryStat(val category: String, val count: Int, val amount: Int, val unpaid: Int) {
    val label: String get() = StudioSaleCat.label(category)

    companion object {
        fun from(o: JSONObject) = SaleCategoryStat(
            o.optString("category"), o.optInt("count"), o.optInt("amount"), o.optInt("unpaid"),
        )
    }
}

data class SalesSummary(val total: Int, val unpaidTotal: Int, val byCategory: List<SaleCategoryStat>) {
    companion object {
        fun from(o: JSONObject) = SalesSummary(
            o.optInt("total"), o.optInt("unpaidTotal"),
            o.optJSONArray("byCategory")?.map(SaleCategoryStat::from) ?: emptyList(),
        )
    }
}

data class StudioSale(
    val id: Int,
    val category: String,
    val title: String,
    val amount: Int,
    val unpaidAmount: Int,
    val method: String?,
    val soldAt: String,
    val memberId: Int?,
) {
    val categoryLabel: String get() = StudioSaleCat.label(category)

    companion object {
        fun from(o: JSONObject) = StudioSale(
            o.optInt("id"), o.optString("category"), o.optString("title"),
            o.optInt("amount"), o.optInt("unpaidAmount"), o.stringOrNull("method"),
            o.optString("soldAt"), o.intOrNull("memberId"),
        )
    }
}

object StudioSaleCat {
    val all = listOf(
        "PASS" to "수강권", "PRODUCT" to "상품", "RENTAL" to "대여",
        "CLASS" to "수업", "POINT" to "포인트", "ETC" to "기타",
    )
    fun label(c: String) = all.firstOrNull { it.first == c }?.second ?: c
}

// ── 일정 ──

data class StudioSession(
    val id: Int,
    val date: String,
    val startTime: String?,
    val endTime: String?,
    val title: String?,
    val reservedCount: Int,
    val capacity: Int,
) {
    companion object {
        fun from(o: JSONObject) = StudioSession(
            o.optInt("id"), o.optString("date"), o.stringOrNull("startTime"), o.stringOrNull("endTime"),
            o.stringOrNull("title"), o.optInt("reservedCount"), o.optInt("capacity"),
        )
    }
}

data class StudioScheduleData(val sessions: List<StudioSession>, val blocks: List<com.muyeon.app.ui.lesson.StudioBlock>) {
    companion object {
        fun from(o: JSONObject) = StudioScheduleData(
            o.optJSONArray("sessions")?.map(StudioSession::from) ?: emptyList(),
            o.optJSONArray("blocks")?.map(com.muyeon.app.ui.lesson.StudioBlock::from) ?: emptyList(),
        )
    }
}

object StudioDateFmt {
    /** 'yyyy-MM-dd' 또는 ISO → 오늘로부터 남은 일수(올림). */
    fun daysUntil(s: String): Int? {
        val t = QuoteUi.parseDate(s) ?: runCatching {
            com.muyeon.app.ui.lesson.kstYmd.parse(s.take(10))?.time
        }.getOrNull() ?: return null
        return ceil((t - System.currentTimeMillis()) / 86_400_000.0).toInt()
    }

    fun won(n: Int): String = String.format(Locale.KOREA, "%,d원", n)
}

class StudioApi(private val token: String?) {

    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    suspend fun members(q: String?, tab: String?): Result<List<StudioMemberSummary>> {
        val query = buildList {
            if (!q.isNullOrEmpty()) add("q=" + java.net.URLEncoder.encode(q, "UTF-8"))
            if (!tab.isNullOrEmpty()) add("tab=$tab")
        }
        val path = "/studio/members" + if (query.isEmpty()) "" else "?" + query.joinToString("&")
        return call(path).map { JSONArray(it.ifBlank { "[]" }).map(StudioMemberSummary::from) }
    }

    suspend fun member(id: Int): Result<StudioMemberDetail> =
        call("/studio/members/$id").map { StudioMemberDetail.from(JSONObject(it)) }

    suspend fun updateMember(id: Int, memo: String?, grade: String?, leadStatus: String?): Result<Unit> =
        call(
            "/studio/members/$id", "PATCH",
            JSONObject().apply {
                memo?.let { put("memo", it) }
                grade?.let { put("grade", it) }
                leadStatus?.let { put("leadStatus", it) }
            },
        ).map { }

    suspend fun issuePass(memberId: Int, body: JSONObject): Result<Unit> =
        call("/studio/members/$memberId/passes", "POST", body).map { }

    suspend fun updatePass(id: Int, body: JSONObject): Result<Unit> =
        call("/studio/passes/$id", "PATCH", body).map { }

    suspend fun salesSummary(from: String, to: String): Result<SalesSummary> =
        call("/studio/sales/summary?from=$from&to=$to").map { SalesSummary.from(JSONObject(it)) }

    suspend fun sales(from: String, to: String): Result<List<StudioSale>> =
        call("/studio/sales?from=$from&to=$to").map { JSONArray(it.ifBlank { "[]" }).map(StudioSale::from) }

    suspend fun createSale(body: JSONObject): Result<Unit> = call("/studio/sales", "POST", body).map { }

    suspend fun schedule(from: String, to: String): Result<StudioScheduleData> =
        call("/studio/schedule?from=$from&to=$to").map { StudioScheduleData.from(JSONObject(it)) }

    suspend fun createBlock(body: JSONObject): Result<Unit> = call("/studio/blocks", "POST", body).map { }

    suspend fun updateBlock(id: Int, body: JSONObject): Result<Unit> = call("/studio/blocks/$id", "PATCH", body).map { }

    suspend fun deleteBlock(id: Int): Result<Unit> = call("/studio/blocks/$id", "DELETE").map { }

    suspend fun getSettings(): Result<Boolean> =
        call("/studio/settings").map { JSONObject(it.ifBlank { "{}" }).optBoolean("noshowConsumes", false) }

    suspend fun updateSettings(noshowConsumes: Boolean): Result<Unit> =
        call("/studio/settings", "PATCH", JSONObject().put("noshowConsumes", noshowConsumes)).map { }

    private suspend fun call(path: String, method: String = "GET", body: JSONObject? = null): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = when {
                    body != null -> body.toString().toRequestBody(JSON)
                    method != "GET" && method != "DELETE" -> "".toRequestBody(JSON)
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
