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
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.data.repository.LocationRepositoryImpl
import com.muyeon.app.domain.use_cases.RequestNotificationPermissionUseCase
import com.muyeon.app.theme.MuyeonTheme
import com.muyeon.app.ui.device_info.DeviceInfoViewModel
import com.muyeon.app.ui.device_info.DeviceInfoViewModelFactory
import com.muyeon.app.ui.imagepicker.FileViewModel
import com.muyeon.app.ui.imagepicker.ImagePickerBottomSheet
import com.muyeon.app.ui.notification.NotificationViewModel
import com.muyeon.app.ui.notification.NotificationViewModelFactory

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
@RequiresExtension(extension = Build.VERSION_CODES.R, version = 2)
class WebViewActivity : ComponentActivity() {
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
        webView.addJavascriptInterface(AppBridgeInterface(this, webView), "AppBridge")

        android.util.Log.d("QR_DEBUG", "🟢 WebViewActivity onCreate savedInstanceState=${savedInstanceState != null}, QrPageManager.value=${com.muyeon.app.utils.QrPageManager.getValue()}")
        // QR 식사평가(qrPage)가 있으면 복원하지 말고 홈을 새로 로드해 DefaultLayout 폴링이 처리하도록 함
        if (savedInstanceState != null && !com.muyeon.app.utils.QrPageManager.hasValue()) {
            android.util.Log.d("QR_DEBUG", "🟢 restoreState 호출 (qrPage 없음 → 이전 화면 복원)")
            webView.restoreState(savedInstanceState)
        } else {
            val baseUrl = com.muyeon.app.utils.Constants.getBaseUrl(this)
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

    /**
     * 네이티브 화면에서 돌아오며 요청한 SPA 이동/콜백 처리(NativeWebRoute).
     *  웹이 아직 로드 전이면 __nativeGo 가 없으므로 짧게 재시도한다(최대 3초).
     */
    private fun consumeNativeRoute(intent: Intent) {
        intent.getStringExtra(NativeWebRoute.EXTRA_GO_PATH)?.takeIf { it.isNotEmpty() }?.let { path ->
            intent.removeExtra(NativeWebRoute.EXTRA_GO_PATH)
            val safe = path.replace("'", "\\'")
            evalWhenReady("(function(){ if(window.__nativeGo){ window.__nativeGo('$safe'); return 1; } return 0; })()")
        }
        intent.getStringExtra(NativeWebRoute.EXTRA_EVAL_JS)?.takeIf { it.isNotEmpty() }?.let { js ->
            intent.removeExtra(NativeWebRoute.EXTRA_EVAL_JS)
            evalWhenReady("(function(){ try { $js } catch(e) {} return 1; })()")
        }
    }

    private fun evalWhenReady(script: String, attempt: Int = 0) {
        webView.evaluateJavascript(script) { result ->
            if (result != "1" && attempt < 10) {
                webView.postDelayed({ evalWhenReady(script, attempt + 1) }, 300)
            }
        }
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
                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                    true
                } catch (e: Exception) {
                    true
                }
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

    override fun onDestroy() {
        downloadInterface.cleanup()
        super.onDestroy()
    }
}