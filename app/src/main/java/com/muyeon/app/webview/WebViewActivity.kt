package com.muyeon.app.webview

import com.muyeon.app.ui.common.applyEdgeToEdge

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.data.repository.LocationRepositoryImpl
import com.muyeon.app.domain.use_cases.RequestNotificationPermissionUseCase
import com.muyeon.app.result.ResultKeys
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import com.muyeon.app.result.payloadJsonFor
import com.muyeon.app.result.resultKeys
import com.muyeon.app.result.resultPayload
import com.muyeon.app.theme.MuyeonTheme
import com.muyeon.app.ui.device_info.DeviceInfoViewModel
import com.muyeon.app.ui.device_info.DeviceInfoViewModelFactory
import com.muyeon.app.ui.imagepicker.FileViewModel
import com.muyeon.app.ui.imagepicker.ImagePickerBottomSheet
import com.muyeon.app.ui.notification.NotificationViewModel
import com.muyeon.app.ui.notification.NotificationViewModelFactory

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@RequiresExtension(extension = Build.VERSION_CODES.R, version = 2)
class WebViewActivity : ComponentActivity(), com.muyeon.app.result.ResultHost {
    private lateinit var webView: WebView
    private lateinit var deviceInfoViewModel: DeviceInfoViewModel
    private lateinit var locationWebViewInterface: LocationWebViewInterface

    private lateinit var downloadInterface: DownloadWebViewInterface
    private lateinit var notificationViewModel: NotificationViewModel
    private lateinit var fileInterface: FileWebViewInterface
    private lateinit var fileViewModel: FileViewModel
    private lateinit var scanQRInterface: ScanQRWebViewInterface

    // Dialog state
    private var showDialog by mutableStateOf(false)
    private var dialogTitle by mutableStateOf("")
    private var dialogContent by mutableStateOf<String?>(null)
    private var dialogLeftButtonText by mutableStateOf<String?>(null)
    private var dialogRightButtonText by mutableStateOf("")
    private var dialogButtonCount by mutableIntStateOf(1)
    private var dialogAlignment by mutableStateOf(ContentAlignment.Middle)
    private var dialogOnLeftClick: () -> Unit by mutableStateOf({})
    private var dialogOnRightClick: () -> Unit by mutableStateOf({})

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationViewModel.updatePermissionStatus(isGranted)
    }

    @Suppress("unused")
    private val mediaPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Handled in PermissionWebViewInterface */ }

    @Suppress("unused")
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Handled in PermissionWebViewInterface */ }

    private val filePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        fileInterface.handlePermissionResult(permissions)
    }

    /**
     * 네이티브 기능 화면(브릿지로 연 화면) 결과 — 닫히면 돌려준 키마다 웹에 재조회를 알린다.
     *  AppBridgeInterface·플로팅 등 이 액티비티에서 여는 화면은 전부 [launchForResult] 를 거친다
     *  (각 화면 companion start(...) → Context.launchScreen → ResultHost).
     */
    private val nativeResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        deliverResults(result.data.resultKeys(), result.data.resultPayload())
    }

    override fun launchForResult(intent: Intent) {
        nativeResultLauncher.launch(intent)
    }

    /**
     * ★ 2026-11-08 앱 로그인 화면(결과로 연 것) — 성공이면 이 창 저장소에 로그인을 넣고 다시 읽은 뒤 이어서 할 일(LoginResume)을 연다.
     *  실패·취소면 이어서 할 일을 버린다(웹 /login 에서 뒤로 나온 것과 같다).
     */
    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) { pendingAfterLogin = null; return@registerForActivityResult }
        WebLogin.applyNow(this, webView) { webView.reload() }
        onLoggedIn()
    }

    /** 소셜 로그인 창(카카오·네이버 웹 OAuth) — [startSocialLogin] 으로 열었을 때만. */
    private val socialProvider: com.muyeon.app.auth.LoginProvider? by lazy {
        intent.getStringExtra(EXTRA_SOCIAL)?.let { v -> com.muyeon.app.auth.LoginProvider.entries.firstOrNull { it.name == v } }
    }
    /** 소셜 로그인이 아직 안 끝났다 — 뒤로·취소면 창을 닫아 앱 로그인 화면으로 돌아간다. */
    private var socialPending = false
    /** 소셜 콜백(/oauth/kakao · /naverAuth)까지 갔다 — 그 뒤 /login 으로 오면 취소·실패다. */
    private var socialReturned = false
    private var socialClicked = false

    /**
     * 웹 로그인 주소 처리. 처리했으면 true.
     *  · 소셜 창: 첫 /login 은 그대로(버튼을 대신 누른다) · 콜백 뒤 /login = 취소·실패 → 창을 닫는다.
     *  · 보통 창: 웹이 /login 으로 가면 앱 로그인 화면을 띄우고 이 창은 이전 화면으로(첫 화면이면 닫는다).
     *    redirect 는 웹 주소창(location.search)에서, 강제 로그아웃 안내는 웹 sessionStorage(muyeon.logoutNotice)에서 읽는다.
     */
    private fun handleLoginRoute(path: String): Boolean {
        // ★ 2026-11-08 [b1] 가입 주소(/signupTerms 가입 모드 · /register · /social-signup)는 앱 가입 화면으로.
        if (com.muyeon.app.ui.auth.LoginActivity.isSignupPath(path)) { openSignupFromWeb(path); return true }
        // ★ [b1] 학원 인증 차단 · 시스템 점검 안내도 앱 화면 — 차단은 이 창 위에 띄운다(풀리면 VerifyGate 가 이 창에서 다시 묻는다), 점검은 이 창을 닫는다.
        if (path.trimEnd('/') == VerifyGate.GATE_PATH) { com.muyeon.app.ui.gate.GateActivity.startVerifyRequired(this); return true }
        if (path.trimEnd('/') == "/maintenance") { openMaintenanceFromWeb(); return true }
        if (socialPending) {
            if (path == "/oauth/kakao" || path == "/naverAuth") socialReturned = true
            if (com.muyeon.app.ui.auth.LoginActivity.isLoginPath(path)) {
                if (socialReturned) finish()
                return true
            }
            return false
        }
        if (!com.muyeon.app.ui.auth.LoginActivity.isLoginPath(path)) {
            webView.alpha = 1f
            return false
        }
        // ★ 2026-11-08 웹 로그인 화면이 비치지 않게 곧바로 가린다 — 앱 로그인 화면이 뜨고 이 창은 이전 화면으로(그때 다시 보인다)·닫힌다.
        webView.alpha = 0f
        val js = "(function(){var n=null;try{n=sessionStorage.getItem('muyeon.logoutNotice');sessionStorage.removeItem('muyeon.logoutNotice')}catch(e){}" +
            "var i=0;try{i=(history.state&&history.state.idx)||0}catch(e){}return JSON.stringify({s:location.search,n:n,i:i})})()"
        webView.evaluateJavascript(js) { raw ->
            if (isFinishing) return@evaluateJavascript
            val o = runCatching { org.json.JSONObject(org.json.JSONArray("[$raw]").getString(0)) }.getOrNull()
            val notice = o?.optString("n")?.takeIf { it.isNotBlank() && it != "null" }?.let { runCatching { org.json.JSONObject(it) }.getOrNull() }
            if (!com.muyeon.app.ui.auth.LoginActivity.isAlive) {
                com.muyeon.app.ui.auth.LoginActivity.start(
                    this,
                    com.muyeon.app.ui.auth.LoginActivity.redirectOf("/login" + (o?.optString("s").orEmpty())),
                    notice?.optString("code")?.ifBlank { "LOGOUT" }, notice?.optString("message"),
                )
            }
            // SPA 로 들어온 /login 이면 이전 화면으로, 문서째 연 /login(강제 로그아웃·401)이나 첫 화면이면 창을 닫는다.
            if ((o?.optInt("i") ?: 0) > 0) webView.evaluateJavascript("history.back()", null) else finish()
        }
        return true
    }

    /**
     * ★ [b1] 웹이 가입 주소로 갔다 — 웹 화면이 비치지 않게 가리고, 주소창 쿼리(redirect·mode)와 웹 저장소의 로그인(sessionId·이름)을 읽어
     *  앱 가입 화면([com.muyeon.app.ui.auth.LoginActivity.startSignup])을 연다. 약관 게이트(mode=gate)면 웹 그대로.
     *  /social-signup 은 이 창을 닫는다(뒤로 가면 소셜 콜백·웹 게이트가 또 돈다). 그 밖은 SPA 이동이면 이전 화면으로, 아니면 닫는다.
     */
    private fun openSignupFromWeb(path: String) {
        webView.alpha = 0f
        val js = "(function(){var a=null;try{a=JSON.parse(localStorage.getItem('muyeonAuth')||'null')}catch(e){}" +
            "var i=0;try{i=(history.state&&history.state.idx)||0}catch(e){}" +
            "return JSON.stringify({s:location.search,sid:(a&&a.sessionId)||'',n:(a&&a.user&&a.user.name)||'',i:i})})()"
        webView.evaluateJavascript(js) { raw ->
            if (isFinishing) return@evaluateJavascript
            val o = runCatching { org.json.JSONObject(org.json.JSONArray("[$raw]").getString(0)) }.getOrNull()
            val search = o?.optString("s").orEmpty()
            if (!com.muyeon.app.ui.auth.LoginActivity.isSignupPath(path, search.removePrefix("?"))) {
                webView.alpha = 1f
                return@evaluateJavascript
            }
            com.muyeon.app.ui.auth.LoginActivity.startSignup(
                this, path + search,
                sessionId = o?.optString("sid")?.takeIf { it.isNotBlank() },
                userName = o?.optString("n")?.takeIf { it.isNotBlank() },
            )
            val social = path.trimEnd('/') == "/social-signup"
            if (!social && (o?.optInt("i") ?: 0) > 0) webView.evaluateJavascript("history.back()", null) else finish()
        }
    }

    /** ★ [b1] 웹 점검 화면 → 앱 점검 화면(웹이 남긴 sessionStorage lastError 를 개발용 상자로 넘긴다). */
    private fun openMaintenanceFromWeb() {
        webView.alpha = 0f
        webView.evaluateJavascript("(function(){try{return sessionStorage.getItem('lastError')||''}catch(e){return ''}})()") { raw ->
            if (isFinishing) return@evaluateJavascript
            val err = runCatching { org.json.JSONArray("[$raw]").getString(0) }.getOrNull()?.takeIf { it.isNotBlank() && it != "null" }
            com.muyeon.app.ui.gate.GateActivity.startMaintenance(this, err)
            finish()
        }
    }

    /** 소셜 창 — 웹 로그인 화면의 그 버튼을 대신 누른다(웹이 state·redirect 를 만든다). 문서당 한 번, 버튼이 그려질 때까지 기다린다. */
    private fun clickSocialButton() {
        val p = socialProvider ?: return
        if (socialClicked || !socialPending) return
        socialClicked = true
        val label = getString(if (p == com.muyeon.app.auth.LoginProvider.NAVER) R.string.auth_login_naver else R.string.auth_login_kakao)
        val sel = org.json.JSONObject.quote("button[aria-label=\"$label\"]")
        webView.evaluateJavascript(
            "(function(){function f(){var b=document.querySelector($sel);if(b){b.click();return true}return false}" +
                "if(f())return;var o=new MutationObserver(function(){if(f())o.disconnect()});" +
                "o.observe(document.documentElement,{childList:true,subtree:true});setTimeout(function(){o.disconnect()},15000)})()",
            null,
        )
    }

    /**
     * 결과 키 → 웹. 키마다 __muyeonResult(없으면 레거시 __onX) 한 번씩(WebCallbacks.resultJs).
     *  채팅·알림이 바뀌었으면 플로팅 배지도 곧바로 다시 읽는다(폴링 대신).
     */
    /** 웹 브리지(액션 → 네이티브 화면). 로그인 후 이어서 열기에도 같은 처리를 쓴다. */
    private val appBridge by lazy { AppBridgeInterface(this, webView, onWebReady = ::onWebReady, onRouteChanged = ::onWebRoute) }

    /**
     * ★ 2026-11-07 앱 탭 셸(MainActivity) 위의 웹 창.
     *  · ★ 2026-11-08 웹 탭은 없다(모든 탭이 앱 화면) — 이 창 아래에 탭바를 그리지 않는다.
     *  · 앱 탭 주소(/home·/lessons·/community·/performance·/mypage)로 가면 이 창을 닫고 앱 탭으로 돌아간다.
     *    웹의 __nativeGo 가 첫 칸을 /home 으로 바꾼 뒤 목적지를 쌓는 순간이 있어, 주소가 잠깐 머문 뒤에만 돌아간다.
     */
    private var lastRoute: String = ""
    private val routeHandler = android.os.Handler(android.os.Looper.getMainLooper())

    private fun onWebRoute(path: String) {
        lastRoute = path
        syncRegionFromWeb()
        if (handleLoginRoute(path)) return
        // 앱 탭 주소 또는 앱으로 옮긴 상세(커뮤니티 글·공연) — 이 창을 닫고 앱 화면으로(NativeRoutes 표).
        // 웹이 스스로 네이티브로 넘기는 주소(Action)는 웹이 이미 열었다 — 여기서 또 열면 두 번 뜬다.
        val target = com.muyeon.app.navigation.NativeRoutes.resolve(path)
        if (target == null || target is com.muyeon.app.navigation.NativeRoutes.Target.Action) return
        routeHandler.postDelayed({
            if (lastRoute == path && !isFinishing) com.muyeon.app.ui.main.MainActivity.openPath(this, path)
        }, ROUTE_SETTLE_MS)
    }

    /**
     * ★ 2026-11-03 로그인 후 이어서 할 일(LoginResume) — 네이티브 화면이 '로그인 필요'로 닫히며 실어 보낸 값.
     *  웹이 로그인을 마치고 토큰을 저장하는 순간([onLoggedIn]) 한 번 다시 열고 비운다. 타이머 없음.
     */
    private var pendingAfterLogin: com.muyeon.app.ui.common.market.LoginResume? = null

    /** AndroidToken.saveToken 성공(웹 로그인·가입 완료) 때 UI 스레드에서 불린다. */
    fun onLoggedIn() {
        // 소셜 로그인 창 — 마지막 로그인 수단을 기억하고(로그인 화면 '최근 로그인'), 이제부터는 보통 웹 창이다.
        socialProvider?.takeIf { socialPending }?.let { com.muyeon.app.auth.LoginProvider.save(this, it) }
        socialPending = false
        val r = pendingAfterLogin ?: return
        pendingAfterLogin = null
        appBridge.openAction(r.action, r.paramsJson())
    }

    private fun deliverResults(keys: Set<String>, payload: Bundle?) {
        if (keys.isEmpty()) return
        if (ResultKeys.LOGIN_REQUIRED in keys) {
            pendingAfterLogin = com.muyeon.app.ui.common.market.LoginResume.fromBundle(payload?.getBundle(ResultKeys.LOGIN_REQUIRED))
        }
        // ★ 2026-11-08 네이티브 화면이 '로그인 필요'로 닫혔다 → 앱 로그인 화면(결과로 받는다 — 성공하면 이 창을 로그인 상태로 다시 읽고 이어서 연다).
        if (ResultKeys.LOGIN_REQUIRED in keys) loginLauncher.launch(com.muyeon.app.ui.auth.LoginActivity.resultIntent(this))
        keys.filter { it != ResultKeys.LOGIN_REQUIRED }
            .forEach { key -> evalWhenReady(readyGuarded(WebCallbacks.resultJs(key, payload.payloadJsonFor(key)))) }
        if (ResultKeys.CHAT_ROOMS in keys || ResultKeys.CHAT_ROOM in keys || ResultKeys.NOTIFICATIONS in keys) {
            com.muyeon.app.ui.floating.FloatingState.requestRefresh()
        }
    }

    // ★ 2026-10-12 웹 <input type="file"> 지원 — 종전엔 onShowFileChooser 가 없어 앱 웹뷰에서 파일 선택 창이 열리지 않았다
    //  (전문 정보 확인 서류·웹 사진 업로드). 시스템 문서 선택기(SAF)라 저장소 권한이 필요 없다.
    private var fileChooserCallback: android.webkit.ValueCallback<Array<android.net.Uri>>? = null
    private fun deliverChosen(uris: List<android.net.Uri>) {
        fileChooserCallback?.onReceiveValue(uris.toTypedArray())
        fileChooserCallback = null
    }
    // 이미지만 받는 입력 → 사진 선택기(갤러리, 권한 불필요). 그 밖(PDF 등) → 문서 선택기. 한 장/여러 장은 input 의 multiple 을 따른다.
    private val photoPickOne = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri -> deliverChosen(listOfNotNull(uri)) }
    private val photoPickMany = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris -> deliverChosen(uris) }
    private val docPickOne = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri -> deliverChosen(listOfNotNull(uri)) }
    private val docPickMany = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> deliverChosen(uris) }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        fileInterface.handleActivityResult(result.resultCode, result.data)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        liveCount++
        applyEdgeToEdge()
        super.onCreate(savedInstanceState)
        pendingAfterLogin = com.muyeon.app.ui.common.market.LoginResume.fromBundle(savedInstanceState?.getBundle(STATE_AFTER_LOGIN))
        setContentView(R.layout.activity_webview)

        val factory = DeviceInfoViewModelFactory(this)
        deviceInfoViewModel = ViewModelProvider(this, factory)[DeviceInfoViewModel::class.java]

        val notificationFactory =
            NotificationViewModelFactory(RequestNotificationPermissionUseCase())
        notificationViewModel =
            ViewModelProvider(this, notificationFactory)[NotificationViewModel::class.java]

        fileViewModel = ViewModelProvider(this)[FileViewModel::class.java]

        val locationProvider = LocationRepositoryImpl(this, this)

        webView = findViewById(R.id.webview)
        val composeView: ComposeView = findViewById(R.id.compose_view)

        setupWebView()
        // ★ 2026-11-07 앱 MY 에서 로그아웃했으면 웹 저장소의 로그인 정보도 지운다(문서 시작 스크립트, 한 번만).
        WebLogout.install(this, webView, com.muyeon.app.utils.Constants.getBaseUrl(this).trimEnd('/'))
        // ★ 2026-11-08 앱 로그인 화면에서 로그인했으면 웹 저장소에도 같은 로그인을 넣는다(문서 시작 스크립트, 한 번만).
        WebLogin.install(this, webView, com.muyeon.app.utils.Constants.getBaseUrl(this).trimEnd('/'))
        // ★ 2026-11-08 (b4) 관심 지역(앱 화면에서 고친 값)을 웹 저장소에도 — 창을 떠날 때 옛 웹 값이 앱 값을 덮지 않게 넣은 값을 기억한다.
        injectedRegionJson = WebRegion.install(webView, com.muyeon.app.utils.Constants.getBaseUrl(this).trimEnd('/'),
            runCatching { org.koin.java.KoinJavaComponent.get<com.muyeon.domain.repository.RegionRepository>(com.muyeon.domain.repository.RegionRepository::class.java).webJson() }.getOrNull())
        socialPending = socialProvider != null && savedInstanceState == null
        attachFloatingScrollListener()

        fileInterface = FileWebViewInterface(
            context = this,
            webView = webView,
            filePermissionLauncher = filePermissionLauncher,
            imagePickerLauncher = imagePickerLauncher,
            fileViewModel = fileViewModel,
            onShowDialog = { title, content, leftText, rightText, buttonCount, alignment, onLeft, onRight ->
                dialogTitle = title
                dialogContent = content
                dialogLeftButtonText = leftText
                dialogRightButtonText = rightText
                dialogButtonCount = buttonCount
                dialogAlignment = alignment
                dialogOnLeftClick = {
                    onLeft()
                    showDialog = false
                }
                dialogOnRightClick = {
                    onRight()
                    showDialog = false
                }
                showDialog = true
            }
        )
        scanQRInterface = ScanQRWebViewInterface(this, webView)
        downloadInterface = DownloadWebViewInterface(this, webView)

        locationWebViewInterface = LocationWebViewInterface(
            context = this,
            webView = webView,
            locationProvider = locationProvider,
            activity = this,
            onShowDialog = { title, content, leftText, rightText, buttonCount, alignment, onLeft, onRight ->
                dialogTitle = title
                dialogContent = content
                dialogLeftButtonText = leftText
                dialogRightButtonText = rightText
                dialogButtonCount = buttonCount
                dialogAlignment = alignment
                dialogOnLeftClick = {
                    onLeft()
                    showDialog = false
                }
                dialogOnRightClick = {
                    onRight()
                    showDialog = false
                }
                showDialog = true
            }
        )

        webView.addJavascriptInterface(downloadInterface, "AndroidDownload")
        webView.addJavascriptInterface(fileInterface, "AndroidStorage")
        webView.addJavascriptInterface(
            DeviceInfoWebViewInterface(
                webView,
                deviceInfoViewModel
            ), "AndroidDeviceInfo"
        )
        webView.addJavascriptInterface(TokenWebViewInterface(this, webView), "AndroidToken")
        webView.addJavascriptInterface(locationWebViewInterface, "AndroidLocation")
        webView.addJavascriptInterface(
            com.muyeon.app.webview.NotificationWebViewInterface(
                context = this,
                webView = webView,
                notificationViewModel = notificationViewModel,
                permissionLauncher = notificationPermissionLauncher,
                onShowDialog = { title, content, leftText, rightText, buttonCount, alignment, onLeft, onRight ->
                    dialogTitle = title
                    dialogContent = content
                    dialogLeftButtonText = leftText
                    dialogRightButtonText = rightText
                    dialogButtonCount = buttonCount
                    dialogAlignment = alignment
                    dialogOnLeftClick = {
                        onLeft()
                        showDialog = false
                    }
                    dialogOnRightClick = {
                        onRight()
                        showDialog = false
                    }
                    showDialog = true
                }
            ),
            "AndroidNotification"
        )
        webView.addJavascriptInterface(
            PermissionWebViewInterface(
            context = this,
            webView = webView,
            activity = this,
            onShowDialog = { title, content, leftText, rightText, buttonCount, alignment, onLeft, onRight ->
                dialogTitle = title
                dialogContent = content
                dialogLeftButtonText = leftText
                dialogRightButtonText = rightText
                dialogButtonCount = buttonCount
                dialogAlignment = alignment
                dialogOnLeftClick = {
                    onLeft()
                    showDialog = false
                }
                dialogOnRightClick = {
                    onRight()
                    showDialog = false
                }
                showDialog = true
            }), "AndroidPermission"
        )
        webView.addJavascriptInterface(scanQRInterface, "AndroidQRInfo")
        webView.addJavascriptInterface(PushNotificationInterface(webView), "PushNotificationBridge")
        webView.addJavascriptInterface(QrPageWebViewInterface(webView), "qrPageBridge")
        // 웹 → 네이티브 단방향 액션 채널(iOS callbackHandler 와 동일 계약).
        //  액션명·데이터 키는 iOS 와 100% 동일. 미이식 화면은 웹 경로 폴백(죽은 버튼 방지).
        webView.addJavascriptInterface(
            appBridge, "AppBridge",
        )
        // 구글 결제가 서버에 반영됐다(앱 복귀 재전송·Play 콜백) — 웹도 멤버십을 다시 읽게 한다(iOS ResultBus .membership).
        //  보이는 동안만 듣는다. 멤버십 화면이 위에 있으면 그 화면이 닫힐 때 결과 키(MEMBERSHIP)로 알린다(중복 재조회 방지).
        // ★ 2026-10-30 강제 로그아웃(다른 기기 로그인·계정 정지) — 네이티브 화면이 위에 떠 있어도 받아야 해서
        //  repeatOnLifecycle 없이 본문이 살아 있는 동안 듣는다. 위 화면을 닫고(CLEAR_TOP) 웹에 알리면
        //  웹(common/forceLogout)이 안내 팝업과 함께 로그인 화면으로 보내고 deleteToken 으로 토큰을 지운다.
        // ★ 2026-11-04 강제 로그아웃은 ForceLogout.trigger 가 이 화면을 앞으로 불러 extras 로 알린다(consumeForceLogout).
        //  여기서 버스를 구독하던 예전 방식은 이 화면이 없으면 신호를 잃었다.
        // 앱이 켜져 있고 토큰이 있으면 네이티브 소켓을 늘 붙여 둔다 — 어느 화면이 떠 있어도 force-logout 을 듣는다.
        com.muyeon.app.chat.socket.ChatSocketLifecycleObserver.ensureAttached(this)
        com.muyeon.app.chat.socket.ChatSocketManager.connect(this)
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                com.muyeon.app.ui.membership.MembershipBilling.changes.collect {
                    deliverResults(setOf(ResultKeys.MEMBERSHIP), null)
                }
            }
        }

        android.util.Log.d("QR_DEBUG", "🟢 WebViewActivity onCreate savedInstanceState=${savedInstanceState != null}, QrPageManager.value=${com.muyeon.app.utils.QrPageManager.getValue()}")
        // QR 식사평가(qrPage)가 있으면 복원하지 말고 홈을 새로 로드해 DefaultLayout 폴링이 처리하도록 함
        if (savedInstanceState != null && !com.muyeon.app.utils.QrPageManager.hasValue()) {
            android.util.Log.d("QR_DEBUG", "🟢 restoreState 호출 (qrPage 없음 → 이전 화면 복원)")
            webView.restoreState(savedInstanceState)
        } else {
            val baseUrl = com.muyeon.app.utils.Constants.getBaseUrl(this)
            // 알림톡 앱링크로 들어왔으면 푸시 탭 인텐트처럼 목적지를 옮겨 싣는다.
            //  푸시로 들어온 경우(notification_url 있음)는 그쪽이 우선.
            var fromLink = false
            if (intent.getStringExtra("notification_url").isNullOrEmpty()) {
                if (com.muyeon.app.utils.AppLinkManager.consumeInto(intent)) {
                    fromLink = true
                    com.muyeon.app.analytics.Analytics.deeplinkOpen(intent.getStringExtra("type"), intent.getStringExtra("notification_url"))
                }
            } else {
                com.muyeon.app.analytics.Analytics.pushOpen(intent.getStringExtra("type"), intent.getStringExtra("notification_url"))
            }
            val notificationUrl = intent.getStringExtra("notification_url")
            if (!notificationUrl.isNullOrEmpty()) {
                val fullUrl = if (notificationUrl.startsWith("http")) notificationUrl else baseUrl + notificationUrl
                // ★ 같은 사이트 목적지는 '/home' 을 열고 웹이 준비되면 목적지로 보낸다(goInApp) — 밑에 홈이 깔린다.
                //  목적지를 바로 열면 그 웹 페이지가 한 번 돌고 홈 깔기로 한 번 더 돌았다(멤버십은 결제창이 겹침, 2026-10-26).
                //  다른 사이트 주소면 예전처럼 바로 연다.
                if (fullUrl.startsWith(baseUrl.trimEnd('/') + "/")) {
                    Log.d("WebViewActivity", "Loading home then notification URL: $fullUrl")
                    webView.loadUrl(baseUrl.trimEnd('/') + "/home")
                    // ★ 2026-11-03 공간 대관·양도양수 푸시는 링크처럼 연다(웹 __openLink → 앱 공간 예약·내 양도 글 화면). 다른 푸시는 그대로.
                    val p = fullUrl.removePrefix(baseUrl.trimEnd('/'))
                    val marketPush = listOf("/spaces", "/mySpace", "/studio-transfers", "/myStudioTransfers").any { p.startsWith(it) }
                    goInApp(fullUrl, viaLink = fromLink || marketPush)
                } else {
                    Log.d("WebViewActivity", "Loading notification URL: $fullUrl")
                    webView.loadUrl(fullUrl)
                }
            } else if (!intent.getStringExtra(EXTRA_START_PATH).isNullOrEmpty()) {
                // ★ 2026-11-07 앱 탭 셸이 연 웹 창 — 그 주소를 바로 연다(밑에 홈을 깔지 않는다. 홈은 앱 탭이다).
                val path = intent.getStringExtra(EXTRA_START_PATH).orEmpty()
                webView.loadUrl(if (path.startsWith("http")) path else baseUrl.trimEnd('/') + path)
            } else {
                android.util.Log.d("QR_DEBUG", "🌐 WebView 로드 URL=$baseUrl (홈 새로 로드)")
                webView.loadUrl(baseUrl)
            }
        }

        // 네이티브 화면이 지정한 SPA 이동/콜백(있으면) — 최초 진입 시에도 처리.
        consumeNativeRoute(intent)
        consumeForceLogout(intent)
        com.muyeon.app.ui.assistant.AssistantOverlay.takeDraftPush(intent) // 'AI가 글을 다 썼어요' 푸시 탭 → AI 창 결과 화면

        composeView.setContent {
            val showImagePicker by fileViewModel.showImagePicker.collectAsStateWithLifecycle()
            val allowedImages by fileViewModel.allowedImages.collectAsStateWithLifecycle()

            MuyeonTheme {
                // 웹 라우트와 무관하게 살아 있는 네이티브 플로팅(채팅 버튼 + 인증 책갈피).
                //  ComposeView 가 웹뷰 위에 깔려 있어 여기 붙이면 페이지 이동에도 사라지지 않는다.
                com.muyeon.app.ui.floating.FloatingOverlay(
                    api = remember { com.muyeon.app.ui.floating.FloatingApi { com.muyeon.app.utils.TokenManager.getAccessToken(this@WebViewActivity) } },
                    onOpenChatList = { com.muyeon.app.ui.chat.ChatActivity.startList(this@WebViewActivity, "") },
                    // 완료 화면 '시작하기' → 웹이 해당 유형 화면으로 전환한다.
                    onRoleApproved = { role ->
                        webView.evaluateJavascript(
                            "(function(){ if(window.__onRoleApproved){ window.__onRoleApproved('$role'); } return null; })()",
                            null,
                        )
                    },
                )
                if (showImagePicker) {
                    ImagePickerBottomSheet(
                        images = allowedImages,
                        onSelect = { uri ->
                            fileInterface.processSelectedImages(uri)
                            fileViewModel.hidePicker()
                        },
                        onCancel = {
                            fileInterface.sendErrorToWeb("NoImageSelected", 400)
                            fileViewModel.hidePicker()
                        },
                        onAddImages = { fileInterface.getFilePicker() },
                        requestPermissions = { fileInterface.getFilePicker() }
                    )
                }
                CustomDialog(
                    title = dialogTitle,
                    content = dialogContent,
                    leftButtonText = dialogLeftButtonText,
                    rightButtonText = dialogRightButtonText,
                    buttonCount = dialogButtonCount,
                    alignment = dialogAlignment,
                    onDismiss = { showDialog = false },
                    onLeftButtonClick = dialogOnLeftClick,
                    onRightButtonClick = dialogOnRightClick,
                    showPopup = showDialog
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // 앱이 켜진 채 들어온 링크(SplashActivity 가 넘김) — 새로 읽지 않고 보던 화면 위에 쌓는다(iOS 와 같음).
        if (intent.getStringExtra("notification_url").isNullOrEmpty() &&
            com.muyeon.app.utils.AppLinkManager.consumeInto(intent)
        ) {
            val path = intent.getStringExtra("notification_url").orEmpty()
            intent.removeExtra("notification_url")
            com.muyeon.app.analytics.Analytics.deeplinkOpen(intent.getStringExtra("type"), path)
            Log.d("WebViewActivity", "onNewIntent app link: $path")
            goInApp(path, viaLink = true)
        }
        val notificationUrl = intent.getStringExtra("notification_url")
        if (!notificationUrl.isNullOrEmpty()) {
            com.muyeon.app.analytics.Analytics.pushOpen(intent.getStringExtra("type"), notificationUrl)
            val baseUrl = com.muyeon.app.utils.Constants.getBaseUrl(this)
            val fullUrl = if (notificationUrl.startsWith("http")) notificationUrl else baseUrl + notificationUrl
            Log.d("WebViewActivity", "onNewIntent notification URL: $fullUrl")
            webView.loadUrl(fullUrl)
        }
        consumeNativeRoute(intent)
        consumeForceLogout(intent)
        com.muyeon.app.ui.assistant.AssistantOverlay.takeDraftPush(intent)
    }

    override fun onResume() {
        super.onResume()
        // ★ 2026-11-07 앱 탭(유형 시트)에서 바꾼 활성유형을 웹에도 — 웹은 자기 저장값으로 헤더·메뉴를 그린다.
        if (::webView.isInitialized) {
            val t = ActiveRole.current(this).replace("'", "")
            webView.evaluateJavascript("(function(){ try{ if(localStorage.getItem('muyeon.activeType')!=='$t' && window.__onActiveTypeChanged){ window.__onActiveTypeChanged('$t'); } }catch(e){} return null; })()", null)
        }
        // 웹뷰에 '다시 보임'을 알린다 → 웹 document.visibilityState 가 visible 로 바뀌고 visibilitychange 가 난다.
        //  웹의 예약내역·견적·지원 목록은 이 이벤트로 '화면 복귀 시 재조회'를 한다(콜백을 못 받는 경로의 안전망).
        //  예전엔 onPause/onResume 을 웹뷰에 넘기지 않아, 네이티브 화면을 닫고 돌아와도 웹은 계속 보이는 중으로 알았다.
        //  ★ 콜백 대기열을 흘리기 전에 부른다 — 먼저 깨워야 스크립트가 바로 돈다.
        if (::webView.isInitialized) webView.onResume()
        // 이용 행동 로그 — 30분 넘게 쉬었다 오면 세션이 바뀌므로 웹에 새 식별값을 다시 넣는다.
        if (::webView.isInitialized) com.muyeon.app.analytics.Analytics.injectInto(webView)
        // 채팅·알림 배지 재조회 — 예전 60초 폴링을 대신한다(돌아올 때마다 한 번).
        com.muyeon.app.ui.floating.FloatingState.requestRefresh()
        // 서버 활성유형으로 X-Active-Type 값 맞춤 — 다른 기기·웹에서 바꾼 유형, 가입 직후 낡은 값 보정.
        //  (종전엔 결과 전달 분기 안에 잘못 들어가 채팅·알림 결과가 있을 때만 돌았다.)
        ActiveRole.syncFromServer(this)
        // 네이티브 화면이 쌓아둔 웹 콜백을 흘려보낸다(WebCallbackQueue).
        //  액티비티가 죽어 인텐트로 못 넘긴 것까지 여기서 회수된다 —
        //  안 하면 "화면엔 반영됐는데 서버는 모르는" 상태로 남는다.
        //  ★ 실행 성공을 확인한 뒤에 비운다(콜드 스타트에서 웹이 아직 준비 안 됐을 수 있다).
        flushCallbackQueue()
        // 준비 신호를 못 받은 채 밀려 있던 것도 한 번 더 시도(옛 웹 안전망 — 타이머 없음).
        flushPendingJs()
    }

    /**
     * 웹이 콜백을 등록하기 전이면 0 을 돌려 재시도하게 만든다.
     *  `window.__nativeGo` 는 콜백들과 **같은 AppRouter effect** 에서 심어지므로 준비 신호로 쓴다.
     *  ★ 이 가드가 없으면 `try{...}catch{} return 1` 이 항상 성공으로 잡혀,
     *    핸들러가 없던 순간의 통지가 조용히 사라진다.
     */
    private fun readyGuarded(js: String) =
        "(function(){ if(!window.__muyeonResult && !window.__nativeGo) return 0; try { $js } catch(e) {} return 1; })()"

    /** 강제 로그아웃 extras(ForceLogout.trigger) — 웹이 준비되면 'muyeonNativeForceLogout' 을 한 번 보낸다. */
    private fun consumeForceLogout(intent: Intent) {
        val code = intent.getStringExtra(com.muyeon.app.chat.socket.ForceLogout.EXTRA_CODE)?.takeIf { it.isNotEmpty() } ?: return
        val message = intent.getStringExtra(com.muyeon.app.chat.socket.ForceLogout.EXTRA_MESSAGE).orEmpty()
        intent.removeExtra(com.muyeon.app.chat.socket.ForceLogout.EXTRA_CODE)
        intent.removeExtra(com.muyeon.app.chat.socket.ForceLogout.EXTRA_MESSAGE)
        val detail = org.json.JSONObject().put("code", code).put("message", message).toString()
        evalWhenReady(readyGuarded("window.dispatchEvent(new CustomEvent('muyeonNativeForceLogout',{detail:$detail}));"))
    }

    /**
     * 네이티브 화면에서 돌아오며 요청한 SPA 이동/콜백 처리(NativeWebRoute).
     *  웹이 아직 로드 전이면 __nativeGo 가 없으므로 준비될 때까지 [pendingJs] 에 둔다.
     */
    private fun consumeNativeRoute(intent: Intent) {
        intent.getStringExtra(NativeWebRoute.EXTRA_GO_PATH)?.takeIf { it.isNotEmpty() }?.let { path ->
            intent.removeExtra(NativeWebRoute.EXTRA_GO_PATH)
            val safe = path.replace("'", "\\'")
            evalWhenReady("(function(){ if(window.__nativeGo){ window.__nativeGo('$safe'); return 1; } return 0; })()")
        }
        intent.getStringExtra(NativeWebRoute.EXTRA_EVAL_JS)?.takeIf { it.isNotEmpty() }?.let { js ->
            intent.removeExtra(NativeWebRoute.EXTRA_EVAL_JS)
            // 같은 이유로 준비 가드를 씌운다 — 종전엔 항상 1 이라 재시도가 한 번도 안 걸렸다.
            evalWhenReady(readyGuarded(js))
        }
    }

    // ── 웹 준비 전 대기열 ──
    //  예전엔 300ms × 10회 postDelayed 재시도였다. 이제 웹이 콜백을 다 심은 뒤 브릿지로 `webReady` 를
    //  1회 보내므로(muyeon-front nativeBridge.notifyWebReady) 그때 한 번에 흘린다. 타이머는 없다.
    //   · 흘리는 시점: webReady 수신 / onPageFinished(webReady 를 안 보내는 옛 웹 안전망) / onResume.
    //   · 스크립트는 준비 가드(readyGuarded)를 쓰므로 아직 준비 전이면 0 → 대기열에 그대로 남는다.
    //   · 순서 보장: 앞에서부터 하나씩, 실패하면 거기서 멈춘다(뒤엣것이 먼저 실행되지 않게).
    //   · script 는 실행 직전에 만든다 — 콜백 대기열(WebCallbackQueue)은 그 사이 더 쌓일 수 있어서다.
    private class PendingJs(val key: String, val script: () -> String?, val onDone: (() -> Unit)?)

    private val pendingJs = ArrayDeque<PendingJs>()
    private var flushing = false

    /** 새 페이지 로드마다 false — webReady 를 받으면 true. */
    private var webReady = false

    private fun evalWhenReady(script: String, onDone: (() -> Unit)? = null) =
        enqueuePending(PendingJs(script, { script }, onDone))

    private fun enqueuePending(p: PendingJs) {
        // 같은 항목이 이미 밀려 있으면 또 쌓지 않는다(onResume 이 여러 번 와도 한 번만 실행).
        if (pendingJs.any { it.key == p.key }) return
        pendingJs.addLast(p)
        flushPendingJs()
    }

    /**
     * 디스크 콜백 대기열(WebCallbackQueue) 흘리기 — 항상 한 항목으로만 밀어 두고,
     *  실행 직전의 대기열 전체를 한 번에 보낸 뒤 **보낸 것만** 지운다.
     */
    private fun flushCallbackQueue() {
        var batch: List<String> = emptyList()
        enqueuePending(
            PendingJs(
                key = QUEUE_KEY,
                script = {
                    batch = WebCallbackQueue.items(this)
                    if (batch.isEmpty()) null else readyGuarded(batch.joinToString("\n"))
                },
                onDone = { WebCallbackQueue.remove(this, batch) },
            ),
        )
    }

    private fun flushPendingJs() {
        if (flushing || pendingJs.isEmpty() || !::webView.isInitialized) return
        val head = pendingJs.first()
        val script = head.script()
        if (script == null) {
            // 보낼 게 없어졌다(대기열이 이미 비었음) — 버리고 다음으로.
            pendingJs.removeFirst()
            flushPendingJs()
            return
        }
        flushing = true
        webView.evaluateJavascript(script) { result ->
            flushing = false
            if (result == "1") {
                // 같은 항목이 아직 맨 앞일 때만 뺀다(그 사이 페이지가 바뀌어 비워졌을 수 있다).
                if (pendingJs.firstOrNull() === head) pendingJs.removeFirst()
                head.onDone?.invoke()
                flushPendingJs()
            }
            // 0(준비 전)이면 멈춘다 — 다음 webReady / onPageFinished / onResume 에서 다시 흘린다.
        }
    }

    companion object {
        private const val QUEUE_KEY = "__webCallbackQueue"
        /** ★ 2026-11-07 앱 탭 셸이 연 웹 창 — 이 주소를 바로 연다. */
        const val EXTRA_START_PATH = "muyeon.web.startPath"
        /** 웹 탭(구인구직·대타)으로 연 창 — 아래에 앱 탭바를 그린다. */
        const val EXTRA_TAB_KEY = "muyeon.web.tabKey"
        private const val ROUTE_SETTLE_MS = 250L

        /**
         * 웹 페이지 창 열기(MainActivity 위). viaLink(링크·알림톡)는 지금은 그 주소를 그대로 연다 —
         *  링크 표(웹 nativeLink.js)로 앱 화면을 여는 일은 웹 페이지가 한다.
         */
        fun startPath(context: android.content.Context, path: String, viaLink: Boolean = false, tabKey: String? = null) {
            // ★ 2026-11-08 로그인은 앱 화면이다 — 웹 창 대신 앱 로그인 화면(redirect 를 들고 간다).
            if (com.muyeon.app.ui.auth.LoginActivity.isLoginPath(path)) {
                com.muyeon.app.ui.auth.LoginActivity.startFromPath(context, path)
                return
            }
            // ★ [b1] 가입 주소도 앱 화면.
            if (com.muyeon.app.ui.auth.LoginActivity.isSignupPath(path)) {
                com.muyeon.app.ui.auth.LoginActivity.startSignup(context, path)
                return
            }
            context.startActivity(
                Intent(context, WebViewActivity::class.java)
                    .putExtra(EXTRA_START_PATH, path)
                    .apply { tabKey?.let { putExtra(EXTRA_TAB_KEY, it) } },
            )
        }
        private const val STATE_AFTER_LOGIN = "muyeon.afterLogin"
        private const val EXTRA_SOCIAL = "muyeon.web.socialLogin"

        /**
         * ★ 2026-11-08 소셜 로그인(카카오·네이버) — 앱에 SDK 가 없어 웹 OAuth 로 한다.
         *  웹 로그인 화면(/login?redirect=)을 열어 그 버튼을 대신 누른다 → 카카오·네이버 인증 → 웹 콜백 페이지가 서버와 토큰을 바꾸고
         *  AndroidToken.saveToken 으로 넘긴다(AuthSession.onWebLogin — 앱 로그인과 같은 후처리) → 웹이 redirect(신규면 /social-signup)로.
         *  취소·실패(콜백 뒤 /login)·뒤로면 이 창을 닫아 앱 로그인 화면으로 돌아간다.
         */
        fun startSocialLogin(context: android.content.Context, provider: com.muyeon.app.auth.LoginProvider, redirect: String) {
            // 네이버 버튼은 웹도 검수 전엔 ?naver=1 일 때만 그린다.
            val path = "/login?redirect=" + android.net.Uri.encode(redirect) + if (provider == com.muyeon.app.auth.LoginProvider.NAVER) "&naver=1" else ""
            context.startActivity(Intent(context, WebViewActivity::class.java).putExtra(EXTRA_START_PATH, path).putExtra(EXTRA_SOCIAL, provider.name))
        }

        /** 떠 있는 본문 화면 수 — 앱이 켜진 채 링크가 오면 스플래시가 새로 만들지 않고 이 화면으로 넘긴다.
         *  개수로 센다: CLEAR_TASK 로 바뀔 때 새 화면 onCreate 가 옛 화면 onDestroy 보다 먼저 온다. */
        @Volatile private var liveCount = 0
        val isAlive: Boolean get() = liveCount > 0
    }

    /**
     * 링크·푸시 목적지로 SPA 이동(웹 __nativeGo). 웹이 준비 전이면 대기열에 둔다.
     *  viaLink(링크·알림톡)면 웹 __openLink 를 먼저 쓴다 — 네이티브 화면이 있는 주소면 그 화면을 연다
     *  (표는 웹 common/nativeLink.js 한 곳, 유형·로그인 검사도 웹 버튼과 같다). 옛 웹이면 __nativeGo.
     *  __nativeGo 는 히스토리 첫 칸(idx 0)이면 그 자리를 /home 으로 바꾸고 목적지를 쌓는다 —
     *  그래서 링크로 들어온 화면 밑에 항상 홈이 깔린다(뒤로가기 키·< 버튼 모두 홈으로).
     */
    private fun goInApp(url: String, viaLink: Boolean = false) {
        val base = com.muyeon.app.utils.Constants.getBaseUrl(this).trimEnd('/')
        val path = if (url.startsWith(base)) url.removePrefix(base) else url
        if (!path.startsWith("/") || path.startsWith("//") || path == "/" || path == "/home") return
        val safe = path.replace("\\", "\\\\").replace("'", "\\'")
        Log.d("WebViewActivity", "goInApp $path")
        val fn = if (viaLink) "(window.__openLink||window.__nativeGo)" else "window.__nativeGo"
        evalWhenReady("(function(){ if(window.__nativeGo){ $fn('$safe'); return 1; } return 0; })()")
    }

    /** 브릿지 `webReady` — 웹이 __muyeonResult 등 콜백을 전부 심었다. 밀린 것을 흘린다. */
    private fun onWebReady() {
        webReady = true
        flushPendingJs()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pendingAfterLogin?.let { outState.putBundle(STATE_AFTER_LOGIN, it.toBundle()) }
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    @Deprecated("")
    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LocationWebViewInterface.LOCATION_PERMISSION_REQUEST_CODE) {
            val isGranted =
                grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            locationWebViewInterface.handlePermissionResult(isGranted)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    /**
     * 스크롤 방향으로 채팅 플로팅 숨김/표시 — iOS `CommonWebView.scrollViewDidScroll` 1:1.
     *  최상단 근처면 항상 표시, 6px 이상 아래로면 숨김, 위로면 표시.
     */
    private var lastScrollY = 0

    private fun attachFloatingScrollListener() {
        webView.setOnScrollChangeListener { _, _, y, _, _ ->
            val dy = y - lastScrollY
            when {
                y <= 0 -> com.muyeon.app.ui.floating.FloatingState.updateHideChatFloat(false)
                dy > 6 -> com.muyeon.app.ui.floating.FloatingState.updateHideChatFloat(true)
                dy < -6 -> com.muyeon.app.ui.floating.FloatingState.updateHideChatFloat(false)
            }
            lastScrollY = y
        }
    }

    /** 웹뷰 밖 주소(카드사·간편결제 앱 등) — 결제 창과 같은 규칙(ExternalScheme). */
    private fun openExternalScheme(url: String) = ExternalScheme.open(this, webView, url)

    private fun setupWebView() {
        android.webkit.WebView.setWebContentsDebuggingEnabled(true)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: android.webkit.WebResourceRequest
            ): Boolean {
                // 교차 앱 스크립팅 방지: http/https 만 WebView 에서 로드한다.
                val scheme = request.url.scheme?.lowercase()
                if (scheme == "http" || scheme == "https") {
                    return false
                }
                // file/content/javascript 등 위험 스킴은 WebView 로드를 막고,
                // 외부 앱으로 처리 가능한 스킴(market/tel/mailto/intent 등)만 외부로 넘긴다.
                openExternalScheme(request.url.toString())
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                // 새 문서 — 콜백을 다시 심을 때까지 준비 전으로 본다(대기열은 유지).
                webReady = false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // 이용 행동 로그 — 웹 화면도 앱과 같은 aid·sid 로 보내게 식별값을 넣는다.
                view?.let { com.muyeon.app.analytics.Analytics.injectInto(it) }
                // 이 앱이 지원하는 기능 표시(iOS __muyeonNativeCapabilities 와 같은 키). 웹은 표시가 없으면 구버전 앱으로 보고 웹 화면을 쓴다.
                //  publicProfileJobs: 공개 프로필이 구인구직 문맥(src=jobs)을 멤버십 규칙 레이아웃으로 그린다(2026-10-19).
                //  nativeCastingStage: 하단 탭 '캐스팅·무대'를 네이티브 화면으로 연다(2026-10-26).
                //  nativeStudioTransfers: 무용센터 양도양수 브리지(openStudioTransfers·Detail·Editor·My)를 네이티브로 연다(2026-11-03).
                //  nativeSpaceBooking: 공간 대관 브리지(openSpaces·Editor·MySpaces·MySpaceReservations·MySpaceBookings)·결제 예약(2026-11-03).
                //  표시를 넣은 뒤 'muyeonCapabilities' 이벤트 — 페이지 로드 뒤라 이미 그린 웹 화면(홈 섹션·MY 메뉴)이 다시 읽는다(2026-11-03).
                //  castingStageLink: openCastingStage payload(castingId·screen·issueNo)로 그 화면부터 연다(2026-10-27). 없으면 웹은 안쪽 링크를 웹으로.
                view?.evaluateJavascript(
                    "window.__muyeonNativeCapabilities=Object.assign({},window.__muyeonNativeCapabilities||{},{publicProfileJobs:true,nativeCastingStage:true,castingStageLink:true,activeTypeSheet:true,nativeStudioTransfers:true,nativeSpaceBooking:true,nativeTabBar:true});try{window.dispatchEvent(new Event('muyeonCapabilities'))}catch(e){}",
                    null,
                )
                // webReady 를 안 보내는 옛 웹 안전망. 준비 전이면 가드가 0 을 돌려 그대로 남는다.
                if (!webReady) flushPendingJs()
                // 소셜 로그인 창 — 첫 로그인 문서에서 카카오·네이버 버튼을 대신 누른다.
                if (socialPending && url?.contains("/login") == true) clickSocialButton()
            }
        }
        webView.webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?,
                callback: android.webkit.ValueCallback<Array<android.net.Uri>>?,
                params: FileChooserParams?,
            ): Boolean {
                // 이전 요청이 남아 있으면 취소로 끝내 준다(웹 input 이 멈추지 않게).
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = callback
                val types = params?.acceptTypes
                    ?.flatMap { it.split(',') }
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.takeIf { it.isNotEmpty() }
                    ?: listOf("*/*")
                val multiple = params?.mode == FileChooserParams.MODE_OPEN_MULTIPLE
                val imagesOnly = types.all { it.startsWith("image/") }
                return try {
                    if (imagesOnly) {
                        val req = androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                        if (multiple) photoPickMany.launch(req) else photoPickOne.launch(req)
                    } else {
                        if (multiple) docPickMany.launch(types.toTypedArray()) else docPickOne.launch(types.toTypedArray())
                    }
                    true
                } catch (e: Exception) {
                    fileChooserCallback = null
                    false
                }
            }

            override fun onConsoleMessage(cm: android.webkit.ConsoleMessage): Boolean {
                android.util.Log.d("QR_WEB", "${cm.message()}  (@${cm.sourceId()}:${cm.lineNumber()})")
                return true
            }
        }
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        // ★ meta viewport 를 존중한다. 기본값(false)이면 안드로이드 WebView 가
        //   <meta name="viewport" content="width=device-width, initial-scale=1, user-scalable=0">
        //   를 통째로 무시해서, 레이아웃 폭이 화면과 어긋나고 핀치 줌도 막히지 않는다.
        //   → 살짝 확대되는 순간부터 가로 스크롤이 생긴다. iOS WKWebView 는 항상 존중하므로
        //     안드로이드만 다르게 보이던 원인.
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true
        // 교차 앱 스크립팅(Cross-App Scripting) 방지: file/content URL 기반 접근 차단
        webSettings.allowFileAccess = false
        webSettings.allowContentAccess = false
        webSettings.allowFileAccessFromFileURLs = false
        webSettings.allowUniversalAccessFromFileURLs = false
        // 혼합 콘텐츠 항상 허용 제거 (COMPATIBILITY 로 완화)
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    }

    @Deprecated("")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // 소셜 로그인 중(카카오·네이버 화면 포함) 뒤로 = 취소 — 앱 로그인 화면으로.
        if (socialPending) { finish(); return }
        // 예전 그대로 canGoBack 이 먼저다. 막혔을 때만 웹(SPA) 기록을 본다.
        //  크롬 웹뷰는 사용자 조작 없이 쌓인 기록(링크·푸시로 들어와 __nativeGo 가 깐 홈)을 canGoBack 에서 건너뛴다
        //  (History Manipulation Intervention — 2026-10-26 실기기: 기록 2칸인데 canGoBack=false 라 종료 팝업).
        //  history.back() 은 그 영향을 받지 않는다. 이전 칸 있음 → 뒤로 / 첫 칸 → 아래 기존 처리.
        if (webView.canGoBack()) {
            webView.goBack()
            return
        }
        val js = "(function(){try{var i=(history.state&&history.state.idx)||0;" +
            "if(window.__nativeGo&&i>0){history.back();return 1;}}catch(e){}return 0;})()"
        webView.evaluateJavascript(js) { handled ->
            if (handled == "1") return@evaluateJavascript
            when {
                // 더 돌아갈 화면이 없고 이 화면이 앱의 마지막 화면이면 바로 끄지 않고 한 번 묻는다.
                //  다른 화면 위에 열린 경우(isTaskRoot=false)는 그 화면으로 돌아가는 게 맞아서 묻지 않는다.
                isTaskRoot -> showExitDialog()
                else -> super.onBackPressed()
            }
        }
    }

    private fun showExitDialog() {
        dialogTitle = "앱을 종료할까요?"
        dialogContent = null
        dialogLeftButtonText = "취소"
        dialogRightButtonText = "종료"
        dialogButtonCount = 2
        dialogAlignment = ContentAlignment.Middle
        dialogOnLeftClick = { showDialog = false }
        dialogOnRightClick = {
            showDialog = false
            finish()
        }
        showDialog = true
    }

    /** ★ 2026-11-07 웹 대표 지역(localStorage muyeonRegions)을 앱으로 — 지역 고르기는 웹 화면이라 창을 떠날 때마다 받아 둔다. */
    /** (b4) 이 창에 넣은 앱 관심 지역 JSON — 웹 값이 이것 그대로면 앱으로 다시 가져오지 않는다(WebRegion). */
    private var injectedRegionJson: String? = null

    private fun syncRegionFromWeb() {
        if (!::webView.isInitialized) return
        webView.evaluateJavascript("(function(){try{return localStorage.getItem('muyeonRegions')||''}catch(e){return ''}})()") { raw ->
            val json = runCatching { org.json.JSONArray("[$raw]").optString(0) }.getOrNull()?.takeIf { it.isNotBlank() } ?: return@evaluateJavascript
            if (json == injectedRegionJson) return@evaluateJavascript
            runCatching { org.koin.java.KoinJavaComponent.get<com.muyeon.domain.repository.RegionRepository>(com.muyeon.domain.repository.RegionRepository::class.java).syncFromWeb(json) }
        }
    }

    override fun onPause() {
        syncRegionFromWeb()
        // 다른 화면(네이티브·카드사 앱)이 덮으면 웹에 '숨김'을 알린다(onResume 의 짝).
        //  애니메이션·위치 등 웹뷰 부가 처리만 멈추고 JS 타이머는 멈추지 않는다(pauseTimers 는 부르지 않는다).
        if (::webView.isInitialized) webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        liveCount--
        routeHandler.removeCallbacksAndMessages(null)
        downloadInterface.cleanup()
        super.onDestroy()
    }
}