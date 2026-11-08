package com.muyeon.app.ui.account.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.muyeon.app.ui.common.UploadImagePreparer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** 프로필 사진 — 고른 사진을 가운데 정사각으로 잘라 [SIZE]px JPEG 로(웹 AvatarUploader 의 가운데 정사각 crop 과 같다). */
object AvatarSquare {
    private const val SIZE = 640
    private const val QUALITY = 88

    suspend fun prepare(context: Context, uri: Uri): ByteArray? {
        val bytes = UploadImagePreparer.prepare(context, uri) ?: return null
        return withContext(Dispatchers.Default) {
            runCatching {
                val src = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@runCatching null
                val side = minOf(src.width, src.height)
                val crop = Bitmap.createBitmap(src, (src.width - side) / 2, (src.height - side) / 2, side, side)
                val out = if (side > SIZE) Bitmap.createScaledBitmap(crop, SIZE, SIZE, true) else crop
                ByteArrayOutputStream().use { s -> out.compress(Bitmap.CompressFormat.JPEG, QUALITY, s); s.toByteArray() }
            }.getOrNull()
        }
    }
}
