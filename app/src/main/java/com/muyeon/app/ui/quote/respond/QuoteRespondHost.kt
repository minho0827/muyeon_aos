package com.muyeon.app.ui.quote.respond

import android.app.Activity
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.muyeon.app.R
import com.muyeon.app.ui.auth.LoginActivity
import com.muyeon.app.ui.main.MainActivity
import com.muyeon.app.ui.quote.QuoteBrowseActivity
import com.muyeon.app.webview.ActiveRole
import com.muyeon.app.webview.NativeActionRouter
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 견적 응답 화면을 띄우는 자리(QuoteBrowseActivity — 요청 id 로 열렸을 때). 화면 밖 이동·유형 전환을 여기서 한다.
 * @param reloadSignal 다른 화면에서 돌아올 때마다 오른다 → 다시 읽기.
 */
@Composable
fun QuoteRespondHost(activity: Activity, quoteId: Int, compose: Boolean, reloadSignal: Int) {
    val state = remember {
        QuoteRespondState(
            QuoteRespondApi(activity), quoteId,
            isProvider = { QuoteRespondApi(activity).isLoggedIn && ActiveRole.current(activity) in PROVIDER_TYPES },
            autoCompose = compose,
        )
    }
    val scope = rememberCoroutineScope()
    LaunchedEffect(reloadSignal) { state.load() }
    val here = "/availableQuotes/$quoteId"
    val actions = remember {
        QuoteRespondActions(
            onBack = { activity.finish() },
            onOpenChat = { rid, replace ->
                NativeActionRouter.open(activity, "openChatRoom", JSONObject().put("roomId", rid.toString()))
                if (replace) activity.finish()
            },
            onToList = { QuoteBrowseActivity.start(activity); activity.finish() },
            onHome = { MainActivity.openPath(activity, "/home"); activity.finish() },
            onLogin = { LoginActivity.start(activity, "$here?compose=1"); activity.finish() },
            onSwitch = { type ->
                scope.launch {
                    state.busy = true
                    if (ActiveRole.saveOnServer(activity, type)) {
                        ActiveRole.store(activity, type)
                        Toast.makeText(activity, activity.getString(R.string.quote_respond_switched, ActiveRole.label(type)), Toast.LENGTH_SHORT).show()
                        state.load()
                    } else {
                        state.toast = activity.getString(R.string.quote_respond_switch_failed)
                    }
                    state.busy = false
                }
            },
            onAddRole = { role ->
                if (role != "TEACHER") {
                    // 학원 — 사업자 인증 서류(앱 역할 인증 화면, 웹 openRoleVerification 과 같다). 돌아오면 다시 읽는다.
                    NativeActionRouter.open(activity, "openRoleVerification", JSONObject().put("role", role))
                } else scope.launch {
                    // 강사 — 서류 없이 본인 인증만으로 바로 부여(웹 goRoleVerification → callAddRole).
                    when (val r = ActiveRole.addRole(activity, role)) {
                        is ActiveRole.AddResult.Added -> {
                            if (ActiveRole.saveOnServer(activity, role)) ActiveRole.store(activity, role)
                            state.load()
                        }
                        // 본인인증 전 — 웹 회원유형 화면의 본인확인(NICE)으로(그 단계만 웹). 끝나면 이 요청으로 돌아온다.
                        ActiveRole.AddResult.IdentityRequired ->
                            MainActivity.openPath(activity, "/my/memberType?verify=$role&redirect=" + Uri.encode(here))
                        is ActiveRole.AddResult.Failed -> state.toast = r.message
                    }
                }
            },
            onOpenPhoto = { urls, i ->
                com.muyeon.app.ui.common.ImageViewerActivity.start(activity, org.json.JSONArray(urls.map { com.muyeon.app.ui.quote.QuoteUi.imageUrl(it) }).toString(), i, allowSave = false)
            },
        )
    }
    QuoteRespondScreen(state, actions)
}

private val PROVIDER_TYPES = setOf(ActiveRole.TEACHER, ActiveRole.ACADEMY)
