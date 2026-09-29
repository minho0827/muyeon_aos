package com.muyeon.app.result

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

/**
 * 결과를 받는 화면 전용 런처 — [launch] 로 띄운 자식이 닫히면 돌려준 키로 [rememberResultLauncher] 의
 *  onResult 가 불린다. 자식에게는 [ResultKeys.EXTRA_FROM_NATIVE] 를 붙여 "웹으로 튕기지 말고
 *  돌아오라"고 알린다.
 */
class ResultLauncher internal constructor(
    private val launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
) {
    fun launch(intent: Intent) {
        launcher.launch(intent.putExtra(ResultKeys.EXTRA_FROM_NATIVE, true))
    }
}

/**
 * `rememberLauncherForActivityResult(StartActivityForResult())` 래퍼.
 *  · 자식이 키를 하나라도 돌려주면 [onResult] 호출(빈 결과·취소는 무시).
 *  · 이 화면이 [ResultActivity] 안에 있으면 받은 키를 그 액티비티 결과에도 합친다(웹까지 버블링).
 *
 * ★ LaunchedEffect(Unit) 재실행에 기대지 말 것 — 복귀 시 재조회는 오직 여기서 한다.
 */
@Composable
fun rememberResultLauncher(onResult: (Set<String>) -> Unit): ResultLauncher {
    val context = LocalContext.current
    val current by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val keys = r.data.resultKeys()
        if (keys.isEmpty()) return@rememberLauncherForActivityResult
        (context.findActivity() as? ResultActivity)?.absorbChildResult(keys, r.data.resultPayload())
        current(keys)
    }
    return remember(launcher) { ResultLauncher(launcher) }
}

/**
 * companion start(...) 로 띄운 자식의 결과 구독 — 호출부가 화면 깊숙이 있어 런처를 넘기기
 *  번거로울 때 쓴다(액티비티 [ResultActivity.childResults] 를 그대로 흘려 받는다).
 */
@Composable
fun OnChildResult(onResult: (Set<String>) -> Unit) {
    val activity = LocalContext.current.findActivity() as? ResultActivity ?: return
    val current by rememberUpdatedState(onResult)
    LaunchedEffect(activity) {
        activity.childResults.collect { current(it) }
    }
}

// ── NavHost 안 결과 ──

/**
 * 바로 아래(이전) 백스택 항목에 결과 키를 심는다. 이미 있던 키와 합친다.
 *  ★ 목적지에 **들어갈 때** 불러 두면 시스템 뒤로·X·예측 뒤로가기 어느 쪽으로 나가도 빠지지 않는다.
 */
fun NavController.setResultKeys(vararg keys: String) {
    val handle = previousBackStackEntry?.savedStateHandle ?: return
    val cur = handle.get<ArrayList<String>>(ResultKeys.EXTRA_KEYS).orEmpty()
    handle[ResultKeys.EXTRA_KEYS] = ArrayList((cur + keys).filter { it in ResultKeys.ALL }.distinct())
}

/**
 * 이 목적지로 돌아왔을 때 위 화면이 심어둔 키를 받는다. 받은 즉시 비운다(재구성 때 중복 재조회 방지).
 *  ★ remove() 대신 null 로 비운다 — remove 는 getStateFlow 가 쥔 흐름을 끊어 다음 결과를 못 받는다.
 */
@Composable
fun NavBackStackEntry.OnResultKeys(onResult: (Set<String>) -> Unit) {
    val handle = savedStateHandle
    val flow = remember(this) { handle.getStateFlow<ArrayList<String>?>(ResultKeys.EXTRA_KEYS, null) }
    val keys by flow.collectAsState()
    val current by rememberUpdatedState(onResult)
    LaunchedEffect(keys) {
        val k = keys?.filter { it in ResultKeys.ALL }?.toSet()
        if (k.isNullOrEmpty()) return@LaunchedEffect
        handle[ResultKeys.EXTRA_KEYS] = null
        current(k)
    }
}

/**
 * 목적지 진입 시 부모 목적지에 돌려줄 키를 미리 심는다([setResultKeys] 의 Composable 판).
 *  entry 가 맨 위일 때만 심는다 — 전환 애니메이션 중 다른 항목에 잘못 심지 않게.
 */
@Composable
fun NavController.ReturnResultKeys(entry: NavBackStackEntry, vararg keys: String) {
    LaunchedEffect(entry) {
        if (currentBackStackEntry?.id == entry.id) setResultKeys(*keys)
    }
}
