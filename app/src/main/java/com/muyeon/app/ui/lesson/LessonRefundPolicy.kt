package com.muyeon.app.ui.lesson

import com.muyeon.app.BuildConfig
import com.muyeon.app.ui.quote.stringOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * 레슨 예약금 환불 규정 — 서버(관리자 '결제·수익화 > 환불 규정')가 정한 문구와 예약금 한도.
 *
 * ★ 화면에서 규정 문구를 새로 쓰지 말 것. 예전엔 같은 문구가 앱·웹 여러 곳에 복사돼 있어
 *   규정을 바꾸면 한 곳이라도 빠질 때 안내와 실제 환불이 어긋났다(iOS LessonRefundPolicy.swift 와 같은 계약).
 * ★ 서버를 못 부르면 [FALLBACK](2026-09-06 규정)으로 보여 준다 — 예약 화면이 비면 안 된다.
 */
data class LessonRefundPolicy(
    val lines: List<Line>,
    val summary: String?,
    val paragraph: String,
    val extraNotice: String?,
    val deposit: Deposit,
) {
    data class Line(val title: String, val detail: String, val accent: Boolean)
    data class Deposit(val unit: Int, val maxAmount: Int, val maxPercent: Int, val hint: String)

    /** 목록형 안내("· 제목 내용" 줄 + 추가 안내). */
    val bulletText: String
        get() = (lines.map { "· ${it.title} ${it.detail}" } +
            listOfNotNull(extraNotice?.takeIf { it.isNotBlank() }?.let { "· $it" })).joinToString("\n")

    /** 예약금 한도 검사 — 문제 없으면 null, 있으면 안내 문장. amount 는 레슨비·견적 금액(모르면 null). */
    fun depositError(value: Int, amount: Int?): String? {
        if (value < deposit.unit || value % deposit.unit != 0 || value > deposit.maxAmount) return deposit.hint
        if (amount != null && amount > 0 && (value > amount || value > amount * deposit.maxPercent / 100.0)) {
            return deposit.hint
        }
        return null
    }

    /** 예약금 선택지 — 1만원 단위로 최대 금액까지(최대가 1만원 미만이면 단위로). */
    val depositChoices: List<Int>
        get() {
            val step = maxOf(deposit.unit, 10_000)
            val values = (step..deposit.maxAmount step step).toList()
            return values.ifEmpty { listOf(deposit.unit) }
        }

    companion object {
        val FALLBACK = LessonRefundPolicy(
            lines = listOf(
                Line("수업 24시간 전까지", "예약금 전액 환불", true),
                Line("수업 24시간 이내", "예약금 환불 불가", false),
                Line("수업 시작 후·노쇼", "예약금 환불 불가", false),
                Line("강사 취소·수업 미제공", "예약금 전액 환불", true),
            ),
            summary = "예약금은 레슨비에 포함되며, 수업 24시간 전까지 취소하면 전액 환불돼요.",
            paragraph = "수업 24시간 전까지는 예약금 전액 환불, 24시간 이내에는 예약금이 환불되지 않습니다. " +
                "수업 시작 후·노쇼는 환불되지 않으며, 강사·학원 취소나 수업 미제공은 전액 환불됩니다.",
            extraNotice = null,
            deposit = Deposit(1_000, 50_000, 30, "예약금은 1,000원 단위로 금액의 30% 이하, 최대 5만원까지 설정할 수 있어요."),
        )

        /** 서버 JSON → 모델. 필수 값이 없으면 null(호출부가 폴백). */
        fun from(o: JSONObject?): LessonRefundPolicy? {
            if (o == null) return null
            val arr = o.optJSONArray("lines") ?: return null
            val dep = o.optJSONObject("deposit") ?: return null
            val lines = (0 until arr.length()).mapNotNull { arr.optJSONObject(it) }.map {
                Line(it.optString("title"), it.optString("detail"), it.optBoolean("accent"))
            }
            if (lines.isEmpty()) return null
            return LessonRefundPolicy(
                lines = lines,
                summary = o.stringOrNull("summary"),
                paragraph = o.optString("paragraph").ifEmpty { FALLBACK.paragraph },
                extraNotice = o.stringOrNull("extraNotice"),
                deposit = Deposit(
                    unit = dep.optInt("unit", 1_000),
                    maxAmount = dep.optInt("maxAmount", 50_000),
                    maxPercent = dep.optInt("maxPercent", 30),
                    hint = dep.optString("hint").ifEmpty { FALLBACK.deposit.hint },
                ),
            )
        }
    }
}

/** 지금 적용 중인 규정 — 앱 전체가 같은 값을 본다. 한 번 받으면 앱이 켜져 있는 동안 재사용한다. */
object LessonRefundPolicyRepo {
    @Volatile private var cached: LessonRefundPolicy? = null

    /** 캐시가 있으면 그 값, 없으면 서버 조회. 실패하면 기본 문구(다음 호출 때 다시 시도). */
    suspend fun get(): LessonRefundPolicy {
        cached?.let { return it }
        val fetched = withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder().url("${BuildConfig.API_BASE_URL}/api/refund-policy/lesson").build()
                OkHttpClient().newCall(req).execute().use { res ->
                    val body = res.body?.string().orEmpty()
                    if (!res.isSuccessful || body.isEmpty()) null else LessonRefundPolicy.from(JSONObject(body))
                }
            }.getOrNull()
        }
        if (fetched != null) cached = fetched
        return fetched ?: LessonRefundPolicy.FALLBACK
    }

    /** 이미 받아 둔 값(없으면 기본 문구) — 화면 첫 그리기용. */
    fun current(): LessonRefundPolicy = cached ?: LessonRefundPolicy.FALLBACK
}
