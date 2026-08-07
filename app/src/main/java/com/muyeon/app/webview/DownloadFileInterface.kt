package com.muyeon.app.webview

import android.app.DownloadManager
import android.content.Context
import android.database.Cursor
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.MimeTypeMap
import android.webkit.WebView
import androidx.core.net.toUri
import com.muyeon.app.data.models.download_file.DownloadFileInfo
import com.muyeon.app.data.models.download_file.DownloadResult
import com.muyeon.app.data.models.webview.WebViewResponse
import com.muyeon.app.utils.WebMessageStatus
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.net.HttpURLConnection
import java.net.URL

class DownloadWebViewInterface(
    context: Context,
    private val webView: WebView
) {
    private val gson = GsonBuilder().create()
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val pollingHandler = Handler(Looper.getMainLooper())

    private val pendingDownloads = mutableMapOf<Long, DownloadResult>()

    private val pollingRunnable: Runnable = object : Runnable {
        override fun run() {
            if (!isNetworkAvailable()) {
                handleNetworkLost()
                return
            }

            checkAndSendResults()

            if (pendingDownloads.isNotEmpty() && !allDownloadsCurrentlyCompleted()) {
                pollingHandler.postDelayed(this, com.muyeon.app.utils.Constants.POLLING_INTERVAL_MS)
            }
        }
    }

    companion object {
        private const val TAG = "DownloadWebViewInterface"

        private val MIME_TYPE_TO_EXTENSION = mapOf(
            "application/pdf" to "pdf", "image/jpeg" to "jpg", "image/jpg" to "jpg",
            "image/png" to "png", "image/gif" to "gif", "image/webp" to "webp",
            "image/svg+xml" to "svg", "video/mp4" to "mp4", "video/mpeg" to "mpeg",
            "video/webm" to "webm", "audio/mpeg" to "mp3", "audio/wav" to "wav",
            "application/zip" to "zip", "application/x-rar-compressed" to "rar",
            "application/vnd.ms-excel" to "xls",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" to "xlsx",
            "application/msword" to "doc",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to "docx",
            "text/plain" to "txt", "text/csv" to "csv", "application/json" to "json"
        )

        private val EXTENSION_TO_MIME_TYPE = MIME_TYPE_TO_EXTENSION.entries
            .associate { it.value to it.key }
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking network availability: ${e.message}", e)
            false
        }
    }

    private fun handleNetworkLost() {
        val finalResults = mutableListOf<DownloadResult>()

        pendingDownloads.forEach { (downloadId, currentResult) ->
            if (downloadId < 0) {
                finalResults.add(currentResult.copy(status = WebMessageStatus.FAILED.message))
                return@forEach
            }

            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor: Cursor? = downloadManager.query(query)

            cursor?.use {
                if (it.moveToFirst()) {
                    val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = it.getInt(statusIndex)

                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            Log.d(TAG, "Download $downloadId already completed, keeping it")
                            finalResults.add(currentResult.copy(status = WebMessageStatus.SUCCESS.message))
                        }
                        DownloadManager.STATUS_FAILED -> {
                            finalResults.add(currentResult.copy(status = WebMessageStatus.FAILED.message))
                        }
                        else -> {
                            try {
                                val removed = downloadManager.remove(downloadId)
                                Log.d(TAG, "Cancelled incomplete download $downloadId due to network loss. Removed: $removed")
                            } catch (e: Exception) {
                                Log.e(TAG, "Error removing download $downloadId: ${e.message}", e)
                            }
                            finalResults.add(currentResult.copy(status = WebMessageStatus.FAILED.message))
                        }
                    }
                } else {
                    finalResults.add(currentResult.copy(status = WebMessageStatus.FAILED.message))
                }
            } ?: run {
                finalResults.add(currentResult.copy(status = WebMessageStatus.FAILED.message))
            }
        }

        stopPolling()

        if (finalResults.isNotEmpty()) {
            sendDownloadMultipleSuccessResponseToWeb(finalResults)
        }

        pendingDownloads.clear()
    }

    private fun isUrlActive(url: String): Boolean {
        return try {
            val connection = URL(url).openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            (connection as? HttpURLConnection)?.let { http ->
                http.requestMethod = "HEAD"
                val responseCode = http.responseCode

                return responseCode in 200..399
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "URL Check failed for $url: ${e.message}", e)
            false
        }
    }

    private fun handleNoNetworkAtStart(fileList: List<DownloadFileInfo>) {

        val failedResults = fileList.map { fileInfo ->
            DownloadResult(
                fileName = fileInfo.fileName,
                status = WebMessageStatus.FAILED.message
            )
        }

        sendDownloadMultipleSuccessResponseToWeb(failedResults)
    }

    @JavascriptInterface
    fun getDownloadFiles(filesJson: String) {
        try {
            val wrapperType = object : TypeToken<Map<String, Any>>() {}.type
            val wrapper: Map<String, Any> = gson.fromJson(filesJson, wrapperType)

            val dataJson = gson.toJson(wrapper["data"])
            val listType = object : TypeToken<List<DownloadFileInfo>>() {}.type
            val fileList: List<DownloadFileInfo> = gson.fromJson(dataJson, listType)

            if (fileList.isEmpty()) {
                return
            }

            if (!isNetworkAvailable()) {
                handleNoNetworkAtStart(fileList)
                return
            }

            pendingDownloads.clear()
            stopPolling()

            var downloadsStarted = false

            fileList.forEach { fileInfo ->
                if (!isUrlActive(fileInfo.url)) {
                    pendingDownloads[-1 * System.currentTimeMillis()] = DownloadResult(
                        fileName = fileInfo.fileName,
                        status = WebMessageStatus.FAILED.message,
                    )
                } else {
                    try {
                        val finalFileName = getFinalFileName(fileInfo.url, fileInfo.fileName)
                        val downloadId = downloadSingleFile(fileInfo.url, finalFileName)

                        pendingDownloads[downloadId] = DownloadResult(
                            fileName = finalFileName,
                            status = WebMessageStatus.FAILED.message,
                        )
                        downloadsStarted = true
                    } catch (e: Exception) {
                        pendingDownloads[-1 * System.currentTimeMillis()] = DownloadResult(
                            fileName = fileInfo.fileName,
                            status = WebMessageStatus.FAILED.message,
                        )
                    }
                }
            }

            if (pendingDownloads.isEmpty() && !downloadsStarted) {
                sendErrorResponseToWeb()
            } else {
                startPolling()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing files JSON: ${e.message}", e)
            sendErrorResponseToWeb()
        }
    }

    private fun startPolling() {
        pollingHandler.removeCallbacks(pollingRunnable)
        pollingHandler.post(pollingRunnable)
    }

    private fun stopPolling() {
        pollingHandler.removeCallbacks(pollingRunnable)
    }

    private fun allDownloadsCurrentlyCompleted(): Boolean {
        var allCompleted = true

        val downloadsToUpdate = mutableListOf<Pair<Long, DownloadResult>>()

        pendingDownloads.forEach { (downloadId, currentResult) ->
            if (downloadId < 0) {
                return@forEach
            }

            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)

            cursor?.use {
                if (it.moveToFirst()) {
                    val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val status = it.getInt(statusIndex)

                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            downloadsToUpdate.add(downloadId to currentResult.copy(status = WebMessageStatus.SUCCESS.message))
                        }
                        DownloadManager.STATUS_FAILED -> {
                            downloadsToUpdate.add(downloadId to currentResult.copy(status = WebMessageStatus.FAILED.message))
                        }
                        else -> {
                            allCompleted = false
                        }
                    }
                } else {
                    downloadsToUpdate.add(downloadId to currentResult.copy(status = WebMessageStatus.FAILED.message))
                }
            } ?: run {
                allCompleted = false
            }
        }

        downloadsToUpdate.forEach { (id, result) ->
            pendingDownloads[id] = result
        }

        return allCompleted
    }

    private fun checkAndSendResults() {
        val allCompleted = allDownloadsCurrentlyCompleted()

        pendingDownloads.count { (downloadId, _) ->
            if (downloadId < 0) return@count true
            val status = getDownloadStatus(downloadId)
            status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED
        }

        if (allCompleted) {
            val results = pendingDownloads.values.toList()

            sendDownloadMultipleSuccessResponseToWeb(results)

            stopPolling()
            pendingDownloads.clear()
        } else {
            Log.d(TAG, "Still waiting for downloads to complete...")
        }
    }

    private fun getDownloadStatus(downloadId: Long): Int {
        if (downloadId < 0) return DownloadManager.STATUS_FAILED

        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor: Cursor? = downloadManager.query(query)

        cursor?.use {
            if (it.moveToFirst()) {
                val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                return it.getInt(statusIndex)
            }
        }
        return -1
    }

    @Suppress("unused")
    private fun getErrorMessage(reason: Int): String {
        return when (reason) {
            DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume download"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "No external storage device found"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File already exists"
            DownloadManager.ERROR_FILE_ERROR -> "Storage error"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP data error"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Insufficient space"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Unhandled HTTP code"
            DownloadManager.ERROR_UNKNOWN -> "Unknown error"
            else -> "Download failed with code: $reason"
        }
    }

    fun cleanup() {
        stopPolling()
        pendingDownloads.clear()
    }

    private fun downloadSingleFile(url: String, fileName: String): Long {
        if (url.isBlank() || fileName.isBlank()) {
            throw IllegalArgumentException("URL or fileName is blank")
        }

        val extensionFromUrl = getExtensionFromUrl(url)
        val finalFileName = ensureFileExtension(fileName, extensionFromUrl, url)
        val mimeType = getMimeTypeFromUrl(url)
            ?: getMimeTypeFromExtension(finalFileName)
            ?: guessMimeTypeFromFileName(finalFileName)

        val request = DownloadManager.Request(url.toUri()).apply {
            setTitle(finalFileName)
            setDescription("Downloading: $finalFileName")

            mimeType?.let {
                setMimeType(it)
            }

            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, finalFileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        return downloadManager.enqueue(request)
    }

    private fun getFinalFileName(url: String, fileName: String): String {
        val extensionFromUrl = getExtensionFromUrl(url)
        return ensureFileExtension(fileName, extensionFromUrl, url)
    }

    private fun getExtensionFromUrl(url: String): String? {
        return try {
            val urlObj = URL(url)
            var path = urlObj.path

            if (path.contains('?')) {
                path = path.substringBefore('?')
            }

            val lastSegment = path.substringAfterLast('/')

            if (lastSegment.contains('.')) {
                val ext = lastSegment.substringAfterLast('.').lowercase()
                if (isValidExtension(ext)) {
                    return ext
                }
            }

            null
        } catch (e: Exception) {
            Log.w(TAG, "Could not parse URL: ${e.message}")
            null
        }
    }

    private fun ensureFileExtension(fileName: String, extensionFromUrl: String?, url: String): String {
        val currentExtension = if (fileName.contains('.')) {
            fileName.substringAfterLast('.').lowercase()
        } else {
            null
        }

        if (currentExtension != null && isValidExtension(currentExtension)) {
            return fileName
        }

        if (currentExtension == null && extensionFromUrl != null) {
            return "$fileName.$extensionFromUrl"
        }

        if (currentExtension != null && !isValidExtension(currentExtension) && extensionFromUrl != null) {
            val nameWithoutExt = fileName.substringBeforeLast('.')
            return "$nameWithoutExt.$extensionFromUrl"
        }

        val detectedExtension = detectExtensionFromUrl(url)
        if (detectedExtension != null) {
            val baseFileName = if (currentExtension != null) {
                fileName.substringBeforeLast('.')
            } else {
                fileName
            }
            return "$baseFileName.$detectedExtension"
        }

        val mimeType = getMimeTypeFromUrl(url)
        if (mimeType != null) {
            val guessedExtension = MIME_TYPE_TO_EXTENSION[mimeType]
            if (guessedExtension != null) {
                val baseFileName = if (currentExtension != null) {
                    fileName.substringBeforeLast('.')
                } else {
                    fileName
                }
                return "$baseFileName.$guessedExtension"
            }
        }

        if (currentExtension == null) {
            val defaultExt = when {
                url.contains("image", ignoreCase = true) -> "jpg"
                url.contains("photo", ignoreCase = true) -> "jpg"
                url.contains("picture", ignoreCase = true) -> "jpg"
                url.contains("document", ignoreCase = true) -> "pdf"
                url.contains("spreadsheet", ignoreCase = true) -> "xlsx"
                url.contains("excel", ignoreCase = true) -> "xlsx"
                url.contains("word", ignoreCase = true) -> "docx"
                else -> "bin"
            }
            return "$fileName.$defaultExt"
        }

        return fileName
    }

    private fun detectExtensionFromUrl(url: String): String? {
        return when {
            url.contains("/upload/img/", ignoreCase = true) -> {
                when {
                    url.endsWith(".png", ignoreCase = true) -> "png"
                    url.endsWith(".jpg", ignoreCase = true) -> "jpg"
                    url.endsWith(".jpeg", ignoreCase = true) -> "jpg"
                    url.endsWith(".gif", ignoreCase = true) -> "gif"
                    url.endsWith(".webp", ignoreCase = true) -> "webp"
                    else -> "jpg"
                }
            }
            url.contains("placeholder", ignoreCase = true) -> "jpg"
            url.contains(".pdf", ignoreCase = true) -> "pdf"
            url.contains(".xlsx", ignoreCase = true) -> "xlsx"
            url.contains(".docx", ignoreCase = true) -> "docx"
            url.contains(".zip", ignoreCase = true) -> "zip"
            url.contains(".csv", ignoreCase = true) -> "csv"
            url.contains(".json", ignoreCase = true) -> "json"
            else -> null
        }
    }

    private fun isValidExtension(extension: String): Boolean {
        val valid = extension.length in 2..5 &&
                extension.all { it.isLetterOrDigit() } &&
                MIME_TYPE_TO_EXTENSION.values.contains(extension.lowercase())
        return valid
    }

    private fun getMimeTypeFromUrl(url: String): String? {
        return try {
            val extension = getExtensionFromUrl(url) ?: return null
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        } catch (e: Exception) {
            null
        }
    }

    private fun getMimeTypeFromExtension(fileName: String): String? {
        return try {
            val extension = if (fileName.contains('.')) {
                fileName.substringAfterLast('.').lowercase()
            } else {
                return null
            }
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        } catch (e: Exception) {
            null
        }
    }

    private fun guessMimeTypeFromFileName(fileName: String): String? {
        val extension = if (fileName.contains('.')) {
            fileName.substringAfterLast('.').lowercase()
        } else {
            return null
        }
        return EXTENSION_TO_MIME_TYPE[extension]
    }

    private fun sendDownloadMultipleSuccessResponseToWeb(
        results: List<DownloadResult>,
    ) {
        val response = WebViewResponse.success(results)
        sendJsonToWeb(response)
    }

    private fun sendErrorResponseToWeb() {
        val response = WebViewResponse.failed(null)
        sendJsonToWeb(response)
    }

    private fun sendJsonToWeb(data: Any) {
        val jsFunctionName = "setDownloadFiles"
        val json = gson.toJson(data)
        val escaped = json
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

        val js = "window.$jsFunctionName('$escaped');"

        webView.post {
            webView.evaluateJavascript(js) { result ->
                Log.d(TAG, "JS executed with result: $result")
            }
        }
    }
}