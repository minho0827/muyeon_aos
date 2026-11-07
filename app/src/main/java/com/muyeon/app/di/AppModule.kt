package com.muyeon.app.di

import android.content.Context
import com.muyeon.app.chat.socket.ForceLogout
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.ActiveRole
import com.muyeon.domain.common.ActiveTypeStore
import com.muyeon.domain.common.ForceLogoutTrigger
import com.muyeon.domain.common.TokenStore
import com.muyeon.app.ui.main.MainViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * :app 이 하위 모듈에 넘겨 주는 구현 — 기존 저장소(TokenManager·ActiveRole·ForceLogout)를 그대로 쓴다.
 *   기존 웹뷰·네이티브 화면과 **같은 토큰·같은 활성유형**을 보게 하려는 것(따로 저장하면 어긋난다).
 *   ViewModel 등록은 기능별 모듈(ui/<feature>/di)이 아니라 여기 [viewModelModule] 한 곳에 모은다.
 */
val appModule = module {
    single<TokenStore> {
        val ctx: Context = androidContext()
        object : TokenStore {
            override fun accessToken(): String? = TokenManager.getAccessToken(ctx)?.takeIf { it.isNotBlank() }
            override fun myUserId(): Long? = com.muyeon.app.ui.common.NativeHttp(ctx).myUserId()?.toLong()
        }
    }
    single<ActiveTypeStore> {
        val ctx: Context = androidContext()
        object : ActiveTypeStore {
            override fun current(): String? = runCatching { ActiveRole.current(ctx) }.getOrNull()
            override fun save(type: String) = ActiveRole.store(ctx, type)
        }
    }
    single<com.muyeon.domain.repository.SessionController> {
        val ctx: Context = androidContext()
        object : com.muyeon.domain.repository.SessionController {
            override fun logout() {
                TokenManager.clearTokens(ctx)
                ActiveRole.clear(ctx)
                com.muyeon.app.chat.socket.ChatSocketManager.disconnect()
                com.muyeon.app.webview.WebLogout.mark(ctx)
                com.muyeon.app.ui.floating.FloatingState.requestRefresh()
            }
        }
    }
    single<com.muyeon.domain.common.NetworkRetryPrompt> { com.muyeon.app.ui.main.NetworkRetryHost }
    single<ForceLogoutTrigger> {
        val ctx: Context = androidContext()
        object : ForceLogoutTrigger {
            override fun check(httpCode: Int, body: String?): Boolean = ForceLogout.checkResponse(ctx, httpCode, body)
        }
    }
}

/** 화면 ViewModel — 새 화면은 여기 한 줄(docs/ARCHITECTURE.md 9단계 체크리스트 7번). */
val viewModelModule = module {
    viewModel { MainViewModel(get(), get(), get(), get(), get(), get()) }
    // 공연
    viewModel { com.muyeon.app.ui.performance.PerformanceListViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { com.muyeon.app.ui.performance.PerformanceDetailViewModel(get(), get(), get(), get()) }
    // 홈
    // 채용 탭
    viewModel { com.muyeon.app.ui.recruit.HireViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { com.muyeon.app.ui.recruit.SeekViewModel(get(), get(), get(), get(), get()) }
    viewModel { com.muyeon.app.ui.recruit.SubsViewModel(get(), get()) }
    viewModel { com.muyeon.app.ui.subwizard.SubWizardViewModel(get(), get(), get()) }
    viewModel { com.muyeon.app.ui.home.HomeViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    // MY
    viewModel { com.muyeon.app.ui.my.MyViewModel(get(), get(), get(), get(), get(), get(), get()) }
    // 강사
    viewModel { com.muyeon.app.ui.lessons.LessonTabViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    // 커뮤니티
    viewModel { com.muyeon.app.ui.community.CommunityListViewModel(get(), get(), get()) }
    viewModel { com.muyeon.app.ui.community.CommunityPostViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}
