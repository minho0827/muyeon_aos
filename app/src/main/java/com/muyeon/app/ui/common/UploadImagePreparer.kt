package com.muyeon.app.ui.common

import android.content.Context
import android.net.Uri
import com.muyeon.app.ui.chat.ChatImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 사진 올리기 전 줄이기(공고·캐스팅 포스터·이력서·후기·레슨) — 2026-10-30, PACERA UploadImagePreparer 이식.
 *  전에는 고른 사진을 원본 그대로 읽어 올렸다. 요즘 폰 원본(2억 화소 등)은 서버 상한 10MB 를 넘어 실패하고,
 *  통째로 메모리에 올리다 앱이 꺼질 수 있었다. 긴 변 [MAX_PIXEL] · JPEG [QUALITY] 로 줄이고 사진 방향도 바로 세운다.
 *  줄일 수 없는 형식이면 원본이 서버 상한 안일 때만 그대로 보낸다(넘으면 null — 호출부가 실패 안내).
 */
object UploadImagePreparer {
    private const val MAX_PIXEL = 2048
    private const val QUALITY = 85
    private const val SERVER_LIMIT_BYTES = 10L * 1024 * 1024

    suspend fun prepare(context: Context, uri: Uri): ByteArray? =
        ChatImageCompressor.compress(context, uri, MAX_PIXEL, QUALITY) ?: readIfSmall(context, uri)

    private suspend fun readIfSmall(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val size = context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
            if (size < 0 || size > SERVER_LIMIT_BYTES) return@runCatching null
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
    }
}
