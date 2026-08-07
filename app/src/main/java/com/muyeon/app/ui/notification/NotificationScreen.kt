package com.muyeon.app.ui.notification

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import android.app.Activity
import androidx.annotation.RequiresApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.collectAsState
import com.muyeon.app.domain.use_cases.RequestNotificationPermissionUseCase

@Suppress("unused")
fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun NotificationScreen(
    viewModel: NotificationViewModel = viewModel(
        factory = NotificationViewModelFactory (
            RequestNotificationPermissionUseCase()
        )
    )
) {
    val context = LocalContext.current

    val isPermissionGranted = viewModel.isPermissionGranted.collectAsState().value

    val statusText = if (isPermissionGranted == true) {
        "Granted"
    } else if (isPermissionGranted == false) {
        "Denied"
    } else {
        "Checking..."
    }
    val statusColor = if (isPermissionGranted == true) {
        Color.Green
    } else if (isPermissionGranted == false) {
        Color.Red
    } else {
        Color.Gray
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            viewModel.updatePermissionStatus(isGranted)
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E88E5)
            ),
            shape = RoundedCornerShape(10.dp),
            onClick = {
                val hasPermission =
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

                (context as? Activity)?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)
                    ?: false

                viewModel.requestNotificationPermission(
                    hasPermission = hasPermission,
                    launcher = requestPermissionLauncher
                )
            }
        ) {
            Text("Request Notification Permission")
        }

        Text("Permission Status: $statusText", color = statusColor)
    }
}
