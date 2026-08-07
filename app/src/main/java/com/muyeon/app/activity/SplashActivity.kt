package com.muyeon.app.activity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.muyeon.app.common_components.dialog.PermissionExplanationDialog
import com.muyeon.app.data.repository.AuthRepositoryImpl
import com.muyeon.app.routers.SplashRouterImpl
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
        }
        viewModel.checkTokenAndNavigate()
    }
}