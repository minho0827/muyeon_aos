package com.muyeon.app.webview

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.muyeon.app.BuildConfig
import com.muyeon.app.utils.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ★ 2026-11-07 학원 인증 강제 차단(관리자 지정) — 차단 중인 회원은 학원 인증 승인 전까지 웹 '/verify-required' 화면만 본다.
 *
 *  · 앱이 앞으로 올 때(ProcessLifecycleOwner ON_START — 콜드 스타트 포함) GET /api/me/verify-gate 로 묻는다.
 *  · 차단 중이면 본문(WebViewActivity) 말고 떠 있는 네이티브 화면(채팅·알림함 등)을 닫고 본문을 앞으로 —
 *    학원 인증 화면(OnboardingActivity: 인증·약관)·스플래시·로그인은 그대로 둔다. 네이티브 화면이 새로 떠도(푸시 탭 등) 같은 처리.
 *  · 웹 버튼이 여는 네이티브 화면도 막는다(AppBridgeInterface.route — 학원 인증·약관만 허용).
 *  · 본문 화면 이동은 웹 라우터 가드가 한다(AppRouter — 차단 중엔 어디로 가도 /verify-required, 뒤로가기도 같다).
 *    상태가 바뀌면 웹에 window.__muyeonVerifyGateRecheck() 로 바로 다시 묻게 한다.
 *  화면 차단만 한다(서버 API 는 막지 않는다). iOS VerifyGate.swift 와 같은 규칙.
 */
object VerifyGate {
    private const val TAG = "VerifyGate"
    const val GATE_PATH = "/verify-required"

    /** 차단 중에도 웹 버튼으로 열 수 있는 네이티브 화면. */
    private val ALLOWED_ACTIONS = setOf("openRoleVerification", "openSignupTerms")
    private const val RECHECK_JS =
        "if(window.__muyeonVerifyGateRecheck){window.__muyeonVerifyGateRecheck();}"

    @Volatile var gated = false
        private set

    private val checking = AtomicBoolean(false)
    private val main = Handler(Looper.getMainLooper())
    private var resumed: WeakReference<Activity>? = null
    private val client = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS).build()

    /** MuyeonApplication.onCreate 에서 1회. */
    fun start(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                resumed = WeakReference(activity)
                if (gated) enforceOn(activity, notifyWeb = false)
            }
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { refresh(app) }
        })
    }

    /** 차단 중이라 이 웹 액션(open*)을 막아야 하나. */
    fun blocksAction(action: String): Boolean =
        gated && action.startsWith("open") && action !in ALLOWED_ACTIONS

    /** 막은 뒤 — 웹이 차단 상태를 다시 읽고 차단 화면으로 가게 한다. */
    fun recheckWeb(webView: android.webkit.WebView) {
        webView.evaluateJavascript("(function(){ try { $RECHECK_JS } catch(e) {} return 1; })()", null)
    }

    /** 서버에 묻는다(어느 스레드에서 불러도 된다). 실패(네트워크·옛 서버 404)면 지금 상태 그대로. */
    fun refresh(context: android.content.Context) {
        val app = context.applicationContext
        val token = TokenManager.getAccessToken(app)?.takeIf { it.isNotBlank() }
        if (token == null) { gated = false; return }
        if (!checking.compareAndSet(false, true)) return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val req = Request.Builder().url("${BuildConfig.API_BASE_URL}/api/me/verify-gate")
                    .addHeader("Authorization", "Bearer $token").get().build()
                val next = client.newCall(req).execute().use { res ->
                    if (!res.isSuccessful) return@use null
                    val o = runCatching { JSONObject(res.body?.string().orEmpty()) }.getOrNull() ?: return@use null
                    if (!o.has("gated")) null else o.optBoolean("gated")
                } ?: return@launch
                main.post { apply(next) }
            } catch (e: Exception) {
                Log.w(TAG, "refresh 실패(무시): ${e.message}")
            } finally {
                checking.set(false)
            }
        }
    }

    private fun apply(next: Boolean) {
        val changed = next != gated
        gated = next
        val top = resumed?.get()?.takeIf { !it.isFinishing } ?: return
        if (next) enforceOn(top, notifyWeb = changed)
        else if (changed) bringWeb(top, closeCurrent = false)   // 풀림 — 웹이 다시 읽고 홈으로 보낸다
    }

    /** 차단 중 이 화면을 계속 둘 수 있나 — 본문·학원 인증(온보딩)·스플래시·로그인. */
    private fun allowed(activity: Activity): Boolean =
        activity is WebViewActivity ||
            activity is com.muyeon.app.ui.onboarding.OnboardingActivity ||
            activity is com.muyeon.app.activity.SplashActivity ||
            activity is com.muyeon.app.activity.LoginActivity ||
            activity is com.muyeon.app.activity.IpConnectActivity

    private fun enforceOn(activity: Activity, notifyWeb: Boolean) {
        if (allowed(activity)) {
            if (notifyWeb && activity is WebViewActivity) bringWeb(activity, closeCurrent = false)
            return
        }
        Log.i(TAG, "차단 중 — ${activity.javaClass.simpleName} 닫고 본문으로")
        bringWeb(activity, closeCurrent = true)
    }

    /** 본문을 앞으로(CLEAR_TOP — 그 위 네이티브 화면은 닫힌다) + 웹에 다시 확인 요청. */
    private fun bringWeb(activity: Activity, closeCurrent: Boolean) {
        runCatching {
            activity.startActivity(
                Intent(activity, WebViewActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra(NativeWebRoute.EXTRA_EVAL_JS, RECHECK_JS),
            )
            if (closeCurrent && activity !is WebViewActivity) activity.finish()
        }.onFailure { Log.w(TAG, "본문 열기 실패: ${it.message}") }
    }
}
