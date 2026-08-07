package com.muyeon.app.webview

import android.net.Uri
import android.webkit.WebView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.muyeon.app.theme.HealthCareDietTheme
import com.muyeon.app.ui.imagepicker.ImagePickerBottomSheet

@Suppress("unused")
@Composable
fun WebViewScreen(
    webView: WebView,
    showImagePicker: Boolean,
    allowedImages: List<Uri>,
    onSelectImage: (Uri) -> Unit,
    onCancel: () -> Unit,
    onAddImages: () -> Unit,
    requestPermissions: () -> Unit
) {
    HealthCareDietTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) {
            Column {
                AndroidView(
                    factory = { webView },
                    modifier = Modifier.fillMaxSize()
                )
                if (showImagePicker) {
                    ImagePickerBottomSheet(
                        images = allowedImages,
                        onSelect = onSelectImage,
                        onCancel = onCancel,
                        onAddImages = onAddImages,
                        requestPermissions = requestPermissions
                    )
                }
            }
        }
    }
}