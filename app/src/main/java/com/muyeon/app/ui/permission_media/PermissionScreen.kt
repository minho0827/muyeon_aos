package com.muyeon.app.ui.permission_media

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.muyeon.app.data.repository.PermissionRepositoryImpl
import com.muyeon.app.domain.models.media.PermissionType
import com.muyeon.app.domain.use_cases.PermissionUseCase

@Composable
fun PermissionScreen(viewModel: PermissionViewModel = viewModel(
    factory = PermissionViewModelFactory (
        PermissionUseCase(PermissionRepositoryImpl())
    ))) {
    val context = LocalContext.current

    val camState by viewModel.cameraPermission.collectAsState()
    val photoState by viewModel.mediaPermission.collectAsState()

    val camLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        viewModel.onCameraPermissionResult(granted)
    }
    val mediaLauncher = rememberLauncherForActivityResult(RequestPermission()) {
        viewModel.onMediaPermissionResult(context)
    }

    val toRequest by viewModel.requestPermission.collectAsState(initial = null)
    LaunchedEffect(toRequest) {
        toRequest?.let { type ->
            val perm = when (type) {
                PermissionType.Camera -> Manifest.permission.CAMERA
                PermissionType.Gallery ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        Manifest.permission.READ_MEDIA_IMAGES
                    else
                        Manifest.permission.READ_EXTERNAL_STORAGE
            }
            if (type == PermissionType.Camera)
                camLauncher.launch(perm)
            else
                mediaLauncher.launch(perm)

            viewModel.onRequestHandled()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = { viewModel.onCameraClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Request camera permission",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Camera permission: ${camState.label}",
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = { viewModel.onGalleryClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Request photos library permission",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Photo permission: ${photoState.label}",
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }
    }
}
