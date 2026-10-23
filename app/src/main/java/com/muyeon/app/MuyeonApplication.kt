package com.muyeon.app

import android.app.Application
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
        MembershipBilling.start(this)
        Analytics.start(this)
        // AI 도우미 플로팅 — 모든 화면 위에 버튼을 붙인다(ui/assistant/AssistantOverlay).
        com.muyeon.app.ui.assistant.AssistantOverlay.start(this)
    }
}
