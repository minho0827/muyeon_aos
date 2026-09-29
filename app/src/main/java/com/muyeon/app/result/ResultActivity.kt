package com.muyeon.app.result

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 결과 콜백을 돌려주는 기능 화면의 공통 부모.
 *
 * 왜 finish() 를 덮어쓰나 — 화면을 나가는 길은 X 버튼·시스템 뒤로가기·예측 뒤로가기·
 *  openWebAndFinish 등 여러 갈래다. 버튼마다 setResult 를 붙이면 반드시 하나가 샌다.
 *  finish() 는 전부가 거쳐 가는 유일한 관문이라 여기서 한 번에 결과를 싣는다.
 *
 *  · [defaultResultKeys] — 이 화면이 닫히면 늘 돌려주는 키(화면이 무엇을 바꿀 수 있는지).
 *  · [addResultKeys]     — 세션 중 실제로 바꾼 것(삭제·예약 등)을 추가로 쌓는다.
 *  · 자식 화면의 결과도 여기에 합쳐진다(버블링) — 웹 → 대시보드 → 레슨 개설처럼 중간에
 *    네이티브 부모가 끼어도 웹이 LESSONS 를 듣게 하려는 것.
 *  · 모은 키는 onSaveInstanceState 에 남겨 프로세스가 정리돼도 유실되지 않는다.
 */
abstract class ResultActivity : ComponentActivity(), ResultHost {

    /** 닫힐 때 항상 돌려주는 키. 라우트에 따라 달라지면 getter 로 계산한다. */
    protected open val defaultResultKeys: Set<String> get() = emptySet()

    private val sessionKeys = LinkedHashSet<String>()
    private val sessionPayload = Bundle()

    private val _childResults = MutableSharedFlow<Set<String>>(extraBufferCapacity = 8)

    /**
     * companion start(...) 로 띄운 자식이 돌려준 키. 화면별 런처([rememberResultLauncher])가 없는
     *  호출부도 이걸 구독하면 복귀 시 다시 읽을 수 있다([OnChildResult]).
     */
    val childResults: SharedFlow<Set<String>> = _childResults.asSharedFlow()

    private val childLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            val keys = r.data.resultKeys()
            if (keys.isEmpty()) return@registerForActivityResult
            absorbChildResult(keys, r.data.resultPayload())
            _childResults.tryEmit(keys)
        }

    /** 네이티브 부모가 띄웠나 — 그렇다면 웹으로 튕기지 말고 결과만 들고 돌아간다. */
    val launchedFromNative: Boolean
        get() = intent?.getBooleanExtra(ResultKeys.EXTRA_FROM_NATIVE, false) == true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.getStringArrayList(STATE_KEYS)?.let { sessionKeys.addAll(it) }
        savedInstanceState?.getBundle(STATE_PAYLOAD)?.let { sessionPayload.putAll(it) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(STATE_KEYS, ArrayList(sessionKeys))
        outState.putBundle(STATE_PAYLOAD, Bundle(sessionPayload))
    }

    /** 세션 중 바뀐 것을 결과에 추가한다(닫힐 때 기본 키와 합쳐 나간다). */
    fun addResultKeys(vararg keys: String) {
        keys.filterTo(sessionKeys) { it in ResultKeys.ALL }
    }

    /** 키 하나의 부가 데이터(예: RESERVATIONS → {canceledId}). 키도 함께 추가된다. */
    fun putResultPayload(key: String, payload: Bundle) {
        addResultKeys(key)
        sessionPayload.putBundle(key, payload)
    }

    internal fun mergeResultPayload(payload: Bundle) {
        for (k in payload.keySet()) payload.getBundle(k)?.let { putResultPayload(k, it) }
    }

    /** 자식 결과를 내 결과에 합친다(버블링). 화면 재조회 신호는 보내지 않는다. */
    internal fun absorbChildResult(keys: Set<String>, payload: Bundle?) {
        addResultKeys(*keys.toTypedArray())
        payload?.let { mergeResultPayload(it) }
    }

    override fun launchForResult(intent: Intent) {
        childLauncher.launch(intent.putExtra(ResultKeys.EXTRA_FROM_NATIVE, true))
    }

    override fun finish() {
        val keys = LinkedHashSet(defaultResultKeys).apply { addAll(sessionKeys) }
        if (keys.isNotEmpty()) {
            setResult(RESULT_OK, resultIntent(keys, sessionPayload.takeIf { !it.isEmpty }))
        }
        super.finish()
    }

    private companion object {
        const val STATE_KEYS = "muyeon.result.keys"
        const val STATE_PAYLOAD = "muyeon.result.payload"
    }
}
