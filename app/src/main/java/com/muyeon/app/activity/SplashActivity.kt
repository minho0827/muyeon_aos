package com.muyeon.app.activity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.muyeon.app.common_components.dialog.PermissionExplanationDialog
import com.muyeon.app.data.repository.AuthRepositoryImpl
import com.muyeon.app.routers.SplashRouterImpl
import com.muyeon.app.ui.splash.AppGateApi
import com.muyeon.app.ui.splash.AppGateDismiss
import com.muyeon.app.ui.splash.AppGateNotice
import com.muyeon.app.ui.splash.AppGateUpdate
import com.muyeon.app.ui.splash.SplashScreen
import com.muyeon.app.ui.splash.SplashViewModel
import com.muyeon.app.utils.PermissionManager
import com.muyeon.app.utils.QrPageManager

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {
    private lateinit var viewModel: SplashViewModel
    private lateinit var navigator: SplashRouterImpl
    private lateinit var authRepository: AuthRepositoryImpl
    private lateinit var permissionManager: PermissionManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showDeepLinkParamsIfPresent()

        navigator = SplashRouterImpl(this)
        authRepository = AuthRepositoryImpl(this)
        viewModel = SplashViewModel(navigator)

        permissionManager = PermissionManager(this) {
            proceedToSplashScreen()
        }
        permissionManager.initialize()

        setContent {
            PermissionScreen()
        }
    }

    @Composable
    private fun PermissionScreen() {
        var showExplanationDialog by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            permissionManager.setExplanationDialogCallback { show ->
                showExplanationDialog = show
            }
            permissionManager.startPermissionFlow()
        }

        if (showExplanationDialog) {
            PermissionExplanationDialog(
                onConfirm = {
                    permissionManager.onExplanationDialogConfirmed()
                },
                onDismiss = {
                }
            )
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showDeepLinkParamsIfPresent()
    }

    private fun showDeepLinkParamsIfPresent() {
        val uri = intent?.data
        android.util.Log.d("QR_DEBUG", "🔗 SplashActivity 딥링크 uri=$uri")
        if (uri == null) return
        val qrPageValue = uri.getQueryParameter("qrPage")
        android.util.Log.d("QR_DEBUG", "🔗 qrPage 파라미터=$qrPageValue")
        if (qrPageValue == null) return
        QrPageManager.save(qrPageValue)
        android.util.Log.d("QR_DEBUG", "🔗 QrPageManager에 저장 완료=$qrPageValue")
    }

    private fun proceedToSplashScreen() {
        setContent {
            SplashScreen()
            AppGateGate()
        }
    }

    /**
     * 앱 시작 게이트 — 업데이트 안내 / 공지 팝업을 띄우고, 끝나면 웹뷰로 넘긴다.
     *
     * ⚠️ 서버 응답이 없으면(장애·비행기모드) 곧장 진입한다. 여기서 막으면 서버 장애가
     *    곧 앱 마비가 된다. 판정은 전부 서버가 하고 앱은 action 만 따른다.
     * ⚠️ 강제 업데이트는 닫을 수 없다 — onDismissRequest 를 비우고 취소 버튼도 없앤다.
     *    뒤로가기로 빠져나가면 강제가 아니게 된다.
     */
    @Composable
    private fun AppGateGate() {
        var update by remember { mutableStateOf<AppGateUpdate?>(null) }
        var notice by remember { mutableStateOf<AppGateNotice?>(null) }
        var stage by remember { mutableStateOf("loading") } // loading | update | notice | done

        LaunchedEffect(Unit) {
            val gate = AppGateApi.fetch()
            val action = gate?.update?.action ?: "NONE"
            notice = gate?.notice
            if (action == "FORCE" || action == "OPTIONAL") {
                update = gate?.update
                stage = "update"
            } else {
                stage = "notice"
            }
        }

        // 공지 차례 — 이미 '안 보기' 한 건은 건너뛴다.
        LaunchedEffect(stage) {
            if (stage == "notice") {
                val n = notice
                if (n == null || AppGateDismiss.isDismissed(this@SplashActivity, n)) stage = "done"
            }
            if (stage == "done") viewModel.checkTokenAndNavigate()
        }

        val u = update
        if (stage == "update" && u != null) {
            val forced = u.action == "FORCE"
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { if (!forced) stage = "notice" },
                title = { androidx.compose.material3.Text(u.title ?: "새 버전이 있습니다") },
                text = { androidx.compose.material3.Text(u.message ?: "") },
                confirmButton = {
                    // ⚠️ 스토어만 열고 흐름은 진행시키지 않는다(stage 를 그대로 둔다).
                    //    전에는 여기서 곧장 다음 단계로 넘겼는데, 그 사이 앱이 백그라운드로 가서
                    //    스토어에 다녀오면 공지 팝업이 떠 있었다. 업데이트하러 간 사람이 그냥 앱에
                    //    들어와졌고, 업데이트를 안 하고 돌아왔어도 다시 물어볼 기회가 없었다.
                    //    Compose 다이얼로그는 스스로 닫히지 않으므로 복귀하면 안내가 그대로 남는다.
                    androidx.compose.material3.TextButton(onClick = {
                        openStore(u.storeUrl)
                    }) { androidx.compose.material3.Text("업데이트") }
                },
                // 진행은 '다음에' 를 눌렀을 때만. 강제는 이 버튼 자체가 없어 빠져나갈 길이 없다.
                dismissButton = if (forced) null else {
                    { androidx.compose.material3.TextButton(onClick = { stage = "notice" }) {
                        androidx.compose.material3.Text("다음에")
                    } }
                },
            )
        }

        val n = notice
        if (stage == "notice" && n != null && !AppGateDismiss.isDismissed(this@SplashActivity, n)) {
            NoticePopup(n) { AppGateDismiss.remember(this@SplashActivity, n); stage = "done" }
        }
    }

    /**
     * 공지 팝업 — 이미지를 띄울 수 있어야 해서 AlertDialog 대신 커스텀 Dialog 를 쓴다.
     * (AlertDialog 는 title/text 가 문자열 슬롯이라 이미지를 넣을 자리가 없다)
     *
     * 이미지는 Coil AsyncImage — 이미 chat/quote 화면에서 쓰는 것과 같은 방식이라
     * 의존성을 새로 넣지 않는다. 로딩 실패·없음이면 그 영역만 빠지고 나머지는 그대로 뜬다.
     */
    @Composable
    private fun NoticePopup(n: AppGateNotice, onClose: () -> Unit) {
        androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.fillMaxWidth(),
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    n.imageUrlAbsolute?.let { url ->
                        coil3.compose.AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                // 세로로 긴 이미지가 팝업을 화면 밖까지 밀지 않게 상한을 둔다.
                                .heightIn(max = 320.dp),
                        )
                    }
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(20.dp),
                    ) {
                        androidx.compose.material3.Text(
                            n.title,
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        )
                        if (!n.body.isNullOrBlank()) {
                            androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
                            androidx.compose.material3.Text(
                                n.body,
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            )
                        }
                        androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
                        androidx.compose.material3.TextButton(
                            onClick = onClose,
                            modifier = Modifier.align(androidx.compose.ui.Alignment.End),
                        ) { androidx.compose.material3.Text("확인") }
                    }
                }
            }
        }
    }

    private fun openStore(url: String?) {
        if (url.isNullOrBlank()) return
        try {
            startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        } catch (e: Exception) {
            // 스토어 앱도 브라우저도 없는 기기 — 진입을 막지는 않는다.
        }
    }
}