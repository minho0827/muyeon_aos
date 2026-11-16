package com.muyeon.app.ui.common

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.muyeon.app.R
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.ActiveRole
import com.muyeon.domain.model.member.ActiveType
import com.muyeon.domain.model.member.TypeFeature
import com.muyeon.domain.model.member.TypeGateDecision
import com.muyeon.domain.model.member.TypeGateRules
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * ★ 2026-11-16 셸 유형 전환 통로 — 셸 밖 화면(채팅·견적 응답·레슨 예약·캐스팅 상세 …)도 유형을 바꿀 때 여기로 보낸다.
 *   탭 셸(MainViewModel)이 듣고 있으면 셸의 단일 전환 함수가 바꾼다 → 탭바 MY 이름·동그라미·글쓰기 메뉴가 같이 바뀌고
 *   "○○ 유형으로 전환됐어요" 토스트가 늘 뜬다. 셸이 없으면(푸시로 바로 연 화면 등) 직접 서버에 저장하고 같은 토스트를 띄운다.
 *   ⚠️ ActiveRole.saveOnServer + store 를 화면에서 직접 부르지 않는다 — 셸이 모른 채 남는다.
 */
object ShellTypeSwitch {
    sealed interface Request {
        data class Switch(val type: String, val done: CompletableDeferred<Boolean>) : Request
        data object OpenRoleManage : Request
    }

    private val _requests = MutableSharedFlow<Request>(extraBufferCapacity = 4)
    val requests: SharedFlow<Request> = _requests.asSharedFlow()

    private val shellListening: Boolean get() = _requests.subscriptionCount.value > 0

    /** 유형 전환. 성공이면 true(로컬 저장소까지 바뀐 뒤). */
    suspend fun switchTo(context: Context, type: String): Boolean {
        val t = type.trim().uppercase()
        if (shellListening) {
            val done = CompletableDeferred<Boolean>()
            _requests.emit(Request.Switch(t, done))
            return done.await()
        }
        val app = context.applicationContext
        val ok = ActiveRole.saveOnServer(app, t)
        if (ok) {
            ActiveRole.store(app, t)
            Toast.makeText(app, app.getString(R.string.main_type_switched, ActiveRole.label(t)), Toast.LENGTH_SHORT).show()
        }
        return ok
    }

    /** 회원유형 관리 화면 — 셸이 있으면 셸이(MainViewModel.openRoleManage), 없으면 직접 역할 원문을 받아 연다. */
    suspend fun openRoleManage(context: Context) {
        if (shellListening) { _requests.emit(Request.OpenRoleManage); return }
        val json = ActiveRole.fetchRolesJson(context)
        com.muyeon.app.ui.onboarding.OnboardingActivity.startRoleManage(context, json, null, ActiveRole.current(context), completeInApp = true)
    }
}

/** GET /me/roles 본문 → (지금 유형, 보유 유형, 심사 중 유형). */
internal data class RolesSnapshot(val activeType: String, val held: List<String>, val pending: List<String>) {
    companion object {
        fun parse(json: String?): RolesSnapshot? {
            val o = runCatching { JSONObject(json ?: return null) }.getOrNull() ?: return null
            val heldArr = o.optJSONArray("held")
            val held = heldArr?.let { a -> (0 until a.length()).map { a.optString(it).uppercase() } }.orEmpty()
            val rolesArr = o.optJSONArray("roles")
            val pending = rolesArr?.let { a ->
                (0 until a.length()).mapNotNull { i ->
                    val r = a.optJSONObject(i) ?: return@mapNotNull null
                    if (r.optString("status").equals("PENDING", true)) r.optString("role").uppercase() else null
                }
            }.orEmpty()
            val active = o.optString("activeType").takeIf { it.isNotBlank() && it != "null" } ?: ActiveType.GENERAL
            return RolesSnapshot(ActiveType.visible(active), held, pending)
        }
    }
}

/**
 * ★ 2026-11-16 "필요한 유형" 공통 관문 — `gate.require(기능) { 이어서 할 일 }`.
 *   판정은 :domain [TypeGateRules] 가 하고, 여기서는 역할 조회·창 띄우기·전환만 한다. 화면은 [TypeGateHost] 를 한 번 그려 둔다.
 */
@Stable
class TypeGate internal constructor(private val context: Context, private val scope: CoroutineScope) {
    internal data class Prompt(val feature: TypeFeature, val decision: TypeGateDecision, val run: () -> Unit)

    internal var prompt by mutableStateOf<Prompt?>(null)
    /** 조회·전환 중 — 같은 버튼을 두 번 눌러도 한 번만. */
    var busy by mutableStateOf(false)
        private set

    /**
     * @param onLogin 비회원일 때(없으면 앱 로그인 화면). 버튼은 애초에 비회원에게 숨기는 게 원칙이다.
     */
    fun require(feature: TypeFeature, onLogin: (() -> Unit)? = null, run: () -> Unit) {
        if (busy) return
        val loggedIn = !TokenManager.getAccessToken(context).isNullOrEmpty()
        when (TypeGateRules.decide(feature, loggedIn, ActiveRole.current(context))) {
            TypeGateDecision.Login -> { onLogin?.invoke() ?: com.muyeon.app.ui.auth.LoginActivity.start(context, null); return }
            TypeGateDecision.Allowed -> { run(); return }
            else -> Unit
        }
        busy = true
        scope.launch {
            val snap = RolesSnapshot.parse(ActiveRole.fetchRolesJson(context))
            busy = false
            if (snap == null) {
                Toast.makeText(context, R.string.type_gate_load_failed, Toast.LENGTH_SHORT).show()
                return@launch
            }
            val d = TypeGateRules.decide(feature, true, snap.activeType, snap.held, snap.pending)
            if (d == TypeGateDecision.Allowed) {
                // 다른 길(웹 등)에서 서버 유형이 이미 바뀌어 있었다 — 로컬만 맞추고 진행한다(셸은 화면 복귀 때 다시 읽는다).
                ActiveRole.store(context, snap.activeType)
                run()
                return@launch
            }
            prompt = Prompt(feature, d, run)
        }
    }

    internal fun switchAndRun(type: String, run: () -> Unit) {
        busy = true
        scope.launch {
            val ok = ShellTypeSwitch.switchTo(context, type)
            busy = false
            if (ok) run()
        }
    }

    internal fun openRoleManage() { scope.launch { ShellTypeSwitch.openRoleManage(context) } }

    companion object {
        /** 화면 밖(코루틴)에서 쓰는 판정 — 지금 가진 필요 유형 중 첫째(없으면 null). 조회 실패도 null. */
        suspend fun firstHeld(context: Context, feature: TypeFeature): String? {
            val snap = RolesSnapshot.parse(ActiveRole.fetchRolesJson(context)) ?: return null
            return feature.required.firstOrNull { it in snap.held }
        }
    }
}

@Composable
fun rememberTypeGate(): TypeGate {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context, scope) { TypeGate(context, scope) }
}

/** 관문 창 — 전환 / 유형 추가 / 심사 중. 화면마다 한 번 그려 둔다. */
@Composable
fun TypeGateHost(gate: TypeGate) {
    val p = gate.prompt ?: return
    val close = { gate.prompt = null }
    when (val d = p.decision) {
        is TypeGateDecision.Switch -> MuyeonDialog(
            title = "회원유형 안내",
            message = TypeGateRules.switchMessage(d, p.feature),
            actions = d.options.map { t ->
                MuyeonDialogAction(TypeGateRules.switchButton(t, p.feature), MuyeonDialogKind.PRIMARY) { gate.switchAndRun(t, p.run) }
            } + MuyeonDialogAction("취소", MuyeonDialogKind.CANCEL),
            onDismiss = close,
        )
        is TypeGateDecision.AddType -> MuyeonDialog(
            title = "회원유형 안내",
            message = TypeGateRules.addTypeMessage(d),
            actions = listOf(
                MuyeonDialogAction("유형 추가", MuyeonDialogKind.PRIMARY) { gate.openRoleManage() },
                MuyeonDialogAction("취소", MuyeonDialogKind.CANCEL),
            ),
            onDismiss = close,
        )
        is TypeGateDecision.Pending -> MuyeonDialog(
            title = "인증 심사 중이에요",
            message = TypeGateRules.pendingMessage(d),
            actions = listOf(
                MuyeonDialogAction("인증 상태 보기", MuyeonDialogKind.PRIMARY) { gate.openRoleManage() },
                MuyeonDialogAction("닫기", MuyeonDialogKind.CANCEL),
            ),
            onDismiss = close,
        )
        else -> Unit
    }
}
