package com.muyeon.app.ui.floating

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.muyeon.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * 플로팅 오버레이 상태 — iOS `WebViewModel` 의 currentRoute/floatingHidden/hideChatFloat 대응.
 *
 * 웹은 이 값을 `routeChanged` / `setFloatingHidden` 상태 통지로 알려준다.
 * AOS 는 웹뷰가 액티비티에 있고 오버레이는 그 위 ComposeView 라, 둘이 공유할 곳이 필요해
 * 전역 object 로 둔다(웹뷰는 앱에 하나뿐이다).
 */
object FloatingState {

    /**
     * 현재 웹 라우트. 기본값 "/" 는 인증 라우트로 취급한다 —
     *  웹이 routeChanged 로 알려주기 전까지는 어느 화면인지 알 수 없으므로 안 띄우는 쪽이 안전하다
     *  (기본값을 /home 으로 두면 앱 실행 직후 로그인 화면에 플로팅이 스친다. iOS 주석과 같은 이유).
     */
    var currentRoute by mutableStateOf("/")
        private set

    /** 웹 모달(하단시트/딤드) 표시 중 — 네이티브 플로팅 전체 숨김(X버튼 가림 방지). */
    var floatingHidden by mutableStateOf(false)
        private set

    /** 스크롤 다운 등으로 채팅 플로팅만 숨김. */
    var hideChatFloat by mutableStateOf(false)
        private set

    /** 상태 재조회를 요청하는 신호(소켓 이벤트·로그인 변경). 값이 바뀌면 오버레이가 다시 읽는다. */
    var refreshTick by mutableIntStateOf(0)
        private set

    // ⚠️ 이름을 setFloatingHidden 으로 두면 `var floatingHidden` 의 JVM setter 와
    //   시그니처가 겹쳐 컴파일이 막힌다(Platform declaration clash).
    fun updateRoute(path: String) { currentRoute = path.ifEmpty { "/" } }
    fun updateFloatingHidden(hidden: Boolean) { floatingHidden = hidden }
    fun updateHideChatFloat(hidden: Boolean) { hideChatFloat = hidden }
    fun requestRefresh() { refreshTick += 1 }

    /**
     * 인증 전(공개) 라우트 — 웹 PublicRoutes 와 1:1. 여기서는 어떤 플로팅도 띄우지 않는다.
     *  ⚠️ 토큰은 기기에 남아 있어 재설치 후 웹 세션만 초기화되면 '네이티브는 로그인 /
     *    웹은 로그인화면' 상태가 된다. 그때 로그인 화면 위에 책갈피가 뜨는 걸 막는다.
     */
    private val AUTH_ROUTES = setOf(
        "/", "/login", "/register", "/signupTerms", "/social-signup",
        "/oauth/kakao", "/accountRecovery", "/notFound", "/maintenance",
    )

    /** 플로팅 채팅버튼 노출 라우트(최상위 탭만) + 일반회원 탭. */
    private val CHAT_FLOAT_ROUTES = setOf(
        "/home", "/lessons", "/casting", "/jobs", "/subs", "/mypage",
        "/community", "/performance",
    )

    private val normalizedRoute: String
        get() {
            val path = currentRoute.substringBefore("?")
            if (path == "/") return "/"
            return if (path.endsWith("/") && path.length > 1) path.dropLast(1) else path
        }

    val isAuthRoute: Boolean get() = normalizedRoute in AUTH_ROUTES

    val isChatFloatRoute: Boolean
        get() = !isAuthRoute && normalizedRoute in CHAT_FLOAT_ROUTES
}

/** 인증 상태 요약 — GET /me/roles/verification-status. */
data class VerifyStatus(val role: String?, val status: String)

class FloatingApi(private val token: String?) {

    private val client = OkHttpClient()
    private val apiBase = BuildConfig.API_BASE_URL + "/api"

    val isLoggedIn: Boolean get() = !token.isNullOrEmpty()

    suspend fun verificationStatus(): VerifyStatus? = get("/me/roles/verification-status")?.let {
        VerifyStatus(it.optString("role").ifEmpty { null }, it.optString("status").ifEmpty { "NONE" })
    }

    suspend fun unreadCount(): Int = get("/chat/unread-count")?.optInt("count") ?: 0

    private suspend fun get(path: String): JSONObject? = withContext(Dispatchers.IO) {
        if (token.isNullOrEmpty()) return@withContext null
        runCatching {
            val req = Request.Builder().url(apiBase + path)
                .addHeader("Authorization", "Bearer $token").build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@use null
                res.body?.string()?.takeIf { it.isNotBlank() }?.let { JSONObject(it) }
            }
        }.getOrNull()
    }
}

/**
 * 완료(APPROVED) 확인 여부 — 역할별로 기억한다. 확인하면 책갈피를 숨긴다.
 *  iOS 는 UserDefaults("verifyAck_<role>") 를 쓴다.
 */
object VerifyAck {
    private const val PREFS = "muyeon.verifyAck"

    fun isAcked(context: Context, role: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(role, false)

    fun acknowledge(context: Context, role: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(role, true).apply()
    }
}
