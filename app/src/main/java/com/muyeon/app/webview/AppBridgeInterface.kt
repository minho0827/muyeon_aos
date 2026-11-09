package com.muyeon.app.webview

import androidx.lifecycle.lifecycleScope
import android.app.Activity
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import org.json.JSONObject

/**
 * 웹 → 네이티브 단방향 액션 채널 (`window.AppBridge`).
 *
 * 계약(iOS와 100% 동일): 웹이 `{"action":"openQuoteWizard","data":{...}}` 문자열을 postMessage 한다.
 *  - iOS 는 `webkit.messageHandlers.callbackHandler` 로 같은 payload 를 받는다(muyeon-front nativeBridge.js).
 *  - 액션명·데이터 키를 절대 바꾸지 말 것(웹 분기 없이 양 플랫폼 동작).
 *
 * 네이티브 화면이 아직 없는 액션은 **웹 경로로 폴백**해 죽은 버튼을 만들지 않는다.
 * (웹 광고 퍼널에서 '반응 없는 버튼' 실사고 이력 — 무반응 금지)
 */
class AppBridgeInterface(
    private val activity: Activity,
    private val webView: WebView,
    /** 웹이 결과 콜백(__muyeonResult 등)을 다 심었다는 신호 — WebViewActivity 가 밀린 JS 를 흘린다. */
    private val onWebReady: () -> Unit = {},
    /** ★ 2026-11-07 웹 라우트 변경 — 웹 창이 앱 탭 주소로 가면 앱 탭으로 돌아간다(WebViewActivity.onWebRoute). */
    private val onRouteChanged: (String) -> Unit = {},
) {

    @JavascriptInterface
    fun postMessage(raw: String?) {
        val msg = raw ?: return
        val action: String
        val data: JSONObject
        try {
            val obj = JSONObject(msg)
            action = obj.optString("action")
            data = obj.optJSONObject("data") ?: JSONObject()
        } catch (e: Exception) {
            return
        }
        if (action.isEmpty()) return
        activity.runOnUiThread { route(action, data) }
    }

    // MARK: - 라우팅

    /** ★ 2026-11-03 로그인 후 이어서 열기(WebViewActivity.onLoggedIn) — 웹 브리지와 같은 액션 처리. */
    fun openAction(action: String, data: JSONObject) = route(action, data)

    private fun route(action: String, data: JSONObject) {
        // 1) 상태 통지류 — UI 없음. 토스트/이동 금지(무시가 정상 동작).
        if (action in SILENT_ACTIONS) {
            onSilentAction(action, data)
            return
        }
        // ★ 2026-11-07 학원 인증 강제 차단 중 — 학원 인증·약관 화면 말고는 네이티브 화면을 열지 않는다(웹이 차단 화면으로).
        if (VerifyGate.blocksAction(action)) {
            VerifyGate.recheckWeb(webView)
            return
        }
        // 하단 MY 탭 길게 누르기 → 내 유형 바텀시트(결과는 이 웹뷰의 __onActiveTypePicked).
        when (action) {
            "openActiveTypeSheet" -> {
                ActiveTypeSheet.show(activity, webView, data.optString("types"), data.optString("current"), data.optString("newTypes"))
                return
            }
        }
        // 2) 네이티브 이식 완료 화면 — 직접 띄운다.
        if (openNative(action, data)) return
        // 3) 웹 경로 폴백 — 네이티브 화면 이식 전까지 동등 웹 화면으로 이동.
        val path = webPathFor(action, data)
        if (path != null) {
            navigateWeb(path)
            return
        }
        // 4) 매핑 없음(네이티브 전용 기능) — 무반응 대신 안내.
        Toast.makeText(activity, "곧 제공될 기능이에요.", Toast.LENGTH_SHORT).show()
    }

    /**
     * 네이티브 이식 완료 화면 라우팅 — 처리했으면 true.
     *  실제 표는 [NativeActionRouter] 에 있다(멤버십 혜택 카드 등 네이티브 화면도 같은 표로 연다).
     */
    private fun openNative(action: String, d: JSONObject): Boolean =
        NativeActionRouter.open(activity, action, d).also { opened ->
            // 웹 버튼으로 앱 화면을 연 것 — 이용 분석의 '앱 화면 열기' 클릭(웹이 보낸 click 과 겹치지 않는다).
            if (opened) com.muyeon.app.analytics.Analytics.click("native:$action")
        }

    /** 상태 통지류 처리 — UI 를 열지 않고 네이티브 상태만 갱신한다. */
    private fun onSilentAction(action: String, data: JSONObject) {
        android.util.Log.d(TAG, "silent action=$action data=$data")
        when (action) {
            // 활성 회원유형은 로그만 찍고 버리면 안 된다 — 네이티브 화면이 "지금 학원인가 강사인가"를
            //  판단할 유일한 근거다(iOS RoleGate.store 대응).
            "syncActiveType" -> {
                ActiveRole.store(activity, data.optString("type"))
                // changed=1 = 이용자가 직접 바꿈 → 전환 안내(iOS RoleGate.showSwitchToast 와 같은 문구). 처음 맞추기는 조용히.
                if (data.optString("changed") == "1") {
                    android.widget.Toast.makeText(activity, "${ActiveRole.label(data.optString("type"))} 유형으로 전환됐어요", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            // 플로팅(채팅 버튼·인증 책갈피) 표시 판정 — iOS WebViewModel.currentRoute / floatingHidden.
            "routeChanged" -> {
                com.muyeon.app.ui.floating.FloatingState.updateRoute(data.optString("path"))
                onRouteChanged(data.optString("path"))
            }
            "setFloatingHidden" ->
                com.muyeon.app.ui.floating.FloatingState.updateFloatingHidden(data.optString("hidden") == "1")
            // 웹이 window.__muyeonResult 를 등록함(페이지 로드당 1회). 예전 300ms 재시도 루프를 대신한다.
            //  ★ 이 수신부가 없으면 백엔드 배포 스크립트(check-bridge-contract.js)가 죽은 액션으로 막는다.
            "webReady" -> onWebReady()
            // 웹이 인증 서류 제출·역할 추가·해제를 끝냄 → 플로팅(인증 책갈피 '심사중') 1회 재조회.
            //  onResume 재조회는 웹이 서류를 보내기 전에 돌아서 '심사중'을 놓칠 수 있다.
            "rolesChanged" -> com.muyeon.app.ui.floating.FloatingState.requestRefresh()
        }
    }

    /** SPA 이동 — 웹이 심어둔 window.__nativeGo(path) 사용(전체 리로드 없이 라우팅). */
    private fun navigateWeb(path: String) {
        val safe = path.replace("'", "\\'")
        webView.evaluateJavascript(
            "(function(){ if(window.__nativeGo){ window.__nativeGo('$safe'); return 1; } return 0; })()",
            null,
        )
    }

    /**
     * 액션 → 동등 웹 경로. null 이면 웹 대응 화면이 없는 네이티브 전용 기능.
     *
     * ★ 2026-09-06 기준 웹 폴백이 남은 건 아래 하나뿐이다 — 나머지 액션은 전부
     *   openNative() 에서 네이티브 화면으로 처리된다(견적·레슨·이력서·리뷰·채팅·이용권·
     *   학원 소속·멤버십 성과·예약 상세·공간 상세·유형 온보딩·이미지 뷰어).
     */
    private fun webPathFor(action: String, @Suppress("UNUSED_PARAMETER") d: JSONObject): String? =
        when (action) {
            // 학원 프로필 자체는 네이티브지만, academyId 가 없으면 열 대상이 없어 학원 목록 웹으로 보낸다.
            "openAcademyProfile" -> "/academyProfile"
            // 애플 로그인 — iOS 전용(웹 대응 화면 없음).
            "getAppleLogin" -> null
            else -> null
        }

    companion object {
        private const val TAG = "AppBridge"

        /** UI 를 열지 않는 상태 통지 액션 — 토스트/이동 금지. */
        private val SILENT_ACTIONS = setOf(
            "routeChanged",       // 웹 라우트 변경 통지(플로팅 버튼 판정용)
            "setFloatingHidden",  // 웹 모달 표시/해제 → 플로팅 숨김
            "syncActiveType",     // 활성 회원유형 동기화(X-Active-Type)
            "closeModal",         // 네이티브 모달 닫기 요청
            "webReady",           // 웹 결과 콜백 등록 완료(밀린 JS 흘리기)
            "rolesChanged",       // 역할 제출·추가·해제 완료 → 플로팅 재조회
        )
    }
}

/**
 * 액션명(웹 브릿지 계약, iOS 와 동일) → 네이티브 화면 표.
 *
 * AppBridge(웹 → 네이티브)와 네이티브 화면(멤버십 '내 혜택 사용 현황' 카드의 행 이동 등)이
 * **같은 표**를 쓴다 — 액션별로 여는 화면이 플랫폼 안에서 갈라지지 않게.
 *
 * ★ 이 파일 밖으로 옮기지 말 것 — 백엔드 배포 스크립트(check-bridge-contract.js)가
 *   AppBridgeInterface.kt 만 읽어 AOS 가 받는 액션을 센다.
 */
object NativeActionRouter {

    /**
     * 네이티브 이식 완료 화면 라우팅 — 처리했으면 true.
     *  iOS 대응: presentQuoteWizard / presentMyQuotes / presentReceivedQuotes /
     *  presentQuoteBrowse / presentAutoQuoteTemplates / presentLessonQuoteHub.
     */
    fun open(activity: Activity, action: String, d: JSONObject): Boolean {
        // AI 빠른 메뉴의 '현재 화면' — 곧 뜰 화면에 이 동작 이름을 붙인다(AssistantOverlay NativeScreens).
        if (action.startsWith("open")) com.muyeon.app.ui.assistant.NativeScreens.opening(action)
        // ★ 2026-11-08 묶음 0 — 경로표가 쓰는 앱 안 전용 액션(NativeActionRouterB0).
        if (NativeActionRouterB0.open(activity, action, d)) return true
        when (action) {
            "openQuoteWizard" -> com.muyeon.app.ui.quote.QuoteWizardActivity.start(
                activity,
                d.optString("category").ifEmpty { d.optString("categoryId") },
                d.optString("targetTeacherId"),
            )
            // 받은/보낸 견적. iOS presentMyQuotes 분기 그대로:
            //  tab=sent → 강사이므로 isPro 강제 / 일반회원 기본 진입(탭 미지정)은 대시보드,
            //  딥링크(tab 지정)·강사 경로만 허브 직행.
            "openMyQuotes" -> {
                val tab = d.optString("tab")
                val initialTab = if (tab == "sent") 1 else 0
                val isPro = d.optString("isPro") == "1" || initialTab == 1
                if (!isPro && tab.isEmpty()) {
                    com.muyeon.app.ui.quote.QuoteDashboardActivity.start(activity, null)
                } else {
                    com.muyeon.app.ui.quote.QuoteHubActivity.start(activity, isPro, initialTab)
                }
            }
            // 특정 요청 상세 — quoteId 없으면 무시(iOS presentReceivedQuotes 의 guard 와 동일).
            //  강조할 응답 id 키는 'r'(웹 딥링크 ?r=).
            "openReceivedQuotes" -> {
                val quoteId = d.optString("quoteId").toIntOrNull() ?: return true
                if (quoteId <= 0) return true
                com.muyeon.app.ui.quote.QuoteHubActivity.start(
                    activity, isPro = false, initialTab = 0,
                    quoteId = quoteId, responseId = d.optString("r").toIntOrNull(),
                )
            }
            "openQuoteBrowse" -> com.muyeon.app.ui.quote.QuoteBrowseActivity.start(activity)
            // 묶음 5 — 요청 하나(웹 /availableQuotes/:id) 상세 · 견적 보내기. 셸 전용(웹 브릿지 계약 아님 — NativeRoutes 가 보낸다).
            "openQuoteRespond" -> {
                val quoteId = d.optString("quoteId").toIntOrNull()?.takeIf { it > 0 } ?: return true
                com.muyeon.app.ui.quote.QuoteBrowseActivity.startRequest(activity, quoteId, compose = d.optString("compose") == "1")
            }
            // ★ 2026-11-08 앱 안 전용 — 추천 제외한 강사(웹 /recommendBlocks, MY 메뉴).
            "openRecommendBlocks" -> com.muyeon.app.ui.quote.QuoteHubActivity.startRecommendBlocks(activity)
            // 10-26 웹 하단 탭 '캐스팅·무대' → 네이티브 섹션 화면(웹은 nativeCastingStage 표시가 있을 때만 보낸다)
            //  10-27 링크(/casting/12 등)는 payload(castingId·screen·issueNo·category)로 그 화면부터.
            // 11-08 앱 셸 안 화면(캐스팅 탭 · 탭 위 상세)으로 연다 — 웹 창은 닫히고 셸로 돌아간다.
            "openCastingStage" -> com.muyeon.app.ui.main.MainActivity.openCastingStage(
                activity,
                com.muyeon.app.ui.castingstage.CastingStageActivity.linkRoute(
                    d.optString("castingId").ifEmpty { null }, d.optString("screen").ifEmpty { null },
                    d.optString("issueNo").ifEmpty { null }, d.optString("category").ifEmpty { null },
                ),
            )
            "openAutoQuoteTemplates" -> com.muyeon.app.ui.quote.QuoteAutoTemplatesActivity.start(activity)
            // 자동견적 발송 내역(강사·학원 전용 — Activity 가 무용수 차단). data.month = "YYYY-MM"(선택).
            "openAutoQuoteHistory" -> com.muyeon.app.ui.quote.AutoQuoteHistoryActivity.start(
                activity, d.optString("month").ifEmpty { null },
            )
            // 주소(/lessonHub)로 오면 role 이 없다 — 지금 유형으로(MY 메뉴는 activeType 을 실어 보낸다).
            "openLessonQuoteHub" -> com.muyeon.app.ui.quote.QuoteDashboardActivity.start(
                activity, d.optString("role").ifEmpty { ActiveRole.current(activity) },
            )
            // 채팅 — 웹 대체 화면이 없어 유일하게 토스트로 막혀 있던 동선.
            "openChatList" -> com.muyeon.app.ui.chat.ChatActivity.startList(activity, d.optString("filter"))
            // 채용 — E 이식 완료
            "openMyJobPostings" -> com.muyeon.app.ui.jobposting.JobPostingActivity.startList(activity)
            // ★ 2026-11-08 앱 안 전용(웹 계약 아님) — 채용 [+ 글쓰기] · 웹 /jobs/create 링크를 앱 채용공고 작성으로.
            // ★ 2026-11-08 앱 안 전용 — 웹 /myLessons(내 레슨 관리) · /lessons/:id(레슨 상세)를 앱 화면으로.
            // 웹 /teachers(강사 찾기) — 홈 [강사] 그리드가 같은 목록이다.
            "openHomeTeachers" -> {
                com.muyeon.app.ui.home.HomeModeRequests.request(com.muyeon.app.ui.home.HomeMode.TEACHERS)
                com.muyeon.app.ui.main.MainActivity.openTab(activity, com.muyeon.app.ui.main.MainTab.HOME)
            }
            "openLessonManage" -> com.muyeon.app.ui.lesson.LessonActivity.startManage(activity)
            "openLessonDetail" -> {
                val lid = d.optString("lessonId").toIntOrNull() ?: return true
                com.muyeon.app.ui.lesson.LessonBrowseActivity.startDetail(activity, lid)
            }
            "openJobPostingForm" -> com.muyeon.app.ui.jobposting.JobPostingActivity.startForm(activity, null)
            // ★ 2026-11-08 앱 안 전용 — 구직 프로필 등록(웹 /profileManage?seek=1) — 지금 유형이 무용수면 무용수 이력서.
            //  2026-11-08 사용자 결정 — 이미 등록한 강사프로필/무용수 이력서가 있으면 이력서 관리, 없으면 등록(SeekProfileEntry).
            "openSeekProfile" -> {
                val scope = (activity as? androidx.activity.ComponentActivity)?.lifecycleScope
                if (scope != null) com.muyeon.app.ui.resume.SeekProfileEntry.open(activity, scope, ActiveRole.isDancer(activity))
                else com.muyeon.app.ui.resume.ResumeActivity.startEdit(
                    activity, null, if (ActiveRole.isDancer(activity)) "dancer" else "", seekProfile = true,
                )
            }

            // 스튜디오 운영 — E 이식 완료
            "openStudioOps" -> com.muyeon.app.ui.studio.StudioActivity.start(activity, "hub")
            "openStudioMembers" -> com.muyeon.app.ui.studio.StudioActivity.start(activity, "members")
            "openStudioSales" -> com.muyeon.app.ui.studio.StudioActivity.start(activity, "sales")
            "openStudioSchedule" -> com.muyeon.app.ui.studio.StudioActivity.start(activity, "schedule")

            // 알림·회원유형 — E 이식 완료
            "openNotifications" -> com.muyeon.app.ui.notification.NotificationActivity.start(activity)
            // 알림 수신 설정 — 앱에서는 항상 네이티브로 연다. 웹 화면(/notificationSettings)이
            //  앱에서 열리면 알림함 톱니가 여는 화면과 한 앱 안에 두 모습이 된다.
            "openNotificationSettings" ->
                com.muyeon.app.ui.notification.NotificationSettingsActivity.start(activity)
            // ★ 웹은 data.roles 에 **JSON 문자열**로 넣어 보낸다(iOS presentRoleManage 와 동일 계약).
            //   'payload' 키는 존재한 적이 없어 늘 data 전체로 폴백했고, 그 안에서 held/roles 배열을
            //   못 찾아 보유·인증상태가 통째로 비었다 → 사업 역할이 전부 자물쇠로 잠겨 보였다.
            "openRoleManage" -> com.muyeon.app.ui.onboarding.OnboardingActivity.startRoleManage(
                activity,
                d.optString("roles").ifEmpty { "{}" },
                d.optString("heroImage"),
                d.optString("activeType").ifEmpty { "GENERAL" },
                completeInApp = activity !is WebViewActivity,
            )
            // 회원유형 선택 온보딩 — 네이티브 이식 완료(iOS RoleOnboardingView).
            "openRoleOnboarding" ->
                com.muyeon.app.ui.onboarding.OnboardingActivity.startRoleOnboarding(activity)
            // ownerName = 본인확인으로 등록된 실명. 학원 인증에서 '대표원장'으로 읽기전용 표시한다.
            //  구웹은 안 보내므로 빈 문자열이 되고, 그때는 화면이 해당 줄을 그리지 않는다.
            //  ★ 2026-11-09 앱 화면(MY 공고 등록·견적 보내기)에서 열면 앱이 직접 제출하고 그 화면으로 돌아간다 —
            //   웹 콜백 모드는 웹 창이 열 때만(아니면 빈 웹 창이 새로 뜬다).
            "openRoleVerification" -> com.muyeon.app.ui.onboarding.OnboardingActivity.startVerification(
                activity, d.optString("role"), d.optString("ownerName"),
                completeInApp = activity !is WebViewActivity,
            )

            // 온보딩·이용권·학원 프로필 — E 이식 완료
            "openSignupTerms" -> com.muyeon.app.ui.onboarding.OnboardingActivity.startTerms(activity)
            "openAddressSetup" -> com.muyeon.app.ui.onboarding.OnboardingActivity.startAddress(activity)
            // 아직 묻지 않았을 때만(PushPrimer) — 이미 물었으면 조용히 넘어간다(iOS 와 같음).
            "openNotificationConsent" -> if (com.muyeon.app.utils.PushPrimer.shouldAsk(activity)) {
                com.muyeon.app.utils.PushPrimer.markAsked(activity)
                com.muyeon.app.ui.onboarding.OnboardingActivity.startNotificationConsent(activity)
            }
            // 멤버십은 계정에 하나 — featureType·memberType 은 받기만 하고 쓰지 않는다.
            "openMembership" -> com.muyeon.app.ui.membership.MembershipActivity.start(activity)
            // 멤버십 성과 — 네이티브 이식 완료(iOS MembershipPerformanceView).
            //  memberType 이 비면 저장된 활동유형을 쓴다(iOS RoleGate.activeType 폴백과 동일).
            "openMembershipPerformance" ->
                com.muyeon.app.ui.membership.MembershipPerformanceActivity.start(
                    activity, d.optString("memberType"),
                )
            // 공간 상세 — 네이티브 이식 완료(iOS SpaceDetailView).
            "openSpaceDetail" -> {
                val sid = d.optString("spaceId").toIntOrNull() ?: return true
                com.muyeon.app.ui.space.SpaceActivity.start(
                    activity, sid, d.optString("date"), d.optString("fromReservation") == "1",
                    then = d.optString("then").takeIf { it.isNotEmpty() },
                )
            }
            // ★ 2026-11-03 공간 대관 — 목록·등록/수정·내 공간·호스트 예약 관리·내 예약(상세는 위 openSpaceDetail).
            //  웹은 nativeSpaceBooking 표시가 있을 때만 보낸다.
            // ★ 2026-11-08 공간 대관·양도양수 목록은 앱 탭(공간 탭 안 위쪽 탭) — 따로 뜨는 목록 화면(탭바 없음)으로 가던 것(전체 메뉴 점검).
            "openSpaces" -> com.muyeon.app.ui.main.MainActivity.openTab(activity, com.muyeon.app.ui.main.MainTab.SPACES)
            "openSpaceEditor" ->
                com.muyeon.app.ui.space.SpaceHubActivity.startEditor(activity, d.optString("id").toIntOrNull()?.takeIf { it > 0 })
            "openMySpaces" -> com.muyeon.app.ui.space.SpaceHubActivity.startMine(activity)
            "openMySpaceReservations" -> com.muyeon.app.ui.space.SpaceHubActivity.startHosting(activity)
            "openMySpaceBookings" -> com.muyeon.app.ui.space.SpaceHubActivity.startBookings(activity)
            // ★ 2026-11-03 무용센터 양도양수 — 네이티브(목록·상세·등록/수정·내 글). 웹은 nativeStudioTransfers 표시가 있을 때만 보낸다.
            "openStudioTransfers" -> com.muyeon.app.ui.main.MainActivity.openTab(activity, com.muyeon.app.ui.main.MainTab.TRANSFERS)
            "openStudioTransferDetail" -> {
                val tid = d.optString("id").toIntOrNull() ?: return true
                com.muyeon.app.ui.transfer.TransferActivity.startDetail(activity, tid, then = d.optString("then").takeIf { it.isNotEmpty() })
            }
            "openStudioTransferEditor" ->
                com.muyeon.app.ui.transfer.TransferActivity.startEditor(activity, d.optString("id").toIntOrNull()?.takeIf { it > 0 })
            "openMyStudioTransfers" -> com.muyeon.app.ui.transfer.TransferActivity.startMine(activity)
            "openAcademyProfile" -> {
                // id 없음(AI 도우미 '학원 정보 관리' 메뉴) = 내 학원 정보 관리 화면(무반응이던 것).
                val aid = d.optString("academyId").toIntOrNull()
                    ?: return true.also { com.muyeon.app.ui.main.MainActivity.openPath(activity, "/academyProfile") }
                com.muyeon.app.ui.academy.AcademyProfileActivity.start(activity, aid)
            }
            // 학원↔강사 소속 — 네이티브 이식 완료(iOS AcademyTeachersView / AcademyInvitesView)
            "openAcademyTeachers" ->
                com.muyeon.app.ui.academy.AcademyMembershipActivity.startTeachers(activity)
            "openAcademyInvites" ->
                com.muyeon.app.ui.academy.AcademyMembershipActivity.startInvites(activity)

            // 레슨 — D 이식 완료
            "openLessonCalendar" -> com.muyeon.app.ui.lesson.LessonActivity.startCalendar(activity)
            // 예약 상세 — 네이티브 이식 완료(iOS LessonReservationDetailSheet).
            "openLessonReservationDetail" -> {
                val rid = d.optString("reservationId").toIntOrNull() ?: return true
                com.muyeon.app.ui.lesson.LessonActivity.startReservationDetail(activity, rid)
            }
            // 웹 LessonManage 의 [수정]은 data.lessonId 를 보낸다 — 있으면 수정 위저드(iOS presentLessonCreate 와 동일).
            //  예전엔 lessonId 를 버리고 항상 개설 위저드를 열어, 수정을 눌러도 빈 화면이 떴다.
            "openLessonCreate" -> {
                val lid = d.optString("lessonId").toIntOrNull()
                if (lid != null && lid > 0) com.muyeon.app.ui.lesson.LessonActivity.startEdit(activity, lid)
                else com.muyeon.app.ui.lesson.LessonActivity.startCreate(activity, d.optString("aiDraftId").ifEmpty { null })
            }
            "openLessonSlotManage" -> com.muyeon.app.ui.lesson.LessonActivity.startSlots(
                activity, d.optString("lessonProductId").toIntOrNull(),
            )
            "openLessonBooking" -> {
                val pid = d.optString("lessonProductId").toIntOrNull()
                if (pid == null) com.muyeon.app.ui.lesson.LessonActivity.startManage(activity)
                else com.muyeon.app.ui.lesson.LessonActivity.startBooking(activity, pid)
            }

            // 이력서/공개프로필/리뷰 — C 이식 완료
            "openResumeList" -> com.muyeon.app.ui.resume.ResumeActivity.startList(activity, d.optString("mode"))
            "openResumeEdit" -> com.muyeon.app.ui.resume.ResumeActivity.startEdit(
                activity, d.optString("resumeId").toIntOrNull(), d.optString("mode"),
                seekProfile = d.optString("seekProfile") == "1",
                aiDraftId = d.optString("aiDraftId").ifEmpty { null },
            )
            "openFieldVisibility" -> com.muyeon.app.ui.resume.ResumeActivity.startVisibility(activity, d.optString("mode"))
            "openPublicProfile", "openTeacherProfile" -> {
                // 인자 없이 오면(전체 메뉴 '공개 프로필') 내 프로필 — 예전엔 아무 일도 없었다(2026-11-08 점검).
                val uid = (d.optString("userId").ifEmpty { d.optString("teacherId") }).toIntOrNull()
                    ?: com.muyeon.app.ui.common.NativeHttp(activity).myUserId() ?: return true
                com.muyeon.app.ui.resume.ResumeActivity.startProfile(
                    activity, uid, d.optString("src"), recruit = d.optString("mode") == "recruit",
                )
            }
            "openReviewList" -> {
                // 전체 메뉴 '받은 리뷰' 는 인자가 없다 — 내 리뷰(예전엔 무반응).
                val tid = d.optString("teacherId").toIntOrNull() ?: com.muyeon.app.ui.common.NativeHttp(activity).myUserId() ?: return true
                com.muyeon.app.ui.resume.ResumeActivity.startReviewList(activity, tid)
            }
            "openReviewWrite" -> {
                val tid = d.optString("teacherId").toIntOrNull() ?: return true
                com.muyeon.app.ui.resume.ResumeActivity.startReviewWrite(
                    activity, tid, d.optString("teacherName"), d.optString("lessonType"),
                )
            }
            // 리뷰 '수정' 은 작성과 다른 화면이다 — payload 에 teacherId 가 없어서
            //  종전처럼 openReviewWrite 로 넘기면 teacherId 파싱에 걸려 **아무것도 안 열렸다**.
            //  저장은 웹이 __onReviewEdited 로 수행한다(iOS presentReviewEdit 와 동일).
            "openReviewEdit" -> com.muyeon.app.ui.resume.ResumeActivity.startReviewEdit(
                activity,
                d.optString("rating").toIntOrNull() ?: 0,
                d.optString("content"),
            )
            "openApplicantResume" -> {
                // 대타(SUB)는 subId 로 온다 — jobId 만 읽으면 대타 지원자 화면이 안 열린다(iOS 와 동일 키).
                val kind = d.optString("applicationKind").ifEmpty { "JOB" }
                val posting = (when (kind) {
                    "SUB" -> d.optString("subId")
                    "CASTING" -> d.optString("callId").ifEmpty { d.optString("jobId") }
                    else -> d.optString("jobId")
                }).toIntOrNull()
                    ?: return true
                val app = d.optString("applicationId").toIntOrNull() ?: return true
                com.muyeon.app.ui.resume.ResumeActivity.startApplicant(activity, posting, app, kind)
            }
            "openChatRoom" -> {
                val roomId = d.optString("roomId").toIntOrNull() ?: return true
                // proposalId 가 오면 방에 들어간 뒤 그 약속 제안 카드로 스크롤(iOS presentChatRoom 과 같은 키).
                com.muyeon.app.ui.chat.ChatActivity.startRoom(
                    activity, roomId, d.optString("title"),
                    proposalId = d.optString("proposalId").toIntOrNull(),
                )
            }
            // 이미지 뷰어 — 웹은 네이티브 핸들러가 있으면 자기 라이트박스를 안 띄운다.
            //  여기서 안 받으면 이미지 탭이 무반응 버튼이 된다(iOS presentImageViewer 와 동일 키).
            "openImageViewer" -> com.muyeon.app.ui.common.ImageViewerActivity.start(
                activity,
                rawUrls = d.optString("urls"),
                index = d.optString("index").toIntOrNull() ?: 0,
                allowSave = d.optString("allowSave") == "true",
            )
            // ★ 2026-11-08 [b1] 가입·학원 인증 차단·점검 앱 화면(NativeRoutes 셸 전용 동작).
            "openSignup" -> com.muyeon.app.ui.auth.LoginActivity.startSignup(activity, d.optString("path").ifEmpty { "/signupTerms" })
            "openVerifyRequired" -> com.muyeon.app.ui.gate.GateActivity.startVerifyRequired(activity)
            "openMaintenance" -> com.muyeon.app.ui.gate.GateActivity.startMaintenance(activity, d.optString("error").ifEmpty { null })
            else -> return false
        }
        return true
    }

    /**
     * 웹뷰 밖(네이티브 화면)에서 액션을 연다. 표에 없으면 AppBridge 의 마지막 단계와 같은 안내 토스트
     *  — 무반응 금지(죽은 버튼 방지). 웹 경로 폴백은 웹뷰가 있어야 해서 여기선 쓰지 않는다.
     */
    fun openOrNotice(activity: Activity, action: String, d: JSONObject = JSONObject()) {
        if (!open(activity, action, d)) {
            Toast.makeText(activity, "곧 제공될 기능이에요.", Toast.LENGTH_SHORT).show()
        }
    }
}
