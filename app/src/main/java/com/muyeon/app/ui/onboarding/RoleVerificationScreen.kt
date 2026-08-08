package com.muyeon.app.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.muyeon.app.BuildConfig
import com.muyeon.app.theme.customFontFamily
import com.muyeon.app.ui.common.MuyeonColors
import com.muyeon.app.ui.quote.QuoteDialog
import com.muyeon.app.ui.quote.QuoteNavBar
import com.muyeon.app.ui.quote.QuoteUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * 사업자 인증 서류 제출 — iOS `Onboarding/Role/RoleVerificationView.swift` 이식.
 *  사진 첨부 → /uploads/image 업로드 → 업로드 URL 배열을 웹 콜백으로 전달(제출은 웹이 수행).
 */
private const val MAX_DOCS = 5

class RoleVerificationApi(private val token: String?) {
    private val client = OkHttpClient()

    suspend fun uploadImage(bytes: ByteArray): String? = withContext(Dispatchers.IO) {
        runCatching {
            val body = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", "doc.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
                .build()
            val req = Request.Builder().url(BuildConfig.API_BASE_URL + "/api/uploads/image").post(body)
                .apply { if (!token.isNullOrEmpty()) addHeader("Authorization", "Bearer $token") }
                .build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@use null
                JSONObject(res.body?.string().orEmpty()).optString("url").ifEmpty { null }
            }
        }.getOrNull()
    }
}

@Composable
fun RoleVerificationScreen(
    api: RoleVerificationApi,
    role: String,
    onClose: () -> Unit,
    onDone: (String, List<String>) -> Unit,
) {
    var images by remember { mutableStateOf(listOf<String>()) }
    var uploading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = MAX_DOCS),
    ) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        uploading = true
        scope.launch {
            uris.forEach { uri ->
                if (images.size >= MAX_DOCS) return@forEach
                val bytes = withContext(Dispatchers.IO) {
                    runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
                }
                bytes?.let { b -> api.uploadImage(b)?.let { images = images + it } }
            }
            uploading = false
        }
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "사업자 인증", onBack = onClose)

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "인증 서류를 올려주세요",
                fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                lineHeight = 24.sp, color = MuyeonColors.textHead, modifier = Modifier.padding(top = 14.dp),
            )
            Text(
                "사업자등록증 등 확인 가능한 서류를 최대 ${MAX_DOCS}장까지 올릴 수 있어요.\n운영팀 확인 후 유형이 추가돼요.",
                fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp, color = MuyeonColors.textSub,
            )

            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.size(96.dp).clip(RoundedCornerShape(10.dp))
                        .border(1.dp, MuyeonColors.border, RoundedCornerShape(10.dp))
                        .clickable(enabled = !uploading && images.size < MAX_DOCS) {
                            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Add, "서류 추가", tint = MuyeonColors.secondary, modifier = Modifier.size(24.dp))
                }
                images.forEach { url ->
                    Box {
                        AsyncImage(
                            QuoteUi.imageUrl(url), null, contentScale = ContentScale.Crop,
                            modifier = Modifier.size(96.dp).clip(RoundedCornerShape(10.dp)),
                        )
                        Icon(
                            Icons.Filled.Close, "삭제", tint = Color.White,
                            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(18.dp)
                                .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.5f))
                                .clickable { images = images - url },
                        )
                    }
                }
            }
            Text(
                "${images.size}/$MAX_DOCS",
                fontFamily = customFontFamily, fontSize = 12.sp, lineHeight = 14.sp, color = MuyeonColors.secondary,
            )
        }

        val canSubmit = images.isNotEmpty() && !uploading
        Text(
            if (uploading) "업로드 중…" else "제출하기",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp)
                .fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(if (canSubmit) MuyeonColors.primary else Color.Gray.copy(alpha = 0.4f))
                .clickable(enabled = canSubmit) { onDone(role, images) }
                .padding(vertical = 16.dp),
        )
    }

    errorMessage?.let { msg ->
        QuoteDialog("알림", msg, "확인", onConfirm = { errorMessage = null }, onDismiss = { errorMessage = null })
    }
}
