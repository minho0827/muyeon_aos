package com.muyeon.app

import android.app.Application
import com.muyeon.app.ui.membership.MembershipBilling

/**
 * 앱 전역 시작점.
 *
 *  · 멤버십 구글 결제(MembershipBilling) — 시작 시 1회 연결하고, 앱이 앞으로 나올 때마다
 *    서버가 아직 모르는 구매를 다시 보낸다(iOS AppDelegate 의 MembershipStore.shared.start() 대응).
 */
class MuyeonApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MembershipBilling.start(this)
    }
}
