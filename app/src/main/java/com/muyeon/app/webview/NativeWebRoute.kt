package com.muyeon.app.webview

import android.app.Activity
import android.content.Intent

/**
 * 네이티브 화면 → 웹뷰 복귀 라우팅.
 *
 *  기존에는 각 네이티브 액티비티가 `getLaunchIntentForPackage` + `nativeGoPath` extra 로 돌아갔지만
 *  런처 인텐트는 **SplashActivity** 를 열고 그 extra 를 아무도 읽지 않아, 완료 화면 CTA
 *  ("내 견적 보기"/"홈으로 이동" 등)가 스플래시만 다시 태우고 이동은 안 되던 문제가 있었다.
 *  → WebViewActivity 를 직접(CLEAR_TOP|SINGLE_TOP) 재사용하고 여기서 SPA 이동을 시킨다.
 *
 *  이동은 웹이 심어둔 `window.__nativeGo(path)` 사용(전체 리로드 없이 라우팅) —
 *  AppBridgeInterface.navigateWeb 과 동일 규약.
 */
object NativeWebRoute {

    const val EXTRA_GO_PATH = "nativeGoPath"
    const val EXTRA_EVAL_JS = "nativeEvalJs"

    /** 웹 경로로 이동하고 현재 네이티브 화면을 종료. */
    fun openWebAndFinish(activity: Activity, path: String) {
        activity.startActivity(webIntent(activity).putExtra(EXTRA_GO_PATH, path))
        activity.finish()
    }

    /** 웹에 콜백만 통지하고 종료(예: __onAutoQuoteChanged). 핸들러가 없으면 no-op. */
    fun notifyWebAndFinish(activity: Activity, js: String) {
        activity.startActivity(webIntent(activity).putExtra(EXTRA_EVAL_JS, js))
        activity.finish()
    }

    private fun webIntent(activity: Activity) =
        Intent(activity, WebViewActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
}
