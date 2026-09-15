package com.muyeon.app.activity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                when {
                    n == null -> {
                        android.util.Log.d("AppGate", "공지 없음 → 진입"); stage = "done"
                    }
                    AppGateDismiss.isDismissed(this@SplashActivity, n) -> {
                        android.util.Log.d("AppGate", "공지 id ${n.id} 는 이미 닫음(${n.dismissType}) → 진입")
                        stage = "done"
                    }
                    else -> android.util.Log.d("AppGate", "공지 팝업 표시 id=${n.id} img=${n.imageUrlAbsolute ?: "없음"}")
                }
            }
            if (stage == "done") viewModel.checkTokenAndNavigate()
        }

        val u = update
        if (stage == "update" && u != null) {
            val forced = u.action == "FORCE"
            GateScrim {
                GateCard {
                    GateTitle(u.title ?: "새 버전이 있습니다")
                    if (!u.message.isNullOrBlank()) GateBody(u.message)
                    GateActions {
                        if (forced) {
                            // 강제면 '종료'만 — 업데이트를 안 할 거면 앱을 쓸 수 없다.
                            //  finishAffinity() 는 태스크의 액티비티를 전부 닫는다(SplashActivity 만
                            //  finish() 하면 뒤에 남은 화면으로 떨어질 수 있다).
                            GateButton("종료", danger = true) { finishAffinity() }
                        } else {
                            GateButton("다음에", subtle = true) { stage = "notice" }
                        }
                        // ⚠️ 스토어만 열고 흐름은 진행시키지 않는다(stage 를 그대로 둔다).
                        //    넘겨버리면 앱이 백그라운드로 가는 사이 다음 단계가 떠서,
                        //    스토어에 다녀왔을 때 공지 팝업이 올라와 있었다.
                        GateButton("업데이트") { openStore(u.storeUrl) }
                    }
                }
            }
        }

        val n = notice
        if (stage == "notice" && n != null && !AppGateDismiss.isDismissed(this@SplashActivity, n)) {
            NoticePopup(n) { AppGateDismiss.remember(this@SplashActivity, n); stage = "done" }
        }
    }

    /**
     * 공지 팝업 — 업데이트 안내와 같은 오버레이를 쓴다.
     * 이미지가 들어가야 해서 AlertDialog 는 애초에 못 쓴다(title/text 가 문자열 슬롯).
     */
    @Composable
    private fun NoticePopup(n: AppGateNotice, onClose: () -> Unit) {
        GateScrim {
            GateCard(padding = 0.dp) {
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    n.imageUrlAbsolute?.let { url ->
                        coil3.compose.AsyncImage(
                            model = url,
                            contentDescription = null,
                            onSuccess = { android.util.Log.d("AppGate", "이미지 로딩 성공") },
                            // 실패해도 팝업은 그대로 뜬다 — 이미지 자리만 빈다.
                            onError = { android.util.Log.d("AppGate", "이미지 로딩 실패: ${it.result.throwable.message}") },
                            contentScale = androidx.compose.ui.layout.ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                // 세로로 긴 이미지가 팝업을 화면 밖까지 밀지 않게 상한을 둔다.
                                .heightIn(max = 320.dp),
                        )
                    }
                    androidx.compose.foundation.layout.Column(Modifier.padding(20.dp)) {
                        GateTitle(n.title)
                        if (!n.body.isNullOrBlank()) GateBody(n.body)
                        GateActions { GateButton("확인", onClick = onClose) }
                    }
                }
            }
        }
    }

    // ── 게이트 공용 조각 — 직접 그린다. 시스템 다이얼로그를 쓰지 않는다 ──────────
    //
    // ⚠️ Material AlertDialog / Dialog 를 쓰지 않는 이유:
    //    · 프레임워크가 버튼을 손댈 여지가 없어야 한다. iOS 에서 .alert 가 cancel 역할 버튼이
    //      없다는 이유로 '취소'를 자동으로 끼워 넣었고, 그걸 누르면 아무 동작 없이 닫혀
    //      강제 업데이트가 뚫렸다. 같은 사고를 안드로이드에서도 만들지 않는다.
    //    · 강제 업데이트는 우리가 지울 때까지 절대 사라지면 안 된다.
    //    바깥을 눌러도 닫히지 않는다 — 스크림이 탭을 먹고 아무것도 하지 않는다.

    @Composable
    private fun GateScrim(content: @Composable () -> Unit) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color(0x73000000))
                .blockTaps(),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) { content() }
    }

    /** 바깥 탭을 흡수만 하고 아무 동작도 하지 않는다(리플도 없다). */
    @Composable
    private fun Modifier.blockTaps(): Modifier {
        val src = remember { MutableInteractionSource() }
        return this.clickable(interactionSource = src, indication = null, onClick = {})
    }

    @Composable
    private fun GateCard(padding: androidx.compose.ui.unit.Dp = 20.dp, content: @Composable () -> Unit) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Color.White,
                    androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                )
                .blockTaps()
                .padding(padding),
        ) { content() }
    }

    @Composable
    private fun GateTitle(text: String) {
        androidx.compose.material3.Text(
            text,
            color = androidx.compose.ui.graphics.Color.Black,
            fontSize = 17.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        )
    }

    @Composable
    private fun GateBody(text: String) {
        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
        androidx.compose.material3.Text(
            text,
            color = androidx.compose.ui.graphics.Color(0xFF4D4D4D),
            fontSize = 15.sp,
        )
    }

    @Composable
    private fun GateActions(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
        androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
            content = content,
        )
    }

    @Composable
    private fun GateButton(
        text: String,
        danger: Boolean = false,
        subtle: Boolean = false,
        onClick: () -> Unit,
    ) {
        androidx.compose.material3.TextButton(onClick = onClick) {
            androidx.compose.material3.Text(
                text,
                color = when {
                    danger -> androidx.compose.ui.graphics.Color(0xFFD32F2F)
                    subtle -> androidx.compose.ui.graphics.Color(0xFF595959)
                    else -> androidx.compose.ui.graphics.Color(0xFF1976D2)
                },
                fontSize = 15.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            )
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