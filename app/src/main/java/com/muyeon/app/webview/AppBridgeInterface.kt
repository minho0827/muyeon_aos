package com.muyeon.app.webview

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

    private fun route(action: String, data: JSONObject) {
        // 1) 상태 통지류 — UI 없음. 토스트/이동 금지(무시가 정상 동작).
        if (action in SILENT_ACTIONS) {
            onSilentAction(action, data)
            return
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
     *  iOS 대응: presentQuoteWizard / presentMyQuotes / presentReceivedQuotes /
     *  presentQuoteBrowse / presentAutoQuoteTemplates / presentLessonQuoteHub.
     */
    private fun openNative(action: String, d: JSONObject): Boolean {
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
            "openAutoQuoteTemplates" -> com.muyeon.app.ui.quote.QuoteAutoTemplatesActivity.start(activity)
            "openLessonQuoteHub" -> com.muyeon.app.ui.quote.QuoteDashboardActivity.start(activity, d.optString("role"))
            // 채팅 — 웹 대체 화면이 없어 유일하게 토스트로 막혀 있던 동선.
            "openChatList" -> com.muyeon.app.ui.chat.ChatActivity.startList(activity, d.optString("filter"))
            // 채용 — E 이식 완료
            "openMyJobPostings" -> com.muyeon.app.ui.jobposting.JobPostingActivity.startList(activity)

            // 스튜디오 운영 — E 이식 완료
            "openStudioOps" -> com.muyeon.app.ui.studio.StudioActivity.start(activity, "hub")
            "openStudioMembers" -> com.muyeon.app.ui.studio.StudioActivity.start(activity, "members")
            "openStudioSales" -> com.muyeon.app.ui.studio.StudioActivity.start(activity, "sales")
            "openStudioSchedule" -> com.muyeon.app.ui.studio.StudioActivity.start(activity, "schedule")

            // 알림·회원유형 — E 이식 완료
            "openNotifications" -> com.muyeon.app.ui.notification.NotificationActivity.start(activity)
            // ★ 웹은 data.roles 에 **JSON 문자열**로 넣어 보낸다(iOS presentRoleManage 와 동일 계약).
            //   'payload' 키는 존재한 적이 없어 늘 data 전체로 폴백했고, 그 안에서 held/roles 배열을
            //   못 찾아 보유·인증상태가 통째로 비었다 → 사업 역할이 전부 자물쇠로 잠겨 보였다.
            "openRoleManage" -> com.muyeon.app.ui.onboarding.OnboardingActivity.startRoleManage(
                activity,
                d.optString("roles").ifEmpty { "{}" },
                d.optString("heroImage"),
                d.optString("activeType").ifEmpty { "GENERAL" },
            )
            "openRoleVerification" -> com.muyeon.app.ui.onboarding.OnboardingActivity.startVerification(
                activity, d.optString("role"),
            )

            // 온보딩·이용권·학원 프로필 — E 이식 완료
            "openSignupTerms" -> com.muyeon.app.ui.onboarding.OnboardingActivity.startTerms(activity)
            "openAddressSetup" -> com.muyeon.app.ui.onboarding.OnboardingActivity.startAddress(activity)
            "openNotificationConsent" ->
                com.muyeon.app.ui.onboarding.OnboardingActivity.startNotificationConsent(activity)
            // 멤버십은 계정에 하나 — featureType·memberType 은 받기만 하고 쓰지 않는다.
            "openMembership" -> com.muyeon.app.ui.membership.MembershipActivity.start(activity)
            "openAcademyProfile" -> {
                val aid = d.optString("academyId").toIntOrNull() ?: return true
                com.muyeon.app.ui.academy.AcademyProfileActivity.start(activity, aid)
            }
            // 학원↔강사 소속 — 네이티브 이식 완료(iOS AcademyTeachersView / AcademyInvitesView)
            "openAcademyTeachers" ->
                com.muyeon.app.ui.academy.AcademyMembershipActivity.startTeachers(activity)
            "openAcademyInvites" ->
                com.muyeon.app.ui.academy.AcademyMembershipActivity.startInvites(activity)

            // 레슨 — D 이식 완료
            "openLessonCalendar" -> com.muyeon.app.ui.lesson.LessonActivity.startCalendar(activity)
            "openLessonCreate" -> com.muyeon.app.ui.lesson.LessonActivity.startCreate(activity)
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
            )
            "openFieldVisibility" -> com.muyeon.app.ui.resume.ResumeActivity.startVisibility(activity, d.optString("mode"))
            "openPublicProfile", "openTeacherProfile" -> {
                val uid = (d.optString("userId").ifEmpty { d.optString("teacherId") }).toIntOrNull() ?: return true
                com.muyeon.app.ui.resume.ResumeActivity.startProfile(activity, uid, d.optString("src"))
            }
            "openReviewList" -> {
                val tid = d.optString("teacherId").toIntOrNull() ?: return true
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
                val posting = (if (kind == "SUB") d.optString("subId") else d.optString("jobId")).toIntOrNull()
                    ?: return true
                val app = d.optString("applicationId").toIntOrNull() ?: return true
                com.muyeon.app.ui.resume.ResumeActivity.startApplicant(activity, posting, app, kind)
            }
            "openChatRoom" -> {
                val roomId = d.optString("roomId").toIntOrNull() ?: return true
                com.muyeon.app.ui.chat.ChatActivity.startRoom(activity, roomId, d.optString("title"))
            }
            // 이미지 뷰어 — 웹은 네이티브 핸들러가 있으면 자기 라이트박스를 안 띄운다.
            //  여기서 안 받으면 이미지 탭이 무반응 버튼이 된다(iOS presentImageViewer 와 동일 키).
            "openImageViewer" -> com.muyeon.app.ui.common.ImageViewerActivity.start(
                activity,
                rawUrls = d.optString("urls"),
                index = d.optString("index").toIntOrNull() ?: 0,
                allowSave = d.optString("allowSave") == "true",
            )
            else -> return false
        }
        return true
    }

    /** 상태 통지류 처리(현재는 기록만 — 플로팅 버튼/활성유형은 네이티브 이식 시 연결). */
    private fun onSilentAction(action: String, data: JSONObject) {
        android.util.Log.d(TAG, "silent action=$action data=$data")
        // 활성 회원유형은 로그만 찍고 버리면 안 된다 — 네이티브 화면이 "지금 학원인가 강사인가"를
        //  판단할 유일한 근거다(iOS RoleGate.store 대응). UI 없음은 그대로 유지.
        if (action == "syncActiveType") ActiveRole.store(activity, data.optString("type"))
    }

    /** SPA 이동 — 웹이 심어둔 window.__nativeGo(path) 사용(전체 리로드 없이 라우팅). */
    private fun navigateWeb(path: String) {
        val safe = path.replace("'", "\\'")
        webView.evaluateJavascript(
            "(function(){ if(window.__nativeGo){ window.__nativeGo('$safe'); return 1; } return 0; })()",
            null,
        )
    }

    /** 액션 → 동등 웹 경로. null 이면 웹 대응 화면이 없는 네이티브 전용 기능. */
    private fun webPathFor(action: String, d: JSONObject): String? {
        fun s(key: String): String = d.optString(key, "")
        return when (action) {
            // 견적 — openQuoteWizard/openMyQuotes/openReceivedQuotes/openQuoteBrowse/
            //  openAutoQuoteTemplates/openLessonQuoteHub 는 openNative() 에서 처리(이식 완료).

            // 레슨 — openNative() 에서 처리(이식 완료). 예약 내역만 웹 유지(목록 화면 미이식).
            "openLessonReservationDetail" -> "/myReservations"

            // 프로필/이력서/리뷰 · 이용권 · 학원 프로필 — openNative() 에서 처리(이식 완료).
            //  단 academyId 가 없으면 학원 목록 웹으로.
            "openAcademyProfile" -> "/academyProfile"

            // 온보딩/계정 — openSignupTerms/openAddressSetup 은 네이티브. 유형 온보딩만 웹 유지.
            "openRoleOnboarding" -> "/mypage"

            // 멤버십 성과 — 네이티브 화면 미이식(iOS 는 MembershipPerformanceView).
            //  폴백이 없으면 안드로이드에서 '성과 보기' 가 무반응이라 죽은 버튼이 된다.
            "openMembershipPerformance" -> "/myMembership/performance"

            // 공간 상세 — iOS 는 네이티브(SpaceDetailView). 지금 공간 기능은 플래그로 닫혀 있고
            //  웹에서도 NATIVE_PLATFORMS 로 iOS 전용이라 여기 도달하지 않지만,
            //  구버전 번들이 캐시된 기기를 위해 웹 경로를 열어 둔다.
            "openSpaceDetail" -> s("spaceId").takeIf { it.isNotEmpty() }?.let { "/spaces/$it" }

            // 채팅 · 이미지 뷰어 — openNative() 에서 처리(이식 완료).
            // 애플 로그인 — iOS 전용.
            "getAppleLogin" -> null

            else -> null
        }
    }

    companion object {
        private const val TAG = "AppBridge"

        /** UI 를 열지 않는 상태 통지 액션 — 토스트/이동 금지. */
        private val SILENT_ACTIONS = setOf(
            "routeChanged",       // 웹 라우트 변경 통지(플로팅 버튼 판정용)
            "setFloatingHidden",  // 웹 모달 표시/해제 → 플로팅 숨김
            "syncActiveType",     // 활성 회원유형 동기화(X-Active-Type)
            "closeModal",         // 네이티브 모달 닫기 요청
        )
    }
}
