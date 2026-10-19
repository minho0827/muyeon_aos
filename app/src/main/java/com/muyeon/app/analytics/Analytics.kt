package com.muyeon.app.analytics

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.muyeon.app.BuildConfig
import com.muyeon.app.utils.TokenManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 이용 행동 로그(2026-10-19) — 앱의 화면·세션·알림 열기·설치 경로를 우리 서버(POST /api/analytics/app-events)에 모은다.
 *  외부 분석 도구로 보내지 않는다(Firebase Analytics 자동수집은 매니페스트에서 껐다).
 *
 *  · 이벤트 이름·키는 웹(muyeon-front util/appAnalytics.js)·iOS 와 같다 — backend app-analytics.normalize.ts.
 *  · aid = 이 설치의 익명 ID, sid = 세션(30분 동안 아무 일 없으면 새로).
 *    앱 안 웹페이지에도 같은 aid·sid 를 넣어(window.__muyeonAnalytics) 웹 화면과 앱 화면이 한 세션으로 이어진다.
 *  · 첫 실행엔 Play 설치 리퍼러를 읽는다 — 웹이 스토어 링크에 넣은 aid·UTM 이 돌아와 "어느 광고·웹 방문에서 설치했는지"가 확정된다.
 *  · 화면: 네이티브 Activity 는 여기서 자동으로(ActivityLifecycleCallbacks), 웹 화면은 웹이 보낸다.
 *
 * ★ 로그 때문에 앱이 실패하면 안 된다 — 모든 진입점은 예외를 삼킨다.
 */
object Analytics {
    private const val PREFS = "muyeon_analytics"
    private const val KEY_AID = "aid"
    private const val KEY_SID = "sid"
    private const val KEY_SID_AT = "sid_at"
    private const val KEY_FIRST_DONE = "first_open_done"
    private const val SESSION_IDLE_MS = 30 * 60 * 1000L
    private const val FLUSH_MS = 15_000L
    private const val FLUSH_AT = 20

    private lateinit var app: Application
    private val queue = mutableListOf<JSONObject>()
    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private val http = OkHttpClient.Builder().callTimeout(10, TimeUnit.SECONDS).build()
    private val flushTask = Runnable { flush() }
    private var coldStart = true

    /** 지금 보이는 네이티브 화면(웹뷰 제외)과 들어온 시각 — 떠날 때 체류 시간을 잰다. */
    private var currentScreen: String? = null
    private var screenStartedAt = 0L
    private var lastScreen: String? = null

    private val prefs get() = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun start(application: Application) {
        app = application
        runCatching {
            application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityResumed(activity: Activity) = onScreenResumed(activity)
                override fun onActivityPaused(activity: Activity) = onScreenPaused(activity)
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                override fun onActivityStarted(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {}
            })
            ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    track("app_open", props = JSONObject().put("cold", coldStart))
                    coldStart = false
                }
                override fun onStop(owner: LifecycleOwner) {
                    track("app_background")
                    flush()
                }
            })
            if (!prefs.getBoolean(KEY_FIRST_DONE, false)) readInstallReferrer()
        }
    }

    // ── 식별 ─────────────────────────────────────────────

    val aid: String
        get() = prefs.getString(KEY_AID, null) ?: UUID.randomUUID().toString().also { prefs.edit().putString(KEY_AID, it).apply() }

    /** 세션 — 30분 동안 아무 이벤트가 없었으면 새로 만든다. 호출이 곧 활동이다. */
    private fun sid(): String {
        val now = System.currentTimeMillis()
        val cur = prefs.getString(KEY_SID, null)
        val at = prefs.getLong(KEY_SID_AT, 0L)
        val sid = if (cur != null && now - at < SESSION_IDLE_MS) cur else UUID.randomUUID().toString()
        prefs.edit().putString(KEY_SID, sid).putLong(KEY_SID_AT, now).apply()
        return sid
    }

    private fun appVersion() = "${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"

    // ── 이벤트 ───────────────────────────────────────────

    fun track(
        name: String,
        screen: String? = null,
        target: String? = null,
        targetId: String? = null,
        value: Long? = null,
        props: JSONObject? = null,
        utm: JSONObject? = null,
    ) {
        if (!::app.isInitialized) return
        runCatching {
            val e = JSONObject().put("name", name).put("ts", System.currentTimeMillis()).put("sid", sid())
            screen?.let { e.put("screen", it) }
            target?.let { e.put("target", it) }
            targetId?.let { e.put("targetId", it) }
            value?.let { e.put("value", it) }
            props?.let { e.put("props", it) }
            utm?.let { e.put("utm", it) }
            val size = synchronized(queue) { queue.add(e); queue.size }
            if (size >= FLUSH_AT) flush() else {
                main.removeCallbacks(flushTask)
                main.postDelayed(flushTask, FLUSH_MS)
            }
        }
    }

    /** 웹에서 네이티브 화면을 연 버튼(브릿지 액션) — 'native:openChat' 처럼. */
    fun click(target: String, targetId: String? = null) = track("click", target = target, targetId = targetId)

    fun flush() {
        if (!::app.isInitialized) return
        main.removeCallbacks(flushTask)
        val events = synchronized(queue) { queue.toList().also { queue.clear() } }
        if (events.isEmpty()) return
        val ctx = JSONObject()
            .put("aid", aid)
            .put("sid", sid())
            .put("platform", "AOS")
            .put("surface", "NATIVE")
            .put("appVersion", appVersion())
            .put("osVersion", Build.VERSION.RELEASE)
            .put("deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}".take(40))
        val body = JSONObject().put("ctx", ctx).put("events", JSONArray(events)).toString()
        val token = runCatching { TokenManager.getAccessToken(app) }.getOrNull()
        io.execute {
            runCatching {
                val req = Request.Builder()
                    .url(BuildConfig.API_BASE_URL + "/api/analytics/app-events")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                    .build()
                http.newCall(req).execute().close()
            }
            // 실패하면 버린다 — 로그 재전송 때문에 배터리·데이터를 더 쓰지 않는다(다음 배치는 정상 전송).
        }
    }

    // ── 네이티브 화면 ────────────────────────────────────

    /** Activity 이름 → 화면 이름. 'ChatActivity' → 'native:Chat'. 웹뷰는 웹이 화면을 보내므로 뺀다. */
    private fun screenName(activity: Activity): String? {
        val n = activity.javaClass.simpleName
        if (n == "WebViewActivity" || n == "SplashActivity") return null
        return "native:" + n.removeSuffix("Activity")
    }

    private fun onScreenResumed(activity: Activity) {
        val name = screenName(activity) ?: return
        currentScreen = name
        screenStartedAt = System.currentTimeMillis()
        track("screen_view", screen = name, props = lastScreen?.let { JSONObject().put("from", it) })
        lastScreen = name
    }

    private fun onScreenPaused(activity: Activity) {
        val name = screenName(activity) ?: return
        if (name != currentScreen) return
        track("screen_leave", screen = name, value = System.currentTimeMillis() - screenStartedAt)
        currentScreen = null
    }

    /** Activity 안에서 화면이 바뀌는 곳(채팅·스튜디오 NavHost) — 목적지 이름으로 따로 남긴다. */
    fun subScreen(owner: String, route: String?) {
        val r = route?.substringBefore('/')?.substringBefore('?')?.takeIf { it.isNotBlank() } ?: return
        track("screen_view", screen = "native:$owner.$r")
    }

    // ── 앱 안 웹페이지에 식별값 넣기 ─────────────────────

    /** 페이지 로드가 끝날 때·앱이 앞으로 나올 때 부른다. 웹(appAnalytics.js)은 보낼 때마다 이 값을 읽는다. */
    fun injectInto(webView: WebView) {
        if (!::app.isInitialized) return
        runCatching {
            val v = JSONObject()
                .put("aid", aid)
                .put("sid", sid())
                .put("platform", "AOS")
                .put("appVersion", appVersion())
                .put("osVersion", Build.VERSION.RELEASE)
                .put("deviceModel", "${Build.MANUFACTURER} ${Build.MODEL}".take(40))
            webView.evaluateJavascript("window.__muyeonAnalytics=$v;", null)
        }
    }

    // ── 알림·링크로 들어옴 ───────────────────────────────

    fun pushOpen(type: String?, path: String?) =
        track("push_open", target = type?.takeIf { it.isNotBlank() } ?: "unknown", props = path?.let { JSONObject().put("path", it.take(120)) })

    fun deeplinkOpen(type: String?, path: String?) =
        track("deeplink_open", target = type?.takeIf { it.isNotBlank() } ?: "unknown", props = path?.let { JSONObject().put("path", it.take(120)) })

    // ── 첫 실행: Play 설치 리퍼러 ────────────────────────

    /**
     * 웹이 Play 링크에 붙인 referrer(aid=…&utm_source=…)를 1회 읽어 first_open 으로 보낸다.
     *  리퍼러가 없거나(직접 검색 설치) 서비스가 안 되면 리퍼러 없이 보낸다. 끝나면 다시 읽지 않는다.
     */
    private fun readInstallReferrer() {
        val client = InstallReferrerClient.newBuilder(app).build()
        val done = { referrer: String? ->
            if (!prefs.getBoolean(KEY_FIRST_DONE, false)) {
                prefs.edit().putBoolean(KEY_FIRST_DONE, true).apply()
                val q = parseQuery(referrer)
                val utm = JSONObject()
                listOf("source", "medium", "campaign", "content").forEach { k -> q["utm_$k"]?.let { utm.put(k, it.take(100)) } }
                val props = JSONObject()
                q["aid"]?.let { props.put("webAid", it) }
                // 로그가 없던 옛 버전에서 업데이트한 첫 실행 — 서버가 새 설치로 세지 않는다.
                if (isUpgrade()) props.put("upgrade", true)
                referrer?.let { props.put("referrer", it.take(200)) }
                track("first_open", props = props, utm = utm.takeIf { it.length() > 0 })
                flush()
            }
            runCatching { client.endConnection() }
        }
        runCatching {
            client.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(code: Int) {
                    val ref = if (code == InstallReferrerClient.InstallReferrerResponse.OK) {
                        runCatching { client.installReferrer.installReferrer }.getOrNull()
                    } else null
                    done(ref)
                }
                override fun onInstallReferrerServiceDisconnected() {}
            })
        }.onFailure { done(null) }
    }

    /** 처음 설치한 뒤 업데이트된 적이 있으면 true. 앱을 지우고 다시 깔면 두 시각이 다시 같아진다. */
    private fun isUpgrade(): Boolean = runCatching {
        val info = app.packageManager.getPackageInfo(app.packageName, 0)
        info.lastUpdateTime - info.firstInstallTime > 60_000L
    }.getOrDefault(false)

    private fun parseQuery(s: String?): Map<String, String> =
        s.orEmpty().split('&').mapNotNull { part ->
            val i = part.indexOf('=')
            if (i <= 0) null else runCatching {
                java.net.URLDecoder.decode(part.substring(0, i), "UTF-8") to java.net.URLDecoder.decode(part.substring(i + 1), "UTF-8")
            }.getOrNull()
        }.toMap()
}
