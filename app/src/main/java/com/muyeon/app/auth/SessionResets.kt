package com.muyeon.app.auth

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.muyeon.domain.common.KeyValueStore
import com.muyeon.domain.common.SessionEvents
import com.muyeon.domain.common.SessionKeys
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * ★ 2026-11-09 로그아웃·계정 바뀜 때 지울 앱 쪽 회원 기억값 — 한 곳(SessionEvents 지우개).
 *
 *  사용자 지적: 로그아웃해도 찜 하트·배지 등이 그대로 남았다. 신호는 TokenManager 가 토큰을 저장·지울 때 보낸다
 *  (MY·설정 로그아웃 · 탈퇴 · 웹 로그아웃 · 강제 로그아웃 · 세션 만료 — 모두 토큰을 지운다).
 *
 *  계정이 떠날 때(previous != null) 지우는 것 — 회원 것:
 *   · prefs `muyeon.uiState` 의 회원 키(SessionKeys.PER_USER — 로그인 전 눌러 둔 찜 등)
 *   · prefs `muyeon.calendar`(강사 레슨 달력 '확인한 예약' 점)
 *   · prefs `muyeon.webcb`(웹에 아직 못 보낸 결과 콜백)
 *   · AI 도우미 최근 검색어·최근 메뉴 · 배지 숫자 · 기능 목록 캐시
 *   · 채팅 안 읽음 점(FloatingState.chatUnread) · 이미지 메모리 캐시 · 웹뷰 쿠키
 *  바뀔 때마다 하는 것:
 *   · 학원 인증 차단(VerifyGate) 다시 묻기 · 캐스팅 첫 화면 다시 읽기(공연팀 여부가 계정마다 다르다)
 *
 *  남기는 것 — 기기 화면값: 칩·보기 방식·탭(SessionKeys.DEVICE) · 대표 지역(muyeonRegions) · [공연] 칸 마지막으로 본 시각
 *   · 권한 물음·안내 본 기록 · AI 버튼 위치 · 최근 로그인 방식/아이디 기억(로그인 화면 기능) · 결제 미완료 영수증(로그인 뒤 보낸다)
 *   · 회원 번호가 붙은 키(muyeon.notice.pendingVerify.<id> — 계정끼리 섞이지 않는다)
 *  웹 창 localStorage(muyeonAuth·활성유형)는 WebLogout 이 다음 창에서 지운다.
 *
 *  화면(ViewModel)은 [onAccountChanged] · Compose 는 [accountEpoch] 로 다시 읽는다.
 */
object SessionResets {
    private val main = Handler(Looper.getMainLooper())

    fun install(context: Context, store: KeyValueStore) {
        val app = context.applicationContext
        SessionEvents.register { previous, _ ->
            if (previous != null) {
                SessionKeys.clearPerUser(store)
                app.getSharedPreferences("muyeon.calendar", Context.MODE_PRIVATE).edit().clear().apply()
                com.muyeon.app.webview.WebCallbackQueue.clear(app)
                com.muyeon.app.ui.assistant.AssistantRecent.clearAll(app)
            }
            main.post {
                if (previous != null) {
                    com.muyeon.app.ui.floating.FloatingState.updateChatUnread(0)
                    com.muyeon.app.ui.assistant.AssistantBadges.clear()
                    com.muyeon.app.ui.assistant.AssistantOverlay.clearCaches()
                    runCatching { coil3.SingletonImageLoader.get(app).memoryCache?.clear() }
                    runCatching { android.webkit.CookieManager.getInstance().removeAllCookies(null) }
                }
                com.muyeon.app.ui.floating.FloatingState.requestRefresh()
                com.muyeon.app.ui.castingstage.CastingStageSession.reloadKey++
                com.muyeon.app.webview.VerifyGate.refresh(app)
            }
        }
    }
}

/** ViewModel — 계정이 바뀌면(로그아웃·다른 계정 로그인) block. 처음 값은 건너뛴다. */
fun ViewModel.onAccountChanged(block: (SessionEvents.Change) -> Unit) {
    viewModelScope.launch { SessionEvents.state.drop(1).collect { block(it) } }
}

/** Compose — 계정 바뀜 번호. LaunchedEffect·remember 키에 넣으면 바뀔 때 다시 읽는다. */
@Composable
fun accountEpoch(): Int = SessionEvents.state.collectAsState().value.epoch

/** 지금 계정 바뀜 번호 — 늦게 온 응답이 앞 계정 것이면 버릴 때(시작 때 적고, 받은 뒤 같은지 본다). */
fun accountEpochNow(): Int = SessionEvents.state.value.epoch
