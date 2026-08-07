package com.muyeon.app.ui.device_info

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@RequiresApi(Build.VERSION_CODES.N_MR1)
@Composable
fun DeviceInfoScreen(viewModel: DeviceInfoViewModel = viewModel(factory = DeviceInfoViewModelFactory(context = LocalContext.current))) {
    val deviceInfo by viewModel.deviceInfo.observeAsState()

    LaunchedEffect(Unit) {
        viewModel.getDeviceInfo()
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (deviceInfo != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Device Infor",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Device ID: ${deviceInfo?.deviceId}")
                Text(text = "Device Name: ${deviceInfo?.deviceName}")
                Text(text = "Platfrom: ${deviceInfo?.systemName}")
            }
        } else {
            CircularProgressIndicator()
        }
    }
}
