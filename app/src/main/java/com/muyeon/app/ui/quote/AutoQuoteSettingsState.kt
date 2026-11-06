package com.muyeon.app.ui.quote

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

/** 편집 시트 상태 — id null = 신규(iOS EditorState). */
data class AutoQuoteEditorState(val id: Int?, val title: String, val message: String, val price: String) {
    companion object { val New = AutoQuoteEditorState(null, "", "", "") }
}

/** 화면 전체 상태 — 처음 읽는 중 / 읽기 실패 / 읽음. */
sealed interface AutoQuoteLoad {
    data object Loading : AutoQuoteLoad
    data object Failed : AutoQuoteLoad
    data class Ready(val status: AutoQuoteStatus, val templates: List<AutoQuoteTemplate>) : AutoQuoteLoad
}

/**
 * ★ 2026-11-06 견적 자동응답 화면 상태 — 스위치·선택·삭제·저장이 바로 서버에 반영된다('완료' 버튼 없음).
 *  모든 변경 뒤에 status 와 목록을 다시 읽는다(서버가 선택·스위치를 바꿀 수 있어서).
 */
class AutoQuoteSettingsState(
    private val api: AutoQuoteApi,
    private val scope: CoroutineScope,
    /** 짧은 안내(토스트). */
    private val toast: (String) -> Unit,
) {
    var load by mutableStateOf<AutoQuoteLoad>(AutoQuoteLoad.Loading)
        private set

    /** 스위치 화면 값 — 누르는 즉시 바뀌고, 실패하면 되돌린다. */
    var switchOn by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var editing by mutableStateOf<AutoQuoteEditorState?>(null)

    /** 삭제 확인 중인 묶음. */
    var confirmDelete by mutableStateOf<AutoQuoteTemplate?>(null)

    private val ready get() = load as? AutoQuoteLoad.Ready

    fun reload(showSpinner: Boolean = false) {
        if (showSpinner) load = AutoQuoteLoad.Loading
        scope.launch { refresh(failHard = showSpinner || ready == null) }
    }

    /** failHard=false 면 읽기 실패해도 지금 화면을 그대로 둔다(뒤늦은 갱신 실패로 화면을 날리지 않게). */
    private suspend fun refresh(failHard: Boolean = false) {
        val s = scope.async { api.status() }
        val t = scope.async { api.templates() }
        val status = s.await()
        val templates = t.await()
        if (status != null && templates != null) {
            load = AutoQuoteLoad.Ready(status, templates)
            switchOn = status.switchOn
        } else if (failHard) {
            load = AutoQuoteLoad.Failed
        }
    }

    fun openNew() { editing = AutoQuoteEditorState.New }

    fun openEdit(t: AutoQuoteTemplate) {
        editing = AutoQuoteEditorState(t.id, t.title.orEmpty(), t.message, t.price.orEmpty())
    }

    /** 큰 스위치. 메시지가 없으면 켜지 않고 편집을 연다. */
    fun toggle(on: Boolean) {
        val r = ready ?: return
        if (busy) return
        if (on && r.templates.isEmpty()) {
            openNew(); toast("먼저 메시지를 만들어 주세요")
            return
        }
        val before = switchOn
        switchOn = on
        busy = true
        scope.launch {
            when (api.setEnabled(on)) {
                AutoQuoteSwitchResult.OK -> refresh()
                AutoQuoteSwitchResult.NO_TEMPLATE -> {
                    switchOn = before; openNew(); toast("먼저 메시지를 만들어 주세요"); refresh()
                }
                AutoQuoteSwitchResult.FAILED -> { switchOn = before; toast("저장하지 못했어요. 잠시 후 다시 시도해 주세요.") }
            }
            busy = false
        }
    }

    /** 라디오 — 고르기만 한다. 이미 고른 걸 다시 누르면 아무것도 안 한다. */
    fun activate(t: AutoQuoteTemplate) {
        val r = ready ?: return
        if (busy || r.status.activeId == t.id) return
        busy = true
        scope.launch {
            if (api.activate(t.id)) refresh() else toast("메시지를 바꾸지 못했어요.")
            busy = false
        }
    }

    fun delete(t: AutoQuoteTemplate) {
        if (busy) return
        busy = true
        scope.launch {
            if (api.remove(t.id)) refresh() else toast("삭제하지 못했어요.")
            busy = false
        }
    }

    fun save(st: AutoQuoteEditorState, title: String, message: String, price: String) {
        if (busy) return
        busy = true
        scope.launch {
            if (api.save(st.id, title, message, price)) { editing = null; refresh() }
            else toast("저장하지 못했어요.")
            busy = false
        }
    }
}

@Composable
fun rememberAutoQuoteSettingsState(api: AutoQuoteApi, toast: (String) -> Unit): AutoQuoteSettingsState {
    val scope = rememberCoroutineScope()
    return remember(api) { AutoQuoteSettingsState(api, scope, toast) }
}
