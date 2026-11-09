package com.muyeon.app

import android.app.Application
import org.koin.android.ext.koin.androidContext
import com.muyeon.app.analytics.Analytics
import com.muyeon.app.ui.membership.MembershipBilling

/**
 * 앱 전역 시작점.
 *
 *  · 멤버십 구글 결제(MembershipBilling) — 시작 시 1회 연결하고, 앱이 앞으로 나올 때마다
 *    서버가 아직 모르는 구매를 다시 보낸다(iOS AppDelegate 의 MembershipStore.shared.start() 대응).
 *  · 이용 행동 로그(Analytics) — 세션·네이티브 화면·첫 실행 설치 경로를 우리 서버에 모은다.
 */
class MuyeonApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // ★ 2026-11-07 탭 네이티브 전환 — Koin(:network → :data → :domain → :app). docs/ARCHITECTURE.md
        org.koin.core.context.startKoin {
            androidContext(this@MuyeonApplication)
            modules(
                com.muyeon.network.di.networkModules() + listOf(
                    com.muyeon.data.di.dataModule,
                    com.muyeon.domain.di.domainModule,
                    com.muyeon.app.di.appModule,
                    com.muyeon.app.di.viewModelModule,
                ),
            )
        }
        com.muyeon.domain.common.UserFacingError.install(
            network = { getString(R.string.common_error_network) },
            server = { getString(R.string.common_error_server) },
        )
        // ★ 2026-11-09 계정 바뀜 신호 — 지금 회원을 적고, 로그아웃 때 지울 기억값 지우개를 건다(auth/SessionResets).
        com.muyeon.domain.common.SessionEvents.start(com.muyeon.app.utils.TokenManager.accountKey(com.muyeon.app.utils.TokenManager.getAccessToken(this)))
        com.muyeon.app.auth.SessionResets.install(this, org.koin.java.KoinJavaComponent.get(com.muyeon.domain.common.KeyValueStore::class.java))
        MembershipBilling.start(this)
        Analytics.start(this)
        // AI 도우미 플로팅 — 모든 화면 위에 버튼을 붙인다(ui/assistant/AssistantOverlay).
        com.muyeon.app.ui.assistant.AssistantOverlay.start(this)
        // ★ 2026-11-07 학원 인증 강제 차단 — 앱이 앞으로 올 때 서버에 묻고, 차단 중이면 네이티브 화면을 닫고 웹 차단 화면으로.
        com.muyeon.app.webview.VerifyGate.start(this)
    }
}
