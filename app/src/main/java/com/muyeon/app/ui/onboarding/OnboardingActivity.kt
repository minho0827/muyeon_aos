package com.muyeon.app.ui.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.ActiveRole
import com.muyeon.app.webview.NativeWebRoute
import com.muyeon.app.webview.WebCallbacks
import com.muyeon.app.webview.WebCallbackQueue
import org.json.JSONArray
import org.json.JSONObject

/**
 * 온보딩/회원유형 컨테이너 — 웹 `openRoleManage` / `openRoleVerification` /
 *  `openSignupTerms` / `openAddressSetup` / `openNotificationConsent` 브릿지 진입점.
 *
 * ⚠️ 역할 추가·해제·활동유형 변경은 **웹 콜백**으로만 서버에 반영된다(iOS 계약).
 *   각 액션은 NativeWebRoute.notifyWebAndFinish 로 웹뷰에 돌아가 콜백을 호출한다.
 */
class OnboardingActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_ROUTE = "route"
        private const val EXTRA_PAYLOAD = "payload"     // 웹이 넘긴 역할 JSON
        private const val EXTRA_HERO = "hero"
        private const val EXTRA_ROLE = "role"
        private const val EXTRA_ACTIVE_TYPE = "activeType"

        /**
         * @param rolesJson 웹 data.roles — `{"held":[...],"roles":[...]}` 형태의 **JSON 문자열**
         * @param activeType 활동 유형. rolesJson 바깥(평평한 키)에 오므로 따로 받는다
         */
        fun startRoleManage(
            context: Context, rolesJson: String?, heroImageUrl: String?, activeType: String?,
        ) = context.go(
            Intent(context, OnboardingActivity::class.java)
                .putExtra(EXTRA_ROUTE, "manage")
                .putExtra(EXTRA_PAYLOAD, rolesJson ?: "")
                .putExtra(EXTRA_HERO, heroImageUrl ?: "")
                .putExtra(EXTRA_ACTIVE_TYPE, activeType ?: "GENERAL"),
        )

        /**
         * 회원유형 선택 온보딩(가입 완료 직후, 강제) — `openRoleOnboarding`.
         *  선택 → (인증 필요 유형이면) 서류 첨부 → 웹 __onRoleComplete 통지.
         */
        fun startRoleOnboarding(context: Context) =
            context.go(Intent(context, OnboardingActivity::class.java).putExtra(EXTRA_ROUTE, "roleOnboarding"))

        fun startVerification(context: Context, role: String?) =
            context.go(
                Intent(context, OnboardingActivity::class.java)
                    .putExtra(EXTRA_ROUTE, "verify")
                    .putExtra(EXTRA_ROLE, role ?: ""),
            )

        /** 약관 동의(강제 게이트) — `openSignupTerms`. */
        fun startTerms(context: Context) =
            context.go(Intent(context, OnboardingActivity::class.java).putExtra(EXTRA_ROUTE, "terms"))

        /** 관심 지역 설정 — `openAddressSetup`. */
        fun startAddress(context: Context) =
            context.go(Intent(context, OnboardingActivity::class.java).putExtra(EXTRA_ROUTE, "address"))

        /** 알림 허용 시트 — `openNotificationConsent`. */
        fun startNotificationConsent(context: Context) =
            context.go(Intent(context, OnboardingActivity::class.java).putExtra(EXTRA_ROUTE, "notification"))

        private fun Context.go(i: Intent) {
            if (this !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        }
    }

    /** 인증 화면 결과(제출 여부)를 관리 화면에 돌려주는 콜백 — iOS onVerify completion 대응. */
    private var verifyCompletion: ((Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 시스템 뒤로가기도 닫기와 같은 경로로 — 쌓아둔 웹 콜백을 흘려보내고 나간다.
        onBackPressedDispatcher.addCallback(this) { closeAndFlush() }
        val route = intent.getStringExtra(EXTRA_ROUTE) ?: "manage"
        val payloadJson = intent.getStringExtra(EXTRA_PAYLOAD)
        val hero = intent.getStringExtra(EXTRA_HERO)?.ifEmpty { null }
        val role = intent.getStringExtra(EXTRA_ROLE)?.ifEmpty { null }
        val activeType = intent.getStringExtra(EXTRA_ACTIVE_TYPE)?.ifEmpty { null } ?: "GENERAL"

        setContent {
            val nav = rememberNavController()
            val token = remember { TokenManager.getAccessToken(this) }
            val api = remember { RoleVerificationApi(token) }
            val payload = remember(payloadJson, activeType) { RoleManagePayload.parse(payloadJson, activeType) }

            NavHost(nav, startDestination = route) {
                composable("manage") {
                    RoleManageScreen(
                        payload = payload,
                        heroImageUrl = hero,
                        onClose = { closeAndFlush() },
                        // ★ 여기서 화면을 닫지 않는다(iOS 동일) — 콜백은 쌓아뒀다가 나갈 때 한 번에 보낸다.
                        //   매번 닫으면 유형을 하나 바꿀 때마다 화면이 튕겨 나가 연속 조작이 불가능하다.
                        onAdd = { r -> queueWeb("if(window.__onRoleManageAdd){ window.__onRoleManageAdd('${esc(r)}'); }") },
                        onRemove = { r -> queueWeb("if(window.__onRoleRemove){ window.__onRoleRemove('${esc(r)}'); }") },
                        onVerify = { r, done -> verifyCompletion = done; nav.navigate("verify/$r") },
                        onSelectActive = { t -> selectActiveType(t) },
                    )
                }
                // 인증 유도 팝업(`openRoleVerification`) 직행 경로 — 관리 화면이 없으므로 제출 즉시 웹으로 돌아간다.
                //  이 경로의 웹 콜백은 __onRoleVerify 다(제출 + 마이페이지 제출완료 안내까지 웹이 처리).
                composable("verify") {
                    RoleVerificationScreen(
                        api, role.orEmpty(),
                        onClose = { finish() },
                        onDone = { r, urls, academyName ->
                            notifyWeb(verifyJs("__onRoleVerify", r, urls, academyName))
                        },
                    )
                }
                composable("verify/{role}") { e ->
                    val code = e.arguments?.getString("role").orEmpty()
                    RoleVerificationScreen(
                        api, code,
                        // 그냥 닫은 것 = 미제출. 심사중으로 바꾸면 사용자가 제출한 줄 알고 기다린다.
                        onClose = {
                            verifyCompletion?.invoke(false); verifyCompletion = null
                            if (!nav.popBackStack()) closeAndFlush()
                        },
                        onDone = { r, urls, academyName ->
                            val submitted = urls.isNotEmpty()
                            verifyDone(r, urls, academyName)
                            verifyCompletion?.invoke(submitted); verifyCompletion = null
                            // 제출을 마쳤으면 인증 화면만이 아니라 회원유형 관리 화면까지 함께 닫는다(iOS 동일).
                            //  관리 화면이 남으면 방금 제출이 처리됐는지 알 수 없고, 웹의 제출완료 안내도
                            //  화면 뒤에 가려진다. 큐에 쌓인 콜백은 웹뷰가 앞으로 나올 때 실행된다.
                            if (submitted) closeAndFlush()
                            else if (!nav.popBackStack()) closeAndFlush()
                        },
                        initialImages = payload.documents[code].orEmpty(),
                    )
                }
                composable("roleOnboarding") {
                    RoleOnboardingScreen(
                        onSelect = { r ->
                            if (roleRequiresVerification(r)) {
                                // 온보딩 위에 인증 첨부화면 — 완료 시 온보딩까지 함께 닫고 웹 통지(iOS 와 동일).
                                nav.navigate("roleOnboardingVerify/$r")
                            } else {
                                notifyWeb(verifyJs("__onRoleComplete", r, emptyList(), null))
                            }
                        },
                    )
                }
                composable("roleOnboardingVerify/{role}") { e ->
                    val code = e.arguments?.getString("role").orEmpty()
                    RoleVerificationScreen(
                        api, code,
                        // 최초 가입 온보딩 — 여기서만 건너뛰기를 허용한다(닫을 다른 수단이 없다).
                        //  ★ iOS allowSkip:true 와 같은 취지. 빈 목록으로 통지해 '미제출'을 그대로 알린다.
                        onClose = { notifyWeb(verifyJs("__onRoleComplete", code, emptyList(), null)) },
                        onDone = { r, urls, academyName ->
                            notifyWeb(verifyJs("__onRoleComplete", r, urls, academyName))
                        },
                    )
                }
                composable("terms") {
                    SignupTermsScreen(
                        onAgree = { m ->
                            val json = JSONObject().apply { m.forEach { (k, v) -> put(k, v) } }
                            notifyWeb("if(window.__onSignupTermsAgreed){ window.__onSignupTermsAgreed($json); }")
                        },
                        onDecline = { notifyWeb("if(window.__onSignupTermsDeclined){ window.__onSignupTermsDeclined(); }") },
                        onOpenPolicy = { doc -> NativeWebRoute.openWebAndFinish(this@OnboardingActivity, "/policy?doc=$doc") },
                    )
                }
                composable("address") {
                    val (r0, c0) = remember { OnboardingAddressStore.read(this@OnboardingActivity) }
                    AddressSetupScreen(
                        token, r0, c0,
                        onComplete = { region, code ->
                            OnboardingAddressStore.persist(this@OnboardingActivity, region, code)
                            // ★ 웹이 실제로 듣는 훅은 onAddressSelected(name, code) 하나다
                            //   (components/muyeon/NativeAddressBridge.js). __onAddressSetupComplete 는
                            //   웹에 정의된 적이 없어 지역이 반영되지 않고 있었다 — iOS notifyWebAddressSelected 와 맞춘다.
                            WebCallbacks.addressSelected(this@OnboardingActivity, region, code)
                            val json = JSONObject().put("region", region).put("code", code)
                            notifyWeb("if(window.__onAddressSetupComplete){ window.__onAddressSetupComplete($json); }")
                        },
                        onSkip = { notifyWeb("if(window.__onAddressSetupSkipped){ window.__onAddressSetupSkipped(); }") },
                    )
                }
                composable("notification") {
                    NotificationConsentSheet(
                        onAllow = { g -> notifyWeb("if(window.__onNotificationConsent){ window.__onNotificationConsent($g); }") },
                        onLater = { notifyWeb("if(window.__onNotificationConsent){ window.__onNotificationConsent(false); }") },
                    )
                }
            }
        }
    }

    /** 서류 제출 완료 → 웹 콜백으로 제출 API 수행 + 심사중 반영. */
    private fun verifyDone(role: String, urls: List<String>, academyName: String?) =
        queueWeb(verifyJs("__onRoleManageVerify", role, urls, academyName))

    /**
     * 인증 콜백 JS 한 줄. (역할, 서류 URL **JSON 문자열**, 학원명)
     *  ★ 웹은 2번째 인자를 `JSON.parse` 한다 — 배열 리터럴을 넘기면 파싱이 실패해
     *    서류가 **한 장도 제출되지 않는다**(화면상으론 제출된 것처럼 보인다). iOS 와 같은 문자열 계약.
     */
    private fun verifyJs(fn: String, role: String, urls: List<String>, academyName: String?): String {
        val json = JSONArray(urls).toString()
        return "if(window.$fn){ window.$fn('${esc(role)}', '${esc(json)}', '${esc(academyName.orEmpty())}'); }"
    }

    /**
     * 활동 유형 선택 — iOS `notifyWebActiveType` 1:1.
     *  ① 네이티브에 저장(X-Active-Type 이 옛값으로 남는 것 방지) ② 바뀌었으면 전환 토스트 ③ 웹 콜백.
     *  ★ 저장이 빠지면 네이티브 화면들이 "지금 학원인가 강사인가"를 영영 알 수 없다.
     */
    private fun selectActiveType(type: String) {
        val t = type.uppercase()
        val changed = ActiveRole.current(this) != t
        ActiveRole.store(this, t)
        if (changed) {
            Toast.makeText(this, "${ActiveRole.label(t)} 유형으로 전환됐어요", Toast.LENGTH_SHORT).show()
        }
        queueWeb("if(window.__onActiveTypeChanged){ window.__onActiveTypeChanged('${esc(t)}'); }")
    }

    // ── 웹 콜백 큐 ──
    //  AOS 는 웹뷰가 다른 액티비티에 있어 즉시 evaluateJavaScript 를 할 수 없다.
    //  그래서 콜백을 모아뒀다가 화면을 나갈 때 실행한다(웹은 뒤에 가려져 있어 결과는 동일).
    //  ★ 큐를 **디스크에 적는다**(WebCallbackQueue). 액티비티 메모리에만 두면 프로세스가
    //    정리되는 순간 조작이 통째로 유실되고, 화면엔 반영됐는데 서버는 모르는 상태가 된다.
    //    웹뷰가 다시 앞으로 나올 때(WebViewActivity.onResume) 남은 것을 흘려보낸다.
    private fun queueWeb(js: String) = WebCallbackQueue.enqueue(this, js)

    private fun esc(s: String) = s.replace("\\", "\\\\").replace("'", "\\'")

    /** 닫기·시스템 뒤로가기 공통. 큐는 디스크에 있으므로 여기선 화면만 닫으면 된다. */
    private fun closeAndFlush() = finish()

    private fun notifyWeb(js: String) = NativeWebRoute.notifyWebAndFinish(this, js)
}
