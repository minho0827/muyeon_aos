package com.muyeon.app.ui.chat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * 채팅 사진 압축 — iOS ChatAttachSheet `compressJPEG(max: 1200, quality: 0.6)` 대응.
 *  긴 변을 [MAX_PIXEL] 이하로 줄이고 JPEG 품질 [QUALITY] 로 인코딩한다.
 *  카메라 원본은 EXIF 회전 정보만 있고 픽셀은 돌아가 있지 않으므로 회전을 적용한다.
 */
object ChatImageCompressor {
    private const val MAX_PIXEL = 1200
    private const val QUALITY = 60

    /** 압축한 JPEG 바이트. 디코딩할 수 없는 형식이면 null(호출부가 원본으로 대체한다). */
    suspend fun compress(context: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver

            // 1) 크기만 먼저 읽어 디코딩 배율을 정한다(큰 원본을 통째로 메모리에 올리지 않기 위해).
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_PIXEL) sample *= 2

            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            } ?: return@runCatching null

            // 2) EXIF 회전 반영.
            val rotation = resolver.openInputStream(uri)?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f

            // 3) 긴 변 기준 축소 + 회전을 한 번에 적용.
            val longSide = maxOf(decoded.width, decoded.height)
            val scale = if (longSide > MAX_PIXEL) MAX_PIXEL.toFloat() / longSide else 1f
            val output = if (scale < 1f || rotation != 0f) {
                val m = Matrix().apply {
                    if (scale < 1f) postScale(scale, scale)
                    if (rotation != 0f) postRotate(rotation)
                }
                Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, m, true)
                    .also { if (it !== decoded) decoded.recycle() }
            } else {
                decoded
            }

            ByteArrayOutputStream().use { out ->
                output.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
                output.recycle()
                out.toByteArray()
            }
        }.getOrNull()
    }
}
