package com.muyeon.app.routers

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.muyeon.app.ui.device_info.DeviceInfoScreen
import com.muyeon.app.ui.home.HomeScreen
import com.muyeon.app.ui.location.LocationScreen
import com.muyeon.app.ui.notification.NotificationScreen
import com.muyeon.app.ui.permission_media.PermissionScreen
import com.muyeon.app.ui.qr.QrScannerScreen
import com.muyeon.app.ui.qr.ResultQRScreen
import com.muyeon.app.webview.ScanQRWebViewInterface

@Composable
fun AppNavHost(
    navController: NavHostController, scanQRInterface: ScanQRWebViewInterface?
) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen()
        }
        composable("deviceInfo") {
            DeviceInfoScreen()
        }
        composable("locationPermission") {
            LocationScreen()
        }
        composable("notificationPermission") {
            NotificationScreen()
        }
        composable("permissionCameraImageAudio") {
            PermissionScreen()
        }

        composable("qr") {
            QrScannerScreen(
                onQrScanned = { result ->
                    navController.navigate("result_qr/$result")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "result_qr/{result}",
            arguments = listOf(navArgument("result") { type = NavType.StringType })
        ) { backStackEntry ->
            val qrResult = backStackEntry.arguments?.getString("result")

            if (qrResult != null) {
                ResultQRScreen(
                    qrResult,
                    navController,
                    scanQRInterface = scanQRInterface,
                )
            } else {
                AppRouter.navigateBack()
            }
        }
    }
}
