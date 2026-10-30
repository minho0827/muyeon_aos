package com.muyeon.app.webview

import android.app.Activity
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.muyeon.app.ui.common.MuyeonDialog
import com.muyeon.app.ui.common.MuyeonDialogAction
import com.muyeon.app.ui.common.MuyeonDialogKind
import org.json.JSONArray

/**
 * 2026-10-31 웹 홈 상단 '지금 유형' 버튼(ActiveTypeSwitcher) → 앱 공통 팝업(MuyeonDialog)으로 유형 고르기. iOS 와 같은 모양.
 *  고르기만 한다 — 전환(서버 저장·헤더 동기·토스트)은 웹 AuthContext.setActiveType 이 한다.
 *  types = [{ type, label }] JSON 문자열, current = 지금 유형.
 *  결과: window.__onActiveTypePicked('TEACHER' | 'MANAGE'). 닫기·지금 유형은 알리지 않는다.
 */
object ActiveTypePicker {
    fun show(activity: Activity, webView: WebView, typesJson: String, current: String) {
        val types = runCatching {
            val arr = JSONArray(typesJson)
            (0 until arr.length()).map { arr.getJSONObject(it) }
                .map { it.optString("type") to it.optString("label") }
                .filter { it.first.isNotEmpty() }
        }.getOrNull().orEmpty()
        if (types.isEmpty()) return
        val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return

        val host = ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        }
        // MuyeonDialog 는 버튼을 누르면 onDismiss → onClick 순서로 부른다. 고른 값을 담아 두고 닫기를 다음 차례로 미뤄 한 번만 처리.
        var picked: String? = null
        var closed = false
        val close = {
            host.post {
                if (!closed) {
                    closed = true
                    root.removeView(host)
                    picked?.let { p ->
                        // ["TEACHER"] 배열로 넘겨 따옴표·특수문자 걱정 없이 전달한다.
                        val arg = JSONArray().put(p).toString()
                        webView.evaluateJavascript("window.__onActiveTypePicked && window.__onActiveTypePicked($arg[0])", null)
                    }
                }
            }
            Unit
        }
        // 지금 유형은 주황(선택됨), 나머지는 회색. 맨 아래 회원유형 추가·관리, 닫기.
        val actions = types.map { (type, label) ->
            if (type == current) MuyeonDialogAction("$label · 지금 이용 중", MuyeonDialogKind.PRIMARY)
            else MuyeonDialogAction(label, MuyeonDialogKind.CANCEL) { picked = type }
        } + listOf(
            MuyeonDialogAction("회원유형 추가·관리", MuyeonDialogKind.CANCEL) { picked = "MANAGE" },
            MuyeonDialogAction("닫기", MuyeonDialogKind.CANCEL),
        )
        host.setContent {
            MuyeonDialog(title = "어떤 유형으로 이용할까요?", actions = actions, onDismiss = close)
        }
        root.addView(host, ViewGroup.LayoutParams(0, 0))
    }
}
