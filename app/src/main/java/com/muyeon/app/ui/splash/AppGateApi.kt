package com.muyeon.app.ui.splash

import android.content.Context
import com.muyeon.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 앱 시작 게이트 — 강제/선택 업데이트 안내 + 공지 팝업. 스플래시에서 한 번 호출한다.
 *
 * ⚠️ 실패하면 **그냥 진입한다(fail-open)**. 여기서 막으면 서버 장애가 곧 앱 마비다.
 *    UX 장치지 보안 장치가 아니다 — 네트워크를 끊으면 우회된다.
 * ⚠️ 판정은 전부 서버가 한다. 앱은 action 만 따른다. 앱이 버전을 비교하면,
 *    비교 로직에 버그가 있는 구버전은 영원히 강제 업데이트를 걸 수 없게 된다.
 *
 * ui/chat 의 ChatApi 와 같은 패턴(OkHttp + org.json).
 */
data class AppGateUpdate(
    val action: String,               // FORCE | OPTIONAL | NONE
    val latestVersionName: String?,
    val storeUrl: String?,
    val title: String?,
    val message: String?,
)

data class AppGateNotice(
    val id: Int,
    val title: String,
    val body: String?,
    val imageUrl: String?,
    val linkUrl: String?,
    val dismissType: String,          // NONE | TODAY | ALWAYS
) {
    /**
     * 표시용 절대 URL. 서버는 상대경로(/images/...)로도 내려준다.
     * ui/quote 의 imageUrl() 과 같은 규칙 — 한쪽만 바꾸면 화면마다 결과가 달라진다.
     */
    val imageUrlAbsolute: String?
        get() = imageUrl?.takeIf { it.isNotBlank() }
            ?.let { if (it.startsWith("http")) it else BuildConfig.API_BASE_URL + it }
}

data class AppGateResult(val update: AppGateUpdate?, val notice: AppGateNotice?)

object AppGateApi {

    /**
     * 비교 기준이 되는 정수 빌드번호. AOS 는 versionCode.
     * ⚠️ versionName("1.0.0")이 아니다. 그건 표시용이라 정수 비교를 못 한다.
     */
    val buildNumber: Int get() = BuildConfig.VERSION_CODE

    private val client = OkHttpClient.Builder()
        // 스플래시에서 사용자를 오래 세워 두면 안 된다.
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .build()

    /** 어떤 실패든 null 을 돌려주고, 호출부는 그대로 진입한다. */
    suspend fun fetch(): AppGateResult? = withContext(Dispatchers.IO) {
        try {
            val url = "${BuildConfig.API_BASE_URL}/api/app-gate?platform=AOS&build=$buildNumber"
            android.util.Log.d("AppGate", "요청 build=$buildNumber url=$url")
            client.newCall(Request.Builder().url(url).get().build()).execute().use { res ->
                if (!res.isSuccessful) {
                    android.util.Log.d("AppGate", "응답 status=${res.code} → 그대로 진입")
                    return@withContext null
                }
                val root = JSONObject(res.body?.string().orEmpty().ifBlank { "{}" })

                val u = root.optJSONObject("update")?.let {
                    AppGateUpdate(
                        action = it.optString("action", "NONE"),
                        latestVersionName = it.optString("latestVersionName").ifBlank { null },
                        storeUrl = it.optString("storeUrl").ifBlank { null },
                        title = it.optString("title").ifBlank { null },
                        message = it.optString("message").ifBlank { null },
                    )
                }
                val n = root.optJSONObject("notice")?.let {
                    AppGateNotice(
                        id = it.optInt("id"),
                        title = it.optString("title"),
                        body = it.optString("body").ifBlank { null },
                        imageUrl = it.optString("imageUrl").ifBlank { null },
                        linkUrl = it.optString("linkUrl").ifBlank { null },
                        dismissType = it.optString("dismissType", "TODAY"),
                    )
                }
                android.util.Log.d(
                    "AppGate",
                    "update=${u?.action ?: "nil"} notice=" +
                        (n?.let { "id ${it.id} '${it.title}' dismiss=${it.dismissType} img=${it.imageUrl ?: "없음"}" } ?: "없음"),
                )
                AppGateResult(u, n)
            }
        } catch (e: Exception) {
            android.util.Log.d("AppGate", "조회 실패 — 그대로 진입: ${e.message}")
            null
        }
    }
}

/**
 * 공지 팝업 '다시 보지 않기' 기억.
 *
 * ⚠️ 업데이트 '다음에'는 저장하지 않는다 — 이번 실행만 건너뛰고 다음에 켜면 다시 뜬다.
 *    기억해 두면 다음 버전이 나올 때까지 영영 안 떠서 업데이트율이 바닥난다.
 */
object AppGateDismiss {
    private const val PREFS = "app_gate"

    private fun today(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.KOREA).format(java.util.Date())

    fun isDismissed(ctx: Context, n: AppGateNotice): Boolean {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return when (n.dismissType) {
            "ALWAYS" -> p.getBoolean("always.${n.id}", false)
            "TODAY" -> p.getString("today.${n.id}", null) == today()
            else -> false
        }
    }

    fun remember(ctx: Context, n: AppGateNotice) {
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        when (n.dismissType) {
            "ALWAYS" -> p.putBoolean("always.${n.id}", true)
            "TODAY" -> p.putString("today.${n.id}", today())
            else -> {}
        }
        p.apply()
    }
}
