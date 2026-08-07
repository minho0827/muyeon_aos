package com.muyeon.app.routers

import android.annotation.SuppressLint
import androidx.navigation.NavHostController

@Suppress("unused")
object AppRouter {
    @SuppressLint("StaticFieldLeak")
    private lateinit var navController: NavHostController

    fun init(controller: NavHostController) {
        navController = controller
    }

    fun navigateToHome() {
        navController.navigate("home") {
            popUpTo("home") { inclusive = true }
        }
    }

    fun navigateToDeviceInfo() {
        navController.navigate("deviceInfo")
    }

    fun navigateBack() {
        navController.popBackStack()
    }

    fun navigateToLocationPermission() {
        navController.navigate("locationPermission")
    }

    fun navigateToNotificationPermission() {
        navController.navigate("notificationPermission")
    }

    fun navigateToPermissionCameraImageAudio() {
        navController.navigate("permissionCameraImageAudio")
    }

    fun navigateToQR() {
        navController.navigate("qr")
    }

    fun navigateToResultQr(result: String) {
        val encodedResult = java.net.URLEncoder.encode(result, "UTF-8")
        navController.navigate("result_qr/$encodedResult")
    }
}
