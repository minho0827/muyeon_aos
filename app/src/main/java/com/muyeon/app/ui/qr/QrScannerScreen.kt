package com.muyeon.app.ui.qr

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.common_components.dialog.CustomDialog
import com.muyeon.app.webview.ScanQRWebViewInterface
import kotlinx.coroutines.flow.collectLatest
import com.muyeon.app.R

@Composable
fun QrScannerScreen(
    onQrScanned: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val scanQRInterface = ScanQRWebViewInterface.getInstance()
    val context = LocalContext.current

    val viewModel: QrScannerViewModel = viewModel(
        factory = QrScannerViewModelFactory(scanQRInterface)
    )
    val uiState by viewModel.uiState.collectAsState()
    var hasNavigated by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        viewModel.onPermissionResult(isGranted)
        if (!isGranted) {
            scanQRInterface?.sendQRResultToWeb(
                data = null,
                success = 401,
            )
            onNavigateBack()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collectLatest { event ->
            when (event) {
                is QrScannerNavigationEvent.NavigateBack -> onNavigateBack()
                is QrScannerNavigationEvent.NavigateToResult -> {
                    if (!hasNavigated) {
                        hasNavigated = true
                        onQrScanned(event.result)
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        val initialPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        viewModel.onPermissionResult(initialPermission)

        if (!initialPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isCameraPermissionGranted) {
                CameraView(
                    onBarcodeDetected = viewModel::handleBarcodeDetected,
                    onNavigateBack = onNavigateBack,
                )
            }
        }
    }

    QrParsingErrorDialog(uiState, viewModel)
}

@Composable
fun QrParsingErrorDialog(
    uiState: QrScannerUiState,
    viewModel: QrScannerViewModel
) {
    if (uiState.showParsingErrorDialog) {
        val dialogTitle = if (uiState.parsingError == "QR_PARSE_ERROR") {
            stringResource(R.string.fail_scan_qr_dialog)
        } else {
            uiState.parsingError ?: stringResource(R.string.fail_scan_qr_dialog)
        }
        CustomDialog(
            title = dialogTitle,
            rightButtonText = stringResource(R.string.ok),
            buttonCount = 1,
            alignment = ContentAlignment.Middle,
            onDismiss = viewModel::handleParsingErrorConfirmed,
            onRightButtonClick = viewModel::handleParsingErrorConfirmed,
            showPopup = true
        )
    }
}