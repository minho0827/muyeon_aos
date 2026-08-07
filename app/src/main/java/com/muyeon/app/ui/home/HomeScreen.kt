package com.muyeon.app.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import com.muyeon.app.routers.AppRouter

@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Welcome to HealthCareDiet!")
        Button(onClick = { AppRouter.navigateToDeviceInfo() }) {
            Text("Navigate to Device Info")
        }
        Button(onClick = { AppRouter.navigateToLocationPermission() }) {
            Text("Navigate to Location Permission")
        }
        Button(onClick = { AppRouter.navigateToNotificationPermission() }) {
            Text("Navigate to Notification Permission")
        }
        Button(onClick = { AppRouter.navigateToPermissionCameraImageAudio() }) {
            Text("Navigate to Permission Camera, Image, Audio")
        }

    }
}