package com.muyeon.app.ui.common

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil3.compose.AsyncImage
import com.muyeon.app.BuildConfig
import com.muyeon.app.theme.customFontFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream

/**
 * 전체화면 이미지 뷰어 — iOS `ChatImageViewerView` 1:1.
 *  좌우 스와이프 페이징 + 상단 닫기/카운터/저장 + 하단 도트.
 *
 * ⚠️ 이게 없으면 웹 `openImageViewer` 브릿지가 안드로이드에서 죽는다 —
 *   웹은 네이티브 핸들러가 있으면 자기 라이트박스를 띄우지 않기 때문에
 *   폴백이 없으면 이미지 탭이 아무 반응 없는 버튼이 된다.
 */
class ImageViewerActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_URLS = "urls"
        private const val EXTRA_INDEX = "index"
        private const val EXTRA_ALLOW_SAVE = "allowSave"

        /** urls 는 JSON 배열 문자열 우선, 실패하면 콤마 분해(웹 nativeBridge 규약과 동일). */
        fun start(context: Context, rawUrls: String, index: Int, allowSave: Boolean) {
            val urls = parseUrls(rawUrls)
            if (urls.isEmpty()) return
            val i = Intent(context, ImageViewerActivity::class.java)
                .putStringArrayListExtra(EXTRA_URLS, ArrayList(urls))
                .putExtra(EXTRA_INDEX, index)
                .putExtra(EXTRA_ALLOW_SAVE, allowSave)
            if (context !is Activity) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(i)
        }

        fun parseUrls(raw: String): List<String> {
            val fromJson = runCatching {
                val arr = JSONArray(raw)
                (0 until arr.length()).map { arr.optString(it) }
            }.getOrNull()
            return (fromJson ?: raw.split(",")).map { it.trim() }.filter { it.isNotEmpty() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val urls = intent.getStringArrayListExtra(EXTRA_URLS).orEmpty()
        if (urls.isEmpty()) { finish(); return }
        val initial = intent.getIntExtra(EXTRA_INDEX, 0).coerceIn(0, urls.size - 1)
        val allowSave = intent.getBooleanExtra(EXTRA_ALLOW_SAVE, false)

        setContent {
            ImageViewer(urls = urls, initialIndex = initial, allowsSaving = allowSave, onClose = { finish() })
        }
    }
}

@Composable
private fun ImageViewer(
    urls: List<String>,
    initialIndex: Int,
    allowsSaving: Boolean,
    onClose: () -> Unit,
) {
    val pager = rememberPagerState(initialPage = initialIndex) { urls.size }
    var toast by remember { mutableStateOf<String?>(null) }
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    fun save(url: String) {
        scope.launch { toast = if (saveToGallery(ctx, url)) "사진을 저장했어요" else "저장에 실패했어요" }
    }

    // Q 미만은 갤러리 쓰기에 런타임 권한이 필요하다(매니페스트 maxSdkVersion=32 와 짝).
    var pendingSaveUrl by remember { mutableStateOf<String?>(null) }
    val storagePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val url = pendingSaveUrl
        pendingSaveUrl = null
        if (granted && url != null) save(url) else toast = "사진 접근 권한이 필요해요"
    }

    fun requestSave(url: String) {
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingSaveUrl = url
            storagePermission.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            save(url)
        }
    }

    LaunchedEffect(toast) {
        if (toast != null) { delay(2000); toast = null }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            AsyncImage(
                resolveImageUrl(urls[page]), null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Column(Modifier.fillMaxSize()) {
            // 상단 바 — 닫기 / 카운터 / 저장
            Row(
                Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.4f))
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Close, "닫기", tint = Color.White,
                    modifier = Modifier.size(22.dp).clickable(onClick = onClose),
                )
                Spacer(Modifier.weight(1f))
                if (urls.size > 1) {
                    Text(
                        "${pager.currentPage + 1} / ${urls.size}",
                        fontFamily = customFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                        lineHeight = 18.sp, color = Color.White,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (allowsSaving) {
                    Icon(
                        Icons.Filled.FileDownload, "저장", tint = Color.White,
                        modifier = Modifier.size(22.dp).clickable {
                            urls.getOrNull(pager.currentPage)?.let { requestSave(it) }
                        },
                    )
                } else {
                    Spacer(Modifier.size(22.dp))
                }
            }

            Spacer(Modifier.weight(1f))

            // 하단 도트 인디케이터
            if (urls.size > 1) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    urls.indices.forEach { i ->
                        Box(
                            Modifier.padding(horizontal = 3.dp).size(6.dp).clip(CircleShape)
                                .background(if (i == pager.currentPage) Color.White else Color.White.copy(alpha = 0.35f)),
                        )
                    }
                }
            }
        }

        toast?.let {
            Text(
                it,
                fontFamily = customFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp,
                lineHeight = 17.sp, color = Color.White,
                modifier = Modifier.align(Alignment.Center)
                    .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
            )
        }
    }
}

private fun resolveImageUrl(path: String): String =
    if (path.startsWith("http")) path else BuildConfig.API_BASE_URL + path

/**
 * 갤러리 저장 — Q 이상은 MediaStore RELATIVE_PATH, 그 아래는 Pictures 폴더에 직접 쓰고
 * MediaStore 에 등록한다(매니페스트의 WRITE_EXTERNAL_STORAGE maxSdkVersion=32 와 짝).
 */
private suspend fun saveToGallery(context: Context, url: String): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val req = Request.Builder().url(resolveImageUrl(url)).build()
        val bytes = OkHttpClient().newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@withContext false
            resp.body?.bytes()
        } ?: return@withContext false

        val name = "muyeon_${System.currentTimeMillis()}.jpg"
        val resolver = context.contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Muyeon")
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext false
            resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return@withContext false
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Muyeon",
            )
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, name)
            FileOutputStream(file).use { it.write(bytes) }
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATA, file.absolutePath)
            }
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }
        true
    }.getOrDefault(false)
}
