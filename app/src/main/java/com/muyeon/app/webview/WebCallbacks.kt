package com.muyeon.app.webview

import android.content.Context

/**
 * 네이티브 → 웹 콜백 한 곳 모음. iOS `WebViewModel.notifyWeb*` 와 **함수명·인자 순서까지 1:1**.
 *
 * ⚠️ 이름이 하나라도 틀리면 웹은 조용히 무시한다(`window.__onX && window.__onX()`).
 *   그래서 화면상으론 정상인데 웹은 갱신되지 않고, `__onReviewEdited` 처럼 저장 자체를
 *   웹이 맡는 콜백은 **조작이 통째로 유실된다**. 웹 정본은 각 함수 주석의 파일을 볼 것.
 *
 * AOS 는 웹뷰가 다른 액티비티라 즉시 실행이 안 되므로 전부 WebCallbackQueue 에 쌓는다
 * (WebViewActivity.onResume 이 흘려보낸다). 중복 실행돼도 안전한 것만 넣을 것.
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

    /** 알림 모두읽음 — 웹 알림 배지 갱신. */
    fun notificationsRead(context: Context) {
        enqueue(context, "if(window.__onNativeNotificationsRead){ window.__onNativeNotificationsRead(); }")
    }

    /** 후기 작성 완료 — 웹 강사 리뷰 목록 재조회. 정본: components/muyeon/TeacherReviews.js */
    fun reviewWritten(context: Context) {
        enqueue(context, "if(window.__onNativeReviewWritten){ window.__onNativeReviewWritten(); }")
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
