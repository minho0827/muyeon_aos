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
            else -> return false
        }
        return true
    }

    /** 상태 통지류 처리(현재는 기록만 — 플로팅 버튼/활성유형은 네이티브 이식 시 연결). */
    private fun onSilentAction(action: String, data: JSONObject) {
        android.util.Log.d(TAG, "silent action=$action data=$data")
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

            // 레슨
            "openLessonCalendar" -> "/lessonCalendar"
            "openLessonCreate" -> "/lessons/create"
            "openLessonSlotManage" -> "/myLessons"
            "openLessonBooking" -> s("lessonProductId").let { if (it.isEmpty()) "/lessons" else "/lessons/$it" }
            "openLessonReservationDetail" -> "/myReservations"

            // 프로필/이력서
            "openPublicProfile" -> s("userId").let { if (it.isEmpty()) "/mypage" else "/teachers/$it" }
            "openTeacherProfile" -> s("teacherId").let { if (it.isEmpty()) "/teachers" else "/teachers/$it" }
            "openAcademyProfile" -> s("academyId").let { if (it.isEmpty()) "/academyProfile" else "/academies/$it" }
            "openResumeList" -> "/profileResume"
            "openResumeEdit" -> "/resumeEdit"
            "openFieldVisibility" -> "/profileManage"
            "openApplicantResume" -> {
                val job = s("jobId"); val app = s("applicationId")
                if (job.isEmpty() || app.isEmpty()) "/receivedApplications" else "/jobs/$job/applicants/$app"
            }

            // 리뷰
            "openReviewWrite", "openReviewEdit" -> s("teacherId").let { if (it.isEmpty()) "/mypage" else "/teachers/$it" }
            "openReviewList" -> s("teacherId").let { if (it.isEmpty()) "/mypage" else "/teachers/$it" }

            // 스튜디오 운영 / 기타 MY
            "openStudioMembers", "openStudioSales", "openStudioSchedule", "openStudioOps" -> "/mypage"
            "openNotifications" -> "/notifications"
            "openMembership" -> s("featureType").let { if (it.isEmpty()) "/myMembership" else "/membership/$it" }
            "openMyJobPostings" -> "/myPostings"

            // 온보딩/계정 — 웹에도 화면 존재
            "openSignupTerms" -> "/signupTerms"
            "openRoleOnboarding", "openRoleManage", "openRoleVerification" -> "/mypage"
            "openAddressSetup" -> "/myRegions"

            // 채팅 — 웹 대응 화면 없음(네이티브 전용). 이식 전까지 안내.
            "openChatRoom", "openChatList" -> null
            // 이미지 뷰어 — 웹 대체 없음.
            "openImageViewer" -> null
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
