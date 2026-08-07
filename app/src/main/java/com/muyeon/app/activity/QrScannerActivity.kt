package com.muyeon.app.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.muyeon.app.ui.qr.QrScannerScreen
import com.muyeon.app.ui.qr.ResultQRNavigationEvent
import com.muyeon.app.ui.qr.ResultQRScreen
import com.muyeon.app.ui.qr.ResultQRViewModel
import com.muyeon.app.ui.qr.ResultQRViewModelFactory
import com.muyeon.app.webview.ScanQRWebViewInterface

class QrScannerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scanQRInterface = ScanQRWebViewInterface.getInstance()

        if (scanQRInterface == null) {
            Log.e("QrScannerActivity", "scanQRInterface is null, finishing activity")
            finish()
            return
        }

        setContent {
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = "qr_scanner"
            ) {
                composable("qr_scanner") {
                    QrScannerScreen(
                        onQrScanned = { result ->
                            navController.navigate("result_qr/$result")
                        },
                        onNavigateBack = {
                            finish()
                        }
                    )
                }

                composable("result_qr/{qrResult}") { backStackEntry ->
                    val qrResult = backStackEntry.arguments?.getString("qrResult") ?: ""

                    val viewModel: ResultQRViewModel = viewModel(
                        factory = ResultQRViewModelFactory(qrResult, scanQRInterface)
                    )

                    LaunchedEffect(Unit) {
                        viewModel.navigationEvent.collect { event ->
                            when (event) {
                                is ResultQRNavigationEvent.NavigateBack -> {
                                    Log.d("QrScannerActivity", "Finishing activity from navigation event")
                                    finish()
                                }
                            }
                        }
                    }

                    ResultQRScreen(
                        result = qrResult,
                        scanQRInterface = scanQRInterface,
                        viewModel = viewModel,
                        navController = navController
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ScanQRWebViewInterface.setInstance(null)
    }
}