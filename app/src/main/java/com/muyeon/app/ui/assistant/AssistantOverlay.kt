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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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

    /** 버튼 위치(아래에서 dp) — 웹 화면(채팅 버튼 위)과 네이티브 화면(채팅 버튼 없음 → 구석 가까이)을 따로 기억한다(10-23:
     *  네이티브 카드 화면에서 오른쪽 아래 카드를 가렸다). */
    private var bottomDp by mutableFloatStateOf(286f)
    private var nativeBottomDp by mutableFloatStateOf(NATIVE_DEFAULT)
    private const val NATIVE_DEFAULT = 28f
    private const val KEY_Y_NATIVE = "fabBottomDpNative"

    /** 기능 목록 캐시(유형별) — 창을 다시 열 때 바로 보여 주고 뒤에서 새로 받는다. */
    internal val cache = mutableMapOf<String, AssistantCatalog>()

    /** 자주 묻는 질문 캐시(앱 실행 중 한 번). */
    internal var faqCache: List<AssistantFaq>? = null

    /** 인기 검색어 캐시(유형별). */
    internal val popularCache = mutableMapOf<String, List<String>>()

    fun start(app: Application) {
        bottomDp = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(KEY_Y, 286f)
        nativeBottomDp = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(KEY_Y_NATIVE, NATIVE_DEFAULT)
        AssistantBadges.start(app)
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                attach(activity)
                if (activity.javaClass.simpleName !in SKIP) AssistantBadges.onScreenResumed(activity)
            }
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
            // 앱 글꼴(Pretendard) — 이 화면 안의 모든 글자가 따르게 기본 글자 모양을 바꾼다(iOS 와 같은 글꼴).
            setContent {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.material3.LocalTextStyle provides androidx.compose.material3.LocalTextStyle.current.copy(fontFamily = com.muyeon.app.theme.customFontFamily),
                ) { AssistantLayer(activity) }
            }
        }
        content.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
    }

    internal fun hide() { hiddenThisRun = true }

    internal fun saveY(context: Context, onWeb: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putFloat(if (onWeb) KEY_Y else KEY_Y_NATIVE, if (onWeb) bottomDp else nativeBottomDp).apply()
    }

    internal fun moveBy(dyDp: Float, maxDp: Float, onWeb: Boolean) {
        if (onWeb) bottomDp = (bottomDp - dyDp).coerceIn(120f, maxDp)
        else nativeBottomDp = (nativeBottomDp - dyDp).coerceIn(16f, maxDp)
    }

    internal fun bottomFor(onWeb: Boolean) = if (onWeb) bottomDp else nativeBottomDp
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
        if (show) AssistantFab(Modifier.align(Alignment.BottomEnd).then(if (onWeb) Modifier else Modifier.navigationBarsPadding()), onWeb = onWeb, onOpen = { open = true })
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
private fun AssistantFab(modifier: Modifier, onWeb: Boolean, onOpen: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var menu by remember { mutableStateOf(false) }
    val screenDp = with(density) { context.resources.displayMetrics.heightPixels.toDp().value }
    Box(
        modifier
            .padding(end = 16.dp)
            .offset { IntOffset(0, -with(density) { AssistantOverlay.bottomFor(onWeb).dp.roundToPx() }) }
            .size(52.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.5.dp, AppColor.Primary, CircleShape)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { AssistantOverlay.saveY(context, onWeb) },
                    onVerticalDrag = { change, dy ->
                        change.consume()
                        AssistantOverlay.moveBy(with(density) { dy.toDp().value }, screenDp - 140f, onWeb)
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
        Icon(Icons.Filled.AutoAwesome, contentDescription = if (AssistantBadges.total > 0) "AI 도우미, 새 소식 있음" else "AI 도우미",
            tint = AppColor.Primary, modifier = Modifier.size(22.dp))
        // 안읽음(알림·채팅·받은 견적)이 있으면 빨간 점 — 소켓으로 실시간(iOS 와 같음).
        if (AssistantBadges.total > 0) {
            Box(Modifier.align(Alignment.TopEnd).padding(top = 3.dp, end = 3.dp).size(11.dp).clip(CircleShape)
                .background(Color.White).padding(2.dp).clip(CircleShape).background(Color(0xFFFF3B30)).testTag("assistant.floating.dot"))
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(text = { Text("이번에는 숨기기") }, onClick = { menu = false; AssistantOverlay.hide() })
        }
    }
}

/**
 * 빠른 이동 검색 화면(전체 화면) — 시안: 뒤로·검색창·돋보기 / 최근 검색어 칩 / 인기 검색어 1~10.
 *  ★ 키보드를 자동으로 올리지 않는다(사용자 요청 10-23) — 검색창을 눌렀을 때만.
 *  새 Activity·웹뷰 없이 같은 ComposeView 의 Dialog 로 띄운다(웹 콜백 대상이 흔들리지 않게).
 */
@Composable
private fun AssistantSheet(onDismiss: () -> Unit, onPick: (AssistantItem) -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var items by remember { mutableStateOf(AssistantOverlay.cache[ActiveRole.current(context)]?.items ?: emptyList()) }
    var config by remember { mutableStateOf<AssistantConfig?>(null) }
    var popular by remember { mutableStateOf(AssistantOverlay.popularCache[ActiveRole.current(context)] ?: emptyList()) }
    var recent by remember { mutableStateOf(AssistantRecent.load(context)) }
    var loading by remember { mutableStateOf(items.isEmpty()) }
    var failed by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(ResultTab.ALL) }
    var searching by remember { mutableStateOf(false) }
    var sections by remember { mutableStateOf(AssistantOverlay.cache[ActiveRole.current(context)]?.sections ?: emptyList()) }
    var recentMenus by remember { mutableStateOf(AssistantRecent.loadMenus(context)) }
    var faqs by remember { mutableStateOf(AssistantOverlay.faqCache ?: emptyList()) }
    val logs = remember { mutableStateListOf<Pair<String, String?>>() }

    LaunchedEffect(Unit) {
        if (faqs.isEmpty()) AssistantApi.faqs()?.let { AssistantOverlay.faqCache = it; faqs = it }
        val catalog = AssistantApi.catalog(context)
        loading = false
        if (catalog != null) {
            AssistantOverlay.cache[catalog.activeType] = catalog
            items = catalog.items
            sections = catalog.sections
        } else if (items.isEmpty()) failed = true
        AssistantApi.popular(context)?.let { (type, list) ->
            AssistantOverlay.popularCache[type] = list
            popular = list
        }
        config = AssistantApi.config(context)
    }

    fun close() {
        val q = query.trim()
        if (q.isNotEmpty()) logs.add(q to null)
        AssistantApi.logSearches(context, logs.toList())
        AssistantBadges.refreshSoon()
        onDismiss()
    }

    fun pick(item: AssistantItem) {
        val q = query.trim()
        if (q.isNotEmpty()) {
            logs.add(q to item.id)
            recent = AssistantRecent.add(context, q)
        }
        AssistantApi.logSearches(context, logs.toList())
        if (item.kind != "FLOW") AssistantRecent.addMenu(context, item.id)
        onPick(item)
    }

    fun submit() {
        val q = query.trim()
        if (q.isEmpty()) return
        recent = AssistantRecent.add(context, q)
        AssistantSearch.search(items, q).firstOrNull()?.let { pick(it) }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { if (searching) { searching = false; query = "" } else close() },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        if (!searching) {
            AllMenuPage(
                items = items, sections = sections, recentIds = recentMenus, loading = loading, failed = failed,
                onClose = { close() },
                onHome = { pick(HOME_ITEM) },
                onSearch = { searching = true },
                onPick = { pick(it) },
            )
            return@Dialog
        }
        androidx.activity.compose.BackHandler { searching = false; query = "" }
        Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding().imePadding()) {
            // ★ 전체 메뉴 화면과 같은 자리·같은 모양의 검색 상자(사용자 요청 10-23, iOS 와 1:1) — 누른 칸이 그대로 입력칸이 된다.
            val searchFocus = remember { FocusRequester() }
            var searchFocused by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { delay(250); runCatching { searchFocus.requestFocus() } } // 검색 상자를 직접 눌러 들어왔으니 바로 입력
            Box(Modifier.fillMaxWidth().height(56.dp)) {
                IconButton(onClick = { searching = false; query = "" }, modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = Color(0xFF101116))
                }
                Text("검색", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF101116), modifier = Modifier.align(Alignment.Center))
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(14.dp))
                    .border(1.dp, if (searchFocused) AppColor.Primary else Color(0xFFE3E4E8), RoundedCornerShape(14.dp))
                    .padding(start = 16.dp, end = 8.dp).height(52.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Text("메뉴, 기능을 검색해 보세요", fontSize = 16.sp, color = Color(0xFFA8AAB0), maxLines = 1)
                    androidx.compose.foundation.text.BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = Color(0xFF101116), fontFamily = com.muyeon.app.theme.customFontFamily),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(AppColor.Primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { submit() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(searchFocus)
                            .onFocusChanged { searchFocused = it.isFocused }.testTag("assistant.search"),
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Cancel, contentDescription = "지우기", tint = Color(0xFFC4C6CC)) }
                }
                IconButton(onClick = { submit() }) {
                    Icon(Icons.Filled.Search, contentDescription = "검색", tint = if (query.isEmpty()) Color(0xFFB0B3B9) else Color(0xFF101116))
                }
            }
            val trimmed = query.trim()
            LazyColumn(Modifier.fillMaxSize()) {
                if (trimmed.isNotEmpty()) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp)) {
                            items(ResultTab.entries, key = { it.name }) { t ->
                                val on = t == tab
                                Text(t.label, fontSize = 16.sp, color = if (on) Color.White else Color(0xFF6D6E71),
                                    modifier = Modifier.clip(CircleShape).background(if (on) Color(0xFF101116) else Color(0xFFF2F3F5))
                                        .clickable { tab = t }.padding(horizontal = 18.dp, vertical = 9.dp))
                            }
                        }
                    }
                    when {
                        loading -> item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColor.Primary) } }
                        failed -> item { EmptyText("목록을 불러오지 못했어요. 잠시 후 다시 열어 주세요.") }
                        else -> {
                            val menus = AssistantSearch.search(items, trimmed)
                            val faqHits = AssistantSearch.searchFaqs(faqs, trimmed)
                            val showMenus = tab != ResultTab.FAQ
                            val showFaqs = tab != ResultTab.MENU
                            if ((!showMenus || menus.isEmpty()) && (!showFaqs || faqHits.isEmpty())) {
                                item { EmptyText("'$trimmed'에 맞는 결과가 없어요.\n다른 말로 검색해 보세요.") }
                            }
                            if (showMenus && menus.isNotEmpty()) {
                                item { CountTitle("메뉴", menus.size) }
                                items(menus, key = { it.id }) { ItemRow(it, trimmed) { pick(it) } }
                            }
                            if (showMenus && showFaqs && menus.isNotEmpty() && faqHits.isNotEmpty()) {
                                item { Box(Modifier.fillMaxWidth().padding(top = 16.dp).height(10.dp).background(Color(0xFFF5F6F8))) }
                            }
                            if (showFaqs && faqHits.isNotEmpty()) {
                                item { CountTitle("고객센터", faqHits.size) }
                                items(faqHits.take(if (tab == ResultTab.FAQ) 30 else 5), key = { "faq${it.id}" }) { f ->
                                    FaqRow(f, trimmed) { pick(FAQ_ITEM.copy(id = "faq.${f.id}")) }
                                }
                                item {
                                    Text("더 궁금한 것이 있다면?  ›", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = AppColor.Primary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(20.dp).clip(RoundedCornerShape(14.dp))
                                            .background(AppColor.Primary.copy(alpha = 0.08f)).clickable { pick(INQUIRY_ITEM) }.padding(vertical = 16.dp))
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                    return@LazyColumn
                }
                // AI 작성 도우미(서버 스위치가 켜졌을 때만 온다)
                val flows = items.filter { it.kind == "FLOW" }
                if (flows.isNotEmpty()) {
                    item { HeaderRow(Icons.Filled.AutoAwesome, AppColor.Primary, "AI 작성 도우미") }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp)) {
                            items(flows, key = { it.id }) { f ->
                                val locked = config?.flows?.firstOrNull { it.id == f.flow }?.locked ?: true
                                Pill(f.title.removePrefix("AI "), trailing = if (locked) Icons.Filled.Lock else null, onClick = { pick(f) })
                            }
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                    item { Box(Modifier.fillMaxWidth().height(10.dp).background(Color(0xFFF5F6F8))) }
                }
                // 최근 검색어
                item {
                    HeaderRow(Icons.Filled.Schedule, Color(0xFF101116), "최근 검색어") {
                        if (recent.isNotEmpty()) Text("모두 지우기", fontSize = 14.sp, color = Color(0xFF9A9CA3),
                            modifier = Modifier.clickable { recent = AssistantRecent.clear(context) }.padding(4.dp))
                    }
                }
                item {
                    if (recent.isEmpty()) {
                        Text("최근 검색어가 없어요.", fontSize = 14.sp, color = Color(0xFFA8AAB0), modifier = Modifier.padding(start = 20.dp, bottom = 4.dp))
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp)) {
                            items(recent, key = { it }) { r ->
                                Pill(r, trailing = Icons.Filled.Close, onClick = { query = r }, onTrailing = { recent = AssistantRecent.remove(context, r) })
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(28.dp)) }
                item { Box(Modifier.fillMaxWidth().height(10.dp).background(Color(0xFFF5F6F8))) }
                // 인기 검색어
                if (popular.isNotEmpty()) {
                    item { HeaderRow(Icons.Filled.MilitaryTech, Color(0xFFF5C344), "인기 검색어") }
                    items(popular.take(10).mapIndexed { i, q -> i to q }, key = { "p${it.first}" }) { (i, q) ->
                        val strong = i < 3
                        Row(
                            Modifier.fillMaxWidth().clickable { query = q }.padding(horizontal = 20.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${i + 1}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = if (strong) Color(0xFF101116) else Color(0xFF9A9CA3), modifier = Modifier.width(32.dp))
                            Text(q, fontSize = 17.sp, color = if (strong) Color(0xFF101116) else Color(0xFF9A9CA3), maxLines = 1)
                        }
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

/**
 * 전체 메뉴(시안: 삼성카드 전체 메뉴) — 뒤로·"전체 메뉴"·[홈] / 검색 상자(누르면 검색 화면) / 묶음 칩(접기·펴기) / 묶음별 메뉴 이름 목록.
 *  묶음·메뉴는 전부 서버(관리자 MY 메뉴 + 기능 목록)에서 온다. [최근] 은 이 기기에서 최근 연 메뉴.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun AllMenuPage(
    items: List<AssistantItem>,
    sections: List<AssistantSection>,
    recentIds: List<String>,
    loading: Boolean,
    failed: Boolean,
    onClose: () -> Unit,
    onHome: () -> Unit,
    onSearch: () -> Unit,
    onPick: (AssistantItem) -> Unit,
) {
    val byId = items.associateBy { it.id }
    val recent = recentIds.mapNotNull { byId[it] }
    // 묶음 순서: 서버 sections 순서(없으면 항목 등장 순서), [최근] 은 맨 앞.
    val secList = buildList {
        if (recent.isNotEmpty()) add(AssistantSection("__recent", "최근", "recent"))
        val keys = if (sections.isNotEmpty()) sections else items.map { it.section }.distinct().map { AssistantSection(it, it, "etc") }
        keys.filter { k -> items.any { it.section == k.key } }.forEach { add(it) }
    }
    val groups = secList.map { sec -> sec to (if (sec.key == "__recent") recent else items.filter { it.section == sec.key }) }
    // 목록에서 각 묶음 머리의 위치(칩 → 스크롤, 스크롤 → 칩 강조).
    val headerIndex = remember(groups) {
        var i = 0
        groups.map { (sec, list) -> (sec.key to i).also { i += 1 + list.size } }.toMap()
    }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // 목록을 받아 오는 사이 맨 위 묶음(최근 등)이 바뀌면 LazyColumn 이 예전 첫 줄을 붙잡아 아래로 밀린다 → 아직 손대기 전이면 맨 위로.
    var userScrolled by remember { mutableStateOf(false) }
    LaunchedEffect(listState.isScrollInProgress) { if (listState.isScrollInProgress) userScrolled = true }
    LaunchedEffect(secList.firstOrNull()?.key, items.size) { if (!userScrolled) listState.scrollToItem(0) }
    var expanded by remember { mutableStateOf(false) }
    val current by androidx.compose.runtime.remember(groups) {
        androidx.compose.runtime.derivedStateOf {
            // 끝까지 내렸으면 마지막 묶음 — 맨 아래 묶음(내 정보·설정)은 화면 위쪽까지 올라오지 못해 잡히지 않았다(10-23).
            if (!listState.canScrollForward && listState.firstVisibleItemIndex > 0) return@derivedStateOf secList.lastOrNull()?.key
            val first = listState.firstVisibleItemIndex
            headerIndex.entries.filter { it.value <= first }.maxByOrNull { it.value }?.key ?: secList.firstOrNull()?.key
        }
    }

    Column(Modifier.fillMaxSize().background(Color.White).statusBarsPadding().navigationBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "닫기", tint = Color(0xFF101116))
            }
            Text("전체 메뉴", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF101116), modifier = Modifier.align(Alignment.Center))
            Row(
                Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).clip(CircleShape).border(1.5.dp, Color(0xFF101116), CircleShape)
                    .clickable(onClick = onHome).padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Home, contentDescription = null, tint = AppColor.Primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("홈", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF101116))
            }
        }
        // 검색 상자 — 누르면 검색 화면(키보드는 거기서 칸을 눌렀을 때만)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(14.dp))
                .border(1.dp, Color(0xFFE3E4E8), RoundedCornerShape(14.dp)).clickable(onClick = onSearch)
                .padding(horizontal = 16.dp, vertical = 15.dp).testTag("assistant.searchBox"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("메뉴, 기능을 검색해 보세요", fontSize = 16.sp, color = Color(0xFFA8AAB0), modifier = Modifier.weight(1f))
            Icon(Icons.Filled.Search, contentDescription = "검색", tint = Color(0xFFB0B3B9))
        }
        // 묶음 칩 — 펼치면 여러 줄, 접으면 한 줄 가로 스크롤(지금 묶음 칩이 늘 보이게 자동으로 따라감). iOS 와 1:1.
        if (secList.isNotEmpty()) {
            val chipRow = androidx.compose.foundation.lazy.rememberLazyListState()
            // 펼치기 규칙(사용자 요청 10-23, iOS 와 1:1): 스크롤을 시작해도 접힌 채로 두고,
            //  지금 묶음의 칩이 접힌 한 줄에서 화면 밖으로 벗어났을 때만 펼친다. 맨 위로 돌아오면 다시 접는다.
            val atTop by androidx.compose.runtime.remember {
                androidx.compose.runtime.derivedStateOf { listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 8 }
            }
            LaunchedEffect(atTop) { if (atTop) expanded = false }
            LaunchedEffect(current) {
                if (atTop || expanded) return@LaunchedEffect
                val idx = secList.indexOfFirst { it.key == current }
                val info = chipRow.layoutInfo
                if (info.visibleItemsInfo.isEmpty()) return@LaunchedEffect // 칩 줄을 아직 재지 않았으면 판단하지 않는다
                val seen = info.visibleItemsInfo.firstOrNull { it.index == idx }
                val fullyVisible = seen != null && seen.offset >= info.viewportStartOffset && seen.offset + seen.size <= info.viewportEndOffset
                if (idx >= 0 && !fullyVisible) expanded = true
            }
            val chip: @Composable (AssistantSection) -> Unit = { sec ->
                val on = sec.key == current
                val hasBadge = groups.firstOrNull { it.first.key == sec.key }?.second?.any { AssistantBadges.count(it.badgeKey) > 0 } == true
                Row(
                    Modifier.clip(CircleShape).background(if (on) Color(0xFF101116) else Color(0xFFF2F3F5))
                        .clickable { scope.launch { listState.animateScrollToItem(headerIndex[sec.key] ?: 0) } }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(sec.title, fontSize = 15.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (on) Color.White else Color(0xFF6D6E71), maxLines = 1)
                    // 이 묶음에 안읽음이 있으면 빨간 점
                    if (hasBadge) { Spacer(Modifier.width(5.dp)); Box(Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFF3B30))) }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.Top) {
                if (expanded) {
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.weight(1f).padding(start = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) { secList.forEach { chip(it) } }
                } else {
                    LazyRow(
                        state = chipRow,
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 8.dp),
                    ) { items(secList, key = { it.key }) { chip(it) } }
                }
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.padding(end = 4.dp)) {
                    Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expanded) "접기" else "펼치기", tint = Color(0xFF6D6E71))
                }
            }
        }
        when {
            loading && items.isEmpty() -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColor.Primary) }
            failed && items.isEmpty() -> EmptyText("메뉴를 불러오지 못했어요. 잠시 후 다시 열어 주세요.")
            else -> LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                groups.forEach { (sec, list) ->
                    item(key = "h_${sec.key}") {
                        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 28.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(iconForKey(sec.icon), contentDescription = null, tint = AppColor.Primary, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(sec.title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color(0xFF101116))
                            }
                            HorizontalDivider(color = Color(0xFFEAEAEA), modifier = Modifier.padding(top = 14.dp))
                        }
                    }
                    items(list, key = { "${sec.key}_${it.id}" }) { item ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(item) }.padding(horizontal = 16.dp, vertical = 13.dp).testTag("assistant.menu.${item.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(item.title, fontSize = 17.sp, color = Color(0xFF101116))
                            if (item.proOnly) {
                                Spacer(Modifier.width(6.dp))
                                Text("프로", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clip(CircleShape).background(AppColor.Primary).padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Spacer(Modifier.weight(1f))
                            CountBadge(AssistantBadges.count(item.badgeKey))
                        }
                    }
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

/** 서버 묶음 아이콘 키 → 아이콘. 모르는 키는 기본 아이콘. */
private fun iconForKey(key: String) = when (key) {
    "recent" -> Icons.Filled.Schedule
    "ai" -> Icons.Filled.AutoAwesome
    "lesson" -> Icons.Filled.EventNote
    "quote" -> Icons.Filled.Description
    "job" -> Icons.Filled.Groups
    "resume" -> Icons.Filled.Badge
    "academy" -> Icons.Filled.Apartment
    "space" -> Icons.Filled.Home
    "membership" -> Icons.Filled.WorkspacePremium
    "chat" -> Icons.Filled.Forum
    "bell" -> Icons.Filled.Notifications
    "support" -> Icons.AutoMirrored.Filled.HelpOutline
    "settings" -> Icons.Filled.Settings
    else -> Icons.Filled.Apps
}

@Composable
private fun HeaderRow(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, title: String, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 28.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF101116))
        Spacer(Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun Pill(text: String, trailing: androidx.compose.ui.graphics.vector.ImageVector?, onClick: () -> Unit, onTrailing: (() -> Unit)? = null) {
    Row(
        Modifier.clip(CircleShape).border(1.dp, Color(0xFFE3E4E8), CircleShape).clickable(onClick = onClick)
            .padding(start = 16.dp, end = if (trailing != null) 10.dp else 16.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, fontSize = 15.sp, color = Color(0xFF101116), maxLines = 1)
        if (trailing != null) {
            Spacer(Modifier.width(6.dp))
            Icon(trailing, contentDescription = if (onTrailing != null) "삭제" else null, tint = Color(0xFF101116),
                modifier = Modifier.size(16.dp).then(if (onTrailing != null) Modifier.clickable(onClick = onTrailing) else Modifier))
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
private fun ItemRow(item: AssistantItem, query: String = "", onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp).testTag("assistant.item.${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0xFFF2F4F7)), contentAlignment = Alignment.Center) {
            Icon(sectionIcon(item), contentDescription = null, tint = AppColor.Primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(highlight(item.title, query), fontSize = 17.sp, color = Color(0xFF101116), maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                if (item.proOnly) {
                    Spacer(Modifier.width(6.dp))
                    Text("프로", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(CircleShape).background(AppColor.Primary).padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Spacer(Modifier.height(3.dp))
            Text("전체메뉴 → ${item.section} → ${item.title}", fontSize = 14.sp, color = Color(0xFF9A9CA3), maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        CountBadge(AssistantBadges.count(item.badgeKey))
    }
}

/** 숫자 뱃지(빨강, 99+). 0 이면 그리지 않는다. */
@Composable
private fun CountBadge(n: Int) {
    if (n <= 0) return
    Text(if (n > 99) "99+" else "$n", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier.defaultMinSize(minWidth = 20.dp).clip(CircleShape).background(Color(0xFFFF3B30)).padding(horizontal = 6.dp, vertical = 2.dp))
}

@Composable
private fun FaqRow(f: AssistantFaq, query: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0xFFF2F4F7)), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null, tint = Color(0xFF8A8D94), modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(highlight(f.question, query), fontSize = 17.sp, color = Color(0xFF101116), maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            if (f.answer.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(f.answer.replace(Regex("\\s+"), " "), fontSize = 14.sp, color = Color(0xFF9A9CA3), maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun CountTitle(label: String, n: Int) {
    Row(Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp)) {
        Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF101116))
        Spacer(Modifier.width(8.dp))
        Text("${n}건", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppColor.Primary)
    }
}

/** 검색어와 같은 부분을 주황색으로(초성 검색은 강조하지 않는다). */
private fun highlight(text: String, query: String) = androidx.compose.ui.text.buildAnnotatedString {
    val q = query.trim()
    val idx = if (q.isEmpty() || AssistantSearch.isChosungQuery(q)) -1 else text.lowercase().indexOf(q.lowercase())
    if (idx < 0) { append(text); return@buildAnnotatedString }
    append(text.substring(0, idx))
    withStyle(androidx.compose.ui.text.SpanStyle(color = AppColor.Primary, fontWeight = FontWeight.SemiBold)) { append(text.substring(idx, idx + q.length)) }
    append(text.substring(idx + q.length))
}

private enum class ResultTab(val label: String) { ALL("전체"), MENU("메뉴"), FAQ("고객센터") }

/** 자주 묻는 질문을 누르면 웹 FAQ, '더 궁금한 것이 있다면?'은 1:1 문의(웹). 실행기는 일반 웹 항목처럼 처리한다. */
private val HOME_ITEM = AssistantItem("home", "홈", null, "기타", "WEB", null, emptyMap(), "/home", null, false, emptyList(), emptyList())
private val FAQ_ITEM = AssistantItem("faq", "자주 묻는 질문", null, "고객센터", "WEB", null, emptyMap(), "/faq", null, false, emptyList(), emptyList())
private val INQUIRY_ITEM = AssistantItem("support.inquiry", "1:1 문의", null, "고객센터", "WEB", null, emptyMap(), "/support/inquiry", null, false, emptyList(), emptyList())

/** 묶음별 아이콘(iOS AssistantSheetView.icon 과 같은 구분). */
private fun sectionIcon(item: AssistantItem) = when (item.section) {
    "레슨" -> Icons.Filled.EventNote
    "견적" -> Icons.Filled.Description
    "구인" -> Icons.Filled.Groups
    "이력서" -> Icons.Filled.Badge
    "학원" -> Icons.Filled.Apartment
    "멤버십" -> Icons.Filled.WorkspacePremium
    "채팅" -> Icons.Filled.Forum
    "알림" -> Icons.Filled.Notifications
    "AI 도우미" -> Icons.Filled.AutoAwesome
    else -> Icons.Filled.Apps
}
