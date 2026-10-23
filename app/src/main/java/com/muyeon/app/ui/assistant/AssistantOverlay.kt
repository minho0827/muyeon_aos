package com.muyeon.app.ui.assistant

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.offset
import com.muyeon.app.theme.AppColor
import com.muyeon.app.ui.floating.FloatingState
import com.muyeon.app.utils.TokenManager
import com.muyeon.app.webview.ActiveRole
import kotlin.math.roundToInt

/**
 * AI 도우미 플로팅 버튼 — 앱의 **모든 Activity 위**에 뜬다(iOS AssistantFloatingController 대응).
 *  - Application 의 Activity 생명주기 콜백으로, 화면이 앞에 올 때 그 화면의 content(FrameLayout) 맨 위에
 *    버튼용 ComposeView 를 한 번 붙인다. 버튼 밖은 터치가 아래 화면으로 그대로 간다(ComposeView 는 버튼 영역만 그린다).
 *  - 창(빠른 이동 검색)은 같은 ComposeView 안의 ModalBottomSheet — 새 Activity·새 웹뷰를 만들지 않는다
 *    (새 웹뷰가 생기면 웹 콜백 대상이 흔들린다 — 플랜 3-1).
 *  - 기존 채팅 플로팅(아래 216dp) 위, 기본 아래 286dp. 위아래로 끌어 옮기고, 길게 누르면 이번 실행 동안 숨긴다.
 *  - 숨김: 로그인 전·스플래시·로그인 화면, 웹 인증 라우트, 웹 모달(setFloatingHidden), 키보드 표시 중.
 */
object AssistantOverlay {
    private const val TAG_VIEW = "muyeon.assistant.overlay"
    private const val PREFS = "muyeon.assistant"
    private const val KEY_Y = "fabBottomDp"
    private val SKIP = setOf("SplashActivity", "LoginActivity", "IpConnectActivity", "QrScannerActivity", "ImageViewerActivity")

    /** 이번 실행 동안 숨김(길게 눌러 숨기기). 앱을 다시 켜면 다시 보인다. */
    var hiddenThisRun by mutableStateOf(false)
        private set

    /** 버튼 위치(아래에서 dp) — 모든 화면이 같은 값을 본다. */
    private var bottomDp by mutableFloatStateOf(286f)

    /** 기능 목록 캐시(유형별) — 창을 다시 열 때 바로 보여 주고 뒤에서 새로 받는다. */
    internal val cache = mutableMapOf<String, AssistantCatalog>()

    fun start(app: Application) {
        bottomDp = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(KEY_Y, 286f)
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) = attach(activity)
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    private fun attach(activity: Activity) {
        if (activity !is ComponentActivity) return
        if (activity.javaClass.simpleName in SKIP) return
        val content = activity.findViewById<FrameLayout?>(android.R.id.content) ?: return
        if (content.findViewWithTag<ComposeView>(TAG_VIEW) != null) return
        val view = ComposeView(activity).apply {
            tag = TAG_VIEW
            setContent { AssistantLayer(activity) }
        }
        content.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    internal fun hide() { hiddenThisRun = true }

    internal fun saveY(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putFloat(KEY_Y, bottomDp).apply()
    }

    internal fun moveBy(dyDp: Float, maxDp: Float) {
        bottomDp = (bottomDp - dyDp).coerceIn(120f, maxDp)
    }

    internal val currentBottomDp get() = bottomDp
}

@Composable
private fun AssistantLayer(activity: Activity) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    // 웹 라우트·웹 모달 상태는 웹뷰 화면에서만 의미가 있다(네이티브 화면은 웹 상태와 무관하게 보인다).
    val onWeb = activity is com.muyeon.app.webview.WebViewActivity
    val loggedIn = !TokenManager.getAccessToken(context).isNullOrEmpty()
    val show = loggedIn && !AssistantOverlay.hiddenThisRun && !imeVisible && !open &&
        !(onWeb && (FloatingState.isAuthRoute || FloatingState.floatingHidden))

    Box(Modifier.fillMaxSize()) {
        if (show) AssistantFab(Modifier.align(Alignment.BottomEnd), onOpen = { open = true })
    }
    if (open) {
        AssistantSheet(
            onDismiss = { open = false },
            onPick = { item ->
                open = false
                when (AssistantExecutor.run(activity, item)) {
                    AssistantExecutor.Outcome.Opened -> Unit
                    AssistantExecutor.Outcome.NeedsUpdate -> Toast.makeText(activity, "앱을 업데이트하면 쓸 수 있어요.", Toast.LENGTH_SHORT).show()
                    // 3단계(AI 작성 대화)에서 연결한다. 서버 스위치가 꺼져 있으면 목록에 흐름 자체가 오지 않는다.
                    is AssistantExecutor.Outcome.Flow -> Toast.makeText(activity, "AI 작성 도우미를 곧 열어 드릴게요.", Toast.LENGTH_SHORT).show()
                }
            },
        )
    }
}

@Composable
private fun AssistantFab(modifier: Modifier, onOpen: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var menu by remember { mutableStateOf(false) }
    val screenDp = with(density) { context.resources.displayMetrics.heightPixels.toDp().value }
    Box(
        modifier
            .padding(end = 16.dp)
            .offset { IntOffset(0, -with(density) { AssistantOverlay.currentBottomDp.dp.roundToPx() }) }
            .size(52.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.5.dp, AppColor.Primary, CircleShape)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { AssistantOverlay.saveY(context) },
                    onVerticalDrag = { change, dy ->
                        change.consume()
                        AssistantOverlay.moveBy(with(density) { dy.toDp().value }, screenDp - 140f)
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onOpen() },
                    onLongPress = { menu = true },
                )
            }
            .testTag("assistant.floating"),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = "AI 도우미", tint = AppColor.Primary, modifier = Modifier.size(22.dp))
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("이번에는 숨기기") }, onClick = { menu = false; AssistantOverlay.hide() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssistantSheet(onDismiss: () -> Unit, onPick: (AssistantItem) -> Unit) {
    val context = LocalContext.current
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(AssistantOverlay.cache[ActiveRole.current(context)]?.items ?: emptyList()) }
    var config by remember { mutableStateOf<AssistantConfig?>(null) }
    var loading by remember { mutableStateOf(items.isEmpty()) }
    var failed by remember { mutableStateOf(false) }
    val logs = remember { mutableStateListOf<Pair<String, String?>>() }
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        val catalog = AssistantApi.catalog(context)
        config = AssistantApi.config(context)
        loading = false
        if (catalog != null) {
            AssistantOverlay.cache[catalog.activeType] = catalog
            items = catalog.items
        } else if (items.isEmpty()) failed = true
        runCatching { focus.requestFocus() }
    }

    fun close() {
        val q = query.trim()
        if (q.isNotEmpty()) logs.add(q to null)
        AssistantApi.logSearches(context, logs.toList())
        onDismiss()
    }

    fun pick(item: AssistantItem) {
        val q = query.trim()
        if (q.isNotEmpty()) logs.add(q to item.id)
        AssistantApi.logSearches(context, logs.toList())
        onPick(item)
    }

    ModalBottomSheet(onDismissRequest = { close() }, sheetState = state, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = AppColor.Primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("무엇을 도와드릴까요?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF101116))
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { close() }) { Icon(Icons.Filled.Close, contentDescription = "닫기", tint = Color(0xFF6D6E71)) }
            }
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).focusRequester(focus).testTag("assistant.search"),
                placeholder = { Text("가고 싶은 메뉴를 검색해 보세요 (예: 레슨, 학원)", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { AssistantSearch.search(items, query).firstOrNull()?.let { pick(it) } }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF5F5F5), unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                ),
            )
            HorizontalDivider(color = Color(0xFFEAEAEA))
            val trimmed = query.trim()
            LazyColumn(Modifier.fillMaxWidth().height(520.dp)) {
                when {
                    loading -> item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColor.Primary) } }
                    failed -> item { EmptyText("목록을 불러오지 못했어요. 잠시 후 다시 열어 주세요.") }
                    trimmed.isEmpty() -> {
                        val flows = items.filter { it.kind == "FLOW" }
                        if (flows.isNotEmpty()) {
                            item { SectionTitle("AI 작성 도우미") }
                            item {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp)) {
                                    items(flows, key = { it.id }) { f ->
                                        val locked = config?.flows?.firstOrNull { it.id == f.flow }?.locked ?: true
                                        Row(
                                            Modifier.clip(CircleShape).background(AppColor.Primary.copy(alpha = 0.1f)).clickable { pick(f) }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = AppColor.Primary, modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(f.title.removePrefix("AI "), color = AppColor.Primary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                            if (locked) {
                                                Spacer(Modifier.width(4.dp))
                                                Icon(Icons.Filled.Lock, contentDescription = "프로 전용", tint = AppColor.Primary, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item { SectionTitle("자주 쓰는 메뉴") }
                        items(items.filter { it.kind != "FLOW" }.take(8), key = { it.id }) { ItemRow(it) { pick(it) } }
                    }
                    else -> {
                        val results = AssistantSearch.search(items, trimmed)
                        if (results.isEmpty()) item { EmptyText("'$trimmed'에 맞는 메뉴가 없어요.\n다른 말로 검색해 보세요.") }
                        else items(results, key = { it.id }) { ItemRow(it) { pick(it) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(t: String) {
    Text(t, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6D6E71), modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp))
}

@Composable
private fun EmptyText(t: String) {
    Text(t, fontSize = 14.sp, color = Color(0xFF6D6E71), modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 24.dp, end = 24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
}

@Composable
private fun ItemRow(item: AssistantItem, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 10.dp).testTag("assistant.item.${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(AppColor.Primary.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.AutoAwesome.takeIf { item.kind == "FLOW" } ?: Icons.Filled.Search, contentDescription = null, tint = AppColor.Primary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF101116))
            Text(item.subtitle ?: item.section, fontSize = 12.sp, color = Color(0xFF6D6E71), maxLines = 1)
        }
        if (item.proOnly) {
            Text("프로", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(CircleShape).background(AppColor.Primary).padding(horizontal = 6.dp, vertical = 2.dp))
            Spacer(Modifier.width(6.dp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFFBFBFBF))
    }
}
