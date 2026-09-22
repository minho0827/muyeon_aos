package com.muyeon.app.ui.notification

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.ActiveRole
import com.muyeon.app.webview.NativeWebRoute
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * 알림 수신 설정 — 종류별 푸시 on/off. PaceERA `NotificationSettingsScreen` / iOS
 * `NotificationSettingsView` 와 같은 구조(목록 → 상세에서 토글).
 *
 * ⚠️ 여기서 끄는 것은 **푸시뿐**이다. 알림함 이력은 그대로 남는다 — 나중에 들어가서
 *    확인할 수 있어야 "알림이 안 왔다"는 문의에 답할 수 있다.
 * ⚠️ 종류·라벨·설명·잠금/동의 여부는 서버가 준다(알림함 칩과 같은 표).
 * ⚠️ PaceERA 는 스위치 35개를 감당하려고 카테고리로 묶었다. 무용연은 종류가 10여 개라
 *    묶는 층을 더 만들지 않는다 — 서버에 없는 층을 앱이 만들면 표가 둘이 된다.
 *    대신 같은 모양(행 → 상세에서 토글)은 그대로 따른다.
 */

/**
 * 목록과 상세가 **같은 상태를 본다.** 각자 서버를 부르면 상세에서 끈 것이 목록의
 * 요약(켜짐/꺼짐)에 안 비친다(PaceERA·iOS 와 같은 이유).
 */
@Stable
class NotificationPrefsState {
    var pushEnabled by mutableStateOf(true)
        private set
    var categories by mutableStateOf<List<NotiPrefCategory>>(emptyList())
        private set

    /** 로드 전에는 토글을 그리지 않는다 — 기본값(켜짐)으로 보였다가 서버 값으로 꺼지면,
     *  그 사이에 누른 토글이 반대 값으로 저장된다(PaceERA 2026-08-06 과 같은 사고). */
    var loaded by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    suspend fun load(api: NotificationApi) {
        api.prefs()
            .onSuccess { apply(it); loaded = true }
            .onFailure { if (categories.isEmpty()) error = "알림 설정을 불러오지 못했어요."; loaded = true }
    }

    /** 낙관적 반영 후 서버에 쓴다. 실패하면 되돌린다 —
     *  안 되돌리면 화면은 껐다는데 알림은 계속 온다. */
    suspend fun setPushEnabled(api: NotificationApi, on: Boolean) {
        val prev = pushEnabled
        pushEnabled = on
        api.updatePrefs(JSONObject().put("pushEnabled", on))
            .onSuccess { apply(it) }
            .onFailure { pushEnabled = prev; error = "저장하지 못했어요." }
    }

    suspend fun setCategory(api: NotificationApi, key: String, on: Boolean) {
        val before = categories
        categories = categories.map { if (it.key == key) it.copy(enabled = on) else it }
        val body = JSONObject().put("categories", JSONObject().put(key, on))
        api.updatePrefs(body)
            .onSuccess { apply(it) }
            .onFailure { categories = before; error = "저장하지 못했어요." }
    }

    private fun apply(p: NotiPrefs) {
        pushEnabled = p.pushEnabled
        categories = p.categories
    }

    fun isOn(c: NotiPrefCategory): Boolean = if (c.locked) true else c.enabled && pushEnabled

    /** 행 오른쪽 요약 — 왜 못 끄는지/왜 꺼져 있는지를 한 낱말로. */
    fun summary(c: NotiPrefCategory): Pair<String, Boolean> = when {
        c.locked -> "필수" to false
        !pushEnabled -> "전체 꺼짐" to false
        c.enabled -> "켜짐" to true
        c.optIn -> "동의 안 함" to false
        else -> "꺼짐" to false
    }
}

@Composable
fun NotificationSettingsScreen(
    api: NotificationApi,
    onClose: () -> Unit,
    /** '맞춤 알림'(관심 조건) 진입. 아직 웹 화면이라 호출부가 이동을 맡는다 — null 이면 줄 자체를 감춘다. */
    onOpenAlerts: (() -> Unit)? = null,
) {
    val state = remember { NotificationPrefsState() }
    val scope = rememberCoroutineScope()
    var detailKey by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { state.load(api) }

    // 상세에서 시스템 뒤로가기는 목록으로 — 화면을 닫아 버리면 한 단계를 건너뛴다.
    BackHandler(enabled = detailKey != null) { detailKey = null }

    val detail = detailKey?.let { k -> state.categories.firstOrNull { it.key == k } }
    if (detail != null) {
        NotificationCategoryDetail(state, api, detail, onBack = { detailKey = null })
        return
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.groupedBg)) {
        QuoteNavBar(title = "알림 설정", onBack = onClose)

        if (!state.loaded) {
            Box(Modifier.weight(1f).fillMaxWidth(), Alignment.Center) {
                CircularProgressIndicator(color = MuyeonColors.primary)
            }
            return@Column
        }

        LazyColumn(Modifier.fillMaxSize()) {
            item(key = "permission") { PermissionBanner() }

            item(key = "master") {
                SettingsRow(
                    title = "푸시 알림 받기",
                    desc = "꺼도 알림함에는 그대로 쌓여요.",
                    trailing = {
                        Switch(
                            checked = state.pushEnabled,
                            onCheckedChange = { v -> scope.launch { state.setPushEnabled(api, v) } },
                            colors = SwitchDefaults.colors(checkedTrackColor = MuyeonColors.primary),
                        )
                    },
                )
            }

            item(key = "header") {
                Text(
                    "종류별",
                    Modifier.padding(start = 20.dp, top = 20.dp, bottom = 6.dp),
                    fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp, color = MuyeonColors.textSub,
                )
            }

            items(state.categories, key = { "cat-" + it.key }) { c ->
                val (summary, highlighted) = state.summary(c)
                SettingsRow(
                    title = c.label,
                    // 무엇이 들어 있는지 한 줄로 — 들어가 보지 않아도 알게.
                    desc = c.desc,
                    onClick = { detailKey = c.key },
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                summary,
                                fontFamily = customFontFamily, fontSize = 13.sp,
                                color = if (highlighted) MuyeonColors.primary else MuyeonColors.secondary,
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null, tint = MuyeonColors.secondary,
                            )
                        }
                    },
                )
            }

            // 이 화면은 '어떤 알림을 푸시로 받을지'만 정한다. "대타만 받고 싶다"의 나머지 절반 —
            //  어떤 새 공고를 알림으로 받을지 — 은 관심 조건에 있다. 두 화면이 떨어져 있어
            //  한쪽만 끄고 "왜 계속 오지" 하던 자리라, 여기서 바로 건너갈 길을 둔다.
            if (onOpenAlerts != null) {
                item(key = "alerts-header") {
                    Text(
                        "맞춤 알림",
                        Modifier.padding(start = 20.dp, top = 20.dp, bottom = 6.dp),
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp, color = MuyeonColors.textSub,
                    )
                }
                item(key = "alerts") {
                    SettingsRow(
                        title = "관심 조건 알림",
                        desc = "받고 싶은 공고 종류(채용·대타·캐스팅)와 지역·장르·수업 대상을 정해요.",
                        onClick = onOpenAlerts,
                        trailing = {
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null, tint = MuyeonColors.secondary,
                            )
                        },
                    )
                }
            }
        }
    }

    ErrorToastArea(state)
}

/** 종류 하나의 상세 — 스위치와, 무슨 알림이 여기로 오는지. */
@Composable
private fun NotificationCategoryDetail(
    state: NotificationPrefsState,
    api: NotificationApi,
    c: NotiPrefCategory,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().background(MuyeonColors.groupedBg)) {
        QuoteNavBar(title = c.label, onBack = onBack)

        SettingsRow(
            title = if (c.optIn) "${c.label} 받기(수신 동의)" else "${c.label} 받기",
            trailing = {
                Switch(
                    checked = state.isOn(c),
                    // 끌 수 없는 종류는 화면에서도 못 만지게 한다(서버도 무시한다).
                    enabled = !c.locked && state.pushEnabled,
                    onCheckedChange = { v -> scope.launch { state.setCategory(api, c.key, v) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = MuyeonColors.primary),
                )
            },
        )
        Text(
            footerText(state, c),
            Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 17.sp,
            color = MuyeonColors.textSub,
        )

        Text(
            "이런 알림이 와요",
            Modifier.padding(start = 20.dp, top = 12.dp, bottom = 6.dp),
            fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp, color = MuyeonColors.textSub,
        )
        SettingsRow(title = c.desc)
    }

    ErrorToastArea(state)
}

private fun footerText(state: NotificationPrefsState, c: NotiPrefCategory): String = when {
    c.locked -> "놓치면 곤란한 알림이라 끌 수 없어요. (지원 결과·계정·소속 안내 등)"
    !state.pushEnabled -> "전체 푸시가 꺼져 있어 이 설정은 지금 적용되지 않아요."
    c.optIn -> "켜면 광고성 정보 수신에 동의하는 거예요. 언제든 다시 끌 수 있어요."
    else -> "꺼도 알림함에는 그대로 쌓여요. 푸시만 오지 않아요."
}

/** 목록/상세 공용 행 — 제목 + 설명 + 우측 슬롯. */
@Composable
private fun SettingsRow(
    title: String,
    desc: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth()
            .background(MuyeonColors.surface)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                fontFamily = customFontFamily, fontSize = 15.sp, lineHeight = 20.sp,
                color = MuyeonColors.textHead,
            )
            desc?.takeIf { it.isNotEmpty() }?.let {
                Text(
                    it,
                    fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 17.sp,
                    color = MuyeonColors.textSub, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.let { Spacer(Modifier.width(10.dp)); it() }
    }
    HorizontalDivider(color = MuyeonColors.border)
}

/**
 * 기기 알림 권한이 꺼져 있으면 앱 안에서 뭘 켜도 소용없다. 그것부터 알려준다.
 *  시스템 설정에 다녀오면 ON_RESUME 으로 다시 확인해 배너가 저절로 사라진다
 *  (PaceERA `PermissionDeniedBanner` 와 같은 동작).
 */
@Composable
private fun PermissionBanner() {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(NotificationManagerCompat.from(ctx).areNotificationsEnabled()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    if (granted) return

    Row(
        Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MuyeonColors.primary.copy(alpha = 0.10f))
            .clickable {
                val i = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                if (ctx !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { ctx.startActivity(i) }
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.NotificationsOff, contentDescription = null, tint = MuyeonColors.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "기기에서 알림이 꺼져 있어요",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp, color = MuyeonColors.textHead,
            )
            Text(
                "아래 설정을 켜도 푸시가 오지 않아요.",
                fontFamily = customFontFamily, fontSize = 12.sp, color = MuyeonColors.textSub,
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier.clip(RoundedCornerShape(50)).background(MuyeonColors.primary)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                "설정 열기",
                fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp, color = Color.White,
            )
        }
    }
}

/** 저장 실패는 화면에 남긴다 — 조용히 넘기면 누른 적이 없는 것처럼 보인다. */
@Composable
private fun ErrorToastArea(state: NotificationPrefsState) {
    val ctx = LocalContext.current
    LaunchedEffect(state.error) {
        state.error?.let {
            android.widget.Toast.makeText(ctx, it, android.widget.Toast.LENGTH_SHORT).show()
            state.error = null
        }
    }
}

/** 알림함 우측 톱니 진입점. */
class NotificationSettingsActivity : ComponentActivity() {

    companion object {
        fun start(context: Context) {
            val i = Intent(context, NotificationSettingsActivity::class.java)
            if (context !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val api = remember {
                NotificationApi(TokenManager.getAccessToken(this), ActiveRole.current(this))
            }
            NotificationSettingsScreen(
                api = api,
                onClose = { finish() },
                // 관심 조건은 아직 웹 화면 — 네이티브를 닫으면서 웹을 그 경로로 보낸다.
                onOpenAlerts = { NativeWebRoute.openWebAndFinish(this, "/alerts") },
            )
        }
    }
}
