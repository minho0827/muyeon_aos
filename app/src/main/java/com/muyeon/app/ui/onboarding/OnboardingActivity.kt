package com.muyeon.app.ui.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.NativeWebRoute
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

        fun startRoleManage(context: Context, payloadJson: String?, heroImageUrl: String?) =
            context.go(
                Intent(context, OnboardingActivity::class.java)
                    .putExtra(EXTRA_ROUTE, "manage")
                    .putExtra(EXTRA_PAYLOAD, payloadJson ?: "")
                    .putExtra(EXTRA_HERO, heroImageUrl ?: ""),
            )

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val route = intent.getStringExtra(EXTRA_ROUTE) ?: "manage"
        val payloadJson = intent.getStringExtra(EXTRA_PAYLOAD)
        val hero = intent.getStringExtra(EXTRA_HERO)?.ifEmpty { null }
        val role = intent.getStringExtra(EXTRA_ROLE)?.ifEmpty { null }

        setContent {
            val nav = rememberNavController()
            val token = remember { TokenManager.getAccessToken(this) }
            val api = remember { RoleVerificationApi(token) }
            val payload = remember(payloadJson) { RoleManagePayload.parse(payloadJson) }

            NavHost(nav, startDestination = route) {
                composable("manage") {
                    RoleManageScreen(
                        payload = payload,
                        heroImageUrl = hero,
                        onClose = { finish() },
                        onAdd = { r -> notifyWeb("if(window.__onRoleManageAdd){ window.__onRoleManageAdd('$r'); }") },
                        onRemove = { r -> notifyWeb("if(window.__onRoleRemove){ window.__onRoleRemove('$r'); }") },
                        onVerify = { r -> nav.navigate("verify/$r") },
                        onSelectActive = { t -> notifyWeb("if(window.__onActiveTypeChanged){ window.__onActiveTypeChanged('$t'); }") },
                    )
                }
                composable("verify") {
                    RoleVerificationScreen(api, role.orEmpty(), onClose = { finish() }, onDone = ::verifyDone)
                }
                composable("verify/{role}") { e ->
                    RoleVerificationScreen(
                        api, e.arguments?.getString("role").orEmpty(),
                        onClose = { if (!nav.popBackStack()) finish() }, onDone = ::verifyDone,
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

    /** 서류 제출 완료 → 웹 콜백으로 심사중 상태 반영. */
    private fun verifyDone(role: String, urls: List<String>) {
        val arr = urls.joinToString(",") { "'${it.replace("'", "\\'")}'" }
        notifyWeb("if(window.__onRoleManageVerify){ window.__onRoleManageVerify('$role', [$arr]); }")
    }

    private fun notifyWeb(js: String) = NativeWebRoute.notifyWebAndFinish(this, js)
}
