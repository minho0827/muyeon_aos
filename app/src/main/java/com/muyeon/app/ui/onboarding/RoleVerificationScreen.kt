package com.muyeon.app.ui.onboarding

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.material3.OutlinedTextField
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
import java.io.ByteArrayOutputStream

/**
 * 회원유형 인증 서류 제출 — iOS `Onboarding/Role/RoleVerificationView.swift` 이식.
 *  사진 첨부 → /uploads/image 업로드 → 업로드 URL 배열을 웹 콜백으로 전달(제출 API 는 웹이 수행).
 *
 * ⚠️ 서류는 화면이 크게 보여야 심사가 가능하므로 업로드 전 최대 1600px / JPEG 70 으로만 줄인다
 *   (iOS 와 동일 수치). 원본을 그대로 올리면 최근 단말 사진은 10MB 를 넘어 업로드가 자주 실패한다.
 */
private const val MAX_DOCS = 5
private const val MAX_DIMENSION = 1600
private const val JPEG_QUALITY = 70

/** 역할 코드 → 화면 표기. iOS `roleLabel` 과 같은 문구를 쓴다. */
private fun roleLabel(code: String): String = when (code) {
    "ACADEMY" -> "학원·원장"
    "SPACE" -> "공간 보유자"
    "TEAM" -> "공연팀·기획자"
    "TEACHER" -> "강사"
    "DANCER" -> "무용수"
    else -> "회원"
}

/** 역할별 제출 서류 안내. iOS `documentGuide` 와 같은 문구를 쓴다. */
private fun documentGuide(code: String): String = when (code) {
    "TEACHER" -> "강사 경력이나 무용 전공을 확인할 수 있는 서류를 첨부해주세요."
    "DANCER" -> "공연 경력, 소속 또는 무용 전공을 확인할 수 있는 서류를 첨부해주세요."
    "ACADEMY" -> "사업자등록증, 학원등록증 또는 재직증명서를 첨부해주세요."
    "SPACE" -> "사업자등록증 또는 임대차계약서를 첨부해주세요."
    "TEAM" -> "사업자등록증, 고유번호증 또는 공연 증빙을 첨부해주세요."
    else -> "회원유형을 확인할 수 있는 서류를 첨부해주세요."
}

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

/**
 * @param initialImages 재제출 시 기존 제출 서류(미리보기). 삭제·추가 후 다시 제출할 수 있다.
 * @param onDone (역할코드, 서류 URL, 학원명) — 학원명은 ACADEMY 에서만 채워진다.
 */
@Composable
fun RoleVerificationScreen(
    api: RoleVerificationApi,
    role: String,
    onClose: () -> Unit,
    onDone: (String, List<String>, String?) -> Unit,
    initialImages: List<String> = emptyList(),
) {
    // 기존 제출 서류를 그대로 이어받는다 — 재제출인데 빈 화면이면 처음부터 다시 찍어 올려야 한다.
    var images by remember(initialImages) { mutableStateOf(initialImages) }
    var academyName by remember { mutableStateOf("") }
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
            var failed = 0
            uris.forEach { uri ->
                if (images.size >= MAX_DOCS) return@forEach
                val bytes = withContext(Dispatchers.IO) { compressedJpeg(context, uri) }
                val url = bytes?.let { api.uploadImage(it) }
                if (url != null) images = images + url else failed++
            }
            uploading = false
            // 업로드 실패를 삼키면 사용자는 첨부한 줄 알고 빈 서류를 제출한다.
            if (failed > 0) errorMessage = "사진 ${failed}장을 올리지 못했어요. 잠시 후 다시 시도해 주세요."
        }
    }

    Column(Modifier.fillMaxSize().background(MuyeonColors.surface)) {
        QuoteNavBar(title = "${roleLabel(role)} 인증", onBack = onClose)

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
                "${documentGuide(role)}\n최대 ${MAX_DOCS}장까지 올릴 수 있고, 관리자 확인 후 승인되면 푸시로 알려드려요.",
                fontFamily = customFontFamily, fontSize = 14.sp, lineHeight = 20.sp, color = MuyeonColors.textSub,
            )

            // 학원 인증 전용 — 학원명(사업자등록증 상호). 공고·홈 추천·공개 프로필이 이 값을 함께 쓴다.
            if (role == "ACADEMY") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "학원명 (필수)",
                        fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        lineHeight = 18.sp, color = MuyeonColors.textHead,
                    )
                    OutlinedTextField(
                        value = academyName, onValueChange = { academyName = it }, singleLine = true,
                        placeholder = {
                            Text(
                                "예: 루체 발레 학원 (사업자등록증 상호)",
                                fontFamily = customFontFamily, fontSize = 14.sp, color = MuyeonColors.secondary,
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

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

        // 학원 인증은 학원명이 있어야 제출된다 — 비면 프로필·공고·홈 추천에 이름 없이 노출된다.
        val academy = academyName.trim().takeIf { role == "ACADEMY" && it.isNotEmpty() }
        val canSubmit = images.isNotEmpty() && !uploading && (role != "ACADEMY" || academy != null)
        Text(
            if (uploading) "업로드 중…" else "제출하기",
            fontFamily = customFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            lineHeight = 19.sp, color = Color.White, textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 20.dp).padding(top = 8.dp, bottom = 12.dp)
                .fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .background(if (canSubmit) MuyeonColors.primary else Color.Gray.copy(alpha = 0.4f))
                .clickable(enabled = canSubmit) { onDone(role, images, academy) }
                .padding(vertical = 16.dp),
        )
    }

    errorMessage?.let { msg ->
        QuoteDialog("알림", msg, "확인", onConfirm = { errorMessage = null }, onDismiss = { errorMessage = null })
    }
}

/**
 * 업로드용 축소본(최대 1600px, JPEG 70). 실패하면 null — 원본을 그대로 올리지 않는다.
 *  ① 크기만 먼저 읽어 inSampleSize 로 대략 줄이고(메모리 폭발 방지) ② 정확한 배율로 맞춘다.
 */
private fun compressedJpeg(context: Context, uri: Uri): ByteArray? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    val longest = maxOf(bounds.outWidth, bounds.outHeight)
    if (longest <= 0) return null
    var sample = 1
    while (longest / sample > MAX_DIMENSION * 2) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, opts)
    } ?: return null

    val scale = minOf(1f, MAX_DIMENSION.toFloat() / maxOf(decoded.width, decoded.height))
    val scaled = if (scale >= 1f) decoded else Bitmap.createScaledBitmap(
        decoded, (decoded.width * scale).toInt().coerceAtLeast(1),
        (decoded.height * scale).toInt().coerceAtLeast(1), true,
    )
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
    if (scaled !== decoded) scaled.recycle()
    decoded.recycle()
    out.toByteArray()
}.getOrNull()
