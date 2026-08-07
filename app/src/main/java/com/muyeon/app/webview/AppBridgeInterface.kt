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
        // 2) 웹 경로 폴백 — 네이티브 화면 이식 전까지 동등 웹 화면으로 이동.
        val path = webPathFor(action, data)
        if (path != null) {
            navigateWeb(path)
            return
        }
        // 3) 매핑 없음(네이티브 전용 기능) — 무반응 대신 안내.
        Toast.makeText(activity, "곧 제공될 기능이에요.", Toast.LENGTH_SHORT).show()
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
            // 견적
            // 견적 문진은 웹에 화면이 없다(네이티브 전용 — 웹은 앱설치 게이트로 유도).
            //  잘못된 곳으로 보내지 말고 안내만: Phase 2 에서 네이티브 위저드 이식.
            "openQuoteWizard" -> null
            "openMyQuotes" -> "/myQuotes"
            "openReceivedQuotes" -> s("quoteId").let { if (it.isEmpty()) "/myQuotes" else "/myQuotes/$it" }
            "openQuoteBrowse" -> "/availableQuotes"
            "openAutoQuoteTemplates" -> "/quoteSettings"

            // 레슨
            "openLessonCalendar" -> "/lessonCalendar"
            "openLessonCreate" -> "/lessons/create"
            "openLessonQuoteHub" -> "/lessonHub"
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
