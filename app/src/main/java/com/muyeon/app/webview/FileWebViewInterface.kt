package com.muyeon.app.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.core.content.ContextCompat
import com.muyeon.app.data.models.file.FileImageInfo
import com.muyeon.app.data.models.webview.WebViewResponse
import com.google.gson.GsonBuilder
import androidx.core.content.edit
import com.muyeon.app.R
import com.muyeon.app.common_components.dialog.ContentAlignment
import com.muyeon.app.ui.imagepicker.FileViewModel
import com.muyeon.app.utils.PermissionManager

class FileWebViewInterface(
    private val context: Context,
    private val webView: WebView,
    private val filePermissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    private val imagePickerLauncher: androidx.activity.result.ActivityResultLauncher<Intent>,
    private val fileViewModel: FileViewModel,
    private val onShowDialog: (
        title: String,
        content: String?,
        leftButtonText: String?,
        rightButtonText: String,
        buttonCount: Int,
        alignment: ContentAlignment,
        onLeftClick: () -> Unit,
        onRightClick: () -> Unit
    ) -> Unit
) {
    private val gson = GsonBuilder().create()
    private var isPendingFilePicker = false
    private var lastPickerType: Int? = null

    private val prefs =
        context.getSharedPreferences(PermissionManager.PREFS_NAME, Context.MODE_PRIVATE)

    @RequiresApi(Build.VERSION_CODES.O)
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showPermissionSettingsDialog(title: String, content: String) {
        onShowDialog(
            title,
            content,
            context.getString(R.string.cancel),
            context.getString(R.string.setting),
            2,
            ContentAlignment.Middle,
            {},
            {
                openAppSettings()
            }
        )
    }


    @RequiresApi(Build.VERSION_CODES.O)
    @JavascriptInterface
    fun getFilePicker() {
        // Google Play 사진/동영상 권한 정책 준수:
        // READ_MEDIA_IMAGES 등 권한 요청 없이, 권한이 필요 없는 시스템 Photo Picker 를 바로 사용한다.
        try {
            val activity = context as? Activity
                ?: throw IllegalStateException("Context must be an Activity")

            val pickImages = Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                type = com.muyeon.app.utils.Constants.TYPE
            }
            if (pickImages.resolveActivity(activity.packageManager) != null) {
                // Android 13+ (및 Photo Picker 지원 단말): 시스템 Photo Picker
                imagePickerLauncher.launch(pickImages)
            } else {
                // 미지원 단말 폴백: 시스템 문서 선택기 (권한 불필요)
                val getContent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    putExtra(Intent.EXTRA_LOCAL_ONLY, true)
                }
                imagePickerLauncher.launch(Intent.createChooser(getContent, "Choose File"))
            }
        } catch (e: Exception) {
            Log.d("FileWebViewInterface", "Error: $e")
            sendErrorToWeb("InternalError", 500)
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @RequiresExtension(extension = Build.VERSION_CODES.R, version = 2)
    fun handlePermissionResult(permissions: Map<String, Boolean>) {
        if (!isPendingFilePicker) return

        val hasFullAccess = permissions[android.Manifest.permission.READ_MEDIA_IMAGES] == true ||
                permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE] == true

        val hasLimitedAccess = permissions[android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true

        if (hasFullAccess || hasLimitedAccess) {
            val newAllowedImages = getAllowedImageUris()
            saveAllowedImages(newAllowedImages)
            fileViewModel.updateAllowedImages(newAllowedImages)

            if (hasLimitedAccess && !hasFullAccess) {
                fileViewModel.showPicker()
            } else {
                launchImagePicker()
            }
        } else {
            sendErrorToWeb("PermissionDenied", 403)
        }
        isPendingFilePicker = false
        lastPickerType = null
    }

    fun handleActivityResult(resultCode: Int, data: Intent?) {
        isPendingFilePicker = false
        lastPickerType = null

        if (resultCode != Activity.RESULT_OK) {
            sendErrorToWeb("NoImageSelected", 400)
            return
        }

        val uri = data?.data
        if (uri == null) {
            sendErrorToWeb("NoImageSelected", 400)
            return
        }

        processSelectedImages(uri)
    }

    private fun getRequiredPermissions(): List<String> {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                listOf(
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                listOf(android.Manifest.permission.READ_MEDIA_IMAGES)

            else -> listOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun areAllPermissionsGranted(): Boolean {
        val fullAccessPermission = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> android.Manifest.permission.READ_MEDIA_IMAGES
            else -> android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(
            context,
            fullAccessPermission
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("ObsoleteSdkInt")
    @RequiresExtension(extension = Build.VERSION_CODES.R, version = 2)
    private fun launchImagePicker() {
        try {
            val activity =
                context as? Activity ?: throw IllegalStateException("Context must be an Activity")
            lastPickerType = com.muyeon.app.utils.Constants.FILE_PICKER_REQUEST_CODE
            context.contentResolver.notifyChange(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null)
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                    val hasFullAccess = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED
                    val hasLimitedAccess = ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasLimitedAccess && !hasFullAccess) {
                        val allowedImages = getAllowedImageUris()
                        saveAllowedImages(allowedImages)
                        fileViewModel.updateAllowedImages(allowedImages)
                        fileViewModel.showPicker()
                    } else {
                        launchPhotoPicker(activity)
                    }
                }

                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                    launchPhotoPicker(activity)
                }

                else -> {
                    launchImageGallery(activity)
                }
            }
        } catch (e: Exception) {
            Log.d("FileWebViewInterface", "Error: $e")
            sendErrorToWeb("InternalError", 500)
            isPendingFilePicker = false
            lastPickerType = null
        }
    }

    @RequiresExtension(extension = Build.VERSION_CODES.R, version = 2)
    private fun launchPhotoPicker(activity: Activity) {
        val intent = Intent(MediaStore.ACTION_PICK_IMAGES).apply {
            type = com.muyeon.app.utils.Constants.TYPE
        }
        if (intent.resolveActivity(activity.packageManager) != null) {
            imagePickerLauncher.launch(intent)
        } else {
            launchImageGallery(activity)
        }
    }

    private fun launchImageGallery(activity: Activity) {
        val intent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = com.muyeon.app.utils.Constants.TYPE
                putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            }
        if (intent.resolveActivity(activity.packageManager) != null) {
            imagePickerLauncher.launch(intent)
        } else {
            val fallbackIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = com.muyeon.app.utils.Constants.TYPE
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            }
            imagePickerLauncher.launch(Intent.createChooser(fallbackIntent, "Choose File"))
        }
    }

    fun processSelectedImages(uri: Uri) {
        try {
            val fileInfo = processImageFile(uri)
            if (fileInfo != null) {
                sendFilesToWeb(listOf(fileInfo))

            } else {
                sendErrorToWeb("ImageConversionFailed", 415)
            }
        } catch (e: Exception) {
            Log.d("FileWebViewInterface", "Error: $e")
            sendErrorToWeb("ImageConversionFailed", 415)
        }
    }

    private fun processImageFile(uri: Uri): FileImageInfo? {
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, flags)
            } catch (e: SecurityException) {
                Log.d("FileWebViewInterface", "SecurityException: $e")
            }
            val mimeType = context.contentResolver.getType(uri) ?: return null
            if (!isValidImageType(mimeType)) return null

            val fullFileName = sanitizeFileName(getFileName(uri))
            val fileName = fullFileName.substringBeforeLast('.', fullFileName)
            val fileSize = getFileSize(uri)
            val fileExt = getFileExtension(fullFileName, mimeType)
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.readBytes()
            inputStream.close()

            if (!isValidImageContent(bytes)) return null

            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

            return FileImageInfo(base64, fileName, fileExt, fileSize, "image", mimeType)
        } catch (e: Exception) {
            Log.d("FileWebViewInterface", "Error: $e")
            return null
        }
    }

    private fun getFileExtension(fileName: String, mimeType: String): String {

        val extFromName = fileName.substringAfterLast('.', "")
        if (extFromName.isNotEmpty()) {
            return extFromName.lowercase()
        }
        return when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            "image/bmp" -> "bmp"
            "image/heic" -> "heic"
            "image/heif" -> "heif"
            else -> "jpg"
        }
    }

    private fun isValidImageType(mimeType: String): Boolean {
        return mimeType.startsWith("image/") && !mimeType.startsWith("video/")
    }

    private fun isValidImageContent(bytes: ByteArray): Boolean {
        return bytes.isNotEmpty()
    }

    private fun getFileName(uri: Uri): String {
        var name = "image_${System.currentTimeMillis()}.jpg"
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) {
                        cursor.getString(idx)?.takeIf { it.isNotBlank() }?.let { name = it }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("FileWebViewInterface", "Error: $e")
        }
        return name
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\"'\\n\\r\\t\\p{Cntrl}]"), "_")
            .trim()
            .ifEmpty { "image_${System.currentTimeMillis()}.jpg" }
    }

    private fun getFileSize(uri: Uri): Long {
        var size = 0L
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx != -1) {
                        size = cursor.getLong(idx)
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("FileWebViewInterface", "Error: $e")
        }
        return size
    }

    private fun getAllowedImageUris(): List<Uri> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return emptyList()

        val uris = mutableListOf<Uri>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE
        )
        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val uri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )
                    uris.add(uri)
                }
            }
        } catch (e: Exception) {
            Log.d("FileWebViewInterface", "Error: $e")
        }
        return uris
    }

    private fun saveAllowedImages(uris: List<Uri>) {
        val sharedPrefs = context.getSharedPreferences("ImagePickerPrefs", Context.MODE_PRIVATE)
        sharedPrefs.edit {
            putStringSet("allowed_images", uris.map { it.toString() }.toSet())
        }
    }

    private fun sendFilesToWeb(files: List<FileImageInfo>) {
        val response = WebViewResponse.success(files)
        sendJsonToWeb(response)
    }

    fun sendErrorToWeb(message: String, status: Int) {
        val response = WebViewResponse.error<List<FileImageInfo>>(message, status, emptyList())
        sendJsonToWeb(response)
    }

    private fun sendJsonToWeb(data: Any) {
        val json = gson.toJson(data)
        val escaped = json
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")
            .replace("'", "\\'")

        val js = "window.setFilePicker('$escaped'); "

        webView.post {
            webView.evaluateJavascript(js, null)
        }
    }
}