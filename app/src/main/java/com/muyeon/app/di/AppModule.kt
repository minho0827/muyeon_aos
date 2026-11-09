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
                com.muyeon.app.webview.WebLogin.clear(ctx)
                com.muyeon.app.ui.floating.FloatingState.requestRefresh()
            }
        }
    }
    single { com.muyeon.app.auth.LoginRememberStore(androidContext()) }
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
    // ★ 2026-11-09 공연 검색 도메인(검색 화면이 koinInject 로 받는다).
    factory { com.muyeon.app.ui.search.PerformanceSearchDomain(get(), get()) }
    viewModel { MainViewModel(get(), get(), get(), get(), get(), get(), get()) }
    // 공연
    viewModel { com.muyeon.app.ui.performance.PerformanceListViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { com.muyeon.app.ui.performance.PerformanceDetailViewModel(get(), get(), get(), get()) }
    // 홈
    // 채용 탭
    viewModel { com.muyeon.app.ui.recruit.HireViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { com.muyeon.app.ui.recruit.SeekViewModel(get(), get(), get(), get(), get()) }
    viewModel { com.muyeon.app.ui.recruit.SubsViewModel(get(), get()) }
    viewModel { p -> com.muyeon.app.ui.subwizard.SubWizardViewModel(get(), get(), get(), p.getOrNull()) }
    viewModel { com.muyeon.app.ui.home.HomeViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    // ★ 2026-11-08 고객센터(공지·FAQ)
    viewModel { com.muyeon.app.ui.support.NoticeListViewModel(get()) }
    viewModel { p -> com.muyeon.app.ui.support.NoticeDetailViewModel(p.get(), get()) }
    viewModel { com.muyeon.app.ui.support.FaqViewModel(get(), get()) }
    viewModel { com.muyeon.app.ui.support.InquiryListViewModel(get()) }
    viewModel { p -> com.muyeon.app.ui.support.InquiryDetailViewModel(p.get(), get()) }
    viewModel { p -> com.muyeon.app.ui.support.SupportComposeViewModel(p.get(), get()) }
    // ★ 2026-11-08 찜/관심
    viewModel { com.muyeon.app.ui.likes.LikesViewModel(get(), get(), get()) }
    // ★ 2026-11-08 내가 예약한 내역
    viewModel { com.muyeon.app.ui.reservation.MyReservationsViewModel(get()) }
    // ★ 2026-11-08 지원 내역 — 받은 지원 인자: kind, postingId(둘 다 선택)
    viewModel { com.muyeon.app.ui.application.MyApplicationsViewModel(get()) }
    viewModel { p -> com.muyeon.app.ui.application.ReceivedApplicationsViewModel(p.getOrNull(), p.getOrNull(), get()) }
    // ★ 2026-11-08 설정 · 차단한 사용자
    viewModel { com.muyeon.app.ui.settings.SettingsViewModel(get(), get()) }
    viewModel { com.muyeon.app.ui.settings.BlockedUsersViewModel(get()) }
    // ★ 2026-11-08 무용수 프로필 열람 — 상세 인자: id
    viewModel { com.muyeon.app.ui.dancer.DancerListViewModel(get(), get(), get(), get(), get(), get(), get()) }
    viewModel { p -> com.muyeon.app.ui.dancer.DancerDetailViewModel(p.get(), get(), get(), get(), get(), get(), get()) }
    // ★ 2026-11-08 개인정보 수정
    viewModel { com.muyeon.app.ui.account.ProfileEditViewModel(get()) }
    // ★ 2026-11-08 경력·자격 확인
    viewModel { com.muyeon.app.ui.credential.CredentialsViewModel(get()) }
    // ★ 2026-11-08 비밀번호 변경 · 대타 신뢰도
    viewModel { com.muyeon.app.ui.account.ChangePasswordViewModel(get()) }
    viewModel { com.muyeon.app.ui.settings.SubTrustViewModel(get()) }
    viewModel { com.muyeon.app.ui.account.WithdrawViewModel(get(), get()) }
    viewModel { com.muyeon.app.ui.account.LessonGenresViewModel(get()) }
    // ★ 2026-11-08 상담 채팅 — 방 인자: roomId
    viewModel { com.muyeon.app.ui.support.SupportChatListViewModel(get()) }
    viewModel { p -> com.muyeon.app.ui.support.SupportChatRoomViewModel(p.get(), get()) }
    // ★ 2026-11-08 홈 '전체보기' 레슨 목록 — 인자: type(popular·experience·oneday)
    viewModel { p -> com.muyeon.app.ui.lessonlist.LessonListViewModel(p.getOrNull(), get(), get(), get(), get(), get(), get(), get()) }
    // ★ 2026-11-08 홈 [레슨] 레슨을 찾는 회원들 전체보기
    viewModel { com.muyeon.app.ui.quotefeed.QuoteFeedViewModel(get(), get()) }
    // ★ 2026-11-08 로그인 · 아이디 찾기/비밀번호 재설정 — 인자: type(id·pw)
    viewModel { com.muyeon.app.ui.auth.LoginViewModel(get(), get()) }
    viewModel { p -> com.muyeon.app.ui.auth.AccountRecoveryViewModel(p.get(), get(), get()) }
    // ★ [b1] 이메일 가입(인자: 약관 동의 체크 상태) · 소셜 추가정보(인자: 로그인 응답 이름)
    viewModel { p -> com.muyeon.app.ui.auth.signup.RegisterViewModel(p.get(), get(), get()) }
    viewModel { p -> com.muyeon.app.ui.auth.signup.SocialSignupViewModel(p.getOrNull(), get(), get()) }
    // MY
    viewModel { com.muyeon.app.ui.my.MyViewModel(get(), get(), get(), get(), get(), get(), get()) }
    // 강사
    viewModel { com.muyeon.app.ui.lessons.LessonTabViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    // 커뮤니티
    viewModel { com.muyeon.app.ui.community.CommunityListViewModel(get(), get(), get()) }
    viewModel { com.muyeon.app.ui.community.CommunityPostViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    // ★ 2026-11-08 (b4) 약관(인자: doc, version) · 관심 지역 · 지역 추가 · 관심 조건 알림
    viewModel { p -> com.muyeon.app.ui.policy.PolicyViewModel(p.get(), p.getOrNull(), get()) }
    viewModel { com.muyeon.app.ui.region.MyRegionsViewModel(get()) }
    viewModel { com.muyeon.app.ui.region.AddRegionViewModel(get(), get()) }
    viewModel { com.muyeon.app.ui.alerts.AlertSettingsViewModel(get()) }
    // ★ 2026-11-08 묶음 2 — 학원 정보 관리 · 캐스팅·공연 자료 업로드(인자: kind)
    viewModel { com.muyeon.app.ui.academyedit.AcademyEditViewModel(get()) }
    viewModel { p -> com.muyeon.app.ui.castingmaterial.CastingMaterialsViewModel(p.get(), get()) }
    // ★ 2026-11-08 묶음 3 — 대타공고 수정(같은 대타 입력 폼 · 인자: 공고 id) · 커뮤니티 글쓰기/수정(인자: 고칠 글 id 또는 null)
    viewModel(org.koin.core.qualifier.named(com.muyeon.app.ui.subwizard.SubWizardViewModel.EDIT)) { p ->
        com.muyeon.app.ui.subwizard.SubWizardViewModel(get(), get(), get(), null, get(), p.get<Long>())
    }
    viewModel { p -> com.muyeon.app.ui.communitywrite.CommunityWriteViewModel(p.getOrNull(), get(), get()) }
}
