package com.muyeon.app.result

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import org.json.JSONObject

/**
 * 화면 복귀 "결과 콜백" 키 — iOS·AOS·웹 공통 계약(웹 정본: muyeon-front src/common/resultKeys.js).
 *
 * ★ 이름을 절대 바꾸지 말 것. 세 레포가 문자열로만 이어져 있어 한쪽만 바뀌어도 빌드는 통과하고
 *   "돌아왔는데 목록이 옛날 그대로"인 조용한 버그가 된다.
 *
 * 규칙 — 화면 B 에서 A 로 돌아오면(시스템 뒤로·X·예측 뒤로가기 전부) A 가 서버에서 다시 읽는다.
 *  · 타이머(폴링·지연 재시도)로 맞추지 않는다. B 가 "무엇이 바뀌었을 수 있는지"를 키로 돌려주고
 *    A 는 그 키를 보고 필요한 것만 다시 읽는다.
 *  · 액티비티 사이: Activity Result API(setResult + [EXTRA_KEYS]).
 *  · NavHost 안:   이전 백스택 항목의 savedStateHandle[[EXTRA_KEYS]].
 *  · 웹(WebViewActivity) 이 부모면 키마다 window.__muyeonResult(KEY, payload) 를 부른다.
 */
object ResultKeys {
    const val QUOTES = "QUOTES"
    const val QUOTE_PREFS = "QUOTE_PREFS"
    const val AUTO_QUOTE = "AUTO_QUOTE"
    const val CHAT_ROOMS = "CHAT_ROOMS"
    const val CHAT_ROOM = "CHAT_ROOM"
    const val LESSONS = "LESSONS"
    const val LESSON_SCHEDULE = "LESSON_SCHEDULE"
    const val RESERVATIONS = "RESERVATIONS"
    const val RESUME = "RESUME"
    const val PROFILE = "PROFILE"
    const val JOB_POSTINGS = "JOB_POSTINGS"
    const val ACADEMY = "ACADEMY"
    const val SPACE = "SPACE"
    const val NOTIFICATIONS = "NOTIFICATIONS"
    const val NOTIFICATION_PREFS = "NOTIFICATION_PREFS"
    const val STUDIO_MEMBERS = "STUDIO_MEMBERS"
    const val REVIEWS = "REVIEWS"
    const val MEMBERSHIP = "MEMBERSHIP"

    /** 결과 인텐트·savedStateHandle 에 키 목록(ArrayList<String>)을 싣는 이름. */
    const val EXTRA_KEYS = "resultKeys"

    /** 키별 부가 데이터 — Bundle(키 이름 → 그 키의 Bundle). 예: RESERVATIONS → {canceledId}. */
    const val EXTRA_PAYLOAD = "resultPayload"

    /**
     * 네이티브 부모가 띄웠다는 표시. 있으면 자식은 웹으로 튕기지(openWebAndFinish) 말고
     *  결과만 들고 부모로 돌아가야 한다 — CLEAR_TOP 이 중간의 네이티브 부모를 죽이기 때문이다.
     */
    const val EXTRA_FROM_NATIVE = "fromNative"

    /** 알려진 키 — 모르는 문자열은 결과에서 걸러낸다(오타가 웹까지 새지 않게). */
    val ALL: Set<String> = setOf(
        QUOTES, QUOTE_PREFS, AUTO_QUOTE, CHAT_ROOMS, CHAT_ROOM, LESSONS, LESSON_SCHEDULE,
        RESERVATIONS, RESUME, PROFILE, JOB_POSTINGS, ACADEMY, SPACE, NOTIFICATIONS,
        NOTIFICATION_PREFS, STUDIO_MEMBERS, REVIEWS, MEMBERSHIP,
    )
}

/** 결과 인텐트에 실린 키. 결과가 없거나 모르는 키뿐이면 빈 집합. */
fun Intent?.resultKeys(): Set<String> =
    this?.getStringArrayListExtra(ResultKeys.EXTRA_KEYS)
        ?.filter { it in ResultKeys.ALL }
        ?.toCollection(LinkedHashSet())
        .orEmpty()

/** 결과 인텐트에 실린 키별 부가 데이터(없으면 null). */
fun Intent?.resultPayload(): Bundle? = this?.getBundleExtra(ResultKeys.EXTRA_PAYLOAD)

/** 결과 인텐트 생성 — [finishWithResult]·[ResultActivity.finish] 공용. */
fun resultIntent(keys: Collection<String>, payload: Bundle? = null): Intent =
    Intent().apply {
        putStringArrayListExtra(ResultKeys.EXTRA_KEYS, ArrayList(keys.filter { it in ResultKeys.ALL }.distinct()))
        if (payload != null && !payload.isEmpty) putExtra(ResultKeys.EXTRA_PAYLOAD, payload)
    }

/**
 * 결과를 싣고 닫는다. [ResultActivity] 면 그 화면이 모아둔 키·기본 키와 합쳐진다.
 * @param payload 키 이름 → 그 키의 Bundle (예: `bundleOf(RESERVATIONS to bundleOf("canceledId" to "12"))`)
 */
fun Activity.finishWithResult(vararg keys: String, payload: Bundle? = null) {
    if (this is ResultActivity) {
        addResultKeys(*keys)
        payload?.let { mergeResultPayload(it) }
        finish()
        return
    }
    setResult(Activity.RESULT_OK, resultIntent(keys.toList(), payload))
    finish()
}

/** 키 한 개의 부가 데이터를 JSON 으로(웹 __muyeonResult 2번째 인자). 없으면 "null". */
fun Bundle?.payloadJsonFor(key: String): String {
    val b = this?.getBundle(key) ?: return "null"
    val o = JSONObject()
    for (k in b.keySet()) {
        @Suppress("DEPRECATION")
        o.put(k, b.get(k)?.let { v -> if (v is Bundle) JSONObject() else v })
    }
    return o.toString()
}

/**
 * 결과를 받아줄 부모 — WebViewActivity 와 [ResultActivity] 가 구현한다.
 *  각 화면 companion 의 start(...) 는 [launchScreen] 으로 띄우므로, 부모가 누구든 자식의 결과가
 *  빠짐없이 돌아온다(호출부가 결과 런처를 깜빡해도 키가 새지 않는 안전망).
 */
interface ResultHost {
    fun launchForResult(intent: Intent)
}

/** ContextWrapper 를 벗겨 실제 액티비티를 찾는다(Compose LocalContext 대비). */
fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/**
 * 화면 띄우기 공용 — 부모가 [ResultHost] 면 결과 런처로, 액티비티면 일반 시작,
 *  그 밖(서비스·앱 컨텍스트)이면 NEW_TASK 로.
 */
fun Context.launchScreen(intent: Intent) {
    val activity = findActivity()
    when {
        activity is ResultHost && !activity.isFinishing -> activity.launchForResult(intent)
        activity != null -> activity.startActivity(intent)
        else -> startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
