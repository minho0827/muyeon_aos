package com.muyeon.app.webview

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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.data.repository.LocationRepositoryImpl
import com.muyeon.app.domain.use_cases.RequestNotificationPermissionUseCase
import com.muyeon.app.result.ResultKeys
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
     * 결과 키 → 웹. 키마다 __muyeonResult(없으면 레거시 __onX) 한 번씩(WebCallbacks.resultJs).
     *  채팅·알림이 바뀌었으면 플로팅 배지도 곧바로 다시 읽는다(폴링 대신).
     */
    private fun deliverResults(keys: Set<String>, payload: Bundle?) {
        if (keys.isEmpty()) return
        keys.forEach { key -> evalWhenReady(readyGuarded(WebCallbacks.resultJs(key, payload.payloadJsonFor(key)))) }
        if (ResultKeys.CHAT_ROOMS in keys || ResultKeys.CHAT_ROOM in keys || ResultKeys.NOTIFICATIONS in keys) {
            com.muyeon.app.ui.floating.FloatingState.requestRefresh()
        }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        fileInterface.handleActivityResult(result.resultCode, result.data)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            AppBridgeInterface(this, webView, onWebReady = ::onWebReady), "AppBridge",
        )

        android.util.Log.d("QR_DEBUG", "🟢 WebViewActivity onCreate savedInstanceState=${savedInstanceState != null}, QrPageManager.value=${com.muyeon.app.utils.QrPageManager.getValue()}")
        // QR 식사평가(qrPage)가 있으면 복원하지 말고 홈을 새로 로드해 DefaultLayout 폴링이 처리하도록 함
        if (savedInstanceState != null && !com.muyeon.app.utils.QrPageManager.hasValue()) {
            android.util.Log.d("QR_DEBUG", "🟢 restoreState 호출 (qrPage 없음 → 이전 화면 복원)")
            webView.restoreState(savedInstanceState)
        } else {
            val baseUrl = com.muyeon.app.utils.Constants.getBaseUrl(this)
            // 알림톡 앱링크로 들어왔으면 푸시 탭 인텐트처럼 목적지를 옮겨 싣는다.
            //  푸시로 들어온 경우(notification_url 있음)는 그쪽이 우선.
            if (intent.getStringExtra("notification_url").isNullOrEmpty()) {
                com.muyeon.app.utils.AppLinkManager.consumeInto(intent)
            }
            val notificationUrl = intent.getStringExtra("notification_url")
            if (!notificationUrl.isNullOrEmpty()) {
                val fullUrl = if (notificationUrl.startsWith("http")) notificationUrl else baseUrl + notificationUrl
                Log.d("WebViewActivity", "Loading notification URL: $fullUrl")
                webView.loadUrl(fullUrl)
            } else {
                android.util.Log.d("QR_DEBUG", "🌐 WebView 로드 URL=$baseUrl (홈 새로 로드)")
                webView.loadUrl(baseUrl)
            }
        }

        // 네이티브 화면이 지정한 SPA 이동/콜백(있으면) — 최초 진입 시에도 처리.
        consumeNativeRoute(intent)

        composeView.setContent {
            val showImagePicker by fileViewModel.showImagePicker.collectAsStateWithLifecycle()
            val allowedImages by fileViewModel.allowedImages.collectAsStateWithLifecycle()

            MuyeonTheme {
                // 웹 라우트와 무관하게 살아 있는 네이티브 플로팅(채팅 버튼 + 인증 책갈피).
                //  ComposeView 가 웹뷰 위에 깔려 있어 여기 붙이면 페이지 이동에도 사라지지 않는다.
                com.muyeon.app.ui.floating.FloatingOverlay(
                    api = remember { com.muyeon.app.ui.floating.FloatingApi(com.muyeon.app.utils.TokenManager.getAccessToken(this@WebViewActivity)) },
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
        val notificationUrl = intent.getStringExtra("notification_url")
        if (!notificationUrl.isNullOrEmpty()) {
            val baseUrl = com.muyeon.app.utils.Constants.getBaseUrl(this)
            val fullUrl = if (notificationUrl.startsWith("http")) notificationUrl else baseUrl + notificationUrl
            Log.d("WebViewActivity", "onNewIntent notification URL: $fullUrl")
            webView.loadUrl(fullUrl)
        }
        consumeNativeRoute(intent)
    }

    override fun onResume() {
        super.onResume()
        // 웹뷰에 '다시 보임'을 알린다 → 웹 document.visibilityState 가 visible 로 바뀌고 visibilitychange 가 난다.
        //  웹의 예약내역·견적·지원 목록은 이 이벤트로 '화면 복귀 시 재조회'를 한다(콜백을 못 받는 경로의 안전망).
        //  예전엔 onPause/onResume 을 웹뷰에 넘기지 않아, 네이티브 화면을 닫고 돌아와도 웹은 계속 보이는 중으로 알았다.
        //  ★ 콜백 대기열을 흘리기 전에 부른다 — 먼저 깨워야 스크립트가 바로 돈다.
        if (::webView.isInitialized) webView.onResume()
        // 채팅·알림 배지 재조회 — 예전 60초 폴링을 대신한다(돌아올 때마다 한 번).
        com.muyeon.app.ui.floating.FloatingState.requestRefresh()
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

    private companion object {
        const val QUEUE_KEY = "__webCallbackQueue"
    }

    /** 브릿지 `webReady` — 웹이 __muyeonResult 등 콜백을 전부 심었다. 밀린 것을 흘린다. */
    private fun onWebReady() {
        webReady = true
        flushPendingJs()
    }

    override fun onSaveInstanceState(outState: Bundle) {
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

    /**
     * 웹뷰 밖으로 보내야 하는 주소(카드사·간편결제 앱, tel, mailto, market 등)를 연다.
     *
     * ★ intent:// 는 반드시 Intent.parseUri(URI_INTENT_SCHEME) 로 풀어야 한다.
     *   토스 결제창은 안드로이드에서 카드사·카카오페이 앱을
     *   `intent://…#Intent;scheme=ispmobile;package=kvp.jjy.MispAndroid320;end` 형태로 부른다.
     *   이걸 ACTION_VIEW 에 그대로 넣으면 받을 앱이 없어 예외가 나고, 예전 코드는 그 예외를
     *   삼켜서 결제 버튼을 눌러도 아무 일도 일어나지 않았다(테스트 키에선 안 드러나고 운영 키에서 터진다).
     *
     * ★ 앱이 설치돼 있지 않으면 browser_fallback_url → 없으면 플레이스토어 설치 화면으로 보낸다.
     * ★ 보안: 웹 페이지가 만든 intent 로 우리 앱·다른 앱의 내부 화면을 직접 열지 못하게
     *   component·selector 를 지우고 BROWSABLE 카테고리만 허용한다.
     */
    private fun openExternalScheme(url: String) {
        try {
            if (url.startsWith("intent:", ignoreCase = true)) {
                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    component = null
                    selector = null
                }
                try {
                    startActivity(intent)
                } catch (e: android.content.ActivityNotFoundException) {
                    val fallback = intent.getStringExtra("browser_fallback_url")
                    val pkg = intent.`package`
                    when {
                        !fallback.isNullOrBlank() -> webView.loadUrl(fallback)
                        !pkg.isNullOrBlank() -> startActivity(
                            Intent(Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$pkg"))
                        )
                        else -> Log.w("WebViewActivity", "외부 앱 없음: $url")
                    }
                }
                return
            }
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        } catch (e: Exception) {
            // 앱이 없거나 잘못된 주소 — 결제가 조용히 멈추지 않게 원인을 남긴다.
            Log.w("WebViewActivity", "외부 스킴 열기 실패: $url (${e.message})")
        }
    }

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
                // webReady 를 안 보내는 옛 웹 안전망. 준비 전이면 가드가 0 을 돌려 그대로 남는다.
                if (!webReady) flushPendingJs()
            }
        }
        webView.webChromeClient = object : android.webkit.WebChromeClient() {
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
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() {
        // 다른 화면(네이티브·카드사 앱)이 덮으면 웹에 '숨김'을 알린다(onResume 의 짝).
        //  애니메이션·위치 등 웹뷰 부가 처리만 멈추고 JS 타이머는 멈추지 않는다(pauseTimers 는 부르지 않는다).
        if (::webView.isInitialized) webView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        downloadInterface.cleanup()
        super.onDestroy()
    }
}