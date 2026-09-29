package com.muyeon.app.webview

import android.content.Context
import com.muyeon.app.result.ResultKeys

/**
 * 네이티브 → 웹 콜백 한 곳 모음. iOS `WebViewModel.notifyWeb*` 와 **함수명·인자 순서까지 1:1**.
 *
 * ⚠️ 이름이 하나라도 틀리면 웹은 조용히 무시한다(`window.__onX && window.__onX()`).
 *   그래서 화면상으론 정상인데 웹은 갱신되지 않고, `__onReviewEdited` 처럼 저장 자체를
 *   웹이 맡는 콜백은 **조작이 통째로 유실된다**. 웹 정본은 각 함수 주석의 파일을 볼 것.
 *
 * AOS 는 웹뷰가 다른 액티비티라 즉시 실행이 안 되므로 전부 WebCallbackQueue 에 쌓는다
 * (WebViewActivity.onResume 이 흘려보낸다). 중복 실행돼도 안전한 것만 넣을 것.
 *
 * ★ 이중 전달 방지 규칙(2026-09-30 결과 콜백 도입) —
 *   "다시 읽어라" 류 재조회 통지(예약·레슨·리뷰·알림·학원·자동응답·이력서·공고)는 **대기열에 넣지 않는다.**
 *   자식 화면이 결과 키(result/ResultKeys)로 돌려주고, WebViewActivity 가 결과를 받을 때
 *   [resultJs] 로 한 번만 보낸다. 대기열과 결과 양쪽에 두면 웹이 같은 재조회를 두 번 돈다.
 *   결과 경로는 프로세스가 정리돼도 살아남는다(Activity Result 레지스트리 + ResultActivity 가 키를
 *   saved state 에 보존). 대기열은 **데이터를 실어 나르는** 콜백(견적 제출·주소·리뷰 수정·역할 변경)
 *   전용으로 남긴다 — 그건 결과 키로 대신할 수 없다.
 */
object WebCallbacks {

    /** 주소(관심지역) 설정 완료 — 웹 RegionContext 반영. 정본: components/muyeon/NativeAddressBridge.js */
    fun addressSelected(context: Context, region: String, code: String) {
        if (region.isEmpty()) return
        enqueue(context, "window.onAddressSelected && window.onAddressSelected('${esc(region)}','${esc(code)}')")
    }

    /** 견적요청 제출 완료 — 웹이 받은견적 목록으로 라우팅. 정본: pages/muyeon/quote */
    fun quoteSubmitted(context: Context) {
        enqueue(context, "window.onQuoteSubmitted && window.onQuoteSubmitted()")
    }

    /**
     * 리뷰 수정 저장 — **웹이 이 콜백으로 실제 저장 API(callWriteReview)를 호출한다.**
     *  네이티브는 UI 만 담당하므로 이 콜백이 빠지면 수정이 아무 데도 반영되지 않는다.
     */
    fun reviewEdited(context: Context, rating: Int, content: String) {
        enqueue(
            context,
            "if(window.__onReviewEdited){ window.__onReviewEdited($rating,'${esc(content)}'); }",
        )
    }

    /**
     * 결과 키 → 웹 전달 JS 한 줄(WebViewActivity 가 자식 결과를 받을 때 쓴다).
     *
     *  새 웹: `window.__muyeonResult(KEY, payload)` 하나로 받는다(muyeon-front src/common/resultKeys.js).
     *  옛 웹: __muyeonResult 가 없을 때만 예전 이름(__onX)으로 폴백한다. ★ 둘 다 부르지 않는다 —
     *   새 웹의 AppRouter 는 레거시 이름도 같은 키로 이어주므로, 둘 다 부르면 재조회가 두 번 돈다.
     *  레거시 이름이 없는 키(QUOTES·CHAT_ROOMS 등)는 옛 웹에선 no-op — 옛 웹은 visibilitychange 로 맞춘다.
     */
    fun resultJs(key: String, payloadJson: String): String {
        val safeKey = esc(key)
        val legacy = legacyJs(key, payloadJson)
        val modern = "window.__muyeonResult('$safeKey', $payloadJson);"
        return if (legacy == null) "if(window.__muyeonResult){ $modern }"
        else "if(window.__muyeonResult){ $modern } else { $legacy }"
    }

    /**
     * 옛 웹 콜백 매핑(iOS notifyWeb* 와 같은 이름). payload 가 필요한 건 RESERVATIONS 뿐 —
     *  단건 취소({canceledId})는 __onLessonReservationCanceled(id) 로 즉시 취소선을 긋게 한다.
     */
    private fun legacyJs(key: String, payloadJson: String): String? {
        val p = runCatching { org.json.JSONObject(payloadJson) }.getOrNull()
        return when (key) {
            ResultKeys.RESERVATIONS -> {
                val canceled = p?.optString("canceledId").orEmpty()
                val changed = p?.optString("reservationId").orEmpty()
                if (canceled.isNotEmpty()) {
                    "if(window.__onLessonReservationCanceled){ window.__onLessonReservationCanceled('${esc(canceled)}'); }"
                } else {
                    val arg = if (changed.isNotEmpty()) "'${esc(changed)}'" else "null"
                    "if(window.__onNativeReservationsChanged){ window.__onNativeReservationsChanged($arg); }"
                }
            }
            ResultKeys.REVIEWS -> call("__onNativeReviewWritten")
            ResultKeys.AUTO_QUOTE -> call("__onAutoQuoteChanged")
            ResultKeys.NOTIFICATIONS -> call("__onNativeNotificationsRead")
            ResultKeys.LESSONS -> call("__refreshMyLessons")
            ResultKeys.ACADEMY -> call("__onNativeAcademyChanged")
            ResultKeys.RESUME -> call("__onNativeResumeChanged")
            ResultKeys.JOB_POSTINGS -> call("__onNativeJobChanged")
            else -> null
        }
    }

    private fun call(fn: String) = "if(window.$fn){ window.$fn(); }"

    /** 알림 모두읽음 — 웹 알림 배지 갱신. */
    fun notificationsRead(context: Context) {
        enqueue(context, "if(window.__onNativeNotificationsRead){ window.__onNativeNotificationsRead(); }")
    }

    /** 후기 작성 완료 — 웹 강사 리뷰 목록 재조회. 정본: components/muyeon/TeacherReviews.js */
    fun reviewWritten(context: Context) {
        enqueue(context, "if(window.__onNativeReviewWritten){ window.__onNativeReviewWritten(); }")
    }

    /** 학원↔강사 소속 변경 — 웹 MY 소속 배지 갱신(iOS presentAcademyFull dismiss 콜백). */
    fun academyChanged(context: Context) {
        enqueue(context, "window.__onNativeAcademyChanged && window.__onNativeAcademyChanged()")
    }

    /**
     * 예약이 바뀜(예약·변경·이의신청 등) — 웹 예약내역·예약상세가 서버에서 다시 읽는다.
     *  iOS `notifyWebReservationsChanged` 와 같은 훅. 정본: pages/muyeon/my/MyReservations.js.
     *  재조회라 멱등 — 대기열에 쌓여 여러 번 실행돼도 안전하다.
     */
    fun reservationsChanged(context: Context, reservationId: Int? = null) {
        val arg = reservationId?.let { "'$it'" } ?: "null"
        enqueue(
            context,
            "if(window.__onNativeReservationsChanged){ window.__onNativeReservationsChanged($arg); }",
        )
    }

    /** 레슨 개설·수정 완료 — 웹 내 레슨(/myLessons) 목록 재조회. 정본: pages/muyeon/lessons/LessonManage.js. */
    fun lessonsChanged(context: Context) {
        enqueue(context, "if(window.__refreshMyLessons){ window.__refreshMyLessons(); }")
    }

    /** 예약 취소 — 웹 예약내역에 즉시 CANCELED 반영. */
    fun lessonReservationCanceled(context: Context, reservationId: Int) {
        enqueue(
            context,
            "if(window.__onLessonReservationCanceled){ window.__onLessonReservationCanceled('$reservationId'); }",
        )
    }

    private fun enqueue(context: Context, js: String) = WebCallbackQueue.enqueue(context, js)

    /**
     * JS 단따옴표 리터럴 이스케이프 — iOS `notifyWebReviewEdited` 와 같은 규칙.
     *  개행류(\n·\r·U+2028·U+2029)까지 막아야 리터럴이 조기 종료돼 SyntaxError 로
     *  통지가 통째로 사라지는 일이 없다. 백슬래시부터 치환(이중 이스케이프 방지).
     */
    private fun esc(s: String) = s
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\u2028", "\\u2028")
        .replace("\u2029", "\\u2029")
}
